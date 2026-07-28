# 04 — Panel recycle

**Category:** special recipe (code)

Break a puzzle panel back down into [tablets](00-ancient-puzzle-tablet.md). This is what keeps the
tablet economy conservative: a panel returns exactly what was invested in it.

## Rule

A **lone** puzzle panel in the crafting grid (nothing else) yields Ancient Puzzle Tablets equal to
the panel's `witness:cost`. Panels that predate the `cost` component (legacy / compat) default to
**4** tablets.

## Edge cases

- Match requires exactly one non-empty slot, and it must be a `PuzzlePanelItem`.
- **Inverse of the build rate.** A panel built for *N* tablets recycles to *N*, so building then
  recycling is a no-op on your tablet count — see [the cost model](README.md#cells-nodes-and-cost).
- **Cost is path-dependent**, so two identical-looking panels can pay out different amounts: see
  [the convenience premium](01-puzzle-panel-crafting.md#the-convenience-premium). Recycle doesn't
  care. It refunds what was invested on whichever route was taken, which is what keeps every route
  conservative. The most any legal panel can be worth is 28, so a refund always fits in one stack.
- The default-4 fallback means recycling a legacy panel can under- or over-pay relative to its true
  size; only affects panels created before the `cost` component existed.

## Status in this mod

Implemented and registered (serializer forced live via `PanelDyeRecipe.init()`). Lives in the same
file as [panel dye](02-panel-dye.md).

## Sources

- `src/main/kotlin/com/xfastgames/witness/recipes/PanelDyeRecipe.kt` — `PanelRecycleRecipe`.
- `src/main/resources/data/witness/recipe/panel_recycle.json` — the recipe stub.
