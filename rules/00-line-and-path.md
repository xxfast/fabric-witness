# 00 — Line and path basics

**Category:** line mechanic

## Rule

The solution is a single continuous, simple path drawn along grid edges, from a start point to an
end point. Formally: a simple path (no repeated vertex) in the panel's grid graph, connecting a
start vertex to a destination vertex.

- The path moves node to node along edges only. No diagonals, no cutting through cells, no
  travelling along an edge that doesn't exist in the graph.
- The path may not cross or touch itself. Every node is visited at most once, and every edge is
  used at most once (a simple path implies both).
- The path does not have to cover the whole grid. Unvisited edges and nodes are fine unless some
  other rule forces them (hexagons, see [04-hexagon-dots.md](04-hexagon-dots.md)).
- Backtracking while drawing is allowed and just erases the tail of the line; only the final shape
  submitted is validated.
- A panel with no symbols at all is solved by reaching any end point from any start point. There
  is nothing else to check.

## Why it matters

Everything else is built on this. The finished path splits the grid into one or more closed
**regions**, bounded by drawn line segments and the panel border, and every region symbol
(squares, stars, polyominoes, triangles, eliminators) is validated against the region it sits in.
Get the path/region model wrong and every downstream rule is unverifiable.

## Grid shapes

The grid is a graph of nodes connected by edges; it does not have to be a full rectangular
lattice. In the actual game, panels commonly have missing nodes and edges cut into an otherwise
rectangular lattice, producing irregular (non-rectangular) outlines, "cut corner" shapes, and
branching tree-like layouts. The formal literature (Demaine et al., "Who witnesses The Witness?")
models this generally as a planar grid graph; their complexity results are stated for rectangular
boards specifically, but the game's puzzle types generalize to any planar grid graph without
changing the line rule itself.

Note: hexagonal *dots* are a symbol placed on a node or edge (see
[04-hexagon-dots.md](04-hexagon-dots.md)); they are not evidence of an actual hexagonal-tiling
grid. The game's panels are built from rectangular cells (with pieces missing), not hex tiles.
"Skewed" or oddly-proportioned panels in the game are still rectangular lattices rendered at an
angle or non-uniform spacing; the topology is unchanged.

## Edge cases

- A path that reaches an end point but is not connected back to a start point is not a solution.
  A dangling line drawn from a start point that stops short, or that reaches a dead-end node with
  no `END` modifier, does not count.
- With multiple start or end points on one panel, exactly one path is drawn per line; picking
  which start/end pair to use is often the puzzle (see [01-start-points.md](01-start-points.md),
  [02-end-points.md](02-end-points.md)).
- Regions are defined by the path plus the panel border. The border always closes a region; a
  path that never touches certain border segments still gets a valid (if large) region bounded by
  that unused border.
- On symmetry panels ([05-symmetry.md](05-symmetry.md)) there are two simple paths drawn at once,
  and neither may cross the other or itself; regions are defined by the union of both lines.
- A broken edge ([03-broken-edges.md](03-broken-edges.md)) is removed from the graph before
  path-finding, not treated as a valid-but-forbidden edge; it also does not close a region on its
  own, since no line segment is drawn there.
- Touching (sharing a single node) counts as crossing for self-intersection purposes; the path
  cannot pass through an already-visited node even without reusing an edge.

## Implementation notes

Data model: an undirected graph of `Node` (grid vertices, each carrying a `Modifier`) connected by
`Edge`s (also `Modifier`-valued, so an edge can itself be `NORMAL`, `BREAK`, etc.). The player's
drawn line is a second, sparser graph over the same node set: a simple path, so at most one
predecessor and one successor per node.

Validating "is this a legal path so far": each new node added to the path must (a) be adjacent to
the current path tip in the panel graph, (b) be connected by an edge that isn't `BREAK`/missing,
and (c) not already appear in the path. This mod enforces (c) proactively during tracing rather
than rejecting after the fact: `PuzzleSolver.traceLimit` treats each candidate segment as a
ray against every already-traced segment (`intersectionParameter`, a standard segment-intersection
test via 2D cross products) and clamps how far the cursor can travel down that edge before it
would cross or touch the existing line, so self-intersection is geometrically impossible to draw
rather than something checked after submission.

Validating "is this a legal complete solution": the path must start at a node whose modifier is
`START` and end at a node whose modifier is `END` (or on the tip of a segment leading to one, see
`tracingTip`). Region-based symbol validation (squares, stars, polyominoes, triangles) is not yet
implemented in this mod (see below), but the standard approach is: build the region graph by
flood-filling grid cells through un-traversed cell-adjacencies (two cells belong to the same region
iff no drawn line segment or panel border separates them), then check each region's symbols
against its contents independently.

## Status in this mod

Partially modelled. The graph types live in
`src/main/kotlin/com/xfastgames/witness/items/data/Edge.kt` (`Modifier` enum, aliased as `Edge`)
and `Node.kt`/`Graph.kt`/`Panel.kt` (`Panel.Grid`, `Panel.Tree`, `Panel.Freeform` variants, all
backed by a Guava `ValueGraph<Node, Edge>`).

`src/main/kotlin/com/xfastgames/witness/screens/solver/PuzzleSolver.kt` implements live line
tracing: starting from a `START` node only, extending along edges toward the movement direction
(`chooseSegment`, edge alignment scoring; `NONE` edges are unusable and `BREAK` edges can only be
entered as far as the gap, see [03-broken-edges.md](03-broken-edges.md)), self-collision
prevention during drawing (`traceLimit`/`intersectionParameter`), node-revisit rejection
(`arriveAt`), and backtracking.

`PuzzleSolver.submit` validates the line rule only: at least one edge, first node `START`, last
node `END`, no repeated node, every consecutive pair joined by an existing traversable edge. It
moves the state through `SolutionSubmitted` to `SolutionAccepted` or `SolutionRejected`
(`PuzzleSolverViewModels.kt`) and returns the line to render, empty on rejection.
`PuzzleSolverScreen` submits on left-click while tracing. There is still no region flood-fill and
no symbol validation, so a panel carrying symbols is accepted on the line rule alone.

## Sources

- [SerGreen/TheWitnessPuzzles, Puzzle Rules Guide](https://github.com/SerGreen/TheWitnessPuzzles/blob/master/Puzzle%20Rules%20Guide/RulesGuide.md), "Lines may not cross." / "Lines will split panel into areas. Each area is isolated from other areas."
- [Abel, Bosboom, Demaine et al., "Who witnesses The Witness? Finding witnesses in The Witness is hard and sometimes impossible" (FUN 2018)](https://drops.dagstuhl.de/entities/document/10.4230/LIPIcs.FUN.2018.3), formal statement: "the goal is to draw a [simple] path in a rectangular grid graph from a start vertex to a destination vertex"; clue types and complexity results.
- `src/main/kotlin/com/xfastgames/witness/items/data/Edge.kt`, `Node.kt`, `Graph.kt`, `Panel.kt`, this mod's data model.
- `src/main/kotlin/com/xfastgames/witness/screens/solver/PuzzleSolver.kt`, `PuzzleSolverViewModels.kt`, this mod's solving/tracing logic.
