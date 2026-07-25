# 06 — Colored squares

**Category:** region symbol

## Rule

Squares of different colors must be separated. Every region cut out by the finished line may
contain squares of at most one color.

Equivalently: the line must cut between every pair of differently-colored squares, so no two
squares of different colors ever end up in the same enclosed region.

Squares are placed in cells (not on grid nodes or edges), so they're validated against regions,
the same as stars, polyominoes, and triangles. They are the simplest region symbol: no count
constraint, no shape constraint, just "one color per region."

## Colors

Confirmed from the base game (via wiki walkthrough references and the SerGreen rules guide):
black, white, blue, green, purple, and orange squares all appear on panels in specific areas
(Entry Area uses black/white; Treehouse uses purple and orange alongside stars; other areas use
blue and green). The earliest panels teach black-vs-white only, then later areas introduce a
third and fourth color on the same panel to raise difficulty.

Unverified: secondary sources (gameplay guides, forum threads) also mention red, cyan, magenta,
and yellow squares somewhere in the game or its Challenge mode, but this could not be confirmed
against a primary source. Treat any color beyond black/white/blue/green/purple/orange as
unconfirmed for this file. The rule itself is color-agnostic regardless: the engine only checks
"same color or not," never which specific color.

## Edge cases

- A region with no squares is fine, vacuously satisfies the rule.
- A region with any number of same-colored squares is fine. There is no count constraint, only a
  separation constraint (contrast with [07-stars.md](07-stars.md), which does count).
- The grid does not wrap. A square in the far-left column and a square in the far-right column are
  not "adjacent" and are not required to be separated unless the same region actually contains
  both after the cut.
- A region can contain squares of one color plus other symbol types (a star, a triangle) as long
  as those other symbols validate independently; colored squares don't interact with shape-based
  rules (polyominoes, triangles) at all, only with same-cell color matching.
- Environmental/perspective puzzles in the base game sometimes recolor squares depending on view
  angle (colored glass panes shifting what's rendered). That's a world-drawing mechanic, out of
  scope per this catalog's README, and irrelevant to the panel's underlying data once solved.
- Two colors of squares plus broken edges is already enough to make solvability NP-complete
  (Abel, Demaine et al., "Who witnesses The Witness?"); the colored-square rule alone forces
  regions to be "partially monochromatic," and is one of the rule types the paper shows is
  independently sufficient (combined with the line-drawing problem) to make the puzzle NP-hard.

## Interactions with other rules

- **Stars** ([07-stars.md](07-stars.md)): a square counts as a colored object for a same-colored
  star's pairing rule. A star paired with exactly one same-colored square is a valid, common
  teaching pattern (seen paired on the same panels in the Treehouse area).
- **Eliminators** ([11-eliminators.md](11-eliminators.md)): an eliminator can cancel a colored
  square's separation failure the same way it cancels any other symbol failure in its region: it
  absorbs exactly one otherwise-failing rule.
- **Polyominoes/triangles**: no direct interaction. They're validated independently against the
  same region; colored squares never affect a shape-based symbol's outcome or vice versa.

## Implementation notes

**Data model.** Add a cell-level symbol, distinct from `Node`'s edge/vertex `Modifier`. Something
like:

```kotlin
data class SquareSymbol(val cellX: Int, val cellY: Int, val color: Int)
```

stored per-panel alongside the existing node graph (cells live between grid nodes, so `cellX`/
`cellY` index the grid of cells, not the grid of nodes the line is drawn on).

**Algorithm sketch.**

1. Build the region partition: treat grid cells as nodes of a dual graph, adjacent cells connected
   unless the drawn line runs along the edge between them. Flood-fill (BFS/DFS or union-find) to
   assign every cell a region ID.
2. For each region, collect the colors of all `SquareSymbol`s whose cell falls in that region.
3. Region fails iff the collected color set has size > 1 (ignoring uncolored cells).
4. If eliminators are present, defer failing regions to eliminator resolution
   ([11-eliminators.md](11-eliminators.md)) before declaring the whole puzzle unsolved.

This is naturally the same flood-fill pass every other region symbol (stars, polyominoes,
triangles) needs, so in practice one region-building step feeds all of them, and the square check
is just "count distinct colors per region."

## Status in this mod

Not modelled. `Modifier` in `src/main/kotlin/com/xfastgames/witness/items/data/Edge.kt` is
`{ NONE, NORMAL, BREAK, DOT, START, END, HIDDEN }`, purely a per-node/edge line modifier. `Node`
(`items/data/Node.kt`) carries only `x`, `y`, and that `modifier`, no concept of a cell or a cell
color. There is no region-partitioning step anywhere in `PuzzleSolverDomain.kt` either: solving
today only traces the line and checks line-mechanics modifiers (start/end/break/dot), it never
cuts the grid into regions or looks at cell contents. Region symbols as a category (this file,
stars, polyominoes, negative polyominoes, triangles, eliminators) are all unimplemented.

## Sources

- [SerGreen/TheWitnessPuzzles rules guide](https://raw.githubusercontent.com/SerGreen/TheWitnessPuzzles/master/Puzzle%20Rules%20Guide/RulesGuide.md)
- [Gameranx: puzzle types and rules guide](https://gameranx.com/features/id/36898/article/the-witness-puzzle-types-and-rules-guide/)
- [The Witness Wiki: puzzle elements](https://thewitness.fandom.com/wiki/Puzzle_elements)
- [The Witness Wiki: Treehouse walkthrough](https://thewitness.fandom.com/wiki/Treehouse_(Walkthrough))
- [The Witness Wiki: Bunker walkthrough](https://thewitness.fandom.com/wiki/Bunker_(Walkthrough))
- [Abel, Demaine et al., "Who witnesses The Witness?" (arXiv abstract)](https://arxiv.org/abs/1804.10193)
- [Abel, Demaine et al., "Who witnesses The Witness?" (full paper, FUN 2018)](https://erikdemaine.org/papers/Witness_FUN2018/paper.pdf)
