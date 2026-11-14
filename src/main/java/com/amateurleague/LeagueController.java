package com.amateurleague;

import static java.util.Map.entry;
import static org.springframework.http.HttpStatus.*;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

    public record SessionInfo(String username, String accountRole, String view, String teamId, List<LeagueService.Membership> memberships, String csrf) {}

    public record ViewInput(String view) {}

    public record TeamSelectionInput(String teamId) {}

    public record AccountEmailInput(String email) {}

    public record RegistrationInput(String email, String displayName, String password) {}

    public record VerificationInput(String email, String code) {}

    public record RegistrationResponse(String message) {}

    public record VerifiedAccount(String email, String displayName) {}

    public record ScoreInput(Integer homeScore, Integer awayScore) {}

    public record ScoreResponseInput(Boolean confirm, @Schema(types = { "integer", "null" }) Integer disputedHomeScore, @Schema(types = { "integer", "null" }) Integer disputedAwayScore) {}

    public record ErrorMessage(String message) {}

    private final LeagueService league;
    private final EmailVerificationService verification;
    private final SecurityContextRepository securityContextRepository;
    private static final Map<String, String> DEVELOPER_TEAMS = Map.ofEntries(entry("rudra", "wolves"), entry("ryan", "bears"), entry("lucas", "hawks"), entry("nebojsa", "tigers"), entry("kiara", "mustangs"));

    public LeagueController(LeagueService league, EmailVerificationService verification, SecurityContextRepository securityContextRepository) {
        this.league = league;
        this.verification = verification;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@RequestBody RegistrationInput input) {
        verification.requestCode(input.email(), input.displayName(), input.password());

        return ResponseEntity.accepted().body(new RegistrationResponse("Verification code requested."));
    }

    @PostMapping("/verify")
    public VerifiedAccount verify(@RequestBody VerificationInput input, HttpServletRequest request, HttpServletResponse response, HttpSession session) {
        EmailVerificationService.VerifiedUser user = verification.verifyCode(input.email(), input.code());

        String accountRole = league.accountRole(user.email());
        var authentication = new UsernamePasswordAuthenticationToken(user.email(), null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + accountRole)));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);

        request.changeSessionId();
        session.setAttribute("view", "PLAYER");

        securityContextRepository.saveContext(context, request, response);

        return new VerifiedAccount(user.email(), user.displayName());
    }

    @GetMapping("/state")
    public LeagueState state() {
        return new LeagueState(league.teams(), league.players(), league.games(), league.standings(), league.scoreSubmissions());
    }

    @GetMapping("/session")
    public SessionInfo session(Principal user, HttpSession session, @Parameter(hidden = true) CsrfToken csrf) {
        String username = user == null ? "" : user.getName();
        List<LeagueService.Membership> memberships = user == null ? List.of() : league.memberships(username);
        String defaultTeamId = memberships
            .stream()
            .filter(membership -> "MANAGER".equals(membership.role()))
            .map(LeagueService.Membership::teamId)
            .findFirst()
            .orElse(DEVELOPER_TEAMS.getOrDefault(username, ""));

        return new SessionInfo(
            username,
            user == null ? "" : DEVELOPER_TEAMS.containsKey(username) ? "COMMISSIONER" : league.accountRole(username),
            user == null ? "GUEST" : view(session),
            Objects.toString(session.getAttribute("selectedTeamId"), defaultTeamId),
            memberships,
            csrf.getToken()
        );
    }

    @PostMapping("/view")
    public void changeView(@RequestBody ViewInput input, HttpSession session, Principal principal) {
        String mode = input.view();

        if (!Set.of("COMMISSIONER", "MANAGER", "PLAYER").contains(Objects.toString(mode, ""))) {
            throw new ResponseStatusException(BAD_REQUEST, "Choose commissioner, manager, or player.");
        }

        boolean isDeveloper =
            principal instanceof Authentication authentication &&
            authentication
                .getAuthorities()
                .stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_COMMISSIONER"));

        boolean canManage =
            isDeveloper ||
            league
                .memberships(principal.getName())
                .stream()
                .anyMatch(m -> "MANAGER".equals(m.role()));

        if ("COMMISSIONER".equals(mode) && !isDeveloper) {
            throw new ResponseStatusException(FORBIDDEN, "You do not have commissioner permissions.");
        }

        if ("MANAGER".equals(mode) && !canManage) {
            throw new ResponseStatusException(FORBIDDEN, "You are not a manager of any team.");
        }

        session.setAttribute("view", mode);
    }

    @PostMapping("/select-team")
    public void selectTeam(@RequestBody TeamSelectionInput input, HttpSession session, Principal user) {
        if (!"MANAGER".equals(view(session))) {
            throw new ResponseStatusException(FORBIDDEN, "Switch to manager view before selecting a team.");
        }

        String teamId = input.teamId();

        // Preserve developer access (testing purposes)
        if (!Objects.equals(DEVELOPER_TEAMS.get(user.getName()), teamId) && !league.isManagerOfTeam(user.getName(), teamId)) {
            throw new ResponseStatusException(FORBIDDEN, "You are not a manager of that team.");
        }

        session.setAttribute("selectedTeamId", teamId);
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

    @PostMapping("/teams/{teamId}/players/{playerId}/account")
    public void linkPlayerAccount(@PathVariable String teamId, @PathVariable String playerId, @RequestBody AccountEmailInput input, HttpSession session, Principal user) {
        rosterPermission(teamId, session, user);
        league.linkPlayerAccount(teamId, playerId, input.email());
    }

    @PutMapping("/teams/{teamId}/manager")
    public void assignTeamManager(@PathVariable String teamId, @RequestBody AccountEmailInput input, HttpSession session) {
        commissioner(session);
        league.assignTeamManager(teamId, input.email());
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
        league.submitScore(gameId, user.getName(), managedTeam(session, user, gameId), input.homeScore(), input.awayScore());
    }

    @PostMapping("/scores/{submissionId}/respond")
    public void respondToScore(@PathVariable String submissionId, @RequestBody ScoreResponseInput input, HttpSession session, Principal user) {
        String gameId = league.gameIdForSubmission(submissionId);
        league.respondToScore(submissionId, user.getName(), managedTeam(session, user, gameId), Boolean.TRUE.equals(input.confirm()), input.disputedHomeScore(), input.disputedAwayScore());
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
        return Objects.toString(session.getAttribute("view"), "PLAYER");
    }

    private void commissioner(HttpSession session) {
        if (!view(session).equals("COMMISSIONER")) {
            throw new ResponseStatusException(FORBIDDEN, "Switch to commissioner view to manage games and teams.");
        }
    }

    private String managedTeam(HttpSession session, Principal user, String gameId) {
        if (!view(session).equals("MANAGER")) {
            throw new ResponseStatusException(FORBIDDEN, "Switch to manager view to submit scores.");
        }

        LeagueService.Game game = league.requireGame(gameId);
        String developerTeam = DEVELOPER_TEAMS.get(user.getName());
        if (Objects.equals(developerTeam, game.homeId()) || Objects.equals(developerTeam, game.awayId())) {
            return developerTeam;
        }

        if (league.isManagerOfTeam(user.getName(), game.homeId())) {
            return game.homeId();
        }
        if (league.isManagerOfTeam(user.getName(), game.awayId())) {
            return game.awayId();
        }
        throw new ResponseStatusException(FORBIDDEN, "You do not manage a team in this game.");
    }

    private void rosterPermission(String teamId, HttpSession session, Principal user) {
        if (view(session).equals("COMMISSIONER") && isCommissioner(user)) {
            return;
        }

        if (!view(session).equals("MANAGER")) {
            throw new ResponseStatusException(FORBIDDEN, "Switch to manager view to edit a roster.");
        }

        String selectedTeam = Objects.toString(session.getAttribute("selectedTeamId"), DEVELOPER_TEAMS.getOrDefault(user.getName(), ""));

        boolean isDatabaseManager = league.isManagerOfTeam(user.getName(), selectedTeam) || Objects.equals(DEVELOPER_TEAMS.get(user.getName()), selectedTeam);

        if (!Objects.equals(teamId, selectedTeam) || !isDatabaseManager) {
            throw new ResponseStatusException(FORBIDDEN, "You can only edit a team you manage.");
        }
    }

    private boolean isCommissioner(Principal user) {
        return (
            user instanceof Authentication authentication &&
            authentication
                .getAuthorities()
                .stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_COMMISSIONER"))
        );
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
