# 01 — Puzzle panel crafting

**Category:** special recipe (code)

One rule builds every puzzle panel and grows every puzzle panel. Lay tablets out in the crafting
grid as a schematic of the panel you want, optionally with a panel you already have somewhere in it,
and get the panel that schematic describes.

Sizes below are in **cells** (the drawable squares) and costs are in tablets. See
[the cost model](README.md#cells-nodes-and-cost) for how cells, nodes, and cost line up.

---

# Design

## The rule

Read the crafting grid as a **schematic** of the panel you want:

- one slot is the **source**, the panel you are starting from,
- every other slot is one added row or column of cells.

A tablet **is** a 1×1-cell panel that cost 1 tablet. So the source is the puzzle panel if there is
one, and otherwise it is any one of the tablets. That single sentence is what makes building and
growing the same rule instead of two.

The footprint ($f_w \times f_h$) is the bounding box of the occupied slots, and it has to be
completely filled. With a source of $w \times h$ cells costing $c$ tablets:

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

Each axis grows by one cell per extra slot along it, and the cost goes up by one per tablet placed.
Since the crafting grid is 3×3, one craft adds at most **+2 cells per axis** and costs at most **8
tablets**. Bigger panels are reached by repeating the craft.

A craft is accepted when all of these hold:

1. **At most one** slot holds a puzzle panel.
2. Every other occupied slot holds an Ancient Puzzle Tablet, no stray items.
3. The occupied slots form a filled rectangle, no gaps inside the bounding box.
4. Something is being added. A lone panel with no tablets is [recycle](03-panel-recycle.md), not this.
5. The result is within the [size cap](#the-size-cap).

## Building from scratch

Fill a rectangle with nothing but tablets. One of them is the 1×1-cell seed and the rest grow it, so
the footprint *is* the panel:

```
"pattern": ["TT",     →   a blank 2×3-cell panel
            "TT",         cost: 6
            "TT"]
```

Substituting $w = h = c = 1$ into the rule collapses it to exactly that:

$$f_w f_h \, T \;=\; P_{f_w \times f_h} \quad (\text{cost: } f_w f_h \, T)$$

Every size a 3×3 crafting grid can describe:

| Cells     | Tablets | Cost |
|-----------|---------|------|
| 1×1       | 1       | 1    |
| 1×2 / 2×1 | 2       | 2    |
| 1×3 / 3×1 | 3       | 3    |
| 2×2       | 4       | 4    |
| 2×3 / 3×2 | 6       | 6    |
| 3×3       | 9       | 9    |

That ceiling of 3×3 cells from a single craft is permanent, because a crafting pattern is at most
3×3 slots. Everything larger is reached by feeding a panel back in.

## Growing a panel

Put the panel in the footprint instead of a tablet. A 1×1-cell panel and one tablet side by side,
footprint 2 wide and 1 tall, so it gains one column:

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

The source's position doesn't change the result's *size*, but it decides **which sides grow**, and so
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

When the source is a tablet the anchor has no effect, because a blank 1×1 seed has no content to
place. All the tablets in a from-scratch craft are interchangeable.

## Cost

$$\text{new cost} = \text{old cost} + \text{tablets placed}$$

Cost counts tablets **invested**, and [recycle](03-panel-recycle.md) hands back exactly `cost`, so
every route is conservative: you always get back what you put in, and no route can ever return more.

### The convenience premium

Cost is **path-dependent**, and deliberately so. Growing by $(a, b)$ cells needs a footprint
$(a+1) \times (b+1)$, so it costs:

$$(a+1)(b+1) - 1 \;=\; \underbrace{a + b}_{\text{the growth}} \;+\; \underbrace{ab}_{\text{the premium}}$$

The $ab$ term is zero whenever a footprint is **thin**, that is, whenever it only grows one axis. So
the rule a player can actually discover is one sentence:

> **Grow one axis at a time and you never pay a premium. The premium is for turning both axes in a
> single craft.**

Thin is not the same as slow. A 3×1 footprint costs 2 tablets for +2 columns, the same rate as two
separate 2×1 crafts, in half the crafts. Only footprints that are fat in *both* axes charge extra.

Two routes from nothing to the 8×8 cap:

| Route | Tablets | Crafts | Tablets per cell |
|-------|---------|--------|------------------|
| Thin strips, one axis at a time | 15 | 9 | 0.23 |
| Fat square footprints          | 28 | 5 | 0.44 |

Same panel either way. The thin route costs 13 fewer tablets, about 1.5 ancient debris, in exchange
for 4 more trips to the crafting table. Finding that is the reward for reading the formula, and
paying the premium is what you do when you can't be bothered.

Panels are also cheaper per cell the bigger they get, on any route, because one tablet extends a
whole row and a row on a wide panel is many cells. The first panel is the expensive one.

### Open question: the premium may be too cheap to skip

Crafting costs nothing but clicks in Minecraft, so once a player works out the thin route there is
no reason to ever pay the premium again, and fat footprints become dead content. 9 crafts against 5
is a small enough gap that this is probably fine, but it is the thing to watch in playtest. If it
needs a lever, raise the price of the *seed* rather than touching the formula, since changing the
formula is what reintroduces the two-rules problem this merge removed.

## The size cap

**8×8 cells (9×9 nodes)** per axis, non-square shapes allowed up to the same limit. A layout whose
result would exceed it simply doesn't craft. Three independent reasons:

1. **Recycle pays out in one stack**, so a panel worth more than 64 tablets can't be refunded
   honestly. The most expensive panel the rule can reach is 28, comfortably under.
2. **Legibility.** A bigger panel keeps its physical size on the block face, so the lines just get
   thinner. Past some density, tracing it with the solver stops being playable.
3. **Weight.** The whole puzzle graph travels with the item and syncs to every client that sees the
   block. 8×8 cells is 81 nodes and 144 edges.

Reason 2 is the one to playtest. If 8×8 is unreadable in the solver, drop the cap to 6×6 rather than
fighting the renderer.

## Edge cases

- **No feedback on a failed match.** Crafting can't explain itself. A gap in the footprint, or a
  result past the cap, just shows an empty output slot. The panel tooltip prints size and cost, so
  at least the inputs are legible.
- **The recipe book can't show an infinite rule.** It shows a representative set of layouts, not
  every accepted one. Those previews illustrate the rule, they aren't the rule.
- **Two identical-looking panels can be worth different amounts.** A 3×3 built in one craft costs 9
  and a 3×3 grown thinly costs 5. That is the premium, not a bug, and the tooltip is where a player
  tells them apart.
- **Panels predating the cost component can't be grown.** Recycle them at the default 4 first.
- **Dye is unaffected.** [02](02-panel-dye.md) copies the panel wholesale and never looks at size.

---

# Implementation

## Status in this mod

Implemented and registered (serializer forced live via `PanelDyeRecipe.init()`).
`PanelGridLayouts.target()` is the formula plus the cap, `Panel.Grid.expandTo()` transplants
the source's nodes and edges into the larger grid at the anchor, and `craft()` calls it.

The whole rule lives in code rather than JSON because the result depends on the source panel's own
data (size, colour, cost), which a static shaped recipe can't read.

## The seed case is the same code path

A from-scratch craft substitutes a virtual source rather than branching: a blank `Panel.Grid.ofSize(2, 2)`
at cost 1, with one tablet slot spent as the source. The layout maths is then called with
`tabletCount = occupied.size - 1` and is otherwise untouched, which is what makes the two halves of
the rule provably the same arithmetic rather than two implementations that happen to agree.

`target()` takes a `sourceIsPanel` flag purely to keep rule 4 (a lone panel is recycle's business)
where it belongs. With a real panel, zero tablets is rejected; with a seed, a 1×1 footprint is the
one-tablet craft and is accepted.

## The nine base recipes it replaced

`puzzle_panel_grid_*.json` were nine ordinary shaped recipes, each hardcoding its entire result panel
as literal `result.components` JSON: every node coordinate, every edge, the background colour, and
the cost. Deleting them changed no craft a player could make, because the formula reproduces all nine
exactly. Their node coordinates matched `Panel.Grid.ofSize` byte for byte, which is what let them go.

What that removed: 26 KB of hand-written graph JSON that could silently drift from the geometry code,
`1x2` and `2x1` needing to be separate files, and the `_a` filename suffix that was a naming slot for
alternate tablet arrangements of the same panel. Their layouts survive as displays so nothing
vanishes from the recipe book.

## The old whitelist was a subset of the rule

Before the formula, growth was a table of 14 accepted layouts. Not a different rule, which is why
replacing it changed no existing craft. Every one of its rows satisfies
$\text{result} = \text{source} + (\text{footprint} - 1)$ on each axis, including the 3-tablet and
8-tablet cases that read as special. Sizes here are in nodes, matching the code:

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
`PanelGridRecipeTests` as a regression net. What the whitelist did that the rule dropped:

- Topped out at 4×4 nodes.
- Rejected some source/side combinations arbitrarily, e.g. a 2×4 grew only on its 2-wide side, and a
  2×2 couldn't reach 3×3 in one craft (it can now, via a 2×2 footprint).
- Required the 8-tablet ring's panel to be dead centre (`sourceX == 1 && sourceY == 1`). Any position
  matches now, and position became the growth anchor instead.

## Why one slot can't pay more than one tablet

This is what forces the premium to be shaped the way it is. `CraftingResultSlot.onTakeItem` (1.21.11)
decrements each input slot by exactly 1, then *merges* the recipe remainder back into that slot, so
returning a reduced remainder increases the slot's count rather than decreasing it. Charging N
tablets from one slot needs a mixin, and the repo prefers not to add those. 8 tablets per craft is
therefore a hard vanilla ceiling, and cost can only ever be counted in slots.

## How matching works

`plan()` in `PanelGridRecipe.kt` collects the occupied slots, requires **at most one**
`PuzzlePanelItem` (whose panel must be a `Panel.Grid` with a `witness:cost` component), requires
everything else to be an `AncientPuzzleTablet`, and checks `Bounds.isFilledBy` for the filled
rectangle. It then asks `PanelGridLayouts.target()` for the result size, which is the formula,
the `Panel.Grid.MAX_NODES` cap, and a `tabletCount == layoutWidth * layoutHeight - 1` check so the
footprint has no holes.

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
  `PanelGridRecipeTests.the anchor grows the panel towards the tablets`.

## Not done

- **Nothing tells a player why a craft failed.** Vanilla crafting has no channel for it. The advanced
  tooltip prints `Maximum size` on a capped panel, which is the only hint available.
- **`displays` is illustrative**, a finite list for an infinite rule. Players reading it as a
  whitelist is a real risk with no clean fix.
- **8×8 legibility is unverified.** See the [size cap](#the-size-cap).
- **Recipe-book unlocks for the deleted JSON recipes are simply gone** from existing saves. Harmless,
  but a player who had them will see the entries reappear under the special recipe instead.

## Where the caps come from in code

- The recycle stack limit is `PanelRecycleRecipe.craft` returning
  `ItemStack(AncientPuzzleTablet.ITEM, cost)`, a single stack.
- The legibility limit is the solver and composer scaling by `maxOf(width, height)` and drawing the
  line at `4/16` of a cell.
- Geometry itself is not a blocker: `Panel.Grid.ofSize(w, h)` regenerates any size already.

## Sources

- `src/main/kotlin/com/xfastgames/witness/recipes/PanelGridRecipe.kt` — `plan()`,
  `craft()`, `getDisplays()`, and the `PanelGridLayouts` maths.
- `src/test/kotlin/com/xfastgames/witness/recipes/PanelGridRecipeTests.kt` — accepted layouts
  and their targets.
- `src/main/kotlin/com/xfastgames/witness/items/data/Panel.kt` — `Panel.Grid.ofSize`, `expandTo`,
  `gridOffsets`, node geometry.
- `src/main/kotlin/com/xfastgames/witness/recipes/PanelDyeRecipe.kt` — `PanelRecycleRecipe`.
- `net.minecraft.screen.slot.CraftingResultSlot#onTakeItem` — the one-tablet-per-slot constraint.
- `src/main/resources/data/witness/recipe/puzzle_panel_grid.json` — the recipe stub.
