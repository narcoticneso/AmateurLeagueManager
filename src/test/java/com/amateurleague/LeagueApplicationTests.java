package com.amateurleague;

import java.net.*;
import java.net.http.*;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:league-tests;DB_CLOSE_DELAY=-1", "server.address=127.0.0.1"
})
class LeagueApplicationTests {
    @Value("${local.server.port}") int port;
    @Autowired LeagueService league;
    @Autowired JdbcTemplate db;
    @Autowired DemoData demo;
    private final JsonMapper json = new JsonMapper();
    private HttpClient client;
    private String csrf;

    @BeforeEach void reset() {
        db.update("DELETE FROM games"); db.update("DELETE FROM players"); db.update("DELETE FROM teams"); demo.run();
        client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    }
    HttpResponse<String> request(String method, String path, String body, String contentType, boolean token) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
        if (token) builder.header("X-CSRF-TOKEN", csrf);
        if (contentType != null) builder.header("Content-Type", contentType);
        return client.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    JsonNode getSession() throws Exception {
        JsonNode result = json.readTree(request("GET", "/api/session", null, null, false).body());
        csrf = result.get("csrf").asText(); return result;
    }
    void login(String name) throws Exception {
        getSession();
        assertEquals(204, request("POST", "/api/login", "username=" + name + "&password=neighborhood-demo", "application/x-www-form-urlencoded", true).statusCode());
        assertEquals(name, getSession().get("username").asText());
    }
    HttpResponse<String> post(String path, Object input) throws Exception {
        return request("POST", "/api" + path, json.writeValueAsString(input), "application/json", true);
    }
    LeagueService.GameInput game(String home, String away, String stage, String state, Integer hs, Integer as) {
        return new LeagueService.GameInput(home, away, "2026-11-21T18:00", "Test field", 8, stage, state, hs, as);
    }

    @Test void publicCanReadButCannotWriteAndCsrfIsRequired() throws Exception {
        assertEquals(200, request("GET", "/", null, null, false).statusCode());
        assertEquals(200, request("GET", "/api/state", null, null, false).statusCode());
        getSession();
        assertEquals(401, post("/games", game("wolves","bears","REGULAR","SCHEDULED",null,null)).statusCode());
        login("rudra");
        assertEquals(403, request("POST", "/api/games", "{}", "application/json", false).statusCode());
    }
    @Test void allFiveDevelopersCanSignInAndSwitchViews() throws Exception {
        for (String name : new String[]{"rudra","ryan","lucas","nebojsa","kiara"}) {
            login(name);
            assertEquals("COMMISSIONER", getSession().get("view").asText());
            for (String view : new String[]{"MANAGER","PLAYER","COMMISSIONER"}) {
                assertEquals(200, post("/view", Map.of("view",view)).statusCode());
                assertEquals(view, getSession().get("view").asText());
            }
            assertEquals(204, request("POST", "/api/logout", null, null, true).statusCode());
        }
    }
    @Test void rolePermissionsAreEnforcedOnServer() throws Exception {
        login("rudra");
        assertEquals(200, post("/view", Map.of("view","MANAGER")).statusCode());
        assertEquals(403, post("/games",game("wolves","bears","REGULAR","SCHEDULED",null,null)).statusCode());
        assertEquals(403, post("/teams/bears/players",Map.of("name","Test Player","position","QB","number",99)).statusCode());
        assertEquals(200, post("/teams/wolves/players",Map.of("name","Test Player","position","QB","number",99)).statusCode());
        assertEquals(200, post("/view",Map.of("view","PLAYER")).statusCode());
        assertEquals(403, post("/teams/wolves/players",Map.of("name","Another Player","position","QB","number",98)).statusCode());
        assertEquals(403, post("/teams",Map.of("name","Test Team","abbreviation","TT","color","#123456","manager","Test")).statusCode());
    }
    @Test void commissionerCanCreateTeamsScheduleAndCorrectResults() throws Exception {
        login("lucas");
        assertEquals(200, post("/teams",Map.of("name","Test Owls","abbreviation","TO","color","#123456","manager","Test")).statusCode());
        assertEquals(7, league.teams().size());
        assertEquals(200, post("/games",game("wolves","bears","REGULAR","SCHEDULED",null,null)).statusCode());
        var g = league.games().getLast();
        assertNull(g.homeScore());
        assertEquals(200, request("PUT","/api/games/"+g.id(),json.writeValueAsString(game("wolves","bears","REGULAR","FINAL",0,0)),"application/json",true).statusCode());
        assertEquals(1, league.standings().stream().filter(s -> s.teamId().equals("wolves")).findFirst().orElseThrow().ties());
    }
    @Test void standingsExcludeTournamentAndUnplayedGamesAndRecalculateCorrections() {
        db.update("DELETE FROM games");
        league.saveGame(null,game("wolves","bears","REGULAR","FINAL",14,7));
        String id = league.games().getFirst().id();
        league.saveGame(null,game("wolves","hawks","REGULAR","FINAL",0,0));
        league.saveGame(null,game("wolves","tigers","SEMIFINAL","FINAL",21,7));
        league.saveGame(null,game("wolves","falcons","REGULAR","POSTPONED",100,0));
        league.saveGame(null,game("wolves","mustangs","REGULAR","CANCELLED",100,0));
        var s = league.standings().stream().filter(row -> row.teamId().equals("wolves")).findFirst().orElseThrow();
        assertEquals(2,s.played()); assertEquals(1,s.wins()); assertEquals(1,s.ties()); assertEquals(.75,s.percentage()); assertEquals(14,s.pointsFor());
        league.saveGame(id,game("wolves","bears","REGULAR","FINAL",7,14));
        s = league.standings().stream().filter(row -> row.teamId().equals("wolves")).findFirst().orElseThrow();
        assertEquals(1,s.losses()); assertEquals(.25,s.percentage()); assertEquals(7,s.pointsFor());
        assertTrue(league.games().stream().filter(g -> !g.status().equals("FINAL")).allMatch(g -> g.homeScore() == null));
    }
    @Test void invalidGamesAndDuplicateJerseyNumbersAreRejected() {
        assertThrows(ResponseStatusException.class, () -> league.saveGame(null,game("wolves","wolves","REGULAR","FINAL",7,0)));
        assertThrows(ResponseStatusException.class, () -> league.saveGame(null,game("wolves","bears","REGULAR","FINAL",null,0)));
        assertThrows(ResponseStatusException.class, () -> league.saveGame(null,game("wolves","bears","REGULAR","FINAL",-1,0)));
        assertThrows(ResponseStatusException.class, () -> league.saveGame(null,game("wolves","bears","SEMIFINAL","FINAL",7,7)));
        assertThrows(ResponseStatusException.class, () -> league.addPlayer("wolves",new LeagueService.PlayerInput("New QB","QB",7)));
        assertThrows(ResponseStatusException.class, () -> league.createTeam(new LeagueService.TeamInput("lakewood wolves","LW","#123456","Rudra")));
    }
    @Test void tournamentRequiresDistinctSemifinalTeamsAndActualWinners() {
        assertThrows(ResponseStatusException.class, () -> league.saveGame(null,game("wolves","bears","CHAMPIONSHIP","SCHEDULED",null,null)));
        league.saveGame(null,game("wolves","bears","SEMIFINAL","FINAL",21,7));
        assertThrows(ResponseStatusException.class, () -> league.saveGame(null,game("wolves","hawks","SEMIFINAL","FINAL",21,7)));
        league.saveGame(null,game("hawks","tigers","SEMIFINAL","FINAL",14,7));
        var finalInput = new LeagueService.GameInput("wolves","hawks","2026-11-28T18:00","Test field",9,"CHAMPIONSHIP","FINAL",28,21);
        league.saveGame(null,finalInput);
        assertThrows(ResponseStatusException.class, () -> league.saveGame(null,finalInput));
        assertEquals(1,league.games().stream().filter(g -> g.phase().equals("CHAMPIONSHIP")).count());
    }
}
