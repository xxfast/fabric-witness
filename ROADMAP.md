# ROADMAP.md

Sequenced plan for fabric-witness. See `GOALS.md` for the destination and `MIGRATION.md` for the
1.17 → 1.21.11 migration that got us to the current baseline.

Phases are ordered by dependency, not calendar. Each item links back to a `GOALS.md` pillar or a row
of its fidelity table. Checkboxes track state; unchecked ≠ started.

---

## Phase 0 — Stabilize the migration

**Goal:** get the 1.21.11 build to a known-good, fully-verified state so later phases build on solid
ground instead of chasing migration ghosts. Nothing here advances new puzzle rules; it pays down the
debt from the port. Ends with a clear-eyed read of what in the codebase actually serves `GOALS.md`.

### 0.1 — Verify before fixing
Work the **"Still needs in-game verification"** checklist in `MIGRATION.md` first, so we fix what's
actually broken rather than what we assume is. Run `./gradlew runClient` and confirm each:

- [ ] Panel rendering in frames and the composer (command-queue path: geometry, lighting, z-fighting)
- [ ] Composer end-to-end: slot-sync payload, dye tinting, editor clicks vs. the 2D preview
- [ ] Solver: raycasting (JOML rewrite), line tracing, sounds, mouse hide/unlock
- [ ] All four recipes: grid crafting (component JSON decode), panel dye, panel recycle, stonecutting
- [ ] Worldgen JSONs validate on datapack load (inert until injected)
- [ ] Dedicated-server boot (`./gradlew runServer`) — client/server split, handler registration timing

Capture results inline in `MIGRATION.md` (tick the checklist, note anything newly broken). Only then
open fix tasks below.

### 0.2 — Fix known breakages (`MIGRATION.md` "Known broken / not ported")
Deliberately **excludes item 2** (pre-1.20.5 save compat) — a `GOALS.md` non-goal.

- [ ] **#1 — In-hand / on-ground puzzle-panel rendering.** Port to a data-driven `special` item
  model + `SpecialModelRenderer` (the `BuiltinItemRendererRegistry` path is gone since 1.21.4).
  Restore the custom first-person arm pose if feasible. *(Pillar 2: puzzles as diegetic blocks.)*
- [ ] **#3 — Composer editor preview is 2D-flat.** Decide: (a) render the real textured panel into
  the GUI via the 1.21.6+ pipeline, or (b) keep the 2D painter but make it faithful (node/edge/
  modifier styling that matches the frame renderer). Recommendation: (b) short-term, revisit (a)
  only if the flat preview proves confusing in playtesting. *(Pillar 2.)*
- [ ] **#4 — Recipe-book entries missing for dye/recycle.** Inherent to `SpecialCraftingRecipe`.
  Fix = surface them another way (a JSON `crafting_special_*` display recipe, or a custom recipe-book
  category) or consciously accept parity with vanilla firework behavior and close it. Decide, don't
  leave dangling.
- [ ] **#5 — `OakLeavesRunners` item tint dropped; `PinkCedarLeaves` has no leaf-fall particles.**
  Move the tint to a `tintindex` + tint source in the item model JSON; wire leaf particles via the
  block's `randomDisplayTick`. *(Migration-parity fix for existing decoration blocks.)*
- [ ] **#6 — Validate the minor behavior swaps.** `player.blockInteractionRange` for solver raycast
  and `onClose → removed()` cleanup are already swapped in code; confirm they behave correctly during
  0.1 and close, or file a follow-up if they don't.

### 0.3 — Codebase alignment review
Read the codebase against `GOALS.md` and record what serves the north star, what's half-built, and
what's dead weight. Seed findings (from the initial pass — verify each before acting):

- **The solver validates no rules.** `PuzzleSolverDomain` is effectively a stub (`startTracingLine`
  adds the start node; `introduceWaypoint` is empty; success = line reached an `END` node). This is
  the central gap between the mod today and `GOALS.md`. Phase 1 is its remedy — flag, don't patch
  ad-hoc here.
- **`Modifier.BREAK` / `Modifier.DOT` exist but are never enforced.** Data-model vocabulary without
  solver semantics. Aligns; needs Phase 1 to mean anything.
- **`Panel.Tree` / `Panel.Freeform` and `resize()` / `grow()` / `shrink()` are `TODO()` stubs.**
  Decide per type: is Tree/Freeform on the fidelity path, or speculative? If speculative, consider
  removing to shrink surface area until needed. **Caveat:** `GOALS.md` cites the sealed
  `Grid | Tree | Freeform` as the reason the face model must be topology-agnostic, and Phase 1
  leans on it ("all get it for free") — removal doesn't break that (the face concept can stay
  topology-agnostic with only Grid implemented), but if removed, update both framings so the
  decision is made against `GOALS.md`, not as local cleanup.
- **`PuzzleSolverScreen` carries several `TODO`s** (post-guava-traverser line tracing, transform/
  rotation math, sweet-spot sizing). These are correctness risks for Pillar 1 "feel"; fold concrete
  bugs found in 0.1 into fix tasks.
- Output: a short "Alignment" section appended to this file (or an issue list) — not code changes.

**Phase 0 exit criteria:** every 0.1 box ticked, every 0.2 item either fixed or consciously closed
with a note, and 0.3 findings written down. Build green (`./gradlew build`), server boots.

---

## Phase 1 — Puzzle foundations (the face/region model)

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
- [ ] Wire real validation into `PuzzleSolverDomain` (replace "reached END" with "solution valid").
- [ ] Solver "feel" pass: fix the `PuzzleSolverScreen` tracing/transform TODOs so the line snaps and
  reads like the source game (Pillar 1).

## Phase 2 — Core cell symbols

- [ ] **Squares** — separation by color (regions may not contain two square colors).
- [ ] **Stars** — exactly two elements of the star's color per region.
- [ ] Composer + renderer support for placing/showing these symbols on faces (cells on a grid).

## Phase 3 — Advanced cell symbols

- [ ] **Tetris / polyominoes** — region area must tile to the shapes; include subtractive (blue) and
  rotatable variants.
- [ ] **Triangles** — line must border exactly N sides of the triangle's cell.
- [ ] **Elimination marks** — cancel exactly one rule violation in the region.

## Phase 4 — Line variants

- [ ] **Symmetry puzzles** — two mirrored lines, shared validation.
- [ ] **Colored lines** and dual-symmetry interactions.

## Phase 5 — Progression & polish

- [ ] Progression tie-ins: panels gating doors/structures; a survival acquisition path for panels and
  the composer. *(Pillar 2.)*
- [ ] Decoration/worldgen: only as it serves a puzzle scene — e.g. enabling the inert worldgen
  (`BiomeModifications.addFeature`) *if* panels want a natural habitat. Not a content-breadth push;
  see `GOALS.md` non-goals.
- [ ] The deferred **MC 1.21.11 → 26.2** engine hop (`MIGRATION.md` notes: Java 25, Mojang mappings,
  ModMenu beta) — schedule once the puzzle work stabilizes.

> **Far future, out of scope here:** a companion map/datapack that rebuilds The Witness's island on
> top of this mod. Tracked as an idea in `GOALS.md` non-goals, not a phase.

---

*Phases 1–5 are direction, not commitment. Re-scope them as Phase 0 and playtesting teach us what
the mod actually needs.*
