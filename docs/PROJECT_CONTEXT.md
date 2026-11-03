# Amateur League Manager context

Last updated: November 3, 2026.

## Standing collaboration rule

**The assistant never stages, commits, or pushes.** The assistant only explains which files to include and supplies commands and suggested commit messages for the user to execute. This applies even when the user says "let's commit." Read-only Git checks, code edits, documentation updates, and appropriate tests are allowed. See `../AGENTS.md`.

## Product decisions

- A browser-based manager for a friendly neighborhood American football league.
- Start with one league and one active season, regular-season games, and one postseason tournament per season.
- Manual scheduling and manual tournament advancement.
- Keep the first version simple; the original 34 user stories are a flexible backlog rather than mandatory initial scope.
- Use Java for the backend. The frontend is plain HTML, CSS, and JavaScript.
- Visual direction: fantasy-football-inspired dark backgrounds, green highlights, matchup cards, roster tables, and league standings, adapted for desktop and mobile browsers.
- Public-facing descriptions must not call it a senior project or mention CS 498.

## People and roles

Developers: Rudra, Ryan, Lucas, Nebojsa, and Kiara. Each has a separate development account with commissioner access and can switch the current session to commissioner, manager, or player view.

- Commissioner: create teams, manage rosters, schedule games, enter/correct scores, and manage the tournament.
- Manager: manage the assigned team's roster and view league information.
- Player/public visitor: read-only access.
- Role restrictions are enforced by the Java backend as well as by visible controls.

## Current implementation

- Java 21, Spring Boot 4.0.9, Maven wrapper, Spring Security sessions and CSRF protection, Spring JDBC, and a persistent local H2 database.
- Public dashboard, schedules/results, teams/rosters, standings, and postseason cup pages.
- Fictional Oakwood Football sample season with six teams and basic player rosters.
- Standings derive from final regular-season results; ties count as half a win. Tournament results do not affect regular-season standings.
- A four-team cup with two semifinals and one championship. The commissioner schedules the championship between the semifinal winners.
- League branding and season labels are currently fixed. User/team assignments are seeded development configuration.
- Seven integration tests passed during the initial implementation, and browser checks covered desktop/mobile layouts, login, role restrictions, saving games, and duplicate jersey validation. Run checks as appropriate when behavior changes; this is historical validation, not a guarantee of future edits.

## Local setup

- On this machine, `dev.ps1` uses the portable Java/Maven installations in ignored `.tools/` when needed.
- On another machine, install Java 21 and run `mvnw.cmd spring-boot:run` on Windows or `sh ./mvnw spring-boot:run` on macOS/Linux. See the README for download links and setup steps.
- The configured default is `http://localhost:8080`; the `PORT` environment variable can override it. On November 3, the user's running server was verified on **8081**. Check the actual listener/startup output before diagnosing a connection issue.
- Keep the server terminal open while using the website. Do not stop a server the user started merely to clean up a troubleshooting task.
- Local demo usernames: `rudra`, `ryan`, `lucas`, `nebojsa`, `kiara`. The documented demo password is `neighborhood-demo`; environment variables can override it per account.
- `.tools/`, `data/`, `target/`, logs, and `.env` remain ignored. Do not stage them or suggest including them in a commit.

## Deferred work

Configurable league/season settings, team editing, persisted user memberships, join requests, reminders, score disputes, historical seasons, uploads, detailed player statistics, and automatic scheduling are not part of the completed starter.

No project license has been selected. MIT was recommended as an option, but the team has not confirmed it. Do not add a license based on the recommendation alone.
