package com.amateurleague;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** A reproducible fictional season, inserted only into an empty database. */
@Component
public class DemoData implements CommandLineRunner {

    private final JdbcTemplate db;

    public DemoData(JdbcTemplate db) {
        this.db = db;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (db.queryForObject("SELECT COUNT(*) FROM teams", Integer.class) > 0) {
            return;
        }
        String[][] teams = {
            { "wolves", "Lakewood Wolves", "LW", "#b4ed62", "Rudra" },
            { "bears", "Brookside Bears", "BB", "#f3b672", "Ryan" },
            { "hawks", "Highland Hawks", "HH", "#7da9f7", "Lucas" },
            { "tigers", "Riverside Tigers", "RT", "#c193ef", "Nebojsa" },
            { "mustangs", "Maplewood Mustangs", "MM", "#ef858b", "Kiara" },
            { "falcons", "Eastside Falcons", "EF", "#6bd0c1", "Alex" },
        };
        for (String[] t : teams) {
            db.update("INSERT INTO teams VALUES (?, ?, ?, ?, ?)", (Object[]) t);
        }
        String[][] names = {
            { "Jordan Ellis", "Marcus Reed", "Dylan Brooks", "Cameron Hayes", "Tyler Bennett", "Sam Parker" },
            { "Avery Cole", "Jamie Walker", "Morgan Bell", "Casey Ward", "Riley Grant", "Taylor Ford" },
            { "Logan West", "Noah Hayes", "Evan Miller", "Owen Brooks", "Blake Turner", "Drew Scott" },
            { "Alex Carter", "Charlie Reed", "Dakota King", "Emerson Lee", "Finley Moore", "Harper Lewis" },
            { "Jesse Lane", "Kai Morgan", "Lee Anderson", "Micah Davis", "Nico Harris", "Parker Young" },
            { "Quinn Allen", "Reese Wright", "Sage Thomas", "Tatum Hill", "Winter Adams", "Skyler Hall" },
        };
        String[] positions = { "QB", "RB", "WR", "WR", "LB", "K" };
        int[] numbers = { 7, 22, 11, 14, 54, 3 };
        for (int t = 0; t < teams.length; t++) {
            for (int p = 0; p < 6; p++) {
                db.update("INSERT INTO players (id, team_id, name, position, jersey_number) VALUES (?, ?, ?, ?, ?)", UUID.randomUUID().toString(), teams[t][0], names[t][p], positions[p], numbers[p]);
            }
        }
        // Each pair plays once over five weeks. These are sample results, not live scores.
        int[][] pairings = { { 0, 5, 1, 4, 2, 3 }, { 0, 4, 5, 3, 1, 2 }, { 0, 3, 4, 2, 5, 1 }, { 0, 2, 3, 1, 4, 5 }, { 0, 1, 2, 5, 3, 4 } };
        int[][] scores = { { 28, 14, 21, 7, 17, 10 }, { 21, 14, 7, 20, 24, 17 }, { 14, 21, 10, 27, 7, 28 }, { 31, 24, 10, 17, 21, 14 }, { 24, 17, 28, 13, 14, 20 } };
        for (int w = 0; w < 5; w++) {
            for (int m = 0; m < 3; m++) {
                game(
                    "week" + (w + 1) + "-" + m,
                    teams[pairings[w][m * 2]][0],
                    teams[pairings[w][m * 2 + 1]][0],
                    LocalDateTime.of(2026, 9, 25, 18, 0).plusWeeks(w).plusHours(m).toString(),
                    w + 1,
                    "REGULAR",
                    "FINAL",
                    scores[w][m * 2],
                    scores[w][m * 2 + 1]
                );
            }
        }
        game("week6-0", "wolves", "hawks", "2026-11-06T19:00", 6, "REGULAR", "SCHEDULED", null, null);
        game("week6-1", "bears", "tigers", "2026-11-07T13:00", 6, "REGULAR", "SCHEDULED", null, null);
        game("week6-2", "mustangs", "falcons", "2026-11-07T15:00", 6, "REGULAR", "SCHEDULED", null, null);
    }

    private void game(String id, String home, String away, String time, int week, String phase, String status, Integer hs, Integer as) {
        db.update("INSERT INTO games VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", id, home, away, time, "Oakwood Community Field", week, phase, status, hs, as);
    }
}
