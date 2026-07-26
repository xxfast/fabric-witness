# 02 — Grid upgrade

**Category:** special recipe (code)

Place an existing puzzle panel plus some Ancient Puzzle Tablets in the crafting grid and get a
**larger** panel back, keeping the original's background colour and raising its cost. It exists as
code, not JSON, because the result depends on the *input panel's data* (its size, colour, and cost)
— a static shaped recipe can't read those. It replaces the old nbtcrafting grid-expansion recipes.

All sizes below are in **nodes** (the `Panel.Grid.width`/`height` fields), matching the code. One
node grid of `W×H` is a `(W−1)×(H−1)`-cell puzzle. See the
[cost vs. cells vs. nodes model](README.md#cells-nodes-and-cost) and
[00-ancient-puzzle-tablet.md](00-ancient-puzzle-tablet.md).

## What counts as a valid upgrade

`upgrade()` (in `PanelGridUpgradeRecipe.kt`) accepts a grid only when **all** of these hold:

1. **Exactly one** occupied slot holds a `PuzzlePanelItem` (the *source*).
2. Every **other** occupied slot holds an `AncientPuzzleTablet` — no stray items.
3. The source panel is a `Panel.Grid` and has a `witness:cost` component.
4. The occupied slots form a **filled rectangle** — their bounding box has no empty cells inside it
   (`Bounds.isFilledBy`).
5. The `(source size, tablet count, footprint size, source position within the footprint)` tuple
   matches a row in the layout table below.

If any check fails, `matches()` returns false and the grid produces nothing.

## Cost rule

```
new cost = old cost + number of tablets placed
```

Cost counts **tablets invested**, not the resulting cell count. Because one tablet can extend the
grid by a whole row or column (which may be several cells), an upgraded panel is often *cheaper*
than an equivalently-sized panel's cell count would imply — e.g. a 3×3 (cost 4) plus 3 tablets
becomes 4×4 nodes (9 cells) but costs 7, not 9. This is **not** a tablet exploit:
[recycle](04-panel-recycle.md) returns exactly `cost` tablets, so you always get back what you put in.

## The transformation table

The full set of accepted upgrades, keyed by how many tablets you add. This is a hardcoded whitelist
(`PanelGridUpgradeLayouts`), not a general formula.

### 1 tablet — grow by one row or column

The source panel and one adjacent tablet form a 1×2 or 2×1 footprint. The tablet extends the grid on
that side.

| Source | Footprint       | → Result |
|--------|-----------------|----------|
| 2×2    | 1 wide × 2 tall | 2×3      |
| 2×2    | 2 wide × 1 tall | 3×2      |
| 2×3    | 1×2             | 2×4      |
| 2×3    | 2×1             | 3×3      |
| 3×2    | 1×2             | 3×3      |
| 3×2    | 2×1             | 4×2      |
| 2×4    | 2×1             | 3×4      |
| 4×2    | 1×2             | 4×3      |
| 3×3    | 1×2             | 3×4      |
| 3×3    | 2×1             | 4×3      |
| 3×4    | 2×1             | 4×4      |
| 4×3    | 1×2             | 4×4      |

Example — a 2×2 panel (`P`) and one tablet (`T`) side by side → a 3×2 panel:

```
┌───┬───┬───┐
│ P │ T │   │
├───┼───┼───┤   →   3×2 panel, cost = old + 1
│   │   │   │
├───┼───┼───┤
│   │   │   │
└───┴───┴───┘
```

### 3 tablets — 3×3 → 4×4

Source panel plus 3 tablets in a 2×2 footprint (source in any corner):

```
┌───┬───┐
│ P │ T │   P = 3×3 panel
├───┼───┤   → 4×4 panel, cost = old + 3
│ T │ T │
└───┴───┘
```

### 8 tablets — 2×2 → 4×4

A 2×2 panel in the **centre** of a 3×3 ring of tablets. The centred position is required
(`sourceX == 1 && sourceY == 1`); a 2×2 source anywhere else in a 3×3 block is rejected.

```
┌───┬───┬───┐
│ T │ T │ T │
├───┼───┼───┤
│ T │ P │ T │   →   4×4 panel, cost = old + 8
├───┼───┼───┤
│ T │ T │ T │
└───┴───┴───┘
```

## Limits worth knowing

- **The ceiling is 4×4.** Every path tops out there — there is no 4×4 → 5×n, no 5×5, nothing
  larger. Lifting this ceiling is the point of "expand to bigger grids."
- **No direct 2×2 → 3×3.** A 2×2 can only become 2×3 or 3×2 in one craft; reach 3×3 in two steps,
  or craft it straight from tablets with the static 3×3 base recipe.
- **Not every source/side combination exists.** The table is a hand-picked set mirroring the legacy
  recipes, not the closure of "add a row anywhere." For instance 2×4 grows only on its 2-wide side.

## Implementation notes

Three things describe each upgrade and **must stay in sync** — this is the main cost of extending
the table by hand:

1. **Matching** — `PanelGridUpgradeLayouts.target()` → `singleTabletTarget()` plus the 3- and
   8-tablet special cases. This is the source of truth `upgrade()` consults.
2. **Recipe-book / JEI display** — the `PanelGridUpgradeLayouts.displays` list of `DisplayLayout`s,
   rendered by `getDisplays()`. Each derives `tabletCount = layoutW·layoutH − 1` and
   `inputCost = (sourceW−1)·(sourceH−1)` so previews show believable numbers.
3. **Tests** — the `cases` list in `PanelGridUpgradeRecipeTests`, which asserts every layout maps to
   its documented target and that `displays` has the same length.

Geometry itself is not a blocker for bigger grids: `craft()` builds the result with
`Panel.Grid.ofSize(w, h)`, which regenerates the whole node graph from scratch, so the stubbed
`Panel.Grid.grow`/`shrink` TODOs (`Panel.kt`) are never on this path.

To make expansion a *formula* instead of a table, the regular pattern to capture is: a tablet strip
of size *k* on one side turns an `a×b` grid into `(a+1)×b` or `a×(b+1)`, adding *k* to the cost —
which generalises the 1-, 3-, and 8-tablet cases into one rule over the footprint and source
position.

## Status in this mod

Implemented and registered (serializer forced live via `PanelDyeRecipe.init()`). Fully functional
up to the 4×4 ceiling; visible in the recipe book via `getDisplays()`. The hardcoded table is the
known limitation for supporting bigger grids.

## Sources

- `src/main/kotlin/com/xfastgames/witness/recipes/PanelGridUpgradeRecipe.kt` — `upgrade()`,
  `craft()`, `getDisplays()`, and the `PanelGridUpgradeLayouts` table.
- `src/test/kotlin/com/xfastgames/witness/recipes/PanelGridUpgradeRecipeTests.kt` — the authoritative
  list of accepted layouts and their targets.
- `src/main/kotlin/com/xfastgames/witness/items/data/Panel.kt` — `Panel.Grid.ofSize`, node geometry.
- `src/main/resources/data/witness/recipe/puzzle_panel_grid_upgrade.json` — the recipe stub.
