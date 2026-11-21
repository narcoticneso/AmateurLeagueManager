package com.amateurleague;

import static org.springframework.http.HttpStatus.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class JoinRequestService {

    public record JoinRequest(String id, String teamId, String teamName, String participantName, String status, LocalDateTime requestedAt) {}

    private record RequestDetails(String teamId, String email, String displayName) {}

    private final JdbcTemplate db;
    private final LeagueService league;

    public JoinRequestService(JdbcTemplate db, LeagueService league) {
        this.db = db;
        this.league = league;
    }

    public List<LeagueService.Team> availableTeams(String email) {
        return db.query(
            """
            SELECT t.* FROM teams t
            WHERE EXISTS (SELECT 1 FROM team_memberships m WHERE m.team_id = t.id AND m.role = 'MANAGER')
            AND NOT EXISTS (SELECT 1 FROM team_memberships m JOIN users u ON u.id = m.user_id WHERE m.team_id = t.id AND u.email = ?)
            AND NOT EXISTS (SELECT 1 FROM players p JOIN users u ON u.id = p.user_id WHERE p.team_id = t.id AND u.email = ?)
            AND NOT EXISTS (SELECT 1 FROM team_join_requests r JOIN users u ON u.id = r.user_id WHERE r.team_id = t.id AND u.email = ? AND r.status = 'PENDING')
            ORDER BY t.name
            """,
            (r, n) -> new LeagueService.Team(r.getString("id"), r.getString("name"), r.getString("abbreviation"), r.getString("color"), r.getString("manager")),
            email,
            email,
            email
        );
    }

    public List<JoinRequest> history(String email) {
        return requests("u.email = ?", email);
    }

    public List<JoinRequest> pendingForTeam(String email, String teamId) {
        if (!league.isManagerOfTeam(email, teamId)) {
            throw new ResponseStatusException(FORBIDDEN, "Only this team's assigned manager can view its join requests.");
        }
        return requests("r.team_id = ? AND r.status = 'PENDING'", teamId);
    }

    private List<JoinRequest> requests(String condition, String value) {
        return db.query(
            """
            SELECT r.*, t.name AS team_name, u.display_name
            FROM team_join_requests r
            JOIN teams t ON t.id = r.team_id
            JOIN users u ON u.id = r.user_id
            WHERE
            """ +
                condition +
                " ORDER BY r.requested_at DESC, r.id",
            (r, n) -> new JoinRequest(r.getString("id"), r.getString("team_id"), r.getString("team_name"), r.getString("display_name"), r.getString("status"), r.getTimestamp("requested_at").toLocalDateTime()),
            value
        );
    }

    @Transactional
    public void submit(String email, String teamId) {
        // Lock the participant so simultaneous submissions cannot create duplicate pending requests.
        List<String> accounts = db.query("SELECT id FROM users WHERE email = ? FOR UPDATE", (r, n) -> r.getString("id"), email);
        if (accounts.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Create and sign in to an account to request a team.");
        }
        if (teamId == null || teamId.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Choose a team.");
        }
        league.requireTeam(teamId);
        if (
            availableTeams(email)
                .stream()
                .noneMatch(team -> team.id().equals(teamId))
        ) {
            throw new ResponseStatusException(CONFLICT, "This team needs an assigned manager, or you already belong to it or have a pending request. Refresh the list.");
        }
        db.update("INSERT INTO team_join_requests (id, user_id, team_id, status, requested_at) VALUES (?, ?, ?, 'PENDING', ?)", UUID.randomUUID().toString(), accounts.getFirst(), teamId, LocalDateTime.now(ZoneOffset.UTC));
    }

    @Transactional
    public void decide(String managerEmail, String requestId, boolean approve, String position, Integer number) {
        // Checks the request exists
        List<RequestDetails> rows = db.query(
            """
            SELECT r.team_id, u.email, u.display_name
            FROM team_join_requests r
            JOIN users u ON u.id = r.user_id
            WHERE r.id = ?
            """,
            (r, n) -> new RequestDetails(r.getString("team_id"), r.getString("email"), r.getString("display_name")),
            requestId
        );
        if (rows.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "Join request not found.");
        }
        RequestDetails request = rows.getFirst();

        // Checks the manager is assigned to the team the request was sent to
        if (!league.isManagerOfTeam(managerEmail, request.teamId())) {
            throw new ResponseStatusException(FORBIDDEN, "Only this team's assigned manager can approve or deny its join requests.");
        }

        // Prevents the same request from being decided twice
        if (db.update("UPDATE team_join_requests SET status = ? WHERE id = ? AND status = 'PENDING'", approve ? "APPROVED" : "DENIED", requestId) == 0) {
            throw new ResponseStatusException(CONFLICT, "This join request has already been decided. Refresh the list.");
        }

        if (approve) {
            // Checks the participant is not already on a roster, because an account links to one roster entry
            if (db.queryForObject("SELECT COUNT(*) FROM players p JOIN users u ON u.id = p.user_id WHERE u.email = ?", Integer.class, request.email()) > 0) {
                throw new ResponseStatusException(CONFLICT, "This participant is already on a roster, so they cannot be added to another team. Deny the request instead.");
            }

            // Adds the participant to the roster, then links the new entry to their account
            league.addPlayer(request.teamId(), new LeagueService.PlayerInput(request.displayName(), position, number));
            String playerId = league
                .players()
                .stream()
                .filter(p -> p.teamId().equals(request.teamId()) && p.number() == number)
                .findFirst()
                .orElseThrow()
                .id();
            league.linkPlayerAccount(request.teamId(), playerId, request.email());
        }
    }
}
