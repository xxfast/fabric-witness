# 01 — Start points

**Category:** line mechanic

## Rule

Drawing begins at a start point, rendered as a filled black circle on a grid node (see
[00-line-and-path.md](00-line-and-path.md) for the path itself). The circle can sit on any node
in the lattice: a corner, an edge midpoint, or an interior intersection. Clicking or dragging from
it picks up the line; nothing else on the panel is a valid pickup point.

- A panel can have multiple start points. Any one of them may be used, and choosing the right one
  is often the puzzle: the same panel can be unsolvable from one circle and trivial from another.
- Exactly one start point is consumed per line drawn. On symmetry panels (two lines at once), each
  line has its own start and the two are not interchangeable, see
  [05-symmetry.md](05-symmetry.md).
- A start point is otherwise a normal graph node: once picked up, the path leaves it along any
  edge incident to it, subject to the usual no-crossing, no-revisit rule.
- A start point carries no symbol of its own. It is not also a hexagon dot, a broken edge, or an
  end point; those are distinct node/edge roles in the underlying model.

## Edge cases

- Multiple start points is a common source of apparently-impossible panels: the layout looks
  unsolvable until you notice a second circle elsewhere on the border.
- On symmetry panels the second line's start point can be invisible (never rendered), but it still
  exists, still moves, and still collides with the visible line. See
  [05-symmetry.md](05-symmetry.md) for how the two lines interact.
- A start point placed at a true corner behaves identically to one placed mid-edge or mid-grid;
  position doesn't change the rule, only which edges are reachable from it.
- Two start points that are both viable (either one solves the panel) are rare but not disallowed;
  the puzzle doesn't require a unique entry point, only a solution to exist from at least one.
- Unverified: whether the original game ever places a start point with only one incident edge
  (a dead-end start). Plausible on irregular lattices but not confirmed against a specific level.

## Implementation notes

Data model: a start point is a `Node` whose modifier/role is `START`, no different in kind from
any other node except for that tag. Multiple `START` nodes can coexist in the same graph.

To validate or drive a solver:

1. Enumerate all nodes tagged `START` in the panel graph.
2. For each candidate start, run the path search (DFS/BFS over the grid graph honoring broken
   edges, no-revisit) looking for any simple path from that start to any node tagged `END`.
3. The panel is solvable if at least one `(start, end)` pair yields a path whose resulting regions
   satisfy every symbol constraint (squares, hexagons, triangles, polyominoes, eliminators).
4. For symmetry panels, the search space is pairs of synchronized moves (one per line) rather than
   a single path; do not reuse the single-line search directly, see
   [05-symmetry.md](05-symmetry.md).
5. A start point with zero incident edges (isolated node) is a malformed panel, not a puzzle
   variant; reject it at panel-authoring time rather than in the solver.

## Status in this mod

Modelled. `Modifier.START` in `src/main/kotlin/com/xfastgames/witness/items/data/Edge.kt`.

- `PuzzleSolverScreen` (around the click handler) scans `puzzlePanel.graph.nodes()` for any node
  with `modifier == Modifier.START` under the cursor and calls
  `PuzzleSolver.startTracingLine(panel, start)` to begin the trace, so multiple start points
  per panel are already supported: any of them can be clicked to start.
- `PuzzleSolver` treats the chosen start like any other node once tracing begins; it doesn't
  special-case `START` beyond the initial pickup (`startTracingLine` only checks the node exists in
  the graph).
- The composer (`PuzzleComposerScreen`, `WPuzzleEditor`) lets an author cycle a node's modifier to
  `START`, so authoring multiple start points on one panel is possible today.
- Symmetry (a second, simultaneous line from its own start) is not modelled; see
  [05-symmetry.md](05-symmetry.md).

## Sources

- [SerGreen/TheWitnessPuzzles Rules Guide](https://raw.githubusercontent.com/SerGreen/TheWitnessPuzzles/master/Puzzle%20Rules%20Guide/RulesGuide.md) (checked for start-point coverage; the guide does not separately document start points, it assumes them)
- [The Witness Symmetry Island Walkthrough (Fandom)](https://thewitness.fandom.com/wiki/Symmetry_Island_(Walkthrough))
- [GameFAQs: Puzzles & Symbols walkthrough](https://gamefaqs.gamespot.com/pc/969704-the-witness/faqs/82392/puzzles-and-symbols)
- [Who witnesses The Witness? (Demaine et al., FUN 2018)](https://erikdemaine.org/papers/Witness_FUN2018/paper.pdf), formalizes puzzles as a simple path from a start vertex to a destination vertex on a grid graph
- [sigma144/witness-randomizer](https://github.com/sigma144/witness-randomizer), puzzle data model reference (nodes tagged start/end/hexagon)
