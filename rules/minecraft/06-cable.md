# 06: Cable

**Category:** redstone block

The wire from the game: a rigid tube that runs along the ground and up walls from a solved panel
to whatever it opens, dark until power flows through it and lit in its colour once it does. It is
what a puzzle frame's power travels along when the door is not right next to the frame
([05](05-puzzle-frame.md#where-the-power-goes)).

---

# Design

## The rule

A cable is a block. It joins to the cables around it, to frames, to stands, and to ordinary
redstone, and it carries power the full length of a run without weakening. A run is **lit** when
any block in it touches a source, and **dark** otherwise; every cable in a run is in the same state.

- **Placement:** anywhere, floating included. A cable does not need a block under it, so a run
  can leave the ground, climb a wall and cross a gap. It is a thin tube down the middle of the
  block with arms out to each side it joins.
- **Sources:** a solved frame's exit side, a powered stand, and any vanilla redstone signal into
  the cable (dust, lever, torch, repeater). A cable does **not** take power from a frame's other
  sides; those are the frame's inputs, and a cable touching one of them feeds the frame instead.
  A cable on a side the panel has a nub on is an output only: it never feeds that frame.
- **Outputs:** a lit run gives strength 15 to every block it joins that is not itself a cable: a
  frame (powering it), a door, a lamp, dust, a piston. Weak power, so a solid block on the end of
  a run does not relay it any further.
- **Length:** a run carries power up to **64 blocks** from its nearest source, measured along the
  cable, then stops. Chain runs through a frame, or drop a repeater in, to go further.

## Colour

There is one kind of cable. Dark, it is near black. Lit, it takes the **background colour of the
panel that powers it**, the way a cable in the game glows in its panel's colour: a yellow panel
lights a yellow run, a blue panel a blue one. A run fed by plain redstone with no panel behind it
lights white. The colour is not the player's to choose and it means nothing to the wiring; it
says which panel the power came from.

| Meets | Joins? | Power flows? |
|-------|--------|--------------|
| Another cable | yes | yes, and the two are one run |
| Frame | yes | in on a nub side (once solved to it); out on every other side |
| Stand | only from underneath | in from its top; out into its base |
| Redstone dust, lever, door, lamp… | yes | both ways, as dust does |

Cables that touch are one run, as dust that touches is one wire. The game keeps its cables from
touching, and so should a builder: leave a block between runs that should stay separate. If two
panels feed one run, it lights in the colour of whichever fed it first.

## Worked examples

A frame on a stand, the door thirty blocks away round a corner and up a step:

```
 [frame]══════╗            ══ lit cable
   stand      ║
              ╚════╗
                   ╚═══[door]
```

Put the frame's end nub on the edge the cable leaves from, solve, and the whole run lights and the
door opens. Cut the power to the frame, or pop its panel, and the run goes dark with it.

Two panels, one door, two runs:

```
 [A]═══════════╗
               ║  [door]      the door opens when either run is lit
 [B]───────────╫──╝
```

Both runs end at the door and neither touches the other (one passes over the other with a block
between). Solving A opens it and lights A's run in A's colour; so does solving B, in B's.

## Cost

**Crafting:** three copper ingots in a row give **six** cables. No tablets; a cable is wiring, not
a puzzle. Nothing to dye: the colour comes from the panel.

**Dominance, stated plainly:** for getting a frame's power to a door, a cable dominates dust and
repeaters: no decay for 64 blocks, no support needed, climbs, and it is one item instead of two.
That is the point of the block. Dust keeps its own jobs: feeding a frame from a vanilla circuit,
tapping a run mid-way for a vanilla mechanism, and anything comparator- or timing-shaped. The stand
relay ([05](05-puzzle-frame.md)) is now mostly a convenience for dust on the ground; a cable up the
stand does the same and shows its state.

No route returns more than it cost: cables are copper in, copper-shaped block out, and nothing
consumes them.

## Edge cases

- **A run is one state.** There is no half-lit cable; if the source is 64 blocks away the whole
  run past that point is dark, not dim.
- **A cable from a frame's exit side round to one of its inputs is a latch**, the same class as a
  loop of frames: once solved it holds itself on. Accepted, as any redstone component that can be
  fed back into itself. Reset by popping the panel.
- **Touching runs merge.** Two runs that share a block face are one run, lit by either source, in
  the colour of whichever came first. That is the bug you get for free by letting two runs meet
  under one stand; keep a block between them.
- **Dust beside a cable connects both ways**, exactly as dust beside dust. Keep dust off a run you
  mean to be private.
- **Off cables are near black**, whatever fed them last; colour is only ever shown with power.
- **Unloaded chunks end a run.** A cable across a chunk border into unloaded land stops there
  until that chunk loads and something updates it. Long runs across a base are fine; runs across
  the map are not.
- **Breaking a cable mid-run** splits it: the source side stays lit, the far side goes dark.

---

# Implementation

## Status in this mod

**Slices 1 and 2 ship, seen in game.** 2026-08-29: a run from a solved frame's exit side down,
along the ground and into the next stand's base lights end to end and turns that frame On; a
second run climbs into a frame's side and does the same. 2026-08-30: the panel-coloured glow with
dark casing, and the ribbon geometry, signed off after the F3-guided fix to the climb plane.

- `CableBlock`, one block (`witness:cable`), six connection flags plus `lit` and a `color`
  (`DyeColor`) block-state value, light 7 when lit, floating.
- **The ribbon.** 3 wide, 1 thick, everywhere (5 x 2, then 4 x 1.5, until 2026-08-30, thinned
  on request twice). The hitbox stays at 4 x 1.5 on purpose, so the selection outline sits
  slightly proud of the drawn ribbon; a 4px tube read as a pipe and a 5x5 column read as
  one too (both tried, both rejected 2026-08-29/30). Horizontal runs lie on the block floor
  (`cable_core` pad, `cable_arm` strips to the block centre). Vertical runs are a thin strip
  (`cable_riser_foot` from the floor pad, `cable_riser` from mid height, `cable_drop` to mid
  height) wide across the `wide` block-state axis. A horizontal cable **lies on the floor only
  when it is on the ground** and so are the cables beside it (`floor` block state); otherwise it
  **stands** as a mid-height ribbon (`cable_band`), which is also how a ribbon leaves a panel and
  what the top of a column runs into. Two earlier rules were wrong: "bands only off a column"
  left a band hanging over a ground cable (2026-08-30 01:21), and "floor whenever the neighbour
  is a cable" drew a suspended run lying flat in mid-air (01:31).
- **`wide`**: a riser faces the same way as the frame it serves. Only the **top** of a climb
  decides: a foot under a stand takes the stand's facing (the ribbon runs up the post face-on, as
  in the game), and a piece that continues downward is wide *along* its band, which for the band
  into a frame's side is the same thing. Every other block in the climb, feet included, copies
  the nearest deciding block above, then below (`wideFor`, written by the same walk as `lit`); a
  climb with no top at all stands along its foot's arm. Deciding feet from their floor arm was
  wrong both ways: *across* it twisted the climb under a face-on top (2026-08-30 00:20, 00:44),
  *along* it did the same whenever the arm and the band ran on different axes, and F3 at 00:55
  showed exactly that (foot `wide=z`, top `wide=x`). Dark casing (tint 1)
  is on the floor ribbon's sides, the band's edges, the foot pad's sides and a riser's two thin
  side faces; a riser glows only on its two broad faces (asked for 2026-08-30 00:42).
  Every other plane rule, a `+` cross (dark rib down the
  face), a 5x5 column and a 4px tube (pipes) were tried against shots on 2026-08-29/30 and
  rejected. Existing cables only pick up a changed rule when something next to them updates.
- Every model face carries `tintindex 0`, and the companion registers a `BlockTintsFactory`
  (`BlockColorRegistry`, Fabric rendering v1 on 26.2; `ColorProviderRegistry` is gone) with two
  tints: index 0 on the broad faces (`color.textureDiffuseColor` when lit, `UNLIT_COLOR` when
  not) and index 1 on the edges, always `UNLIT_COLOR`, the black casing beside the game's lit
  strip. Seen 2026-08-30: a lit run reads as a glowing ribbon with a dark rim, a dark run as black. The item tints
  dark through a `minecraft:constant` tint in its item definition JSON.
- `color` is written by the same walk that sets `lit`: `sourceColor` looks through each source
  neighbour for a `PuzzleFrameBlockEntity` and takes its panel's `backgroundColor`; plain
  redstone counts as white, and a panel's colour wins over white. One colour per run, the first
  source found. Unlit cables keep their last colour in state but draw dark.
- Recipe: 3 copper → 6 cables. No dye.
- `walkCables` (`CableNetwork.kt`) is the pure component-then-spread walk, bounded at
  `CABLE_MAX_DISTANCE = 64` and `CABLE_MAX_VISITED = 512`; `CableNetworkTests` pins it.
- Sources: any non-cable neighbour with `getSignal > 0` towards the cable. Frames answer only
  on their exit side, so a cable on an input side stays dark. Stands answer upward only, so a
  cable **joins and feeds a stand only from underneath**: a stub into a stand's side was drawn
  at first and read as a lie (seen, then removed), and a run passing beside a stand's base fed
  the frame on it back through the stand (seen: lever off, frame stayed On; then removed from
  `getSignal` too).
- Emission: `isSignalSource`, `getSignal` = 15 when lit, all sides; no `getDirectSignal`.
- The texture is vanilla white concrete under the tint; no emissive glow yet.

Still an assumption: the frame's vertical exit (top / bottom edge). Cables now make vertical
runs routine, so that shot matters more than before.

## Proposed shape

**Block.** `CableBlock`, one block per dye colour registered from a loop (the stained stone
family is the in-repo precedent for a colour set), or one block with a `color` block-state
property if the model count is a problem; sixteen blocks keeps the blockstate JSON simple.
Properties: `north/south/east/west/up/down` connection flags (multipart model, as the frame's
brackets) and `lit`. Light level when lit: 7, a hint on the wall, not a torch.

**Connection predicate**, single-sourced like the frame's: same-colour cable on that side; a frame
on that side; a stand on that side; a block with `isSignalSource` or a redstone conductor being
asked. Frame joins draw the arm regardless of exit; the frame decides whether power comes out.

**Network power is a walk, not a neighbour update.** A signal with no decay cannot be carried by
`neighborChanged` alone. On any change to a cable or to a block touching one, walk the connected
same-colour cable graph from the changed block (bounded at 64 steps from each source, or 512
blocks total, whichever first), find whether any cable touches a source, and set `lit` on every
cable in the walk that differs. `Panel.regions` (`items/data/Regions.kt`) is the in-repo flood
fill to model on; keep the graph walk pure over an abstract neighbour function so it is unit
testable without Minecraft.

**Sources**, per cable, checked during the walk: a frame whose `exit` faces this cable and is
`solved`; a `powered` stand whose base touches this cable; any neighbour whose
`getSignal` towards this cable is > 0, excluding cables.

**Emission.** `isSignalSource` true, `getSignal` = 15 when `lit`, all sides, cables excluded
(they read `lit` directly). No `getDirectSignal`, same reasoning as the frame.

**Render.** Lit: the dye colour at a constant lightmap, as lit panel faces are. Dark:
the same colour at `UNLIT_BRIGHTNESS`, the panel's unlit treatment reused.

**Slices:**

1. White cable only: block, model, connection flags, walk, lit from a frame's exit side, emits to
   a door. Feelable: frame to a door round a corner.
2. Colours and the channel rule.
3. Stand and dust as sources / sinks.
4. Length cap tuning and the chunk-border behaviour.

## Not done

- Slices 3 and 4: stand base / dust as sources (dust already works through `getSignal`), cap
  tuning and chunk borders.
- Proper cable textures and a lit glow (emissive), instead of borrowed concrete.
- Junction boxes and multi-cable gates (the game's door with several cables into one box, lit
  segments per input). A door here is just a block a lit run touches; AND-ing two panels means
  vanilla logic.
- Cable-to-cable colour bridging blocks, if the bundle rule turns out too strict.

## Sources

- `rules/minecraft/05-puzzle-frame.md`: what a cable plugs into and where power leaves a frame.
- `src/main/kotlin/com/xfastgames/witness/blocks/redstone/IronPuzzleFrameBlock.kt`: `exit`,
  `getSignal`, the connection predicate pattern.
- `src/main/kotlin/com/xfastgames/witness/blocks/redstone/IronStandBlock.kt`: the relay.
- `src/main/kotlin/com/xfastgames/witness/items/data/Regions.kt`: the flood fill to model the walk
  on.
- `src/main/kotlin/com/xfastgames/witness/blocks/building/StainedStone*.kt`: colour-family
  registration precedent.
- `src/main/resources/assets/witness/blockstates/iron_puzzle_frame.json`: multipart connections.
