# Phase 1 — Puzzle foundations (the face/region model)

← [ROADMAP index](../ROADMAP.md) · [GOALS](../GOALS.md)

**The pivotal phase.** Everything past the line-drawing basics depends on it. See `GOALS.md`
"the one architectural fact" — this must be **topology-agnostic** (grid, tree, freeform), not
grid-only.

- [ ] Introduce a **face** concept (the bounded areas of the planar panel graph — cells on a grid,
  their equivalents on tree/freeform) and a **region** solver that partitions the panel by the drawn
  line. Define it once against `Panel`'s graph so `Grid`/`Tree`/`Freeform` all get it for free.
- [ ] Model **symbols attached to faces** in the data model (GOALS: no such representation exists
  today). Decide the serialization here too — `Panel.CODEC` wraps legacy NBT and recipe JSONs embed
  the same shape — so Phase 2+ symbols don't each retrofit the format.
- [ ] Build a **validation framework**: a solution is valid iff every rule predicate passes — split
  into **line-path predicates** (over the line) and **region predicates** (over the partitioned
  faces). Design it so each future symbol is one pluggable predicate.
- [ ] Enforce the three line-path rules that need no regions: **no self-intersection** (closes the
  "partial" continuous-line row in the fidelity table), **broken edges** (`BREAK`), and
  **hexagon dots** (`DOT`, line must pass over) — first real fidelity wins, and they exercise the
  framework without waiting on the region solver.
- [ ] Wire real validation into `PuzzleSolver` (replace "reached END" with "solution valid").
- [ ] Solver "feel" pass: fix the `PuzzleSolverScreen` tracing/transform TODOs so the line snaps and
  reads like the source game (Pillar 1).
