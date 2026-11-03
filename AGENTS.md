# Project instructions

## Git ownership

The user performs all staging, commits, and pushes personally.

- Never run `git add`, `git commit`, or `git push`, or perform equivalent staging, committing, or publishing actions through an IDE, browser, connector, script, or other tool.
- Treat requests such as "let's commit this" as requests for instructions, not authorization to execute Git changes.
- Explain which files to stage, suggest a commit message, and provide the exact commit and push commands for the user to run.
- Read-only Git inspection is allowed, including status, diff, log, branch, and remote inspection. Verify the current branch and remote before recommending a push command.
- Do not amend commits, rewrite history, or alter the Git index as a substitute for committing. Leave repository changes for the user to review and manage.
- Editing source files, updating documentation, and running appropriate checks remain allowed within the requested task.

## Project context

Read `docs/PROJECT_CONTEXT.md` for the agreed scope and development setup. Keep it current when a project decision changes. Use `README.md` for end-user setup and running instructions.

Keep the implementation straightforward and suitable for the team's Java experience. Do not expand features or introduce extra languages/services without a task-driven reason. Do not describe the project publicly as a senior project or as a CS 498 assignment.
