# ROADMAP.md

Sequenced plan for fabric-witness. See [`GOALS.md`](GOALS.md) for the destination and
[`MIGRATION.md`](MIGRATION.md) for the 1.17 → 1.21.11 migration that got us to the current baseline.

Phases are ordered by dependency, not calendar. Each phase lives in its own file under the
`roadmap/` directory (linked below); every item there links back to a `GOALS.md` pillar or a row of
its fidelity table. Checkboxes track state; unchecked ≠ started.

## Phases

| Phase                                                                     | Focus                                                                             | Depends on |
|---------------------------------------------------------------------------|-----------------------------------------------------------------------------------|------------|
| **[0 — Stabilize the migration](roadmap/phase-0-stabilize-migration.md)** | Verify the 1.21.11 port, fix known breakages, review codebase alignment           | —          |
| **[1 — Puzzle foundations](roadmap/phase-1-foundations.md)**              | The topology-agnostic face/region model + validation framework (the pivotal phase) | 0          |
| **[2 — Core cell symbols](roadmap/phase-2-core-symbols.md)**              | Squares (separation), stars                                                       | 1          |
| **[3 — Advanced cell symbols](roadmap/phase-3-advanced-symbols.md)**      | Tetris/polyominoes, triangles, elimination                                        | 1, 2       |
| **[4 — Line variants](roadmap/phase-4-line-variants.md)**                 | Symmetry, colored lines                                                           | 1          |
| **[5 — Progression & polish](roadmap/phase-5-progression.md)**            | Panels gating structures, worldgen, the 26.2 engine hop                           | 1          |

---

*Phases 1–5 are direction, not commitment. Re-scope them as Phase 0 and playtesting teach us what
the mod actually needs.*
