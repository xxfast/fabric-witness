# The mod's Minecraft rules

Catalog of every mod rule that is about the *item* or the *workstation*, not about validating a
drawn path. Crafting builds, grows, recolours, and recycles panels; the composer composes the marks
on them. For Witness puzzle validation see [../witness/](../witness/README.md).

Each mechanic gets one file. Files are shallow by design: what the player does, the numbers it
produces, the edge cases that bite, and where it lives in the code.

## Three categories

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

**Workstations**: placed blocks with their own screen. Not crafting-table recipes; they transform a
panel the player already has.

| # | Workstation | File | What it does |
|---|-------------|------|--------------|
| 04 | Puzzle composer | [04-puzzle-composer.md](04-puzzle-composer.md) | the block, the slots, and getting a panel in and out |
| 04-1 | ↳ Modifiers tab | [04-1-puzzle-composer-modifiers.md](04-1-puzzle-composer-modifiers.md) | what a panel means: start / end / break / hexagon |
| 04-2 | ↳ Grid tab | [04-2-puzzle-composer-grid.md](04-2-puzzle-composer-grid.md) | what a panel is: which nodes and segments exist |
| 05 | Puzzle frame | [05-puzzle-frame.md](05-puzzle-frame.md) | where a panel is solved; redstone in, power out of the used end to the next frame |
| 06 | Cable | [06-cable.md](06-cable.md) | the wire from a solved frame to a far door, lit in its panel's colour |

The composer is one block with two tabs, and the tabs are two different mechanics: one edits the
panel's topology, the other edits what that topology means. They get a file each.

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
4. Composer ([04](04-puzzle-composer.md)) shapes the panel's grid
   ([04-2](04-2-puzzle-composer-grid.md)) and marks it up ([04-1](04-1-puzzle-composer-modifiers.md)).
   No tablet cost; colour is unchanged (dye is step 3). Shape is free, size is paid for.
5. Panel → **`cost`** tablets back ([03](03-panel-recycle.md)).

Steps 2 and 5 are inverses on every route, so the economy is conservative: recycling returns exactly
what was invested, never more. Composer edits do not touch `cost`.

## Where the mod stands

Unlike [../witness/](../witness/README.md) (mostly unmodelled puzzle logic), crafting and the
composer are **implemented and live**. The three special recipes register their serializers through
`PanelDyeRecipe.init()` before datapacks load; the tablet recipe is plain JSON. The composer ships
with start / end / break / hexagon working ([04-1](04-1-puzzle-composer-modifiers.md)); it has no
tabs yet, and the Grid tab that edits which nodes and segments exist at all is designed but not
built ([04-2](04-2-puzzle-composer-grid.md)). Crafting open questions are economy and legibility,
listed in [01](01-puzzle-panel-crafting.md#not-done). The frame ([05](05-puzzle-frame.md)) is
the mod's redstone component: powered by redstone, solved server-side, and passing power to the
next frame out of the end the line used.

## Sources

- `src/main/kotlin/com/xfastgames/witness/recipes/PanelGridRecipe.kt`
- `src/main/kotlin/com/xfastgames/witness/recipes/PanelDyeRecipe.kt` (holds both `PanelDyeRecipe`
  and `PanelRecycleRecipe`)
- `src/main/kotlin/com/xfastgames/witness/items/data/Panel.kt` (`Panel.Grid`, node geometry)
- `src/main/kotlin/com/xfastgames/witness/blocks/redstone/PuzzleComposerBlock.kt`
- `src/main/kotlin/com/xfastgames/witness/entities/PuzzleComposerBlockEntity.kt`
- `src/main/kotlin/com/xfastgames/witness/screens/composer/PuzzleComposerScreen.kt`
- `src/main/resources/data/witness/recipe/` (the `type: witness:*` special-recipe stubs,
  `ancient_puzzle_tablet.json`, `puzzle_composer.json`; the nine hardcoded `puzzle_panel_grid_*`
  files are gone, folded into `PanelGridRecipe`)
