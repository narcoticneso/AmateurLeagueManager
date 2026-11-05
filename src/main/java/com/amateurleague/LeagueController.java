package com.amateurleague;

import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api")
public class LeagueController {
    private final LeagueService league;
    private static final Map<String, String> TEAMS = Map.of("rudra", "wolves", "ryan", "bears", "lucas", "hawks", "nebojsa", "tigers", "kiara", "mustangs");
    public LeagueController(LeagueService league) { this.league = league; }

    @GetMapping("/state")
    public Map<String, Object> state() {
        return Map.of(
            "teams", league.teams(),
            "players", league.players(),
            "games", league.games(),
            "standings", league.standings(),
            "scoreSubmissions", league.scoreSubmissions()
        );
    }
    @GetMapping("/session")
    public Map<String, Object> session(Principal user, HttpSession session, CsrfToken csrf) {
        return Map.of("username", user == null ? "" : user.getName(), "view", user == null ? "GUEST" : view(session),
            "teamId", user == null ? "" : TEAMS.getOrDefault(user.getName(), ""), "csrf", csrf.getToken());
    }
    @PostMapping("/view")
    public void changeView(@RequestBody Map<String, String> input, HttpSession session) {
        String mode = input.get("view");
        if (!Set.of("COMMISSIONER", "MANAGER", "PLAYER").contains(Objects.toString(mode, "")))
            throw new ResponseStatusException(BAD_REQUEST, "Choose commissioner, manager, or player.");
        session.setAttribute("view", mode);
    }
    @PostMapping("/teams")
    public void createTeam(@RequestBody LeagueService.TeamInput input, HttpSession session) {
        commissioner(session); league.createTeam(input);
    }
    @PostMapping("/teams/{teamId}/players")
    public void addPlayer(@PathVariable String teamId, @RequestBody LeagueService.PlayerInput input, HttpSession session, Principal user) {
        rosterPermission(teamId, session, user); league.addPlayer(teamId, input);
    }
    @DeleteMapping("/teams/{teamId}/players/{playerId}")
    public void removePlayer(@PathVariable String teamId, @PathVariable String playerId, HttpSession session, Principal user) {
        rosterPermission(teamId, session, user); league.removePlayer(teamId, playerId);
    }
    @PostMapping("/games")
    public void createGame(@RequestBody LeagueService.GameInput input, HttpSession session) {
        commissioner(session); league.saveGame(null, input);
    }
    @PostMapping("/games/{gameId}/scores")
    public void submitScore(
            @PathVariable String gameId,
            @RequestBody Map<String, Integer> input,
            HttpSession session,
            Principal user) {

        manager(session);

        String teamId = TEAMS.get(user.getName());
        if (teamId == null) {
            throw new ResponseStatusException(FORBIDDEN, "You do not manage a team.");
        }

        league.submitScore(
            gameId,
            user.getName(),
            teamId,
            input.get("homeScore"),
            input.get("awayScore")
        );
    }
    @PostMapping("/scores/{submissionId}/respond")
    public void respondToScore(
            @PathVariable String submissionId,
            @RequestBody Map<String, Object> input,
            HttpSession session,
            Principal user) {

        manager(session);

        String teamId = TEAMS.get(user.getName());
        if (teamId == null) {
            throw new ResponseStatusException(FORBIDDEN, "You do not manage a team.");
        }

        boolean confirm = Boolean.TRUE.equals(input.get("confirm"));

        Integer disputedHomeScore = input.get("disputedHomeScore") instanceof Number
                ? ((Number) input.get("disputedHomeScore")).intValue()
                : null;

        Integer disputedAwayScore = input.get("disputedAwayScore") instanceof Number
                ? ((Number) input.get("disputedAwayScore")).intValue()
                : null;

        league.respondToScore(
            submissionId,
            user.getName(),
            teamId,
            confirm,
            disputedHomeScore,
            disputedAwayScore
        );
    }
    @PostMapping("/scores/{submissionId}/resolve")
    public void resolveDisputedScore(
            @PathVariable String submissionId,
            @RequestBody Map<String, Integer> input,
            HttpSession session,
            Principal user) {

        commissioner(session);

        league.resolveDisputedScore(
            submissionId,
            user.getName(),
            input.get("homeScore"),
            input.get("awayScore")
        );
    }
    @PutMapping("/games/{id}")
    public void editGame(@PathVariable String id, @RequestBody LeagueService.GameInput input, HttpSession session) {
        commissioner(session); league.saveGame(id, input);
    }
    private String view(HttpSession session) { return Objects.toString(session.getAttribute("view"), "COMMISSIONER"); }
    private void commissioner(HttpSession session) {
        if (!view(session).equals("COMMISSIONER")) throw new ResponseStatusException(FORBIDDEN, "Switch to commissioner view to manage games and teams.");
    }
    private void manager(HttpSession session) {
        if (!view(session).equals("MANAGER")) throw new ResponseStatusException(FORBIDDEN, "Switch to manager view to submit scores.");
    }
    private void rosterPermission(String teamId, HttpSession session, Principal user) {
        if (view(session).equals("COMMISSIONER")) return;
        if (view(session).equals("MANAGER") && teamId.equals(TEAMS.get(user.getName()))) return;
        throw new ResponseStatusException(FORBIDDEN, "You can only edit your own team's roster in manager view.");
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> validation(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("message", Objects.toString(error.getReason(), "Request failed.")));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> conflict(DataIntegrityViolationException error) {
        return ResponseEntity.status(CONFLICT).body(Map.of("message", "That name or jersey number is already in use. Refresh and try again."));
    }
}
