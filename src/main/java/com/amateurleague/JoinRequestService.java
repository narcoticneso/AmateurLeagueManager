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
            throw new ResponseStatusException(BAD_REQUEST, "Create and sign in to a verified email account to request a team. Demo logins cannot submit requests.");
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
}
