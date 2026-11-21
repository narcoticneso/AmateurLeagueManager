package com.amateurleague;

import static org.springframework.http.HttpStatus.*;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class LeagueController {

    public record LeagueState(List<LeagueService.Team> teams, List<LeagueService.Player> players, List<LeagueService.Game> games, List<LeagueService.Standing> standings, List<LeagueService.ScoreSubmission> scoreSubmissions) {}

    public record SessionInfo(String username, String displayName, boolean commissioner, List<LeagueService.Membership> memberships, String csrf) {}

    public record AccountEmailInput(String email) {}

    public record JoinRequestInput(String teamId) {}

    public record JoinDecisionInput(Boolean approve, @Schema(types = { "string", "null" }) String position, @Schema(types = { "integer", "null" }) Integer number) {}

    public record RegistrationInput(String email, String displayName, String password) {}

    public record VerificationInput(String email, String code) {}

    public record RegistrationResponse(String message) {}

    public record VerifiedAccount(String email, String displayName) {}

    public record ScoreInput(Integer homeScore, Integer awayScore) {}

    public record ScoreResponseInput(Boolean confirm, @Schema(types = { "integer", "null" }) Integer disputedHomeScore, @Schema(types = { "integer", "null" }) Integer disputedAwayScore) {}

    public record ErrorMessage(String message) {}

    private final LeagueService league;
    private final JoinRequestService joinRequests;
    private final EmailVerificationService verification;
    private final SecurityContextRepository securityContextRepository;

    public LeagueController(LeagueService league, JoinRequestService joinRequests, EmailVerificationService verification, SecurityContextRepository securityContextRepository) {
        this.league = league;
        this.joinRequests = joinRequests;
        this.verification = verification;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@RequestBody RegistrationInput input) {
        verification.requestCode(input.email(), input.displayName(), input.password());

        return ResponseEntity.accepted().body(new RegistrationResponse("Verification code requested."));
    }

    @PostMapping("/verify")
    public VerifiedAccount verify(@RequestBody VerificationInput input, HttpServletRequest request, HttpServletResponse response) {
        EmailVerificationService.VerifiedUser user = verification.verifyCode(input.email(), input.code());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(user.email(), null, List.of()));

        request.changeSessionId();
        securityContextRepository.saveContext(context, request, response);

        return new VerifiedAccount(user.email(), user.displayName());
    }

    @GetMapping("/state")
    public LeagueState state() {
        return new LeagueState(league.teams(), league.players(), league.games(), league.standings(), league.scoreSubmissions());
    }

    @GetMapping("/join-requests/teams")
    public List<LeagueService.Team> availableJoinTeams(Principal user) {
        return joinRequests.availableTeams(user.getName());
    }

    @GetMapping("/join-requests")
    public List<JoinRequestService.JoinRequest> joinRequestHistory(Principal user) {
        return joinRequests.history(user.getName());
    }

    @PostMapping("/join-requests")
    @ResponseStatus(CREATED)
    public void requestToJoin(@RequestBody JoinRequestInput input, Principal user) {
        joinRequests.submit(user.getName(), input.teamId());
    }

    @GetMapping("/teams/{teamId}/join-requests")
    public List<JoinRequestService.JoinRequest> teamJoinRequests(@PathVariable String teamId, Principal user) {
        return joinRequests.pendingForTeam(user.getName(), teamId);
    }

    @PostMapping("/join-requests/{requestId}/decision")
    public void decideJoinRequest(@PathVariable String requestId, @RequestBody JoinDecisionInput input, Principal user) {
        joinRequests.decide(user.getName(), requestId, Boolean.TRUE.equals(input.approve()), input.position(), input.number());
    }

    @GetMapping("/session")
    public SessionInfo session(Principal user, @Parameter(hidden = true) CsrfToken csrf) {
        if (user == null) {
            return new SessionInfo("", "", false, List.of(), csrf.getToken());
        }
        String email = user.getName();
        return new SessionInfo(email, league.displayName(email), league.isCommissioner(email), league.memberships(email), csrf.getToken());
    }

    @PostMapping("/teams")
    public void createTeam(@RequestBody LeagueService.TeamInput input, Principal user) {
        commissioner(user);
        league.createTeam(input);
    }

    @PutMapping("/teams/{teamId}")
    public void editTeam(@PathVariable String teamId, @RequestBody LeagueService.TeamInput input, Principal user) {
        commissioner(user);
        league.updateTeam(teamId, input);
    }

    @PostMapping("/teams/{teamId}/players")
    public void addPlayer(@PathVariable String teamId, @RequestBody LeagueService.PlayerInput input, Principal user) {
        rosterPermission(teamId, user);
        league.addPlayer(teamId, input);
    }

    @PostMapping("/teams/{teamId}/players/{playerId}/account")
    public void linkPlayerAccount(@PathVariable String teamId, @PathVariable String playerId, @RequestBody AccountEmailInput input, Principal user) {
        rosterPermission(teamId, user);
        league.linkPlayerAccount(teamId, playerId, input.email());
    }

    @PutMapping("/teams/{teamId}/manager")
    public void assignTeamManager(@PathVariable String teamId, @RequestBody AccountEmailInput input, Principal user) {
        commissioner(user);
        league.assignTeamManager(teamId, input.email());
    }

    @DeleteMapping("/teams/{teamId}/players/{playerId}")
    public void removePlayer(@PathVariable String teamId, @PathVariable String playerId, Principal user) {
        rosterPermission(teamId, user);
        league.removePlayer(teamId, playerId);
    }

    @PostMapping("/games")
    public void createGame(@RequestBody LeagueService.GameInput input, Principal user) {
        commissioner(user);
        league.saveGame(null, input);
    }

    @PostMapping("/games/{gameId}/scores")
    public void submitScore(@PathVariable String gameId, @RequestBody ScoreInput input, Principal user) {
        league.submitScore(gameId, user.getName(), managedTeam(user, gameId), input.homeScore(), input.awayScore());
    }

    @PostMapping("/scores/{submissionId}/respond")
    public void respondToScore(@PathVariable String submissionId, @RequestBody ScoreResponseInput input, Principal user) {
        String gameId = league.gameIdForSubmission(submissionId);
        league.respondToScore(submissionId, user.getName(), managedTeam(user, gameId), Boolean.TRUE.equals(input.confirm()), input.disputedHomeScore(), input.disputedAwayScore());
    }

    @PostMapping("/scores/{submissionId}/resolve")
    public void resolveDisputedScore(@PathVariable String submissionId, @RequestBody ScoreInput input, Principal user) {
        commissioner(user);
        league.resolveDisputedScore(submissionId, user.getName(), input.homeScore(), input.awayScore());
    }

    @PutMapping("/games/{id}")
    public void editGame(@PathVariable String id, @RequestBody LeagueService.GameInput input, Principal user) {
        commissioner(user);
        league.saveGame(id, input);
    }

    private void commissioner(Principal user) {
        if (!league.isCommissioner(user.getName())) {
            throw new ResponseStatusException(FORBIDDEN, "Only the commissioner can do that.");
        }
    }

    private String managedTeam(Principal user, String gameId) {
        LeagueService.Game game = league.requireGame(gameId);
        if (league.isManagerOfTeam(user.getName(), game.homeId())) {
            return game.homeId();
        }
        if (league.isManagerOfTeam(user.getName(), game.awayId())) {
            return game.awayId();
        }
        throw new ResponseStatusException(FORBIDDEN, "You do not manage a team in this game.");
    }

    private void rosterPermission(String teamId, Principal user) {
        if (!league.isCommissioner(user.getName()) && !league.isManagerOfTeam(user.getName(), teamId)) {
            throw new ResponseStatusException(FORBIDDEN, "You can only edit a team you manage.");
        }
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
