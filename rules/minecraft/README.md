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
| 01 | Puzzle panel crafting (base grids) | [01-puzzle-panel-crafting.md](01-puzzle-panel-crafting.md) | 9 shaped recipes, 1×1–3×3 cells |

**Special recipes**: `SpecialCraftingRecipe` subclasses in Kotlin. They read the input panel's
`witness:panel` / `witness:cost` components and compute the result — impossible to express in JSON.

| # | Recipe | File | Reads from input |
|---|--------|------|------------------|
| 02 | Grid upgrade | [02-grid-upgrade.md](02-grid-upgrade.md) | size, colour, cost |
| 03 | Panel dye | [03-panel-dye.md](03-panel-dye.md) | whole panel, sets colour |
| 04 | Panel recycle | [04-panel-recycle.md](04-panel-recycle.md) | cost |

## Cells, nodes, and cost

A grid is described three ways, and the recipe files mix them freely — get this straight before
reading any file:

| Term | Meaning | A "2×2-cell" grid |
|------|---------|-------------------|
| **Cells** | Drawable squares. Recipe *filenames* (`grid_2x2`) and JEI use this. | 2×2 = 4 cells |
| **Nodes** | Line intersections. The `Panel.Grid` `width`/`height` fields. | 3×3 nodes |
| **Cost** | Tablets invested. The `witness:cost` component. | 4 |

The relationships are fixed per axis:

```
nodes = cells + 1
cost  = cells = tablets invested = (width_nodes − 1) × (height_nodes − 1)
```

So a *W×H-cell* panel is stored as `(W+1)×(H+1)` nodes and costs `W×H`. The one place `cost` stops
equalling cell count is the multi-tablet grid upgrade — see [02](02-grid-upgrade.md#cost-rule).

## How the tablet economy composes

1. `ancient_debris` → **9** tablets ([00](00-ancient-puzzle-tablet.md)).
2. Tablets → a fresh base panel, `cost = cells` ([01](01-puzzle-panel-crafting.md)).
3. Panel + tablets → a **bigger** panel, colour kept, `cost += tablets` ([02](02-grid-upgrade.md)).
4. Panel + dye → the **same** panel recoloured ([03](03-panel-dye.md)).
5. Panel → **`cost`** tablets back ([04](04-panel-recycle.md)).

Steps 2 and 5 are inverses, so the economy is conservative: recycling returns exactly what was
invested. The upgrade path (3) is the only source of panels larger than 3×3 cells, and the only
place invested cost can drift below the resulting cell count.

## Where the mod stands

Unlike [../witness/](../witness/README.md) (mostly unmodelled puzzle logic), all crafting here is
**implemented and live**. The three special recipes register their serializers through
`PanelDyeRecipe.init()` before datapacks load; the two static recipe groups are plain JSON. The one
open limitation is the upgrade's hardcoded 4×4 ceiling ([02](02-grid-upgrade.md#limits-worth-knowing)).

## Sources

- `src/main/kotlin/com/xfastgames/witness/recipes/PanelGridUpgradeRecipe.kt`
- `src/main/kotlin/com/xfastgames/witness/recipes/PanelDyeRecipe.kt` (holds both `PanelDyeRecipe`
  and `PanelRecycleRecipe`)
- `src/main/kotlin/com/xfastgames/witness/items/data/Panel.kt` (`Panel.Grid`, node geometry)
- `src/main/resources/data/witness/recipe/` (the nine `puzzle_panel_grid_*` files, the four
  `type: witness:*` special-recipe stubs, and `ancient_puzzle_tablet.json`)
