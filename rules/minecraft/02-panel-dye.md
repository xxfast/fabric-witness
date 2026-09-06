# 02 — Panel dye

**Category:** special recipe (code)

Recolour a puzzle panel without disturbing anything else about it. A panel has two colours the
player can set: the **background** it is painted, and the **line** that lights up on it when it is
traced. Each is set at the crafting table with a dye; the line needs a glow ink sac as well, since
the line is the part of the panel that glows.

---

# Design

## The rule

Shapeless, one panel plus what recolours it, nothing else in the grid:

| In the grid | Out | What changed |
|-------------|-----|--------------|
| panel + **dye** | the same panel | **background** is the dye's colour |
| panel + **dye + glow ink sac** | the same panel | **line** is the dye's colour |

"The same panel" means exactly that: grid, marks, symbols, size, type and `cost` all come through
untouched, and the other colour is left alone. Dyeing the background never touches the line, and
dyeing the line never touches the background.

Any of the 16 vanilla dyes works for either. The glow ink sac is the vanilla item that makes writing
glow (a sign takes dye for its colour and a glow ink sac to light it), so "dye says which colour,
glow ink sac says it is the lit part" is a pairing a player already knows.

## What the line colour paints

The **line** is everything drawn in the panel's lit colour once the player starts tracing:

- the drawn path itself, on the frame, in the hand, and in the composer's preview;
- the start disc the line was picked up from;
- the tip of the line while it is being traced.

It does **not** paint:

- the **lattice**, the unlit grid the line is drawn over: that stays its fixed dark grey;
- **hexagons**, which are notches in the panel's background colour and keep reading as notches
  whatever colour the line over them is;
- **coloured squares**, which carry their own dye;
- the tutorial attract ring and the red error flash.

## Defaults

A fresh panel has a **white** background and a **white** line, which is what every panel has today.
A panel made before the line had a colour of its own has a white line, so nothing already in a
world changes how it looks.

Setting the line back to white is white dye plus a glow ink sac, the same craft as any other colour.

## Worked examples

- A tutorial row in the game's opening colours: dye the background black, then dye the line white.
  Both crafts are one panel each; the second one is the no-op that makes the point that the two
  colours are independent.
- The symmetry area's look: black background, one panel with a blue line and one with a yellow line.
  Two panels, two dye + glow ink sac crafts.
- Dye the line the same colour as the background and the line vanishes into the panel while tracing.
  That is allowed, as the game allows it; it is the author's problem.

## Cost

One dye and one glow ink sac per line recolour, one dye per background recolour. Neither is refunded
by any route: [recycle](03-panel-recycle.md) hands back `cost` tablets and nothing else, and dye and
glow ink sac are spent the way dye already is. `cost` itself is untouched by either craft.

There is nothing to dominate and nothing to dupe: recolouring is the only route to a recoloured
panel, and it puts nothing back into the player's hands. Repeating a craft with the colour a panel
already has just spends the ingredients for an identical panel.

## Edge cases

- Rejected unless the grid holds precisely one panel, one dye, and either zero or one glow ink sac.
  Two dyes, two glow ink sacs, or any stray item fails the match, so nothing has to guess which dye
  meant what.
- A panel plus a glow ink sac with no dye does not craft. There is no "make it glow" without a
  colour; the line already glows.
- Works on any panel shape: grid, tree, or freeform.
- Idempotent: dyeing a colour a panel already has returns an identical panel.
- Shows in the recipe book using the panel's default stack as a representative output, since the
  real output depends on the input.

## Open questions

- **Which colour should a cable take?** A cable lit by a solved frame takes the panel's
  *background* colour today ([06](06-cable.md#colour)). In the game the light that runs out into a
  cable is the line's glow, so the honest reading is that a cable should take the **line** colour.
  Doing that with a white default would turn every cable in every existing world white, since no
  panel has a line colour yet. Left on the background for now; revisit once line colours are in
  play and it is clear whether builders reach for them.
- **The lattice has no colour of its own.** It is a fixed dark grey whatever the background, which
  is low contrast on a black panel. In the game the lattice is usually a shade of the background.
  A derived lattice colour is a visual-only change and a separate piece of work.

---

# Implementation

## Status in this mod

Both colours are implemented and registered. `Panel` holds a `backgroundColor` and a `lineColor`
on all three types, serialised under their own NBT keys with the same tolerant read; an absent
`lineColor` key is `WHITE` (ordinal 0), so panels saved before the line had a colour load with a
white line. `Panel.withBackgroundColor` and `Panel.withLineColor` are the one-colour `copy` helpers
the recipes use, so the other colour cannot be disturbed by construction.

## Recipes

`PanelDyeing.target(panels, dyes, glowInkSacs, others)` is the pure count rule behind both
recipes: exactly one panel and one dye, no strays, and the glow ink sac count picks the target
(`0` → `BACKGROUND`, `1` → `LINE`, anything else → no match). `PanelDyeRecipe` matches
`BACKGROUND` and `PanelLineDyeRecipe` (`witness:panel_line_dye`) matches `LINE`; both serializers
are reached from `PanelDyeRecipe.init()` so they register before the registry freezes. A panel plus
a glow ink sac with no dye hits neither. Each has a `data/witness/recipe/` stub and a recipe-book
unlock advancement.

## Rendering

The line is drawn from a pure white texture with a vertex tint, so the dye's diffuse RGB is all
either pass needs:

- World and item: `PuzzlePanelRenderer.renderLine` takes the panel's `lineColor` and tints the
  node circles and edges. `VertexConsumer.line` and `RenderContext.line` gained `r, g, b, a`
  parameters (default `1f`) to carry it; the `NORMAL` edge path was the only shape helper without
  colour.
- Composer preview: `WPuzzleEditor.drawSolution` takes the `DyeColor` and tints `texturedRect` and
  `drawCircle` with it; the fixed `SOLUTION_*` floats are gone.

Lattice, hexagons, squares, the attract ring and the error flash draw from their own colours and
are untouched, as the design asks.

The advanced tooltip prints both colour names: `(3x3 White Grid, Blue line)`.

## Not done

- Both open questions under [Design](#open-questions): cable colour and lattice colour.

## Sources

- `src/main/kotlin/com/xfastgames/witness/recipes/PanelDyeing.kt` — `PanelDyeing.target`, the
  count rule.
- `src/main/kotlin/com/xfastgames/witness/recipes/PanelDyeRecipe.kt` — `PanelDyeRecipe`,
  `PanelLineDyeRecipe`, the `CraftingInput` counting.
- `src/main/resources/data/witness/recipe/panel_dye.json`, `panel_line_dye.json` — the recipe
  stubs.
- `src/main/kotlin/com/xfastgames/witness/items/data/Panel.kt` — `backgroundColor`, `lineColor`,
  `withBackgroundColor`, `withLineColor`, `toPanel()`, `toNbt()`.
- `src/main/kotlin/com/xfastgames/witness/items/renderer/PuzzlePanelRenderer.kt` — `renderLine`,
  the solution pass.
- `src/main/kotlin/com/xfastgames/witness/screens/widgets/WPuzzleEditor.kt` — `drawSolution`,
  the composer's 2D solution pass.
- `src/main/kotlin/com/xfastgames/witness/utils/RenderContext.kt`, `VertexConsumer.kt` — the
  tinted `line` helpers.
- `src/main/resources/assets/witness/textures/entity/puzzle_panel_solution_fill.png` — the pure
  white the line is tinted from.
- `src/test/kotlin/com/xfastgames/witness/recipes/PanelDyeingTests.kt` — the count rule.
- `src/test/kotlin/com/xfastgames/witness/items/data/LatticeTests.kt` — line colour NBT round
  trip and the absent-key default.
