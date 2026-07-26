# 03 — Panel dye

**Category:** special recipe (code)

Recolour a puzzle panel's background without disturbing anything else about it. Exists as code
because it must copy the *input* panel's data forward and change only one field — a static recipe
can't preserve an arbitrary input.

## Rule

Shapeless: exactly **one** puzzle panel + exactly **one** dye item (any of the 16 vanilla dyes) →
the same panel with its `backgroundColor` set to the dye's colour. The puzzle graph, size, line, and
`cost` are all preserved; only the colour changes. Replaces the dead nbtcrafting `puzzle_panel_color_*`
recipes.

## Edge cases

- Rejected unless the grid holds precisely one panel and one dye; a stray third item fails the match.
- Works on any panel shape — `Grid`, `Tree`, or `Freeform` — via a `when` over the sealed `Panel`
  type; each branch `copy`es with the new colour.
- Idempotent: dyeing a panel its current colour just returns an identical panel.
- Shows in the recipe book (`isIgnoredInRecipeBook = false`) using the panel's default stack as a
  representative output, since the real output depends on the input.

## Status in this mod

Implemented and registered (`PanelDyeRecipe.init()`).

## Sources

- `src/main/kotlin/com/xfastgames/witness/recipes/PanelDyeRecipe.kt` — `PanelDyeRecipe`.
- `src/main/resources/data/witness/recipe/panel_dye.json` — the recipe stub.
