# 05: Puzzle frame

**Category:** workstation (block + solver screen)

The block a puzzle is solved on. A panel goes in an Iron Puzzle Frame, the frame is powered, the
player traces the line, and a solved frame passes its power on: out of the side the line **finished
on** when the panel offers a choice of ends, to every joined frame when it has only one, and to the
one thing beside it when there is only one place power can go. It is the
mod's redstone component: redstone goes in at the head of a chain, and the chain lights up one
frame at a time as it is solved, in whatever direction the end points send it.

This is solving, not composing. Panels are built at the crafting table
([01](01-puzzle-panel-crafting.md)) and marked up at the composer ([04](04-puzzle-composer.md)); the
frame only displays and validates a panel you already have. Which lines count as solved is the
whole of [../witness/](../witness/README.md); which *end* the line used is what this file wires up,
the "environment wiring" that [02-end-points](../witness/02-end-points.md) leaves out of scope.

---

# Design

## The rule

A frame is in exactly one of three states, and every state is visible from across the room:

| State | Looks like | Responds to a click | Powers a neighbour |
|-------|------------|---------------------|--------------------|
| **Off** | Dark. The panel is there but unlit. | No, a dull click. | No |
| **On** | Lit, panel bright. | Yes, opens the solver. | No |
| **Solved** | Lit, and stays lit. | Yes, can be re-traced. | Yes, out of the used end |

A frame is **On** when it has a panel and it is powered. It is powered by either:

1. A redstone signal into the frame block, from any side, exactly like a lamp; or
2. A joined frame that is **Solved** and whose used end points at this one.

A frame becomes **Solved** the moment a traced line is accepted, and Solved is sticky: it does not
depend on the line staying there. The player can open a solved panel again, trace something else,
even fail it, and the frame stays Solved (this is how the game behaves in the tutorial rows). Only
two things clear Solved: losing power, and losing the panel.

## Where the power goes

An end point is a nub hanging off the border of the grid, pointing out of one side of the panel
([02-end-points](../witness/02-end-points.md)). How many the panel has decides the routing:

- **One end point: a solved frame powers every frame joined to it**, whichever side the nub is on.
  The game's own panels put their single nub wherever the grid has room, not where the next panel
  sits, so a one-end panel has to be free to feed a neighbour on any side.
- **Two or more end points: the side the used nub points out of is the side the power leaves
  by.** A solved frame powers the frame joined to it on that side, if there is one. Offering
  the player a choice of ends is what makes the exit a route.
- **Only one place to go: it goes there, whatever the nub says.** Leave out the side the frame's
  own power came in on; when exactly one of the other bracket sides holds a frame or a cable, a
  solved frame powers that one, whichever end the line used and whichever way its nub points. A
  choice of ends is only a route when there is a choice of neighbours; with one neighbour the
  ends are the puzzle and the wiring is a given. This is the Orchard: every tree's tips point up,
  and the game's cable still leaves each post sideways.

The **redstone signal** (below) goes the same way: out of every bracket side for a one-end frame,
out of the used nub's side for a frame with a choice of ends.

**Power comes from the source, never from the chain itself.** A frame is powered while there is
an unbroken path of solved frames back to a redstone signal. Cut that signal and the whole chain
goes dark, however the frames are arranged: two solved frames side by side do not hold each
other up, and a ring of them does not either.

**Anchored.** A frame with no stand under it, placed against a solid wall, shows a short bracket
from its back to the wall, so it reads as bolted on rather than floating. Purely a look: it sits
in the same place as any other frame, and cables and joins are unchanged. The bracket comes and
goes with the wall and the stand (break the wall and it vanishes; put a stand under and it
vanishes).

*Joined* means visibly bracketed: the frame directly above, below, left or right. A frame behind,
in front, or diagonal is never joined. An Iron Stand is joined to the frame above it and carries
redstone: a signal into its base from any side, or a cable underneath it, comes out of its top
into that frame, so a row can be fed by dust along the ground. It carries nothing frame-to-frame.

Nothing on the frame is configured. Routing is authored on the panel, at the composer, by where
the ends go:

- One end anywhere: a link to every joined frame. A row wired left to right is a row of one-end
  panels; where their nubs sit does not matter, and a frame above or below the row lights too.
- Two ends on different edges: the player picks the route by picking the exit. Solve it to the top
  nub and the frame above lights; re-solve it to the right nub and power switches: the top goes
  dark (and everything it was feeding resets) and the right comes on.
- A **diagonal corner nub** points out of two sides and powers **both**: that is the fork. A
  corner nub squared off along one border powers that side only. Note the composer's default for
  a corner end is diagonal, so a corner end forks unless the author squares it off. On a one-end
  panel the distinction is moot: it feeds every side regardless.
- A nub pointing at a side with nothing joined is still a perfectly good solution. If exactly one
  other side has a frame or cable, not counting the side power came in on, that is where the
  power goes (only one place to go). If two or more do, the nub rules and it powers nothing: the
  frame is then the **tail** of its chain. A one-end frame is a tail only when nothing at all is
  joined to it.

**Redstone out.** A solved frame also puts out a redstone signal (strength 15). With a choice of
ends it leaves by the used nub's side(s), the way the nub points at the cable. With one end it
leaves by all four bracket sides (top, bottom, left, right), the way the game's cable leaves a
panel on whichever side the room needs. With one place to go, it leaves by that side. Put a frame
there and it is a chain link; put dust, a lamp or a door there and the chain ends in ordinary
redstone. Nothing ever comes out of the back or the front, so a lever behind a frame is never lit
by it.

Only frames and cables count as a place to go. Dust, a repeater or a door beside a frame is not a
neighbour for this rule: a frame with two ends pointing up and a lone repeater on its right stays
a tail. Run a cable to the repeater instead, or square the nub off.

**Running it somewhere.** The frame's output is weak power, so a solid block on the exit side
is a dead end. Put a **repeater** directly against the exit side, facing away (it sits on the
ground block beside the stand), and run dust from it to the door. Seen working 2026-08-29.
For anything further than a few blocks, or up a wall, that is what a cable is for
([06](06-cable.md)).

**Inputs are decided by the panel, not the solve.** With a choice of ends, a side the panel has an
end nub on is an exit: a signal can only ever leave by it, never enter, whether or not the frame
is solved yet, and every other side (the back, the stand, any edge without a nub) is an input.
With one end, every side is an input and every bracket side is an output, exactly as the game
runs its cable in on one side and out on another with nothing on the panel to say which. That is
safe because power is traced from its source: a cable the frame itself lights can never be the
thing that powers it.

## Worked examples

The tutorial row: five frames on one bracket, redstone behind the first, every end on the right.

```
 dustâ [F1] â [F2] â [F3] â [F4] â [F5] â door
         On     Off    Off    Off    Off
```

Solve F1 â F2 turns On. Solve F2 â F3 turns On, and so on. F5's nub points right at the door, so solving
it opens the door. Trace a wrong line on F1 afterwards: nothing
changes, F1 is still Solved, F2 is still On. Cut the dust â all five go dark, Solved is cleared on
all five, the door closes. Dust back on: only F1 is On, and the row is solved again from the start.

A junction: F2 has an end on its top edge and one on its right edge.

```
              [F3]
 dustâ [F1] â [F2] â [F4]
```

Finish F2 upward and F3 lights; finish it rightward and F4 lights and F3 goes dark. Give F2 a
diagonal corner nub at its top-right instead and one solution lights both.

A panel from the game, one end on its top edge, with the next panel to its right:

```
 dust→ [F1] → [F2] → door
        nub↑
```

F1 has one end, so solving it lights F2 whatever side the nub is on; the redstone signal itself
still leaves upward, so dust or a door fed straight from F1 has to sit above it, not beside it. Put a third frame
above F1 and solving F1 lights that one too. Give F1 a second end instead and the choice comes
back: the used nub decides.

The Orchard: tree panels on stands, each cabled sideways to the next post. Every tip points up.

```
 dust→ [T1]═══╗  [T2]═══╗  [T3]═══ door
       stand  ╚══stand  ╚══stand
```

T1 has a choice of tips and nothing above it, but the cable on its right is the only place power
can go, so solving T1 to any tip lights the cable and T2 comes On. Put a second frame on top of T1
and the choice is real again: the used tip points up, so the frame above lights and the cable goes
dark until the nub is squared off sideways or the frame above is removed.

The same trees bracketed in a row:

```
 dust→ [T1] → [T2] → [T3] → door
       tips↑  tips↑  tips↑
```

T2 has frames on both sides, but T1 is where its power comes in, so T3 is its only place to go and
the row chains left to right. T3 has only T2 beside it, which is its input, so it is a tail on its
nub and the door beside it stays shut. Square T3's outer tip off to the right and the door opens.

## Cost

None. No tablets in, none out, and nothing about the panel item changes. No rule is dominated
(nothing else produces power) and there is no route that returns more than it cost. A one-end
panel feeding every side does not dominate a two-end one: one end is an unconditional fork, two
ends are a route the player picks, and a frame on the wrong side of a one-end panel cannot be
kept dark. The one-place-to-go rule does make squaring off a corner nub pointless when there is a
single neighbour, but it costs nothing and changes nothing with two, so nothing is dominated.

The one thing that *would* have been an exploit is a portable solution: today the drawn line lives
in the panel item, so a solved panel carried to another frame would arrive with its line drawn. The
line is therefore cleared **when a panel goes into a frame**, and Solved is a property of the
frame, never of the panel. Every way out of a frame (sneak-take, left-click, breaking the block)
hands back a plain, unsolved panel; a solution only counts where it was drawn.

## Edge cases

- **An unpowered frame is inert, not hidden.** The panel is visible, just dark, so the player can
  see there is a puzzle and go looking for what powers it. Clicking makes the pointless-click sound.
- **Removing the panel clears Solved**, and so unpowers everything downstream. Popping a panel is
  the manual reset for the rest of the chain.
- **Power loss clears Solved.** A redstone pulse into the head of a chain resets the whole chain.
- **On is not contagious.** A powered but unsolved frame passes nothing along.
- **Re-tracing a solved frame is free**, but the exit follows the **last accepted** line. A failed
  or abandoned re-trace leaves the previous exit powered.
- **Redstone reaches every frame, not just the head.** Like a row of lamps, a dust line run
  behind a row powers all of them and bypasses the chain. Builders feed only the head and route
  wiring away from the rest. Deliberately vanilla; not a bug.
- **A frame with no panel** is Off regardless of power.
- **A source on a nub side does nothing.** Nub sides are exits and never take power in, so a
  lever or cable on the same edge as an end nub leaves the frame dark. Feed it from the back, the
  stand, or an edge without a nub. A panel with nubs on all four edges takes power from the back
  and the stand only.
- **Breaking a frame** mid-chain leaves the one before it pointing at air, so the rest goes dark
  until a frame is placed there again.
- **Adding a neighbour can re-route a solved frame.** A frame routed by only-one-place-to-go
  reverts to its nub the moment a second frame or cable is placed beside it, and the first
  neighbour goes dark if the nub does not point at it. Removing that second neighbour brings the
  fallback back. Nothing is remembered; the rule reads what is there.
- **The only neighbour may be the one feeding it.** A frame fed by a single cable into its side,
  with nothing else joined, does not count that cable as a place to go: it falls back to its nub,
  and with the nub pointing at air it is a tail.
- **A source frame has no input side.** A frame lit by a lever on its back or dust on its stand
  counts every bracket-side neighbour. The stand is never a neighbour for this rule.
- **Symmetry panels** ([05-symmetry](../witness/05-symmetry.md), not yet modelled) finish two lines
  on two ends at once. Every used end counts, so a symmetry solve can power two sides, and the
  invisible line's end routes power just like the visible one. Write symmetry with this in mind.
- **Loops do not latch.** Frames whose ends point round a square, or a row of one-end frames each
  feeding the one before it, go dark with the rest of the chain when the redstone is cut: power
  is traced back to a source, never round the chain. A **cable** from a solved frame's output
  round to one of its inputs is no latch either: cables, frames and stands are one network and
  power is traced through all of it ([06](06-cable.md)). Only vanilla redstone in the loop, dust
  pressed against a one-end frame's side or a repeater, can hold a chain up, as it would any
  vanilla component.
- **Facing does not matter** for joining. Two adjacent frames facing different ways are still joined
  if the model draws the bracket between them; "left" and "right" for the exit are the panel's own.

---

# Implementation

## Status in this mod

**All four slices are built and verified for a horizontal chain, plus redstone out.**
The one-end routing and the network walk (2026-09-03) pass their tests and were seen working in
game the same day. Seen in game
2026-08-29: frames dark with the source off, lit when a lever on the back is flipped, the solver
opens only when lit, and solving a frame whose end sits on the edge facing the next frame lights
that frame (and the one after stays dark). A repeater on the exit side of a solved frame drives dust (slice 4). Not yet seen: a vertical
exit (top / bottom) and the lever-off cascade with the line stripped.

- `powered`: written by `RedstoneNetwork.refresh`, the one walk over cables, frames and stands
  that [06](06-cable.md) describes. The frame's part of it: `joinedFrames` (symmetric across
  facings) links frames; `inputDirections` (any side for `Panel.hasSingleEnd`, every side but the
  nub sides per `Panel.endSides` otherwise) says which cables feed it and where `hasVanillaInput`
  looks for a lever or dust, **members of the network excluded**; a solved frame feeds the frames
  `feedsFrame` names (every joined frame for one end, the `outputDirections` otherwise) and the
  cables on `outputDirections` (all four bracket sides for one end; else the one bracket side with
  a frame or cable once the input side is left out; else the `EXIT` sides: `outputSides` in
  `FrameRouting.kt`, pure). `write` sets `powered`, keeps `solved` only while powered, records the
  input direction on the block entity, refreshes the joins and strips the line.
  Light 10. Rewritten 2026-09-03 for the one-end rule and the network walk; seen working in game the same day.
- `solved` + `exit`: set by `PuzzleFrameBlockEntity.submitSolution` on the server after re-judging
  the submitted path with `Panel.verdict` (`items/data/Solutions.kt`, the same pure function the
  client's `PuzzleSolver.submit` uses). Light 11, a step above On rather than a lamp. Sticky: cleared only when `powered` drops, and
  the line is stripped from the panel at that moment.
- `exit` is `List<Node>.exitSides()` of the accepted path, as an `Exit` enum on the block state
  (one side, or two for a diagonal corner nub).
- Lit faces draw at a constant lightmap (`pack(PANEL_GLOW, PANEL_GLOW)`), not floored ambient:
  with ambient in the mix an eight-frame row visibly dimmed along its length (seen 2026-08-29).
  Block light is 10 On / 11 Solved and only affects the spill on stands and floor.
- Unpowered: `PuzzlePanelRenderer.renderPanel(lit = false)` draws only the backdrop at
  `UNLIT_BRIGHTNESS` under ambient light; the face click plays `POINTLESS_CLICK` and returns
  without a screen.
- The line is stripped on insert and on every way out (`interact`, `retrieveOnAttack`,
  `playerWillDestroy`), so a solution never travels in the item.
- `connections()` is the single source for the four `*_connected` flags (placement and
  `neighborChanged` both call it). They still only select model parts; power uses
  `sideDirection` directly.
- `getBlockSupportShape` reports the back face as full so a lever / button / torch attaches there.
- `anchored` (2026-08-30, seen in game 22:33: a frame on a sandstone wall, bar reaching the wall): set in `connections()` when there is no stand
  below and `isFaceSturdy` holds on the block behind; draws `iron_puzzle_frame_anchor` (a 2 x 4
  px bar from the base plate back to the wall).
- `IronStandBlock` holds `powered` as block state, written by the same network walk as the frame
  on it (a vanilla signal into any side but the top, or a cable directly underneath), and answers
  `getSignal` with 15 to the block above only.
- Redstone out: `isSignalSource`, `getSignal` = 15 for a block on an `outputDirections` side.
  Weak power only; no `getDirectSignal`, so a solid block on the exit side cannot relay the
  signal back.

## Traps, do not re-derive

- **Panels are drawn mirrored on x.** A nub at the panel's *low*-x edge is the one the player sees
  on the *right*. `exitSides` maps `dx < 0` to `Side.RIGHT` for that reason, and the unit test
  pins it with the observed case. Reasoning from the render transforms gives the opposite answer
  and cost a full test round.
- **`getSignal`'s `direction` runs from the asking block towards this one**, so a block on the
  exit side asks with the *opposite* of that side's direction. Verified 2026-08-29 by a repeater
  against the exit side driving dust; the chain itself goes through the network walk and never
  calls `getSignal`, so the lit-neighbour shot alone would not have proved it.
- **Two earlier input rules fed back and were replaced.** Back-face emission powered the block
  behind, which powered the frame. Then "a solved frame ignores its exit side" let a shared cable
  run un-solve and re-light frames in a loop (seen 2026-08-29 with two runs meeting under one
  stand). Both were local rules reading a signal the frame had itself caused. The rule now is
  static per panel (nub sides never take input when there is a choice of ends) and, for the rest,
  a walk from real sources; do not reintroduce a solve-dependent local input check.
- **Only `setBlock` on a real change** in `refresh`, or two frames update each other forever.
- **Write the whole network before telling any neighbour.** `RedstoneNetwork.refresh` writes with
  `UPDATE_CLIENTS` and calls `updateNeighborsAt` afterwards. With `UPDATE_ALL` in the loop, dust
  beside the first written frame re-walks the half-written network from a nested refresh, writes
  the right answer, and the outer loop then overwrites it from its stale walk (found in review
  2026-09-03, before the cable joined the walk: F1 (lever) to F3 in a row with a cable from F1's
  exit round to F3's back, pop F1's panel, and F3 stayed On).
- **`onPlace` runs before the block entity exists**, so a freshly placed frame walks as
  panel-less. Right for a player placing one; a frame placed with a pre-filled entity (a
  structure, `/setblock` with data) is not powered until something beside it changes.
- **`Panel.hasSingleEnd` counts nubs, `Panel.endSides` counts anchored nubs**, so an orphan nub
  with no anchor edge makes them disagree. Neither the composer nor crafting makes one today.
- **A member of the network is never a redstone input.** `getSignal` from a solved frame, a lit
  cable or a powered stand looks like any other signal, and letting one in brings the latch
  straight back through the local check even with the walk in place (`RedstoneNetwork.vanillaSignal`).
- **`via` is first arrival, not best path.** A frame's distance resets to 0 on arrival so its
  first feeder is final; a cable's `via` may not be on its shortest path and is not used.
- **Dust against a one-end frame's side latches.** The frame emits into it and reads it back, and
  dust is outside the walk. Vanilla-class behaviour, the same as a repeater loop; not fixed.

## Why the chain is a walk, not a neighbour update

A solved frame's emission toward its exit neighbour would satisfy that neighbour's redstone input
on its own, and until 2026-09-03 the chain was exactly that plus an explicit `isFedByChain`. That
only works while power flows one way. The one-end rule sends it every way, and two solved one-end
frames side by side then each count the other as a source: cut the redstone and the row stays lit
(the design half, "power comes from the source"). A frame remembering which side its power came
in on was weighed and dropped: it needs state, an invariant, and an "ignored until reset" edge
case, and it still leaves the cable latch. So cables, frames and stands are one network walked
from vanilla sources (`RedstoneNetwork`, [06](06-cable.md)): members ignore neighbour updates
whose source is a member, and placement (`onPlace`), removal (`affectNeighborsAfterRemoval`), a
panel going in or out, and `submitSolution` each start a walk directly.

## Only one place to go (built and seen in game 2026-09-24)

`outputSides(singleEnd, exit, neighbours, input)` in `FrameRouting.kt` is the whole priority,
tested in `FrameRoutingTests`. The frame supplies `neighbourSides` (bracket sides holding a frame
or a `CableBlock`; the stand below is `Side.BOTTOM`'s block but never a member on a bracket side,
so it never counts) and the input side.

The input side comes from the walk: `walkNetwork` records `via`, the neighbour that first fed each
member (none for a source), and hands it to `feedsVia` so a frame being expanded knows where its
own power came in. `RedstoneNetwork.refresh` then writes it to `PuzzleFrameBlockEntity.inputFrom`
(saved as `input`, a `Direction`), because vanilla asks `getSignal` outside any walk. It is
derived state, rewritten by every walk and cleared when the frame goes dark; it is never read as
an input check, which is the latch the walk exists to prevent. `write` reports a change when only
the input moved, so dust beside a re-routed frame is told.

`inputDirections` is untouched: the fallback side stays an input, safe for the reason the one-end
rule is (power is traced from the source, so a cable the frame lights cannot power it).

## Not done

- `submitSolution` trusts the payload's position: no range check, no check that the sender has
  the solver open. A modified client could solve any powered frame from anywhere.
- No visual mark for the exit on the frame itself; the line is the only cue.
- Symmetry's two-ends-at-once routing waits on symmetry itself.
- The stand relays redstone upward only; a solved frame exiting downward into a stand goes no
  further.

## Sources

- `src/main/kotlin/com/xfastgames/witness/blocks/redstone/IronPuzzleFrameBlock.kt`: block state,
  connection flags, interaction, `refresh`.
- `src/main/kotlin/com/xfastgames/witness/blocks/redstone/RedstoneNetwork.kt`: the network the
  frame is a member of, and the one `refresh` for it; `NetworkWalk.kt` is the pure walk under it.
- `src/main/kotlin/com/xfastgames/witness/blocks/redstone/FrameRouting.kt`: `outputSides`, the
  output priority, with `FrameRoutingTests`.
- `src/main/kotlin/com/xfastgames/witness/blocks/redstone/IronStandBlock.kt`: the stand.
- `src/main/kotlin/com/xfastgames/witness/entities/PuzzleFrameBlockEntity.kt`: inventory, sync.
- `src/main/kotlin/com/xfastgames/witness/entities/renderer/PuzzleFrameBlockRenderer.kt`: world
  render.
- `src/main/kotlin/com/xfastgames/witness/items/data/EndPoints.kt`: nub placement and
  orientation, the source of the exit side.
- `src/main/kotlin/com/xfastgames/witness/screens/solver/PuzzleSolver.kt`: `submit`, the pure
  validation the server must reuse.
- `src/main/kotlin/com/xfastgames/witness/screens/solver/PuzzleSolverScreen.kt`: `submitTrace`,
  `updateLine` (the client-only write).
- `src/main/resources/assets/witness/blockstates/iron_puzzle_frame.json`: the connection models.
- `rules/witness/02-end-points.md`, `rules/witness/05-symmetry.md`: the rules this wiring hangs
  off.
