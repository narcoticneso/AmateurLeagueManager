package com.amateurleague;

import java.time.LocalDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
public class LeagueService {
    public record Team(String id, String name, String abbreviation, String color, String manager) {}
    public record Player(String id, String teamId, String name, String position, int number) {}
    public record Game(String id, String homeId, String awayId, String kickoff, String location,
                       int week, String phase, String status, Integer homeScore, Integer awayScore) {}
    public record Standing(String teamId, int played, int wins, int losses, int ties, int pointsFor,
                           int pointsAgainst, double percentage) {}
    public record GameInput(String homeId, String awayId, String kickoff, String location, Integer week,
                            String phase, String status, Integer homeScore, Integer awayScore) {}
    public record TeamInput(String name, String abbreviation, String color, String manager) {}
    public record PlayerInput(String name, String position, Integer number) {}
    private final JdbcTemplate db;
    public LeagueService(JdbcTemplate db) { this.db = db; }

    public List<Team> teams() {
        return db.query("SELECT * FROM teams ORDER BY name", (r, n) -> new Team(r.getString("id"), r.getString("name"), r.getString("abbreviation"), r.getString("color"), r.getString("manager")));
    }
    public List<Player> players() {
        return db.query("SELECT * FROM players ORDER BY jersey_number, name", (r, n) -> new Player(r.getString("id"), r.getString("team_id"), r.getString("name"), r.getString("position"), r.getInt("jersey_number")));
    }
    public List<Game> games() {
        return db.query("SELECT * FROM games ORDER BY kickoff, id", (r, n) -> new Game(r.getString("id"), r.getString("home_id"), r.getString("away_id"), r.getString("kickoff"), r.getString("location"), r.getInt("week"), r.getString("phase"), r.getString("status"), r.getObject("home_score", Integer.class), r.getObject("away_score", Integer.class)));
    }
    public List<Standing> standings() {
        List<Game> games = games();
        List<Standing> result = new ArrayList<>();
        for (Team team : teams()) {
            int wins = 0, losses = 0, ties = 0, pf = 0, pa = 0;
            for (Game game : games) {
                if (!game.phase().equals("REGULAR") || !game.status().equals("FINAL")) continue;
                if (!game.homeId().equals(team.id()) && !game.awayId().equals(team.id())) continue;
                int own = game.homeId().equals(team.id()) ? game.homeScore() : game.awayScore();
                int other = game.homeId().equals(team.id()) ? game.awayScore() : game.homeScore();
                pf += own; pa += other;
                if (own > other) wins++; else if (own < other) losses++; else ties++;
            }
            int played = wins + losses + ties;
            result.add(new Standing(team.id(), played, wins, losses, ties, pf, pa,
                played == 0 ? 0 : (wins + ties * 0.5) / played));
        }
        result.sort(Comparator.comparingDouble(Standing::percentage).reversed()
            .thenComparing(Comparator.comparingInt((Standing s) -> s.pointsFor() - s.pointsAgainst()).reversed())
            .thenComparing(Standing::teamId));
        return result;
    }
    public Team requireTeam(String id) {
        return teams().stream().filter(t -> t.id().equals(id)).findFirst()
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Team not found."));
    }
    @Transactional
    public void createTeam(TeamInput input) {
        String name = text(input.name(), "Team name", 60);
        String abbreviation = text(input.abbreviation(), "Abbreviation", 3).toUpperCase(Locale.ROOT);
        if (!abbreviation.matches("[A-Z]{2,3}")) fail("Use two or three letters for the abbreviation.");
        if (input.color() == null || !input.color().matches("#[0-9a-fA-F]{6}")) fail("Choose a valid team color.");
        if (teams().stream().anyMatch(t -> t.name().equalsIgnoreCase(name))) fail("A team with that name already exists.");
        db.update("INSERT INTO teams VALUES (?, ?, ?, ?, ?)", UUID.randomUUID().toString(), name, abbreviation, input.color(), text(input.manager(), "Manager", 60));
    }
    @Transactional
    public void addPlayer(String teamId, PlayerInput input) {
        requireTeam(teamId);
        String name = text(input.name(), "Player name", 60);
        if (!Set.of("QB", "RB", "WR", "TE", "OL", "DL", "LB", "CB", "S", "K").contains(Objects.toString(input.position(), ""))) fail("Choose a valid position.");
        if (input.number() == null || input.number() < 0 || input.number() > 99) fail("Jersey number must be between 0 and 99.");
        if (players().stream().anyMatch(p -> p.teamId().equals(teamId) && p.number() == input.number())) fail("That jersey number is already on this roster.");
        db.update("INSERT INTO players VALUES (?, ?, ?, ?, ?)", UUID.randomUUID().toString(), teamId, name, input.position(), input.number());
    }
    public void removePlayer(String teamId, String playerId) {
        if (db.update("DELETE FROM players WHERE id = ? AND team_id = ?", playerId, teamId) == 0)
            throw new ResponseStatusException(NOT_FOUND, "Player not found.");
    }
    @Transactional
    public void saveGame(String id, GameInput input) {
        requireTeam(input.homeId()); requireTeam(input.awayId());
        if (input.homeId().equals(input.awayId())) fail("Choose two different teams.");
        String kickoff = text(input.kickoff(), "Kickoff", 30);
        try { LocalDateTime.parse(kickoff); } catch (Exception e) { fail("Enter a valid date and time."); }
        String location = text(input.location(), "Location", 100);
        if (input.week() == null || input.week() < 1 || input.week() > 30) fail("Week must be between 1 and 30.");
        if (!Set.of("REGULAR", "SEMIFINAL", "CHAMPIONSHIP").contains(Objects.toString(input.phase(), ""))) fail("Choose a valid competition stage.");
        if (!Set.of("SCHEDULED", "FINAL", "POSTPONED", "CANCELLED").contains(Objects.toString(input.status(), ""))) fail("Choose a valid game status.");
        Integer home = null, away = null;
        if (input.status().equals("FINAL")) {
            home = input.homeScore(); away = input.awayScore();
            if (home == null || away == null || home < 0 || away < 0 || home > 200 || away > 200) fail("Final scores must be whole numbers between 0 and 200.");
            if (!input.phase().equals("REGULAR") && home.equals(away)) fail("Tournament games must have a winner.");
        }
        List<Game> proposed = new ArrayList<>(games().stream().filter(g -> !g.id().equals(id)).toList());
        proposed.add(new Game(id == null ? "new" : id, input.homeId(), input.awayId(), kickoff, location, input.week(), input.phase(), input.status(), home, away));
        validateTournament(proposed);
        if (id == null) db.update("INSERT INTO games VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", UUID.randomUUID().toString(), input.homeId(), input.awayId(), kickoff, location, input.week(), input.phase(), input.status(), home, away);
        else if (db.update("UPDATE games SET home_id=?, away_id=?, kickoff=?, location=?, week=?, phase=?, status=?, home_score=?, away_score=? WHERE id=?", input.homeId(), input.awayId(), kickoff, location, input.week(), input.phase(), input.status(), home, away, id) == 0)
            throw new ResponseStatusException(NOT_FOUND, "Game not found.");
    }
    private void validateTournament(List<Game> proposed) {
        List<Game> semifinals = proposed.stream().filter(g -> g.phase().equals("SEMIFINAL") && !g.status().equals("CANCELLED")).toList();
        List<Game> championships = proposed.stream().filter(g -> g.phase().equals("CHAMPIONSHIP") && !g.status().equals("CANCELLED")).toList();
        if (semifinals.size() > 2 || championships.size() > 1) fail("The cup has two semifinals and one championship. Edit an existing game instead.");
        Set<String> entrants = new HashSet<>();
        for (Game g : semifinals) if (!entrants.add(g.homeId()) || !entrants.add(g.awayId())) fail("A team can only appear in one semifinal.");
        if (!championships.isEmpty()) {
            if (semifinals.size() != 2 || semifinals.stream().anyMatch(g -> !g.status().equals("FINAL"))) fail("Finalize both semifinals before scheduling the championship.");
            Set<String> winners = new HashSet<>();
            for (Game g : semifinals) winners.add(g.homeScore() > g.awayScore() ? g.homeId() : g.awayId());
            Game finalGame = championships.getFirst();
            if (!winners.equals(Set.of(finalGame.homeId(), finalGame.awayId()))) fail("The championship must feature the two semifinal winners. Cancel the championship before changing a semifinal winner.");
            if (semifinals.stream().anyMatch(g -> !LocalDateTime.parse(g.kickoff()).isBefore(LocalDateTime.parse(finalGame.kickoff())))) fail("The championship must start after both semifinals.");
        }
    }
    private String text(String value, String label, int max) {
        if (value == null || value.isBlank() || value.strip().length() > max) fail(label + " is required (maximum " + max + " characters).");
        return value.strip();
    }
    private void fail(String message) { throw new ResponseStatusException(BAD_REQUEST, message); }
}
