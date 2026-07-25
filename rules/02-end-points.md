# 02 — End points

**Category:** line mechanic

## Rule

The path is complete when it reaches an end point, rendered as a short nub protruding from the
grid, usually off an edge or a corner (occasionally off a node with no edge at all, pointing
outward into empty space).

- A panel can have multiple end points. Reaching any one of them terminates that line.
- Only one end point is consumed per line. On a normal (non-symmetry) panel that means exactly
  one end point is used per solution; the rest are simply unreached.
- The nub itself is a terminal node: nothing continues past it, and it cannot be passed through to
  reach further grid.
- Which end point is reachable at all depends on the rest of the solution: an end point is not
  automatically valid just because it exists, the drawn line still has to arrive there without
  self-intersecting and while satisfying every other rule.

## Edge cases

- End points sit outside the main lattice, but the final segment (last grid node to nub) is a real
  edge like any other. It can carry a hexagon dot and it does close off a region on that side, the
  same as a normal edge. See [04-hexagon-dots.md](04-hexagon-dots.md).
- That final segment also counts toward a triangle's edge total if a cell borders it, same as any
  other drawn edge (see [10-triangles.md](10-triangles.md)) and the same as broken edges affect
  triangle counts on the other side.
- Which end point you use changes where the line falls and therefore how the grid splits into
  regions. On symbol-heavy panels the "solution" is really "line shape and end point choice
  together"; two different valid-looking lines to two different end points can produce different
  pass/fail results against the same squares, stars, or polyominoes.
- Symmetry panels need both lines to land on an end point at the same moment: the player's line on
  one end point and the mirrored line on its own, simultaneously. Arriving at an end point on one
  side while the mirrored line is still short, or has nowhere valid to land, is not a solution. See
  [05-symmetry.md](05-symmetry.md).
- Some panels use multiple end points as the actual puzzle content: the panel is trivially solvable
  to one end point and unsolvable (or symbol-violating) to the others, so picking the right exit is
  the challenge rather than a formality.
- In the base game, reaching different end points can also trigger different world-side effects
  (opening different doors, activating different segments of a path). That is an environment/panel
  wiring concern, not a line-drawing rule, and is out of scope for this file.
- Unverified: whether an end point's connecting edge can itself carry a broken-edge gap in the
  original game (i.e. an unreachable end point). Rules sources consulted don't call this out
  either way; treat it as plausible but unconfirmed.

## Implementation notes

Data model: an end point is just a `Node` whose `Modifier` is `END`, sitting in the same
`ValueGraph<Node, Edge>` as every other node, connected to the lattice by one edge. There is no
separate "exit" type, only a modifier value, so a panel can have as many `END` nodes as it wants
by construction.

Validating a candidate solution against end points needs two things:

1. **Termination check** - the traced path (an ordered list of nodes with edges between
   consecutive pairs) is only a candidate solution if its last node has `Modifier.END`. Anything
   short of that is an incomplete trace, not a failed one.
2. **Region computation** - once terminated, treat every edge actually drawn (including the final
   segment into the nub) plus the panel border as region boundaries, flood-fill the faces of the
   planar graph, and validate each region's symbols independently. The end point's own edge
   participates in this flood fill exactly like a `NORMAL` edge; it is not special-cased away.

A straightforward solver (see `TheWitnessSolver` reference implementation) just does
branch-and-bound: DFS/backtrack from each `START` node over the graph, treat arrival at any `END`
node as a candidate full solution, and only then run the symbol validators over the resulting
regions. Multiple end points just mean multiple leaves to try in that search, not a special case in
the traversal itself.

## Status in this mod

Partially modelled. `Modifier.END` exists on `Node` (`items/data/Edge.kt`).

Authoring: the composer's end toggle (`PuzzleComposerScreen`, `EndIcon`) places one. Clicking a
border node with it selected hangs a nub off that node via `Panel.withEndPointToggled`
(`items/data/EndPoints.kt`): a new `END` node `END_POINT_LENGTH` (one line thickness) outside the
lattice, joined by a `NORMAL` edge. Clicking the same node again advances it through
`Panel.endPointOrientations`: an edge node has one way to point, so that's a plain on/off toggle,
while a corner cycles diagonal (the default), then squared off along each of its two borders, then
bare. Clicking the nub itself removes it. Interior nodes are refused. Existing nubs are excluded
when measuring the lattice bounds, so one nub doesn't move the border for the next edit.

Rendering: the nub's edge draws like any other edge; `PuzzlePanelRenderer.renderNode` and
`WPuzzleEditor.drawGraph` round off its tip with a half-thickness disc.

Solving: `PuzzleSolver` traces along the nub's edge like any other and stops advancing once the
current node is `Modifier.END` with no active segment (`buildLine`), and `isValidSolution` requires
the last node of a submitted path to be `END` (see [00-line-and-path.md](00-line-and-path.md)).

What's not modelled yet: there is no region computation and no symbol validation at all in this
mod (regions, hexagons-on-edges, squares, stars, polyominoes, triangles, eliminators are all
unimplemented). So which end point you use terminates the line and decides accept/reject on the
line rule alone, it can't yet change the outcome through the symbols in the regions it forms.

## Sources

- [The Witness Puzzle Rules Guide (SerGreen/TheWitnessPuzzles)](https://raw.githubusercontent.com/SerGreen/TheWitnessPuzzles/master/Puzzle%20Rules%20Guide/RulesGuide.md) - confirms region-splitting-by-line and general symbol rules; does not itself call out endpoint mechanics explicitly.
- [The Witness Wiki - Entry Area (Walkthrough)](https://thewitness.fandom.com/wiki/Entry_Area_%28Walkthrough%29) and related Fandom pages (via search) - confirms multiple start/end points per panel, that only a rounded nub counts as an exit, and that different exits can gate different in-game effects.
- [GameFAQs - Puzzles & Symbols walkthrough](https://gamefaqs.gamespot.com/pc/969704-the-witness/faqs/82392/puzzles-and-symbols) - general panel/symbol reference.
- [Overv/TheWitnessSolver](https://github.com/Overv/TheWitnessSolver) and its [write-up](https://overv.github.io/TheWitnessSolver/) - branch-and-bound solver treating arrival at an exit node as a candidate solution, validated afterward.
- [Demaine, Hearn, et al., "Who witnesses The Witness?"](https://export.arxiv.org/pdf/1804.10193) - complexity results for panels with hexagons/broken edges (background on graph-model framing; PDF text did not extract cleanly, treated as directional context only, not directly quoted).
