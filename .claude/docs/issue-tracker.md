# Issue tracker: GitHub

Issues and specs for this repo live as GitHub issues in `Sonophage/echo-launcher`. Use the `gh` CLI for all operations.

## Conventions

- **Create an issue**: `gh issue create --title "..." --body "..." --label ready-for-agent`. Use a heredoc for multi-line bodies.
- **Read an issue**: `gh issue view <number> --comments`.
- **List issues**: `gh issue list --state open --label ready-for-agent --json number,title,labels`.
- **Comment on an issue**: `gh issue comment <number> --body "..."`
- **Apply / remove labels**: `gh issue edit <number> --add-label "..."` / `--remove-label "..."`
- **Close**: `gh issue close <number> --comment "..."`

## When a skill says "publish to the issue tracker"

Create a GitHub issue.

## When a skill says "fetch the relevant ticket"

Run `gh issue view <number> --comments`.

## Tickets under a spec

- **Parent**: link each ticket to its spec as a sub-issue: `gh api --method POST repos/Sonophage/echo-launcher/issues/<spec>/sub_issues -F sub_issue_id=<ticket-db-id>`. Where that fails, put `Part of #<spec>` at the top of the ticket body.
- **Blocking**: GitHub's native issue dependencies: `gh api --method POST repos/Sonophage/echo-launcher/issues/<ticket>/dependencies/blocked_by -F issue_id=<blocker-db-id>`. The db id is `gh api repos/Sonophage/echo-launcher/issues/<n> --jq .id`, not the `#number`. Where that fails, put `Blocked by: #<n>, #<n>` at the top of the ticket body.
- **Frontier**: open tickets under the spec with no open blocker.
- **Done**: a ticket closes when its commit lands (`Closes #<n>` in the PR body, or `gh issue close`).
