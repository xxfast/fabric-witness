# Phase 0 — Stabilize the migration

← [ROADMAP index](../ROADMAP.md) · [GOALS](../GOALS.md) · [MIGRATION](../MIGRATION.md)

**Goal:** get the 1.21.11 build to a known-good, fully-verified state so later phases build on solid
ground instead of chasing migration ghosts. Nothing here advances new puzzle rules; it pays down the
debt from the port. Ends with a clear-eyed read of what in the codebase actually serves `GOALS.md`.

## 0.1 — Verify before fixing
Work the **"Still needs in-game verification"** checklist in `MIGRATION.md` first, so we fix what's
actually broken rather than what we assume is. Run `./gradlew runClient` and confirm each:

- [x] Panel rendering in frames 
- [x] Panel rendering in the composer (command-queue path: geometry, lighting, z-fighting)
- [x] Composer end-to-end: slot-sync payload, dye tinting, editor clicks vs. the 2D preview
- [ ] Solver: raycasting (JOML rewrite), line tracing, sounds, mouse hide/unlock
- [ ] All four recipes: grid crafting (component JSON decode and component-preserving upgrades),
  panel dye, panel recycle, stonecutting
- [ ] Worldgen JSONs validate on datapack load (inert until injected)
- [ ] Dedicated-server boot (`./gradlew runServer`) — client/server split, handler registration timing

Capture results inline in `MIGRATION.md` (tick the checklist, note anything newly broken). Only then
open fix tasks below.

## 0.2 — Fix known breakages (`MIGRATION.md` "Known broken / not ported")
Deliberately **excludes item 2** (pre-1.20.5 save compat) — a `GOALS.md` non-goal.

- [x] **#1 — In-hand / on-ground puzzle-panel rendering.** Restored with a `SpecialItemModel` and
  `SpecialModelRenderer` installed through Fabric's item-model bake hook. The live `witness:panel`
  component now renders in GUI, first-/third-person, and ground contexts. The old custom arm pose
  was not restored; the vanilla item-holding pose is retained. *(Pillar 2: puzzles as diegetic
  blocks.)*
- [x] **#3 — Composer editor preview is 2D-flat.** Chose (b): the click-aligned 2D painter remains,
  but now shares the frame renderer's dyed backdrop and graph/solution textures, and mirrors its
  junction, endpoint, start, break, hidden, and solution styling. Revisit a real 3D GUI render only
  if the faithful flat preview proves confusing in playtesting. *(Pillar 2.)*
- [x] **#4 — Recipe-book entries missing for dye/recycle.** Kept the component-preserving
  `SpecialCraftingRecipe` implementations, but opted them into the 1.21 recipe-display API with
  explicit placement ingredients and shapeless displays. Both unlock when the player obtains a
  puzzle panel; dye cycles through all dye inputs, while recycle displays the default four-tablet
  return (the crafted count still follows the panel's stored cost). *(Migration-parity fix.)*
- [x] **#7 — Grid upgrade recipes used NbtCrafting expressions in vanilla component JSON.** Vanilla
  can decode literal `witness:cost` values only; it cannot evaluate the old `$ iN.cost + N`
  expressions or copy a source panel's tint. Replaced the upgrade variants with one
  component-aware special recipe that accepts the legacy layouts, requires the expected grid size
  and cost component, rebuilds the target grid, preserves the source background colour and other
  stack components, and adds the consumed tablet count to the cost. The base-grid recipes remain
  ordinary shaped JSON. *(Migration-parity regression.)*
- [ ] **#5 — `OakLeavesRunners` item tint dropped; `PinkCedarLeaves` has no leaf-fall particles.**
  Move the tint to a `tintindex` + tint source in the item model JSON; wire leaf particles via the
  block's `randomDisplayTick`. *(Migration-parity fix for existing decoration blocks.)*
- [ ] **#6 — Validate the minor behavior swaps.** `player.blockInteractionRange` for solver raycast
  and `onClose → removed()` cleanup are already swapped in code; confirm they behave correctly during
  0.1 and close, or file a follow-up if they don't.

## 0.3 — Codebase alignment review
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
- Output: a short "Alignment" section (here or a separate note) — not code changes.

## Exit criteria
Every 0.1 box ticked, every 0.2 item either fixed or consciously closed with a note, and 0.3 findings
written down. Build green (`./gradlew build`), server boots.
