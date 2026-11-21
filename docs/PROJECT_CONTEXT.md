# Amateur League Manager context

Last updated: November 20, 2026.

## Standing collaboration rule

**The assistant may execute requested Git and GitHub work as `narcoticneso`.** This replaces the earlier prohibition on staging, committing, pushing, and GitHub publishing. Requests such as "let's commit" authorize the relevant action; unrelated publishing and destructive operations still need task-specific authorization. See the local `../AGENTS.md`.

Git and GitHub work performed for Nebojsa belongs to his `narcoticneso` identity, never ChatGPT, Codex, OpenAI, or an assistant account. Verify the authenticated account before GitHub changes and both author and committer identity before local commits. Do not add assistant authorship, co-authorship, ownership, assignments, or signatures. Preserve teammates' authorship and unrelated edits. Requested fetches and fast-forward-only pulls are allowed after checking local changes. Do not change persistent identity settings without authorization or invent an email address. Keep `AGENTS.md` local and do not reintroduce it on GitHub; it was removed from remote `main` at the user's request.

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

- Commissioner: create teams, edit team details through the API, manage rosters, schedule games, enter/correct scores, resolve score disputes, and manage the tournament. Team editing does not yet have a frontend control.
- Manager: manage the assigned team's roster, submit game scores, confirm or dispute opposing submissions, and view league information.
- Players with verified email accounts can submit join requests and view their own history. Public visitors have read-only access.
- Role restrictions are enforced by the Java backend as well as by visible controls.

## Next story assignments

The November 17 user-story document in the parent CS 498 folder records claims beside story headings. Preserve Kiara's, Rudra's, and Ryan's existing claims.

- US-13, Edit my team ([issue #14](https://github.com/narcoticneso/AmateurLeagueManager/issues/14)): Luke (Lucas) owns the full story, including frontend and backend. Start with the editing UI and permission checks for a manager's own team; league changes and season changes retain their existing dependencies.
- US-09, View my team as a manager ([issue #10](https://github.com/narcoticneso/AmateurLeagueManager/issues/10)): Nebojsa. The existing team page now shows league rank from the standings order alongside the name, roster, and season record. For this simple implementation, "status" means season activity derived from the team's games: no games scheduled, awaiting first result, season underway, or no upcoming games. Scheduled and postponed games count as remaining; cancelled games do not. This does not represent league membership approval or playoff qualification. If membership status is required later, coordinate with Rudra's US-16.
- US-05, Request to join a team ([issue #6](https://github.com/narcoticneso/AmateurLeagueManager/issues/6)): Nebojsa. Implemented on `nebojsa-dev`: persisted requests, available-team selection, private status history, and an assigned-manager pending inbox. Uses real email accounts and existing manager memberships. Duplicate pending requests and requests from existing members are rejected. Requests do not change memberships or rosters; decisions remain Luke's US-14. See README for local testing and endpoint/schema details.
- US-14, Approve join requests ([issue #15](https://github.com/narcoticneso/AmateurLeagueManager/issues/15)): Luke (Lucas), with a backend emphasis on persisted requests, authorization, and roster updates.
- Ryan's account/membership foundation ([PR #44](https://github.com/narcoticneso/AmateurLeagueManager/pull/44)) was merged when checked on November 20 and integrated into `nebojsa-dev`. US-05 builds on it. Byteful's PR #45 was merged into main as narcoticneso, then integrated on nebojsa-dev; roles now derive from memberships and there is no view switcher. US-05/US-09 must remain on nebojsa-dev until the user requests their merge. Full US-14 acceptance also depends on participant profiles and in-app notifications.
- All 34 original stories were verified to have GitHub issues. These claims were added to the document; GitHub assignees were not changed.

## Current implementation

- Java 21, Spring Boot 4.0.9, Maven wrapper, Spring Security sessions and CSRF protection, Spring JDBC, and H2 with Flyway migrations. The default profile uses a persistent local database; the `dev` profile uses an in-memory database recreated at each startup.
- Maven downloads Node.js and builds the frontend, so only Java has to be installed. TypeScript types for the API are generated from the Java code on every build.
- Code checks are strict and enforced by `mvnw verify`: Prettier formats both Java (prettier-plugin-java) and the frontend (`mvnw -Pformat`); Checkstyle (braces required) and Error Prone with warnings as errors for Java; strict TypeScript, svelte-check, and ESLint (`strictTypeChecked`, braces required) for the frontend.
- Frontend unit and browser tests are out of scope.
- Public dashboard, schedules/results, teams/rosters, standings, and postseason cup pages.
- Fictional Oakwood Football sample season with six teams and basic player rosters.
- Standings derive from final regular-season results; ties count as half a win. Tournament results do not affect regular-season standings.
- A four-team cup with two semifinals and one championship. The commissioner schedules the championship between the semifinal winners.
- League branding and season labels are currently fixed. All accounts use stored memberships; seeded developer accounts use email logins, and player memberships also come from linked roster entries.
- Score submission, confirmation, dispute, and commissioner resolution are implemented. A scheduled task checks every minute for pending submissions older than 24 hours to auto-confirm. The integration suite covers manager score submission permissions; the full confirmation/dispute workflows are not covered.
- Seven integration tests passed during the initial implementation, and browser checks covered desktop/mobile layouts, login, role restrictions, saving games, and duplicate jersey validation. Run checks as appropriate when behavior changes; this is historical validation, not a guarantee of future edits.
- November 20 validation for US-05: full Maven `verify` passed (16 tests plus frontend types/build, ESLint, Prettier, and Java checks). Browser checks on an isolated temporary database covered submitting a request, retaining it after reload/sign-in, manager delivery, and the phone layout. US-09 was committed and pushed to `nebojsa-dev` as `narcoticneso`. US-05 is implemented on the same branch; approval/denial remains US-14.
- Integration with PR #45: `clean verify` passes all 16 tests. Browser checks confirm participants without a team can request one, history survives reload, and assigned managers see requests without switching views. Main's `V3__derived_roles.sql` is preserved; the join-request migration is now `V4__team_join_requests.sql`. An older development database that already applied `V3__team_join_requests.sql` needs a backed-up, one-time history reconciliation before use. On the original development machine this upgrade was tested on a copy, applied locally, and verified to preserve every account, team, roster, membership, and request. The original backup and upgrade logs are in ignored `.tools/account-integration/`. A copy also started successfully with normal Flyway settings afterward.

## Local setup

- On this machine, `dev.ps1` uses the portable Java/Maven installations in ignored `.tools/` when needed.
- On another machine, install Java 21 and run `mvnw.cmd spring-boot:run` on Windows or `sh ./mvnw spring-boot:run` on macOS/Linux. See the README for download links and setup steps.
- For frontend work, `mvnw spring-boot:run -Pdev` also starts the Vite dev server on port 5173.
- The configured default is `http://localhost:8080`; the `PORT` environment variable can override it. On November 3, the user's running server was verified on **8081**. Check the actual listener/startup output before diagnosing a connection issue.
- Keep the server terminal open while using the website. Do not stop a server the user started merely to clean up a troubleshooting task.
- Local demo accounts: `rudra@example.com`, `ryan@example.com`, `lucas@example.com`, `nebojsa@example.com`, `kiara@example.com`. The documented demo password is `neighborhood-demo`; `ALM_PASSWORD_<NAME>` overrides it per account.
- `.tools/`, `data/`, `target/`, `frontend/node/`, `frontend/node_modules/`, logs, and `.env` remain ignored. Do not stage them or suggest including them in a commit.

## Deferred work

Configurable league/season settings, a team-editing UI, join-request decisions and approved roster updates (US-14), reminders, historical seasons, uploads, detailed player statistics, and automatic scheduling remain deferred. Team editing is implemented in the API; score confirmation and disputes are implemented in both the API and frontend. User accounts, manager memberships, roster account links, and join-request submission/history are implemented.

No project license has been selected. MIT was recommended as an option, but the team has not confirmed it. Do not add a license based on the recommendation alone.
