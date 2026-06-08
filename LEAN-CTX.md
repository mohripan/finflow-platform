# Lean Context Notes

This repository is currently documentation-only. Before making implementation decisions, read `AGENTS.md` and the relevant product, architecture, boundary, API, event, roadmap, and ADR documents.

Project-specific context rules:

- Treat the ledger as the financial source of truth.
- Keep service ownership boundaries explicit.
- Do not add placeholder endpoints, fake workflow data, or mock production paths.
- Update docs and ADRs when behavior, APIs, events, service ownership, or business rules change.
- Prefer small vertical slices that can be verified end to end.

When implementation starts, follow:

- `docs/REPO_STRUCTURE.md` for the intended monorepo layout.
- `docs/IMPLEMENTATION_START.md` for the first build sequence.
