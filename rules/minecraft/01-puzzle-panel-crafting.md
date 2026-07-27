# 01 — Puzzle panel crafting (base grids)

**Category:** static recipe (datapack JSON)

Nine ordinary shaped recipes that turn a block of [tablets](00-ancient-puzzle-tablet.md) into a
fresh puzzle panel. These are the only way to make a panel from scratch; every other recipe
transforms an existing one.

## Rule

Arrange tablets (`T`) in a solid rectangle matching the desired **cell** grid, and get a blank
`witness:puzzle_panel` of that size. The pattern's width×height in tablets *is* the panel's cell
count.

```
"pattern": ["TT",     →   witness:puzzle_panel
            "TT",          cost: 6, panel 3×4 nodes  (a 2×3-cell grid)
            "TT"]
```

The nine recipes cover every cell size from 1×1 to 3×3:

| Cells     | Tablets | Cost | Stored panel (nodes) |
|-----------|---------|------|----------------------|
| 1×1       | 1       | 1    | 2×2                  |
| 1×2 / 2×1 | 2       | 2    | 2×3 / 3×2            |
| 1×3 / 3×1 | 3       | 3    | 2×4 / 4×2            |
| 2×2       | 4       | 4    | 3×3                  |
| 2×3 / 3×2 | 6       | 6    | 3×4 / 4×3            |
| 3×3       | 9       | 9    | 4×4                  |

Each recipe hardcodes the **entire** result panel in its `result.components` — every graph node and
edge of the blank grid, the `backgroundColor` (0 = white), and `cost` = tablet count. See
[the shared cost model](README.md#cells-nodes-and-cost) for why cost equals cell count here.

## Edge cases

- **Fixed output.** Because the panel is literal JSON, these recipes cannot read or grow an existing
  panel, copy a colour, or compute a cost. That is exactly the gap [02-grid-upgrade.md](02-grid-upgrade.md)
  fills in code.
- **Orientation matters.** `1×2` and `2×1` are distinct shaped recipes; the tablet block must match
  the panel's orientation.
- All nine share `"group": "puzzle_panel"`, so they collapse into one recipe-book entry.
- The `_a` filename suffix (`puzzle_panel_grid_2x2_a.json`) is a naming slot for alternate tablet
  arrangements of the same panel; only the `_a` variants ship today.

## Status in this mod

Implemented as datapack JSON — no code involved. Note the base-grid ceiling is 3×3 cells (4×4
nodes), and it is permanent: a crafting pattern is at most 3×3 slots. Larger panels only exist via
the upgrade recipe ([02](02-grid-upgrade.md)).

## Sources

- `src/main/resources/data/witness/recipe/puzzle_panel_grid_*.json` — the nine recipes.
- `src/main/kotlin/com/xfastgames/witness/items/data/Panel.kt` — `Panel.Grid`, node geometry the
  hardcoded graphs match.
