package com.amateurleague;

import static java.util.Map.entry;
import static org.springframework.http.HttpStatus.*;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class LeagueController {

    public record LeagueState(List<LeagueService.Team> teams, List<LeagueService.Player> players, List<LeagueService.Game> games, List<LeagueService.Standing> standings, List<LeagueService.ScoreSubmission> scoreSubmissions) {}

    public record SessionInfo(String username, String view, String teamId, String csrf) {}

    public record ViewInput(String view) {}

    public record ScoreInput(Integer homeScore, Integer awayScore) {}

    public record ScoreResponseInput(Boolean confirm, @Schema(types = { "integer", "null" }) Integer disputedHomeScore, @Schema(types = { "integer", "null" }) Integer disputedAwayScore) {}

    public record ErrorMessage(String message) {}

    private final LeagueService league;
    private static final Map<String, String> TEAMS = Map.ofEntries(entry("rudra", "wolves"), entry("ryan", "bears"), entry("lucas", "hawks"), entry("nebojsa", "tigers"), entry("kiara", "mustangs"));

    public LeagueController(LeagueService league) {
        this.league = league;
    }

    @GetMapping("/state")
    public LeagueState state() {
        return new LeagueState(league.teams(), league.players(), league.games(), league.standings(), league.scoreSubmissions());
    }

    @GetMapping("/session")
    public SessionInfo session(Principal user, HttpSession session, @Parameter(hidden = true) CsrfToken csrf) {
        return new SessionInfo(user == null ? "" : user.getName(), user == null ? "GUEST" : view(session), user == null ? "" : TEAMS.getOrDefault(user.getName(), ""), csrf.getToken());
    }

    @PostMapping("/view")
    public void changeView(@RequestBody ViewInput input, HttpSession session) {
        String mode = input.view();
        if (!Set.of("COMMISSIONER", "MANAGER", "PLAYER").contains(Objects.toString(mode, ""))) {
            throw new ResponseStatusException(BAD_REQUEST, "Choose commissioner, manager, or player.");
        }
        session.setAttribute("view", mode);
    }

    @PostMapping("/teams")
    public void createTeam(@RequestBody LeagueService.TeamInput input, HttpSession session) {
        commissioner(session);
        league.createTeam(input);
    }

    @PutMapping("/teams/{teamId}")
    public void editTeam(@PathVariable String teamId, @RequestBody LeagueService.TeamInput input, HttpSession session) {
        commissioner(session);
        league.updateTeam(teamId, input);
    }

    @PostMapping("/teams/{teamId}/players")
    public void addPlayer(@PathVariable String teamId, @RequestBody LeagueService.PlayerInput input, HttpSession session, Principal user) {
        rosterPermission(teamId, session, user);
        league.addPlayer(teamId, input);
    }

    @DeleteMapping("/teams/{teamId}/players/{playerId}")
    public void removePlayer(@PathVariable String teamId, @PathVariable String playerId, HttpSession session, Principal user) {
        rosterPermission(teamId, session, user);
        league.removePlayer(teamId, playerId);
    }

    @PostMapping("/games")
    public void createGame(@RequestBody LeagueService.GameInput input, HttpSession session) {
        commissioner(session);
        league.saveGame(null, input);
    }

    @PostMapping("/games/{gameId}/scores")
    public void submitScore(@PathVariable String gameId, @RequestBody ScoreInput input, HttpSession session, Principal user) {
        league.submitScore(gameId, user.getName(), managedTeam(session, user), input.homeScore(), input.awayScore());
    }

    @PostMapping("/scores/{submissionId}/respond")
    public void respondToScore(@PathVariable String submissionId, @RequestBody ScoreResponseInput input, HttpSession session, Principal user) {
        league.respondToScore(submissionId, user.getName(), managedTeam(session, user), Boolean.TRUE.equals(input.confirm()), input.disputedHomeScore(), input.disputedAwayScore());
    }

    @PostMapping("/scores/{submissionId}/resolve")
    public void resolveDisputedScore(@PathVariable String submissionId, @RequestBody ScoreInput input, HttpSession session, Principal user) {
        commissioner(session);
        league.resolveDisputedScore(submissionId, user.getName(), input.homeScore(), input.awayScore());
    }

    @PutMapping("/games/{id}")
    public void editGame(@PathVariable String id, @RequestBody LeagueService.GameInput input, HttpSession session) {
        commissioner(session);
        league.saveGame(id, input);
    }

    private String view(HttpSession session) {
        return Objects.toString(session.getAttribute("view"), "COMMISSIONER");
    }

    private void commissioner(HttpSession session) {
        if (!view(session).equals("COMMISSIONER")) {
            throw new ResponseStatusException(FORBIDDEN, "Switch to commissioner view to manage games and teams.");
        }
    }

    private String managedTeam(HttpSession session, Principal user) {
        if (!view(session).equals("MANAGER")) {
            throw new ResponseStatusException(FORBIDDEN, "Switch to manager view to submit scores.");
        }
        String teamId = TEAMS.get(user.getName());
        if (teamId == null) {
            throw new ResponseStatusException(FORBIDDEN, "You do not manage a team.");
        }
        return teamId;
    }

    private void rosterPermission(String teamId, HttpSession session, Principal user) {
        if (view(session).equals("COMMISSIONER")) {
            return;
        }
        if (view(session).equals("MANAGER") && teamId.equals(TEAMS.get(user.getName()))) {
            return;
        }
        throw new ResponseStatusException(FORBIDDEN, "You can only edit your own team's roster in manager view.");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorMessage> validation(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(new ErrorMessage(Objects.toString(error.getReason(), "Request failed.")));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorMessage> conflict(DataIntegrityViolationException error) {
        return ResponseEntity.status(CONFLICT).body(new ErrorMessage("That name or jersey number is already in use. Refresh and try again."));
    }
}
