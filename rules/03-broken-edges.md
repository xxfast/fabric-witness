# 03 — Broken edges (gaps)

**Category:** line mechanic

## Rule

A gap drawn in the middle of a grid edge means that edge cannot be traversed. The path must route
around it: the two nodes it would normally connect are, for path-drawing purposes, not adjacent.

Purely subtractive. A broken edge asserts nothing that needs to be satisfied; it just removes an
option from the graph the player is drawing on. There's no count, no color, no direction to it.

In the base game, gaps are introduced on the Glass Factory plates, where they combine with
[symmetry lines](05-symmetry.md): both the real line and its mirrored twin have to route around
the same break, which is what actually makes those puzzles hard (a break that's trivial to dodge
alone can be awkward to dodge for two paths moving in lockstep).

## Edge cases

- A broken edge still occupies a position on the grid and still looks like part of a cell
  boundary, but since no line segment can ever cover it, it does **not** help close a region.
  Regions are bounded only by drawn line segments plus the panel border
  ([00-line-and-path.md](00-line-and-path.md)); an un-traversable edge is exactly like an edge the
  player simply chose not to draw on.
- Interacts with [triangles](10-triangles.md): a cell edge that's broken can never be one of the
  edges counted toward that cell's triangle total, since the line can never lie on it. A triangle
  clue that requires all four of a cell's edges is already impossible in the base game (it would
  seal the cell into its own region); a triangle clue on a cell with a broken edge is similarly
  constrained to at most 3 satisfiable edges.
- Interacts with [hexagon dots](04-hexagon-dots.md): a dot cannot be placed in the middle of a
  broken edge, because the line would have to pass through a point it can never reach. A dot at
  one of the edge's *endpoints* (a shared vertex) is unaffected; the vertex itself is still a valid
  node, only the edge leading through the gap is removed.
- A broken edge adjacent to a start or end point doesn't disable the point itself, only that one
  approach to it; if it's the only edge touching that node, the node becomes unreachable and the
  puzzle is unsolvable (an authoring bug, not a rule interaction).
- Two adjacent broken edges meeting at a node don't do anything special beyond removing both
  edges; if every edge at a node is broken, that node is simply isolated from the graph.
- Breaking an edge that lies on the panel border doesn't change the border's role in closing
  regions from the outside; border adjacency for region-closing purposes is about the panel
  boundary itself, not about whether an edge could be drawn along it.

**Unverified:** community materials describe gaps as a single visual/mechanical element (a fixed
gap in the middle of an edge); I could not find a primary source describing a distinct second
"hairline" gap variant with different solving rules. The distinction some fan write-ups draw
between a "full" and a "thin" gap may just be a visual-design/readability choice per puzzle area
rather than a separate rule, since every source found treats "the line cannot cross this edge" as
the entire mechanic regardless of how the gap is drawn. Treat "full break" and "hairline break" as
the same rule with the same effect until a primary source says otherwise.

## Implementation notes

**Data model.** Represent the grid as an undirected graph of nodes with edges carrying a modifier
value. A broken edge is an edge that exists in the graph's topology (it still has two endpoints
and a position, needed for rendering and for region-boundary math) but is tagged as impassable.

**Validation / traversal.** When computing which moves are legal from the player's current node
(or when statically checking whether a proposed solution path is legal), any edge tagged as broken
must be excluded from the adjacency the tracer is allowed to step onto. This is a graph *filter*,
applied once, not a per-rule check: everything downstream (win condition, region flood-fill,
triangle counts) just needs to see a graph that never contained the broken edges, or needs to
independently skip them when walking adjacency.

Two equivalent implementation strategies:
1. **Filter at trace time.** Wherever the tracer enumerates candidate next-nodes, drop any
   candidate reached only through a broken edge.
2. **Filter once into a derived graph.** Build a "traversable subgraph" (drop all broken edges up
   front) and hand that to the tracer, the region flood-fill, and the win-condition check alike.
   This is usually cleaner since it means only one place in the codebase needs to know that
   `BREAK` exists.

For region-building specifically (used by squares/stars/polyominoes/triangles/eliminators):
broken edges should be treated as *not drawn* when building the dual graph of cells, i.e. they
never split two cells into separate regions on their own; only the player's actual drawn line does
that.

## Status in this mod

Declared but not enforced by the solver. `Modifier.BREAK` exists in
`src/main/kotlin/com/xfastgames/witness/items/data/Edge.kt`
(`enum class Modifier { NONE, NORMAL, BREAK, DOT, START, END, HIDDEN }`), and it round-trips
through the panel graph, the composer editor, and rendering:
- `PuzzleComposerScreen` lets you toggle an edge between `NORMAL` and `BREAK`
  (`edge.nextIn(Modifier.BREAK, Modifier.NORMAL)`).
- `WPuzzleEditor.drawBrokenLine` and `PuzzlePanelRenderer`'s `` `break`() `` draw the gap visually
  in the composer preview and on the physical panel.

But `PuzzleSolverDomain.chooseSegment`, which decides which edges the player's trace is allowed to
extend onto, calls `panel.graph.adjacentNodes(current)` and filters candidates only by movement
alignment and self-intersection; it never reads the edge's `Modifier` value at all. A `BREAK` edge
is exactly as traversable as a `NORMAL` one during solving today. So the gap renders correctly but
does not currently block the line; enforcing it requires `chooseSegment` (or a graph built ahead
of it) to exclude edges whose `graph.edgeValueOrDefault(current, candidate, ...)` is `BREAK` before
alignment/self-collision filtering runs. There's also no region-partitioning step anywhere in the
solver yet, so the "doesn't close a region" edge case has nothing to plug into either.

## Sources

- [SerGreen/TheWitnessPuzzles rules guide](https://raw.githubusercontent.com/SerGreen/TheWitnessPuzzles/master/Puzzle%20Rules%20Guide/RulesGuide.md) (covers lines, hexagons, squares, suns, tetrominoes, erasers, triangles; does not separately document gaps, consistent with treating them as a plain subtractive edge removal rather than a distinct symbol)
- [GameFAQs: Puzzles & Symbols](https://gamefaqs.gamespot.com/pc/969704-the-witness/faqs/82392/puzzles-and-symbols) and community summaries of it ("dots tell you where the line must go while gaps tell you where it can't go")
- [GameFAQs: Environmental Puzzles](https://gamefaqs.gamespot.com/pc/969704-the-witness/faqs/82392/environmental-puzzles) and [Glass Factory area discussion](https://thewitness.fandom.com/wiki/Glass_Factory) (gaps introduced alongside symmetry lines; "dividers and breaks in the lines... forcing you to consider your path in both directions")
- [Demaine et al., "Who witnesses The Witness?"](https://erikdemaine.org/papers/Witness_FUN2018/paper.pdf) (formal treatment of Witness-style constraints; consulted for terminology, not directly quotable from the fetched text)
- This repo: `src/main/kotlin/com/xfastgames/witness/items/data/Edge.kt`,
  `src/main/kotlin/com/xfastgames/witness/screens/solver/PuzzleSolverDomain.kt`,
  `src/main/kotlin/com/xfastgames/witness/screens/composer/PuzzleComposerScreen.kt`,
  `src/main/kotlin/com/xfastgames/witness/screens/widgets/WPuzzleEditor.kt`
