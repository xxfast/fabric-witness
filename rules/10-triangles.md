# 10 — Triangles

**Category:** region symbol (but validated per cell, not per region)

Also called "Doritos" by the community, for their shape.

## Rule

A cell marked with N orange triangles (N is 1, 2, or 3) must have the solution line drawn across
exactly N of its four edges. One triangle means exactly one bounding edge is on the line, two means
exactly two, three means exactly three.

Exact, not "at least" and not "at most". A cell with one triangle whose two edges are both drawn
fails, same as if zero were drawn.

Four triangles never appear in the base game or in Challenge mode. The reason is structural, not
stylistic: the solution is a single simple path from a start point to an end point, and a simple
path cannot use all four edges of one cell without revisiting a node, since that would require the
path to close into a loop around the cell. A count of 4 is therefore unsatisfiable by construction
and the game never generates it. (Loop-drawing panels, if any existed, would be the exception, but
none ship in the retail game.)

## Edge cases

- The cell's region membership is irrelevant. Every other region symbol is checked against the
  region containing it; triangles are the odd one out and only ever look at their own four edges,
  regardless of which region ends up owning the cell.
- A broken edge ([03-broken-edges.md](03-broken-edges.md)) can never be drawn, so it can never
  count toward a triangle's total, even though it still occupies one of the cell's four sides.
- A hexagon dot ([04-hexagon-dots.md](04-hexagon-dots.md)) sitting on one of the cell's edges does
  not change the triangle count; it only requires that edge (if drawn) to pass through the dot.
  The edge still counts once toward the triangle total whether or not it happens to carry a dot.
- A corner-only touch does not count. The line must lie along the edge itself (endpoint to
  endpoint), not merely pass through one shared corner node.
- Triangles on edge cells or corner cells of the grid still need all of their (fewer than 4, if on
  the border) edges considered normally; the panel border is not a special case for the count.
- Triangles are colored and therefore count as a colored object for a same-colored star's pairing
  rule ([07-stars.md](07-stars.md)), on top of satisfying their own edge-count rule. In practice
  the color is always the same fixed orange, it never varies per panel the way square or polyomino
  colors do, so a triangle only ever pairs with an orange star ([07-stars.md](07-stars.md)).
- An eliminator ([11-eliminators.md](11-eliminators.md)) can cancel a failed triangle's edge-count
  rule, but a cancelled triangle still counts as a colored object for star pairing, exactly like a
  cancelled colored square.
- Verified: no source describes triangles as a minimum-count or maximum-count rule; every source
  (SerGreen's rules guide, the Fandom wiki, GameFAQs, community explainers) agrees the count is
  exact. Unverified: whether any DLC, community-made, or Challenge-mode panel ever bends this to a
  minimum; nothing found suggests it does.

## Interactions

- Composes with polyominoes/negative polyominoes ([08](08-polyominoes.md),
  [09](09-negative-polyominoes.md)) in the same panel with no special-case behavior: triangles are
  evaluated per cell, polyominoes per region, independently.
- Composes with symmetry ([05-symmetry.md](05-symmetry.md)): a symmetric panel with a triangle on
  one side needs its mirrored partner cell (if it has a triangle too) satisfied independently; the
  line's mirror image does not automatically satisfy the mirrored triangle unless the panel's
  geometry is itself symmetric about the drawn line.

## Implementation notes

Data model: for a given cell, identify its (up to) four bounding edges from the grid graph, i.e.
the four `Node`-to-`Node` edges around the cell's four corners. A cell's triangle count is a small
integer (1-3) attached to the cell (not to any single edge or node), so the panel's data model needs
a per-cell annotation distinct from the per-edge `Modifier` used for line mechanics.

Validation algorithm sketch, given a solved path (a set of drawn edges) and the panel graph:

1. For each cell that carries a triangle count `n`, gather its four boundary edges (fewer at panel
   borders, but triangles should not appear on cells missing edges).
2. Count how many of those edges are members of the drawn path's edge set. Treat a `BREAK` edge as
   always absent regardless of what the path claims, since it can never be traversed.
3. The cell passes iff the count equals `n` exactly. This check is independent of region
   computation, unlike every other symbol in this catalog: it does not need the flood-fill that
   partitions the grid into regions.
4. If eliminators are modelled, a failed triangle is a candidate error for eliminator resolution,
   exactly like a failed square grouping or hexagon.

This is one of the cheaper rules to implement precisely because it skips the region step: no
flood-fill or union-find over cells is needed, just an edge-membership count per marked cell.

## Status in this mod

Not modelled. `Modifier` (`src/main/kotlin/com/xfastgames/witness/items/data/Edge.kt`) only has
`NONE, NORMAL, BREAK, DOT, START, END, HIDDEN`, no per-cell triangle annotation exists in the data
model, and `PuzzleSolver`
(`src/main/kotlin/com/xfastgames/witness/screens/solver/PuzzleSolver.kt`) only traces the
line (movement, self-intersection, backtracking); it does not evaluate any region symbol, triangle
included.

## Sources

- [SerGreen/TheWitnessPuzzles rules guide](https://raw.githubusercontent.com/SerGreen/TheWitnessPuzzles/master/Puzzle%20Rules%20Guide/RulesGuide.md)
- [The Witness Wiki: puzzle elements](https://thewitness.fandom.com/wiki/Puzzle_elements)
- [GameFAQs: puzzles & symbols](https://gamefaqs.gamespot.com/pc/969704-the-witness/faqs/82392/puzzles-and-symbols)
- [Witness Game Secrets: Doritos](http://witnessgame.blogspot.com/2016/02/doritos.html)
- [Demaine et al., "Who witnesses The Witness?"](https://erikdemaine.org/papers/Witness_FUN2018/paper.pdf)
