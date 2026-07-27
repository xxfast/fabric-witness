# 02 — Grid upgrade

**Category:** special recipe (code)

Place an existing puzzle panel plus some Ancient Puzzle Tablets in the crafting grid and get a
**larger** panel back, keeping the original's background colour and raising its cost.

This is the **only** way to get a panel bigger than 3×3 cells. Base recipes
([01](01-puzzle-panel-crafting.md)) are capped at 3×3 cells forever, because a crafting pattern is
at most 3×3 slots.

Sizes below are in **cells** (the drawable squares) and costs are in tablets. See
[the cost model](README.md#cells-nodes-and-cost) for how cells, nodes, and cost line up.

---

# Design

## The rule

Read the crafting grid as a **schematic** of the panel you want:

- the slot holding the panel is the panel you already have,
- every tablet slot next to it is one added row or column of cells.

The footprint ($f_w \times f_h$) is the bounding box of the occupied slots, and it has to be
completely filled. Below, the source panel is $w \times h$ cells and costs $c$ tablets:

```
  ├──── fw ───┤
  ┌───┬───┬───┐  ┬
  │ P │ T │ T │  │
  ├───┼───┼───┤  fh
  │ T │ T │ T │  │
  ├───┼───┼───┤  ┴
  │   │   │   │
  └───┴───┴───┘
```

$$P_{w \times h} \;+\; (f_w f_h - 1)T \;=\; P_{(w + f_w - 1) \times (h + f_h - 1)} \quad (\text{cost: } (c + f_w f_h - 1)T)$$

So each axis grows by one cell per extra slot along it, and the cost goes up by one per tablet.
Since the crafting grid is 3×3, one craft adds at most **+2 cells per axis** and costs at most **8
tablets**. Bigger panels are reached by repeating the craft.

An upgrade is accepted when all of these hold:

1. Exactly one slot holds a puzzle panel.
2. Every other occupied slot holds an Ancient Puzzle Tablet, no stray items.
3. The occupied slots form a filled rectangle, no gaps inside the bounding box.
4. The result is within the [size cap](#the-size-cap).

A 1×1-cell panel (`P`) and one tablet (`T`) side by side. Footprint is 2 wide, 1 tall, so it gains
one column:

```
┌───┬───┬───┐
│ P │ T │   │
├───┼───┼───┤
│   │   │   │
├───┼───┼───┤
│   │   │   │
└───┴───┴───┘
```

$$P_{1 \times 1} \;+\; 1T \;=\; P_{2 \times 1} \quad (\text{cost: } 2T)$$

An 8-tablet ring. Footprint is 3×3, so both axes gain 2 cells:

```
┌───┬───┬───┐
│ T │ T │ T │
├───┼───┼───┤
│ T │ P │ T │
├───┼───┼───┤
│ T │ T │ T │
└───┴───┴───┘
```

$$P_{4 \times 4} \;+\; 8T \;=\; P_{6 \times 6} \quad (\text{cost: } 33T)$$

## Where the panel sits in the footprint

The panel's position doesn't change the result's *size*, but it decides **which sides grow**, and so
where the old puzzle's content ends up in the new grid:

```
┌───┬───┬───┐        ┌───┬───┬───┐
│ P │ T │ T │        │ T │ T │ T │
├───┼───┼───┤        ├───┼───┼───┤
│ T │ T │ T │        │ T │ P │ T │
├───┼───┼───┤        ├───┼───┼───┤
│ T │ T │ T │        │ T │ T │ T │
└───┴───┴───┘        └───┴───┴───┘
 grows right+down     grows on all four sides
 old content lands    old content stays
 in the top-left      centred
```

Both layouts:

$$P_{4 \times 4} \;+\; 8T \;=\; P_{6 \times 6} \quad (\text{cost: } 33T)$$

Same size out, same cost, different placement. Laying out where the panel grows is the point of the
schematic, and it's what makes upgrading a composed puzzle worth doing: start points, end points,
hexagons and broken edges survive the craft and move with the anchor. The drawn solution line does
not, a partial line on a resized grid is meaningless.

## Cost

$$\text{new cost} = \text{old cost} + \text{tablets placed}$$

Cost counts **tablets invested**, not the resulting cell count, so large panels come at a **bulk
discount**: one tablet extends the grid by a whole row, which on a wide panel is many cells. That is
not an exploit, [recycle](04-panel-recycle.md) returns exactly `cost`, so you always get back what
you put in. It's a curve where the first panel is the expensive one.

The full chain from a 3×3-cell base panel to the cap:

| Craft | Footprint | Tablets | Result cells | Cost | Tablets per cell |
|-------|-----------|---------|--------------|------|------------------|
| base  | -         | 9       | 3×3          | 9    | 1.00             |
| +1    | 3×3       | 8       | 5×5          | 17   | 0.68             |
| +2    | 3×3       | 8       | 7×7          | 25   | 0.51             |
| +3    | 2×2       | 3       | 8×8          | 28   | 0.44             |

Four crafts and 28 tablets, about 3 ancient debris, for the biggest panel in the game.

The discount isn't a choice so much as a consequence: a craft can spend at most 8 tablets, one per
slot, while the area added grows with the panel's size. Pricing by resulting cell count instead
would hand back more tablets on recycle than were ever spent, which is a dupe. If the discount ever
feels too steep, the lever is a higher-denomination ingredient (a tablet block worth 9) rather than
a different formula.

## The size cap

**8×8 cells (9×9 nodes)** per axis, non-square shapes allowed up to the same limit. A layout whose
result would exceed it simply doesn't craft. Three independent reasons:

1. **Recycle pays out in one stack**, so a panel worth more than 64 tablets can't be refunded
   honestly. The cap keeps every reachable panel well under that: 28 at the ceiling.
2. **Legibility.** A bigger panel keeps its physical size on the block face, so the lines just get
   thinner. Past some density, tracing it with the solver stops being playable.
3. **Weight.** The whole puzzle graph travels with the item and syncs to every client that sees the
   block. 8×8 cells is 81 nodes and 144 edges.

Reason 2 is the one to playtest. If 8×8 is unreadable in the solver, drop the cap to 6×6 rather than
fighting the renderer.

## Edge cases

- **No feedback on a failed match.** Crafting can't explain itself. A gap in the footprint, or a
  result past the cap, just shows an empty output slot. The panel tooltip already prints size and
  cost, so at least the inputs are legible.
- **The recipe book can't show an infinite rule.** It shows a representative set of layouts, not
  every accepted one. Those previews illustrate the rule, they aren't the rule.
- **Panels predating the cost component can't be upgraded.** Recycle them at the default 4 first.
- **Dye is unaffected.** [03](03-panel-dye.md) copies the panel wholesale and never looks at size.

---

# Implementation

## Status in this mod

Implemented and registered (serializer forced live via `PanelDyeRecipe.init()`). The rule above is
live: `PanelGridUpgradeLayouts.target()` is the formula plus the cap, `Panel.Grid.expandTo()`
transplants the source's nodes and edges into the larger grid at the anchor, and `craft()` calls it.

It lives in code rather than JSON because the result depends on the input panel's own data (size,
colour, cost), which a static shaped recipe can't read. It replaced the old nbtcrafting
grid-expansion recipes, whose layouts survive as the first fourteen entries of `displays`.

**Known gaps**, all economy rather than mechanics:

- Multi-tablet footprints are priced above the single-tablet chain that achieves the same growth, so
  a player who works out the arithmetic will never use them.
- The base recipes in [01](01-puzzle-panel-crafting.md) are dominated by upgrade chains from a 1×1
  seed, which makes eight of the nine of them pointless.

Both are pricing consequences of the [cost rule](#cost) and are open design questions, not bugs.

## The old whitelist was a subset of the rule

Not a different rule, which is why replacing it changed no existing craft. Every one of its 14 rows
satisfies
$\text{result} = \text{source} + (\text{footprint} - 1)$ on each axis, including the 3-tablet and 8-tablet cases that read
as special. Sizes here are in nodes, matching the code:

| Source (nodes) | Source cells | Footprint | Formula → cells | Whitelisted target |
|----------------|--------------|-----------|-----------------|--------------------|
| 2×2 | 1×1 | 1×2 | 1×2 | 2×3 ✓ |
| 2×2 | 1×1 | 2×1 | 2×1 | 3×2 ✓ |
| 2×3 | 1×2 | 2×1 | 2×2 | 3×3 ✓ |
| 3×2 | 2×1 | 2×1 | 3×1 | 4×2 ✓ |
| 2×4 | 1×3 | 2×1 | 2×3 | 3×4 ✓ |
| 3×3 | 2×2 | 2×2 | 3×3 | 4×4 ✓ |
| 3×4 | 2×3 | 2×1 | 3×3 | 4×4 ✓ |
| 2×2 | 1×1 | 3×3 | 3×3 | 4×4 ✓ |

(The remaining rows are mirrors of these.) All 14 are still asserted verbatim in
`PanelGridUpgradeRecipeTests` as a regression net. What the whitelist did that the rule dropped:

- Topped out at 4×4 nodes.
- Rejected some source/side combinations arbitrarily, e.g. a 2×4 grew only on its 2-wide side, and a
  2×2 couldn't reach 3×3 in one craft (it can now, via a 2×2 footprint).
- Required the 8-tablet ring's panel to be dead centre (`sourceX == 1 && sourceY == 1`). Any position
  matches now, and position became the growth anchor instead.

## Why one slot can't pay more than one tablet

This is what forces the bulk discount. `CraftingResultSlot.onTakeItem` (1.21.11) decrements each
input slot by exactly 1, then *merges* the recipe remainder back into that slot, so returning a
reduced remainder increases the slot's count rather than decreasing it. Charging N tablets from one
slot needs a mixin, and the repo prefers not to add those. 8 tablets per craft is therefore a hard
vanilla ceiling.

## How matching works

`upgrade()` in `PanelGridUpgradeRecipe.kt` collects the occupied slots, requires a single
`PuzzlePanelItem` whose panel is a `Panel.Grid` with a `witness:cost` component, requires everything
else to be an `AncientPuzzleTablet`, and checks `Bounds.isFilledBy` for the filled rectangle. It then
asks `PanelGridUpgradeLayouts.target()` for the result size, which is the formula, the
`Panel.Grid.MAX_NODES` cap, a rejection of zero tablets (a lone panel belongs to
`PanelRecycleRecipe`), and a `tabletCount == layoutWidth * layoutHeight - 1` check so the footprint
has no holes.

## How the anchor maps to node indices

`Panel.Grid.expandTo(width, height, offsetX, offsetY)` builds the new graph and transplants the
source into it at an index offset, keeping node modifiers and edge modifiers, and treating a missing
source edge as deliberate rather than filling it in. The drawn line is dropped.

Two traps live here:

- **Re-centring.** A grid is centred inside the square its longest side describes, so node
  coordinates shift when the aspect ratio changes. The copy goes through `gridOffsets()` in index
  space rather than reusing raw floats.
- **Both axes are mirrored** between the crafting grid and node indices, so `anchorOffset()` is
  `footprint - 1 - sourcePosition` on x and y alike. The y flip is the obvious one (the crafting grid
  counts rows downwards, the panel renders `+y` up). The x flip is not: reading the render transforms
  suggests `+x` should draw right in a frame, and it does not. **Do not re-derive this from the
  matrices, it gives the wrong answer.** It was settled by crafting the four cases in game, in a
  frame and in the item icon, and the results are pinned in
  `PanelGridUpgradeRecipeTests.the anchor grows the panel towards the tablets`.

## Not done

- **Nothing tells a player why a craft failed.** Vanilla crafting has no channel for it. The advanced
  tooltip prints `Maximum size` on a capped panel, which is the only hint available.
- **`displays` is illustrative**, 17 entries for an infinite rule. Players reading it as a whitelist
  is a real risk with no clean fix.
- **8×8 legibility is unverified.** See the [size cap](#the-size-cap).

## Where the caps come from in code

- The recycle stack limit is `PanelRecycleRecipe.craft` returning
  `ItemStack(AncientPuzzleTablet.ITEM, cost)`, a single stack.
- The legibility limit is the solver and composer scaling by `maxOf(width, height)` and drawing the
  line at `4/16` of a cell.
- Geometry itself is not a blocker: `Panel.Grid.ofSize(w, h)` regenerates any size already.

## Sources

- `src/main/kotlin/com/xfastgames/witness/recipes/PanelGridUpgradeRecipe.kt` — `upgrade()`,
  `craft()`, `getDisplays()`, and the `PanelGridUpgradeLayouts` table to replace.
- `src/test/kotlin/com/xfastgames/witness/recipes/PanelGridUpgradeRecipeTests.kt` — the current list
  of accepted layouts and their targets.
- `src/main/kotlin/com/xfastgames/witness/items/data/Panel.kt` — `Panel.Grid.ofSize`, the `grow` stub,
  node geometry.
- `src/main/kotlin/com/xfastgames/witness/recipes/PanelDyeRecipe.kt` — `PanelRecycleRecipe`.
- `net.minecraft.screen.slot.CraftingResultSlot#onTakeItem` — the one-tablet-per-slot constraint.
- `src/main/resources/data/witness/recipe/puzzle_panel_grid_upgrade.json` — the recipe stub.
