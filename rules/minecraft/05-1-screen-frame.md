# 05-1 Screen frame

**Category:** workstation (block + solver screen). The frame rule it is a variant of is
[05](05-puzzle-frame.md).

The CRT-style puzzle screen from the game's opening shed: a row of green tubes set in one slate-grey
cabinet, each one a puzzle. It is the **puzzle frame in a different housing**. Everything 05 says
about power, solving, Solved being sticky, where power leaves, joins, stands and cables holds here
unchanged and is not restated. This file is only about what a screen frame looks like, what it costs,
and the few places the look changes what a player sees.

Proportions below are fractions of one screen unit's width, read off the shed screenshot
(2026-09-26, frontal shot, middle screen, pixel transitions measured rather than eyeballed).

---

# Design

## In the original game

The shed's five tutorial puzzles are not five framed panels. They are five **tubes in one cabinet**: a
deep slate-grey box runs the length of the row, an unbroken rail along its top and its bottom, and a
thin divider stands between each tube and the next. Each tube has four pale screw heads at the
corners of its glass. Behind the glass the face is black, and glowing inside the black is a
**rounded square of green**, bulged very slightly like a television tube. The puzzle's lattice is
drawn on the green in a **darker green**, its lines straight; only the tube's outline is rounded.
The traced line is a hot yellow-green, brighter than the tube and a different hue from it, with the
same fat start disc as any panel. Unpowered screens further along the row are black glass.

## The rule

A screen frame is a puzzle frame. Put a panel in it, power it, trace it, and it passes power on
exactly as [05](05-puzzle-frame.md#the-rule) says, out of the used end. What is different is
entirely on its face:

| Part | Iron frame | Screen frame |
|------|------------|--------------|
| Housing | Thin iron lip, base plate behind | Deep slate cabinet, wide bezel, four pale screws per unit |
| Joined frames | A bracket drawn between them | One cabinet: shared top and bottom rails, one thin divider between units |
| Face | The panel's background fills the frame | Black glass, a rounded **tube** in the panel's background colour inside it |
| Lattice | Fixed dark grey | A darker shade of the tube's colour |
| Line | The panel's line colour | The panel's line colour, unchanged |

Nested from the outside in, as fractions of one unit's width:

```
unit    1.00   ┌──────────────────────────┐   slate bezel, screws at the glass corners
glass   0.80   │ ┌──────────────────────┐ │   black
tube    0.73   │ │  ╭────────────────╮  │ │   background colour, corners rounded, edges bulged
lattice 0.54   │ │  │   ┼───┼───┼    │  │ │   darker background colour, straight lines
               │ │  ╰────────────────╯  │ │
               │ └──────────────────────┘ │
               └──────────────────────────┘
```

The lattice sits at about three quarters of the tube, and the tube at about three quarters of the
unit, so the puzzle itself is drawn at a little over half the block's width. On an iron frame the
same puzzle is drawn at nearly two thirds. **A screen frame's puzzle is about 15% smaller than an
iron frame's**, which is the price of the housing.

## The tube's colours

The tube wears the panel's **background** dye and the line wears the panel's **line** dye, the same
two colours [02](02-panel-dye.md) already sets. Nothing new is dyed. A screen frame with a fresh
panel in it is therefore a white tube with a white line, and the shed's look is a green background
with a lime line, set at the crafting table like any other panel.

The tube glows, so which shade of the dye it wears is a real question ([02](02-panel-dye.md#the-lit-colour)
draws the same distinction for the line). Every panel today paints its background in a **hand-picked
backdrop colour per dye**, not the dye's block shade: the mod's "green" backdrop is a teal and its
"lime" a yellow-green (a finding of 2026-09-26; [02](02-panel-dye.md) said block shade and was wrong
about the code). The tube wears that same backdrop, so the frame does not change what a panel's
colour means and a panel moved between an iron frame and a screen frame keeps its colour. The
measured tube is a saturated phosphor green that neither backdrop reaches, so the closest dye is a
by-eye pick between green and lime, in game.

The lattice is **the tube's colour at about half its brightness**, measured. This is the one place
the screen frame reaches outside itself: the mod's lattice is a fixed dark grey on every panel, and
[02](02-panel-dye.md#open-questions) already lists that as an open question because it is nearly
invisible on a black panel. A grey lattice on a green tube looks wrong, so the screen frame forces
the decision. See the open questions.

## Off, On, Solved

The three states of [05](05-puzzle-frame.md#the-rule), with the screen's look:

| State | Screen frame |
|-------|--------------|
| **Off** | Black glass, the tube dark: dim enough to read as a switched-off set, bright enough to make out the puzzle up close, as an unpowered iron frame is today |
| **On** | Tube lit in the background colour, lattice on it |
| **Solved** | As On, with the line in the line colour |

No new state, no new light level. The alternative for Off, pure black glass with nothing readable,
is the more faithful television but breaks 05's rule that an unpowered frame is inert, not hidden;
it is listed under the open questions rather than taken.

## Cost

A screen frame is an iron frame with a screen in front of it:

| In the grid | Out |
|-------------|-----|
| iron puzzle frame + glass pane | screen frame |

Shapeless, one of each. One frame in, one frame out, so frames are never created or destroyed by the
craft; the pane is spent. There is no craft back to an iron frame, so nothing is returned by any
route: [recycle](03-panel-recycle.md) takes panels, not frames, and the frame itself is not a panel.
No dupe.

On function the screen frame is exactly the iron frame, so neither dominates the other and the pane
buys the look alone. The cheapest path to a shed row is five iron frames and five panes on top of
the tablets the iron frames already cost, and that is the intended path.

## Edge cases

- **Big grids get small.** A 10×10 panel that is already at the edge of legibility on an iron frame
  ([01](01-puzzle-panel-crafting.md#the-size-cap)) loses another 15% here. The screen frame is for
  the small tutorial puzzles it comes from; nothing stops a big panel going in, it is just harder
  to trace.
- **A screen frame joins an iron frame.** Joined is joined ([05](05-puzzle-frame.md#the-rule)): a
  screen frame beside an iron frame is a chain link the same as two of a kind. Each draws its own
  join on its own side, the cabinet's rail from the screen frame and the bracket from the iron one,
  so a mixed row reads as two housings butted together rather than one. Mixed rows are legal, not
  pretty.
- **Vertical stacks share a cabinet too.** The shed only shows a row, so a column is extrapolated:
  the side rails run through and a divider sits between the units, the same rule turned ninety
  degrees. Unverified against the game; nothing in the game seems to stack these.
- **Stands and anchors.** A screen frame sits on an iron stand like any frame. Against a wall with no
  stand it draws no anchor bar: the cabinet is deep enough to meet the wall itself, which is how the
  shed mounts them.
- **The face is deeper in the block than an iron frame's.** The glass sits behind the bezel, so
  clicks land a little further in and a cable lit off the frame meets the cabinet's side, not a
  panel's edge. Cables and joins are unchanged in what they do.
- **The hexagon reads differently.** A hexagon is a notch punched in the panel's background colour
  ([04](../witness/04-hexagon-dots.md)); on a tube that is a notch of tube colour in a darker-tube
  lattice, which is closer to the game than the notch-in-grey it is today, and untested.
- **Symbols and squares are unchanged.** They carry their own colours and draw inside the lattice
  as before.

## Open questions

- **Lattice colour: this frame, or every frame.** The tube needs a lattice derived from the
  background. Making that global (every panel's lattice is a shade of its background, darkened on a
  light background and lifted on a dark one so the black tutorial panels keep a visible grid) is a
  visual-only change that fixes 02's open question at the same time and stops the frame deciding how
  a panel is drawn. Making it local keeps the iron frame exactly as it is. **Recommended: global**,
  since the game draws every panel that way and a frame-dependent lattice is a rule nobody can see
  the reason for. It is the one question here that changes other work.
- **Off as black glass.** Faithful, and the row in the shed does go dark, but 05 says an unpowered
  frame shows its puzzle so the player knows to look for power. Recommended: keep 05's rule.
- **The panel's backdrop or a brighter shade for the tube.** The backdrop keeps a panel's colour
  meaning the same in every frame; a lifted shade gets nearer the phosphor. Recommended: the
  backdrop, and pick the dye by eye.
- **Screen frame in a mixed row** draws two housings. Acceptable, or should a screen frame's rail
  reach across to an iron frame? Recommended: leave it.

---

# Implementation

## Status in this mod

**Built 2026-09-26 and signed off on sight the same day; no screenshot on file.** The block, the
cabinet model and textures, the tube face, the recipe, and the solver's hit test compile, the tests
pass, and the user looked at it in game and called it good. Not individually checked against a
shot: the tube's proportions against the shed's, the seam between units, the Off look, the empty
cabinet, and picking up a corner start point on a screen frame against the iron frame. Any of those
that looks off later is a shot to take, not a rule to re-derive.

Defaults taken in this slice without an explicit answer, each bounceable:

- **The lattice shade is local to the screen frame.** The design recommends global (every panel's
  lattice a shade of its backdrop); this slice only passes a `Lattice` into the lattice pass from the
  screen path and leaves the iron frame's grey exactly as it was. Global stays an open question.
- Off keeps 05's dim-but-readable rule, drawn with the same brightness as an unlit iron frame.
- Glass pane recipe, no reverse craft. No anchor bar. Tube fills the glass at the measured
  proportions.

## How it is built

- `ScreenPuzzleFrameBlock` **subclasses** `IronPuzzleFrameBlock`, which is now `open`. Every
  `is IronPuzzleFrameBlock` in the network, the stand, the cable, the composer's solver hit and
  `retrieveOnAttack` therefore already covers it; nothing in `RedstoneNetwork` changed. It shares the
  block entity type (`PuzzleFrameBlockEntity.ENTITY_TYPE` lists both blocks) and the block settings
  (`IronPuzzleFrameBlock.frameSettings`). It overrides only the codec and `housingShape`, a
  `protected open val` the base `getShape` rotates.
- **No join models.** The cabinet is a full-width box (`screen_puzzle_frame.json`: body 0..9 deep,
  four 1 px bezel strips at 9..10), so neighbouring units abut and the top and bottom rails run
  through with nothing selected per side. The `*_connected` flags still exist on the state and still
  drive the network; the screen blockstate just never reads them. The divider between two units is
  the two bezels meeting: 4 px against the shot's 3.5.
- **The face** is `PuzzlePanelRenderer.renderPanel(screen = true)`, chosen by
  `PuzzleFrameBlockRenderer` off the block class, drawn at `SCREEN_FRAME_SCALE = 0.75` (the 12 px
  glass) instead of the iron frame's 0.85.
- **The solver's hit test** inverts the same scale through `PuzzleFrameBlockRenderer.faceScale(state)`
  in both `projectPanelPosition` and `toPanelCoordinate`. Before this the 0.85 was a constant in
  three places.

## How the glass is drawn

All flat opaque quads on the `text` layer at the frame's constant lightmap, like every other pass.
Nothing translucent (the depth-test note in `PuzzlePanelRenderer` still stands) and nothing
fullbright (shader packs bloom it).

| Pass | Depth | What |
|------|-------|------|
| glass | 0 | `square` over the whole face, `solutionFill` tinted to 3% grey: black with a hint of surface |
| tube | -.005 | `roundedSquareFan`: the panel's backdrop texture, side 0.91 of the face, corner 0.08 of the side, vertex grey 1.0 at the centre falling to 0.7 at the rim |
| lattice | -.01 | the existing graph pass, but on the backdrop texture at shade 0.5 (`Lattice.onTube`) |
| line, symbols, flash | -.011 .. | unchanged |

The fall-off is what stands in for the CRT's curve: no geometry is bowed. Unlit, the tube and the
lattice both take the unlit brightness (0.35) so the lattice stays at half the tube.

`RenderContext` gained a `shade` (default 1) that every primitive's `r`/`g`/`b` default to, which is
how one lattice pass draws grey on the iron frame and tinted backdrop on the screen without touching
the node and edge drawing code.

## Traps, do not re-derive

- **The doc's colour claim was wrong.** [02](02-panel-dye.md) says the background is the dye's block
  shade; the backdrop textures are hand-picked (`puzzle_panel_backdrop_green.png` is `#54D9AF`, lime
  is `#ACF14A`). Any colour reasoning from `DyeColor` shades is wrong for the face.
- **A full-width cabinet has no divider of its own.** The seam between two units is the light left
  edge of one bezel against the dark right edge of the next. If the row reads as one long slab with
  no dividers, that bevel is what to darken, not a new connector model.
- **Panels draw mirrored on x** ([05](05-puzzle-frame.md#traps-do-not-re-derive)). The tube is
  symmetric; the screws are at all four corners; nothing here is asymmetric yet.
- **`housingShape` is an open property read by the base `getShape`.** If the hover outline on a
  screen frame ever shows the iron frame's plates, that is the base initialiser reading it before
  the subclass has set it; convert it to an open function then.

## Not done

- **No screenshot on file** for the five checks listed under Status; signed off on sight only.
- The lattice shade global-or-local question ([Open questions](#open-questions)).
- The solver GUI's 2D preview and the item icon draw a screen frame's panel as a plain panel; only
  the block face has the tube.
- The symbol pass is not dimmed by the unlit brightness on any frame, so a hexagon on a dark tube
  may glow at full colour. Pre-existing on the iron frame, more visible here; report before fixing.
- Textures are placeholder pixel art painted by script (slate with a bevel, four screw pixels,
  near-black glass); a hand-drawn pass is expected once the proportions are signed off.

## Sources

- The two shed screenshots the measurements come from (user's attachments, 2026-09-26): a frontal
  row of five and a close-up of the first screen mid-trace.
- Measured colours, for the texture work: bezel `#383A3D`, screw `#978E74`, glass `#010702`,
  tube `#00F854`, lattice `#007A2D`, line `#B9FF1C`.
- `rules/minecraft/05-puzzle-frame.md`: everything this frame does.
- `rules/minecraft/02-panel-dye.md`: the two colours the face wears, and the lattice question.
- `src/main/kotlin/com/xfastgames/witness/blocks/redstone/ScreenPuzzleFrameBlock.kt`,
  `src/main/kotlin/com/xfastgames/witness/blocks/redstone/IronPuzzleFrameBlock.kt` (`frameSettings`, `housingShape`),
  `src/main/kotlin/com/xfastgames/witness/entities/renderer/PuzzleFrameBlockRenderer.kt` (`faceScale`),
  `src/main/kotlin/com/xfastgames/witness/items/renderer/PuzzlePanelRenderer.kt` (`renderScreen`, `Lattice`),
  `src/main/kotlin/com/xfastgames/witness/utils/VertexConsumer.kt` (`roundedSquareFan`),
  `src/main/kotlin/com/xfastgames/witness/utils/RenderContext.kt` (`shade`),
  `src/main/resources/assets/witness/models/block/screen_puzzle_frame.json`,
  `src/main/resources/assets/witness/textures/block/screen_puzzle_frame*.png`,
  `src/main/resources/data/witness/recipe/screen_puzzle_frame.json`.
