# Amateur League Manager context

Last updated: November 20, 2026.

## Standing collaboration rule

**The assistant never stages, commits, or pushes.** The assistant only explains which files to include and supplies commands and suggested commit messages for the user to execute. This applies even when the user says "let's commit." Read-only Git checks, code edits, documentation updates, and appropriate tests are allowed. See `../AGENTS.md`.

## Product decisions

- A browser-based manager for a friendly neighborhood American football league.
- Start with one league and one active season, regular-season games, and one postseason tournament per season.
- Manual scheduling and manual tournament advancement.
- Keep the first version simple; the original 34 user stories are a flexible backlog rather than mandatory initial scope.
- Use Java for the backend. The frontend is Svelte 5 + TypeScript with Tailwind and shadcn-svelte, built into the jar by Maven.
- Visual direction: fantasy-football-inspired dark backgrounds, green highlights, matchup cards, roster tables, and league standings, adapted for desktop and mobile browsers.
- Public-facing descriptions must not call it a senior project or mention CS 498.

## People and roles

Developers: Rudra, Ryan, Lucas, Nebojsa, and Kiara. Each has a seeded account (`<name>@example.com`) that is a commissioner and manages one demo team.

Accounts live in the `users` table. People sign up with name, email, and password and confirm with a six-digit code (printed to the server log until email delivery exists). Roles are derived, not chosen: commissioner is a flag on the account (developer accounts and `ALM_COMMISSIONER_EMAILS`), manager means a `MANAGER` row in `team_memberships`, and player means a roster entry linked through `players.user_id`. There is no view switcher.

- Commissioner: create teams, manage rosters, schedule games, enter/correct scores, and manage the tournament.
- Manager: manage the assigned team's roster, link roster entries to accounts, and submit/confirm that team's scores.
- Player/public visitor: read-only access.
- Role restrictions are enforced by the Java backend as well as by visible controls.

## Current implementation

- Java 21, Spring Boot 4.0.9, Maven wrapper, Spring Security sessions and CSRF protection, Spring JDBC, and H2 with Flyway migrations. The default profile uses a persistent local database; the `dev` profile uses an in-memory database recreated at each startup.
- Maven downloads Node.js and builds the frontend, so only Java has to be installed. TypeScript types for the API are generated from the Java code on every build.
- Code checks are strict and enforced by `mvnw verify`: Prettier formats both Java (prettier-plugin-java) and the frontend (`mvnw -Pformat`); Checkstyle (braces required) and Error Prone with warnings as errors for Java; strict TypeScript, svelte-check, and ESLint (`strictTypeChecked`, braces required) for the frontend.
- Frontend unit and browser tests are out of scope.
- Public dashboard, schedules/results, teams/rosters, standings, and postseason cup pages.
- Fictional Oakwood Football sample season with six teams and basic player rosters.
- Standings derive from final regular-season results; ties count as half a win. Tournament results do not affect regular-season standings.
- A four-team cup with two semifinals and one championship. The commissioner schedules the championship between the semifinal winners.
- League branding and season labels are currently fixed. Developer accounts and their team assignments are seeded demo data; the commissioner assigns managers and links roster entries in the app.
- Seven integration tests passed during the initial implementation, and browser checks covered desktop/mobile layouts, login, role restrictions, saving games, and duplicate jersey validation. Run checks as appropriate when behavior changes; this is historical validation, not a guarantee of future edits.

## Local setup

- On this machine, `dev.ps1` uses the portable Java/Maven installations in ignored `.tools/` when needed.
- On another machine, install Java 21 and run `mvnw.cmd spring-boot:run` on Windows or `sh ./mvnw spring-boot:run` on macOS/Linux. See the README for download links and setup steps.
- For frontend work, `mvnw spring-boot:run -Pdev` also starts the Vite dev server on port 5173.
- The configured default is `http://localhost:8080`; the `PORT` environment variable can override it. On November 3, the user's running server was verified on **8081**. Check the actual listener/startup output before diagnosing a connection issue.
- Keep the server terminal open while using the website. Do not stop a server the user started merely to clean up a troubleshooting task.
- Local demo accounts: `rudra@example.com`, `ryan@example.com`, `lucas@example.com`, `nebojsa@example.com`, `kiara@example.com`. The documented demo password is `neighborhood-demo`; `ALM_PASSWORD_<NAME>` overrides it per account.
- `.tools/`, `data/`, `target/`, `frontend/node/`, `frontend/node_modules/`, logs, and `.env` remain ignored. Do not stage them or suggest including them in a commit.

## Deferred work

Configurable league/season settings, join requests, reminders, historical seasons, uploads, detailed player statistics, and automatic scheduling are not part of the completed starter.

No project license has been selected. MIT was recommended as an option, but the team has not confirmed it. Do not add a license based on the recommendation alone.
