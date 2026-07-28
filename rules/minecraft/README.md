# The mod's crafting rules

Catalog of every crafting-table recipe the mod adds for puzzle panels — how they are built, grown,
recoloured, and recycled. This is mod economy, not Witness puzzle validation; for the puzzle rules
see [../witness/](../witness/README.md).

Each recipe gets one file. Files are shallow by design: what the recipe does, the numbers it
produces, the edge cases that bite, and where it lives in the code.

## Two categories

**Static recipes**: ordinary datapack `minecraft:crafting_*` JSON. The output is fully hardcoded, so
they cannot read or transform an existing item's data.

| # | Recipe | File | Kind |
|---|--------|------|------|
| 00 | Ancient puzzle tablet | [00-ancient-puzzle-tablet.md](00-ancient-puzzle-tablet.md) | shapeless — the currency |

**Special recipes**: `SpecialCraftingRecipe` subclasses in Kotlin. They read the input panel's
`witness:panel` / `witness:cost` components and compute the result — impossible to express in JSON.

| # | Recipe | File | Reads from input |
|---|--------|------|------------------|
| 01 | Puzzle panel crafting | [01-puzzle-panel-crafting.md](01-puzzle-panel-crafting.md) | size, colour, cost |
| 02 | Panel dye | [02-panel-dye.md](02-panel-dye.md) | whole panel, sets colour |
| 03 | Panel recycle | [03-panel-recycle.md](03-panel-recycle.md) | cost |

## Cells, nodes, and cost

A grid is described three ways, and the recipe files mix them freely — get this straight before
reading any file:

| Term | Meaning | A "2×2-cell" grid |
|------|---------|-------------------|
| **Cells** | Drawable squares. What the player sees and what JEI shows. | 2×2 = 4 cells |
| **Nodes** | Line intersections. The `Panel.Grid` `width`/`height` fields. | 3×3 nodes |
| **Cost** | Tablets invested. The `witness:cost` component. | 4 |

Nodes are fixed per axis:

```
nodes = cells + 1
```

Cost is **not** a function of size. It counts tablets actually invested, which depends on the route
taken, so a panel built in one craft can cost more than the same panel grown one axis at a time. See
[the convenience premium](01-puzzle-panel-crafting.md#the-convenience-premium). The only invariant
is that cost never exceeds what was spent, which is what makes recycling safe.

## How the tablet economy composes

1. `ancient_debris` → **9** tablets ([00](00-ancient-puzzle-tablet.md)).
2. Tablets → a panel, or panel + tablets → a **bigger** panel, colour kept, `cost += tablets placed`
   ([01](01-puzzle-panel-crafting.md)). One rule, because a tablet is a 1×1-cell panel costing 1.
3. Panel + dye → the **same** panel recoloured ([02](02-panel-dye.md)).
4. Panel → **`cost`** tablets back ([03](03-panel-recycle.md)).

Steps 2 and 4 are inverses on every route, so the economy is conservative: recycling returns exactly
what was invested, never more.

## Where the mod stands

Unlike [../witness/](../witness/README.md) (mostly unmodelled puzzle logic), all crafting here is
**implemented and live**. The three special recipes register their serializers through
`PanelDyeRecipe.init()` before datapacks load; the tablet recipe is plain JSON. Open limitations are
economy and legibility questions rather than mechanics, listed in
[01](01-puzzle-panel-crafting.md#not-done).

## Sources

- `src/main/kotlin/com/xfastgames/witness/recipes/PanelGridRecipe.kt`
- `src/main/kotlin/com/xfastgames/witness/recipes/PanelDyeRecipe.kt` (holds both `PanelDyeRecipe`
  and `PanelRecycleRecipe`)
- `src/main/kotlin/com/xfastgames/witness/items/data/Panel.kt` (`Panel.Grid`, node geometry)
- `src/main/resources/data/witness/recipe/` (the `type: witness:*` special-recipe stubs and
  `ancient_puzzle_tablet.json`; the nine hardcoded `puzzle_panel_grid_*` files are gone, folded into
  `PanelGridRecipe`)
