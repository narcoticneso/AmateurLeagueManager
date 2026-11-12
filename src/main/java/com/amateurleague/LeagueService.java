package com.amateurleague;

import static org.springframework.http.HttpStatus.*;

import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LeagueService {

    public record Team(String id, String name, String abbreviation, String color, String manager) {}


    public record Membership(
        String teamId,
        String teamName,
        String role
    ) {}

    private record AccountIdentity(String id, String displayName) {}


    public record Player(String id, String teamId, String name, String position, int number) {}

    public record Game(
        String id,
        String homeId,
        String awayId,
        String kickoff,
        String location,
        int week,
        String phase,
        String status,
        @Schema(types = { "integer", "null" }) Integer homeScore,
        @Schema(types = { "integer", "null" }) Integer awayScore
    ) {}

    public record Standing(String teamId, int played, int wins, int losses, int ties, int pointsFor, int pointsAgainst, double percentage) {}

    public record GameInput(
        String homeId,
        String awayId,
        String kickoff,
        String location,
        Integer week,
        String phase,
        String status,
        @Schema(types = { "integer", "null" }) Integer homeScore,
        @Schema(types = { "integer", "null" }) Integer awayScore
    ) {}

    public record TeamInput(String name, String abbreviation, String color, String manager) {}

    public record PlayerInput(String name, String position, Integer number) {}

    public record ScoreSubmission(
        String id,
        String gameId,
        String submittedBy,
        Integer submittedHomeScore,
        Integer submittedAwayScore,
        @Schema(types = { "integer", "null" }) Integer disputedHomeScore,
        @Schema(types = { "integer", "null" }) Integer disputedAwayScore,
        String status,
        LocalDateTime submittedAt,
        @Schema(types = { "string", "null" }) LocalDateTime confirmedAt,
        @Schema(types = { "string", "null" }) String resolvedBy
    ) {}

    private static final ZoneId LEAGUE_TIME = ZoneId.of("America/New_York");
    private final JdbcTemplate db;

    public LeagueService(JdbcTemplate db) {
        this.db = db;
    }

    public List<Team> teams() {
        return db.query("SELECT * FROM teams ORDER BY name", (r, n) -> new Team(r.getString("id"), r.getString("name"), r.getString("abbreviation"), r.getString("color"), r.getString("manager")));
    }


    public List<Membership> memberships(String email) {
        return db.query(
            """
            SELECT t.id AS team_id, t.name AS team_name, tm.role
            FROM users u
            JOIN team_memberships tm ON tm.user_id = u.id
            JOIN teams t ON t.id = tm.team_id
            WHERE u.email = ?
            ORDER BY t.name
            """,
            (r, n) -> new Membership(
                r.getString("team_id"),
                r.getString("team_name"),
                r.getString("role")
            ),
            email
        );
    }

    public String accountRole(String email) {
        return db.queryForObject("SELECT account_role FROM users WHERE email = ?", String.class, email);
    }

    public boolean isManagerOfTeam(String email, String teamId) {
        return db.queryForObject(
            """
            SELECT COUNT(*)
            FROM users u
            JOIN team_memberships tm ON tm.user_id = u.id
            WHERE u.email = ?
            AND tm.team_id = ?
            AND tm.role = 'MANAGER'
            """,
            Integer.class,
            email,
            teamId
        ) > 0;
    }


    public List<Player> players() {
        return db.query("SELECT * FROM players ORDER BY jersey_number, name", (r, n) -> new Player(r.getString("id"), r.getString("team_id"), r.getString("name"), r.getString("position"), r.getInt("jersey_number")));
    }

    public List<Game> games() {
        return db.query("SELECT * FROM games ORDER BY kickoff, id", (r, n) ->
            new Game(
                r.getString("id"),
                r.getString("home_id"),
                r.getString("away_id"),
                r.getString("kickoff"),
                r.getString("location"),
                r.getInt("week"),
                r.getString("phase"),
                r.getString("status"),
                r.getObject("home_score", Integer.class),
                r.getObject("away_score", Integer.class)
            )
        );
    }

    public List<ScoreSubmission> scoreSubmissions() {
        return db.query("SELECT * FROM score_submissions ORDER BY submitted_at DESC", LeagueService::mapSubmission);
    }

    private ScoreSubmission requireSubmission(String id) {
        List<ScoreSubmission> rows = db.query("SELECT * FROM score_submissions WHERE id = ?", LeagueService::mapSubmission, id);
        return rows
            .stream()
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Score submission not found."));
    }

    private static ScoreSubmission mapSubmission(ResultSet r, int rowNum) throws SQLException {
        return new ScoreSubmission(
            r.getString("id"),
            r.getString("game_id"),
            r.getString("submitted_by"),
            r.getObject("submitted_home_score", Integer.class),
            r.getObject("submitted_away_score", Integer.class),
            r.getObject("disputed_home_score", Integer.class),
            r.getObject("disputed_away_score", Integer.class),
            r.getString("status"),
            r.getTimestamp("submitted_at").toLocalDateTime(),
            r.getTimestamp("confirmed_at") == null ? null : r.getTimestamp("confirmed_at").toLocalDateTime(),
            r.getString("resolved_by")
        );
    }

    public List<Standing> standings() {
        List<Game> games = games();
        List<Standing> result = new ArrayList<>();
        for (Team team : teams()) {
            int wins = 0;
            int losses = 0;
            int ties = 0;
            int pf = 0;
            int pa = 0;
            for (Game game : games) {
                if (!game.phase().equals("REGULAR") || !game.status().equals("FINAL")) {
                    continue;
                }
                if (!game.homeId().equals(team.id()) && !game.awayId().equals(team.id())) {
                    continue;
                }
                int own = game.homeId().equals(team.id()) ? game.homeScore() : game.awayScore();
                int other = game.homeId().equals(team.id()) ? game.awayScore() : game.homeScore();
                pf += own;
                pa += other;
                if (own > other) {
                    wins++;
                } else if (own < other) {
                    losses++;
                } else {
                    ties++;
                }
            }
            int played = wins + losses + ties;
            result.add(new Standing(team.id(), played, wins, losses, ties, pf, pa, played == 0 ? 0 : (wins + ties * 0.5) / played));
        }
        result.sort(
            Comparator.comparingDouble(Standing::percentage)
                .reversed()
                .thenComparing(Comparator.comparingInt((Standing s) -> s.pointsFor() - s.pointsAgainst()).reversed())
                .thenComparing(Standing::teamId)
        );
        return result;
    }

    public Team requireTeam(String id) {
        return teams()
            .stream()
            .filter(t -> t.id().equals(id))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Team not found."));
    }

    public Game requireGame(String id) {
        return games()
            .stream()
            .filter(g -> g.id().equals(id))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Game not found."));
    }

    @Transactional
    public void submitScore(String gameId, String username, String teamId, Integer homeScore, Integer awayScore) {
        Game game = requireGame(gameId);

        // Checks the manager belongs to one of the teams playing
        if (!game.homeId().equals(teamId) && !game.awayId().equals(teamId)) {
            fail("You can only submit scores for games involving your team.");
        }

        // Checks game has started
        LocalDateTime kickoff = LocalDateTime.parse(game.kickoff());
        if (LocalDateTime.now(LEAGUE_TIME).isBefore(kickoff)) {
            fail("You cannot submit a score before the game starts.");
        }
        // Validate the scores
        if (homeScore == null || awayScore == null || homeScore < 0 || awayScore < 0 || homeScore > 200 || awayScore > 200) {
            fail("Scores must be whole numbers between 0 and 200.");
        }
        // Checks for winner
        if (!game.phase().equals("REGULAR") && homeScore.equals(awayScore)) {
            fail("Tournament games must have a winner.");
        }
        // Checks if the game has already been finalized
        if (game.status().equals("FINAL")) {
            fail("This game already has an official result.");
        }

        // Prevents multiple pending or disputed submissions of the same game
        Integer existing = db.queryForObject("SELECT COUNT(*) FROM score_submissions WHERE game_id = ? AND status IN ('PENDING', 'DISPUTED')", Integer.class, gameId);

        if (existing != null && existing > 0) {
            fail("This game already has a pending or disputed score submission.");
        }

        db.update(
            "INSERT INTO score_submissions (id, game_id, submitted_by, submitted_home_score, submitted_away_score, status, submitted_at) VALUES (?, ?, ?, ?, ?, 'PENDING', ?)",
            UUID.randomUUID().toString(),
            gameId,
            username,
            homeScore,
            awayScore,
            LocalDateTime.now(LEAGUE_TIME)
        );
    }

    @Transactional
    public void respondToScore(String submissionId, String username, String teamId, boolean confirm, Integer disputedHomeScore, Integer disputedAwayScore) {
        ScoreSubmission submission = requireSubmission(submissionId);

        Game game = requireGame(submission.gameId());

        // Only the opposing team's manager can respond.
        if (!game.homeId().equals(teamId) && !game.awayId().equals(teamId)) {
            fail("You are not part of this game.");
        }

        Team team = requireTeam(teamId);
        if (!isManagerOfTeam(username, teamId) && !team.manager().equalsIgnoreCase(username)) {
            fail("Only your team's manager can respond.");
        }
        if (submission.submittedBy().equalsIgnoreCase(username)) {
            fail("You cannot respond to your own score submission.");
        }

        if (!submission.status().equals("PENDING")) {
            fail("This score submission is no longer pending.");
        }
        if (confirm) {
            db.update("UPDATE score_submissions SET status = 'CONFIRMED', confirmed_at = ? WHERE id = ?", LocalDateTime.now(LEAGUE_TIME), submissionId);

            db.update("UPDATE games SET status = 'FINAL', home_score = ?, away_score = ? WHERE id = ?", submission.submittedHomeScore(), submission.submittedAwayScore(), submission.gameId());
        } else {
            if (disputedHomeScore == null || disputedAwayScore == null || disputedHomeScore < 0 || disputedAwayScore < 0 || disputedHomeScore > 200 || disputedAwayScore > 200) {
                fail("Disputed scores must be between 0 and 200.");
            }

            if (!game.phase().equals("REGULAR") && disputedHomeScore.equals(disputedAwayScore)) {
                fail("Tournament games must have a winner.");
            }

            db.update("UPDATE score_submissions SET status = 'DISPUTED', disputed_home_score = ?, disputed_away_score = ? WHERE id = ?", disputedHomeScore, disputedAwayScore, submissionId);
        }
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void autoConfirmExpiredSubmissions() {
        LocalDateTime deadline = LocalDateTime.now(LEAGUE_TIME).minusHours(24);

        List<ScoreSubmission> expired = db.query("SELECT * FROM score_submissions WHERE status = 'PENDING' AND submitted_at <= ?", LeagueService::mapSubmission, deadline);

        for (ScoreSubmission submission : expired) {
            db.update("UPDATE games SET status = 'FINAL', home_score = ?, away_score = ? WHERE id = ? AND status = 'SCHEDULED'", submission.submittedHomeScore(), submission.submittedAwayScore(), submission.gameId());

            db.update("UPDATE score_submissions SET status = 'CONFIRMED', confirmed_at = ? WHERE id = ? AND status = 'PENDING'", LocalDateTime.now(LEAGUE_TIME), submission.id());
        }
    }

    @Transactional
    public void resolveDisputedScore(String submissionId, String commissioner, Integer homeScore, Integer awayScore) {
        ScoreSubmission submission = requireSubmission(submissionId);

        if (!submission.status().equals("DISPUTED")) {
            fail("Only disputed scores can be resolved.");
        }

        Game game = requireGame(submission.gameId());

        if (homeScore == null || awayScore == null || homeScore < 0 || awayScore < 0 || homeScore > 200 || awayScore > 200) {
            fail("Scores must be whole numbers between 0 and 200.");
        }

        if (!game.phase().equals("REGULAR") && homeScore.equals(awayScore)) {
            fail("Tournament games must have a winner.");
        }

        db.update("UPDATE games SET status = 'FINAL', home_score = ?, away_score = ? WHERE id = ?", homeScore, awayScore, game.id());

        db.update("UPDATE score_submissions SET status = 'RESOLVED', confirmed_at = ?, resolved_by = ? WHERE id = ?", LocalDateTime.now(LEAGUE_TIME), commissioner, submissionId);
    }

    @Transactional
    public void createTeam(TeamInput input) {
        String name = text(input.name(), "Team name", 60);
        String abbreviation = text(input.abbreviation(), "Abbreviation", 3).toUpperCase(Locale.ROOT);
        if (!abbreviation.matches("[A-Z]{2,3}")) {
            fail("Use two or three letters for the abbreviation.");
        }
        if (input.color() == null || !input.color().matches("#[0-9a-fA-F]{6}")) {
            fail("Choose a valid team color.");
        }
        if (
            teams()
                .stream()
                .anyMatch(t -> t.name().equalsIgnoreCase(name))
        ) {
            fail("A team with that name already exists.");
        }
        db.update("INSERT INTO teams VALUES (?, ?, ?, ?, ?)", UUID.randomUUID().toString(), name, abbreviation, input.color(), text(input.manager(), "Manager", 60));
    }

    @Transactional
    public void updateTeam(String id, TeamInput input) {
        // Checks the team exists
        requireTeam(id);

        // Validate the team details
        String name = text(input.name(), "Team name", 60);
        String abbreviation = text(input.abbreviation(), "Abbreviation", 3).toUpperCase(Locale.ROOT);
        if (!abbreviation.matches("[A-Z]{2,3}")) {
            fail("Use two or three letters for the abbreviation.");
        }
        if (input.color() == null || !input.color().matches("#[0-9a-fA-F]{6}")) {
            fail("Choose a valid team color.");
        }
        // Prevents taking another team's name, but lets the team keep its own
        if (
            teams()
                .stream()
                .anyMatch(t -> !t.id().equals(id) && t.name().equalsIgnoreCase(name))
        ) {
            fail("A team with that name already exists.");
        }

        // The team id never changes, so its players and games stay attached
        db.update("UPDATE teams SET name = ?, abbreviation = ?, color = ?, manager = ? WHERE id = ?", name, abbreviation, input.color(), text(input.manager(), "Manager", 60), id);
    }

    @Transactional
    public void addPlayer(String teamId, PlayerInput input) {
        requireTeam(teamId);
        String name = text(input.name(), "Player name", 60);
        if (!Set.of("QB", "RB", "WR", "TE", "OL", "DL", "LB", "CB", "S", "K").contains(Objects.toString(input.position(), ""))) {
            fail("Choose a valid position.");
        }
        if (input.number() == null || input.number() < 0 || input.number() > 99) {
            fail("Jersey number must be between 0 and 99.");
        }
        if (
            players()
                .stream()
                .anyMatch(p -> p.teamId().equals(teamId) && p.number() == input.number())
        ) {
            fail("That jersey number is already on this roster.");
        }
        db.update(
            "INSERT INTO players (id, team_id, name, position, jersey_number) VALUES (?, ?, ?, ?, ?)",
            UUID.randomUUID().toString(),
            teamId,
            name,
            input.position(),
            input.number()
        );
    }

    public void removePlayer(String teamId, String playerId) {
        if (db.update("DELETE FROM players WHERE id = ? AND team_id = ?", playerId, teamId) == 0) {
            throw new ResponseStatusException(NOT_FOUND, "Player not found.");
        }
    }

    @Transactional
    public void linkPlayerAccount(String teamId, String playerId, String email) {
        String normalizedEmail = Objects.toString(email, "").trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail.isBlank()) {
            fail("Enter the account email address.");
        }
        Integer accountExists = db.queryForObject(
            "SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, normalizedEmail);
        if (accountExists == null || accountExists == 0) {
            fail("No account exists for that email address.");
        }
        if (db.update("UPDATE players SET user_id = (SELECT id FROM users WHERE email = ?) WHERE id = ? AND team_id = ?",
            normalizedEmail, playerId, teamId) == 0) {
            throw new ResponseStatusException(NOT_FOUND, "Roster entry not found.");
        }
    }

    @Transactional
    public void assignTeamManager(String teamId, String email) {
        requireTeam(teamId);
        String normalizedEmail = Objects.toString(email, "").trim().toLowerCase(Locale.ROOT);
        List<AccountIdentity> accounts = db.query("SELECT id, display_name FROM users WHERE email = ?",
            (rs, row) -> new AccountIdentity(rs.getString("id"), rs.getString("display_name")), normalizedEmail);
        if (accounts.isEmpty()) {
            fail("No account exists for that email address.");
        }
        AccountIdentity account = accounts.getFirst();
        db.update("DELETE FROM team_memberships WHERE team_id = ? AND role = 'MANAGER'", teamId);
        db.update("MERGE INTO team_memberships (id, user_id, team_id, role, created_at) KEY (user_id, team_id) VALUES (?, ?, ?, 'MANAGER', ?)",
            UUID.randomUUID().toString(), account.id(), teamId, LocalDateTime.now(ZoneId.of("UTC")));
        db.update("UPDATE teams SET manager = ? WHERE id = ?", account.displayName(), teamId);
    }

    @Transactional
    public void saveGame(String id, GameInput input) {
        requireTeam(input.homeId());
        requireTeam(input.awayId());
        if (input.homeId().equals(input.awayId())) {
            fail("Choose two different teams.");
        }
        String kickoff = text(input.kickoff(), "Kickoff", 30);
        try {
            LocalDateTime.parse(kickoff);
        } catch (Exception e) {
            fail("Enter a valid date and time.");
        }
        String location = text(input.location(), "Location", 100);
        if (input.week() == null || input.week() < 1 || input.week() > 30) {
            fail("Week must be between 1 and 30.");
        }
        if (!Set.of("REGULAR", "SEMIFINAL", "CHAMPIONSHIP").contains(Objects.toString(input.phase(), ""))) {
            fail("Choose a valid competition stage.");
        }
        if (!Set.of("SCHEDULED", "FINAL", "POSTPONED", "CANCELLED").contains(Objects.toString(input.status(), ""))) {
            fail("Choose a valid game status.");
        }
        Integer home = null;
        Integer away = null;
        if (input.status().equals("FINAL")) {
            home = input.homeScore();
            away = input.awayScore();
            if (home == null || away == null || home < 0 || away < 0 || home > 200 || away > 200) {
                fail("Final scores must be whole numbers between 0 and 200.");
            }
            if (!input.phase().equals("REGULAR") && home.equals(away)) {
                fail("Tournament games must have a winner.");
            }
        }
        List<Game> proposed = new ArrayList<>(
            games()
                .stream()
                .filter(g -> !g.id().equals(id))
                .toList()
        );
        proposed.add(new Game(id == null ? "new" : id, input.homeId(), input.awayId(), kickoff, location, input.week(), input.phase(), input.status(), home, away));
        validateTournament(proposed);
        if (id == null) {
            db.update("INSERT INTO games VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", UUID.randomUUID().toString(), input.homeId(), input.awayId(), kickoff, location, input.week(), input.phase(), input.status(), home, away);
            return;
        }
        int updated = db.update(
            "UPDATE games SET home_id=?, away_id=?, kickoff=?, location=?, week=?, phase=?, status=?, home_score=?, away_score=? WHERE id=?",
            input.homeId(),
            input.awayId(),
            kickoff,
            location,
            input.week(),
            input.phase(),
            input.status(),
            home,
            away,
            id
        );
        if (updated == 0) {
            throw new ResponseStatusException(NOT_FOUND, "Game not found.");
        }
    }

    private void validateTournament(List<Game> proposed) {
        List<Game> semifinals = proposed
            .stream()
            .filter(g -> g.phase().equals("SEMIFINAL") && !g.status().equals("CANCELLED"))
            .toList();
        List<Game> championships = proposed
            .stream()
            .filter(g -> g.phase().equals("CHAMPIONSHIP") && !g.status().equals("CANCELLED"))
            .toList();
        if (semifinals.size() > 2 || championships.size() > 1) {
            fail("The cup has two semifinals and one championship. Edit an existing game instead.");
        }
        Set<String> entrants = new HashSet<>();
        for (Game g : semifinals) {
            if (!entrants.add(g.homeId()) || !entrants.add(g.awayId())) {
                fail("A team can only appear in one semifinal.");
            }
        }
        if (!championships.isEmpty()) {
            if (semifinals.size() != 2 || semifinals.stream().anyMatch(g -> !g.status().equals("FINAL"))) {
                fail("Finalize both semifinals before scheduling the championship.");
            }
            Set<String> winners = new HashSet<>();
            for (Game g : semifinals) {
                winners.add(g.homeScore() > g.awayScore() ? g.homeId() : g.awayId());
            }
            Game finalGame = championships.getFirst();
            if (!winners.equals(Set.of(finalGame.homeId(), finalGame.awayId()))) {
                fail("The championship must feature the two semifinal winners. Cancel the championship before changing a semifinal winner.");
            }
            if (semifinals.stream().anyMatch(g -> !LocalDateTime.parse(g.kickoff()).isBefore(LocalDateTime.parse(finalGame.kickoff())))) {
                fail("The championship must start after both semifinals.");
            }
        }
    }

    private String text(String value, String label, int max) {
        if (value == null || value.isBlank() || value.strip().length() > max) {
            fail(label + " is required (maximum " + max + " characters).");
        }
        return value.strip();
    }

    private void fail(String message) {
        throw new ResponseStatusException(BAD_REQUEST, message);
    }
}
