# 08 — Polyominoes

**Category:** region symbol

Yellow shapes, informally "tetris blocks." The name suggests tetrominoes (4-cell pieces), and
those are the most common size in the base game, but the rule generalizes to any polyomino size:
monominoes (1 cell), dominoes (2 cells), trominoes (3 cells), pentominoes (5 cells), and so on.

## Rule

Every polyomino clue in a region has a fixed shape. All polyomino clues in one region must jointly
and exactly tile that region: every cell covered exactly once, nothing left uncovered, nothing
spilling outside.

The formal definition (Abel et al., "Who witnesses The Witness?") states this as a coverage
condition rather than a jigsaw metaphor: it must be possible to place every polyomino clue in the
region (each piece placed as a whole, not necessarily anchored to the cell it's drawn on) so that
every cell inside the region is covered by exactly one polyomino and every cell outside the region
is covered by zero. Without a negative piece in play there is nothing to cancel a polyomino's
coverage, so in practice, for an all-positive region, that condition forces every piece to land
entirely inside the region. This is also why the drawn cell of a clue is irrelevant: only the
shape and the region matter, and the "not necessarily within the region" clause in the formal rule
only starts to matter once [09-negative-polyominoes.md](09-negative-polyominoes.md) enters the
picture.

The shape's **position** inside the region is irrelevant, and so is which specific cell the clue
icon is drawn on. Only the region's total area and shape matter for solvability.

## Variants

- **Fixed (yellow, drawn upright/orthogonal)**: used only in its drawn orientation. Translation
  only, no rotation, no reflection.
- **Rotatable (yellow, drawn tilted ~15°)**: may be rotated in 90° steps before placement.
  Reflection (mirroring) is never allowed for either variant; there is no "flippable" polyomino
  clue in the base game.
- **Negative (blue, hollow)**: see [09-negative-polyominoes.md](09-negative-polyominoes.md).

## Edge cases

- Multiple polyominoes in one region combine: their total cell count must equal the region's cell
  count, and they must jigsaw together to fill it exactly with no overlap between pieces.
- Pieces of the same (positive) clue set may not overlap each other. Overlap is only meaningful
  once a negative piece is present to cancel the excess (09).
- Total-area matching is a fast necessary check but not sufficient: two pieces can have the right
  combined area and still fail to tile the region's shape (e.g. an L-tromino and a straight
  tromino cannot always jointly fill an arbitrary 6-cell region even though 3+3=6).
- A region containing only monomino (1-cell) clues is trivial: since a monomino has no shape
  constraint beyond area, one monomino clue per region cell is equivalent to "region has no shape
  constraint at all," and the region is satisfiable in polynomial time as long as the clue count
  equals the region's cell count. Larger shapes are where hardness comes from (see Implementation
  notes).
- Interaction with [07-stars.md](07-stars.md): a polyomino clue counts as a star's colored
  partner just like any other object, regardless of whether that polyomino's region ends up
  satisfiable.
- Interaction with [11-eliminators.md](11-eliminators.md): an antibody/eliminator in a region with
  multiple polyomino clues can annul exactly one of them; whether doing so is *necessary* depends
  on whether the region is packable without the elimination, which itself is a full polyomino-
  packing check.
- Unverified: the exact catalog of shapes and sizes used across the base game's actual panels
  (e.g. whether pentominoes or larger appear outside the Challenge/endgame areas). Treat any
  specific shape inventory as flavor, not a hard rule.

## Implementation notes

Data model: a polyomino clue is a set of `(dx, dy)` offsets (its cell shape) plus a `rotatable:
Boolean` flag. A region is the set of grid cells enclosed by the current line and the panel
boundary (already needed for squares/stars/eliminators). Validating a region's polyomino clues is
an exact-cover problem:

1. Collect the region's cells as the universe to cover.
2. For each polyomino clue, generate its candidate placements: all translations (and, if
   `rotatable`, all four 90° rotations) of the shape such that every offset cell lands inside the
   region.
3. Search for a selection of exactly one placement per clue such that the selected placements are
   pairwise disjoint and their union is the entire region (Knuth's Algorithm X / dancing links is
   the standard efficient implementation of this search; plain backtracking with early area and
   overlap pruning is enough at panel scale).
4. If a negative clue is present in the same region, fold it in per 09's arithmetic instead of
   requiring disjoint coverage.

Complexity: Abel et al. prove this is NP-complete in general, and pin the hardness down more
precisely than "shape fit is hard" would suggest:

- Monominoes alone (no other clue types) are solvable in polynomial time, because a monomino has
  no shape constraint beyond area.
- Rotatable dominoes alone are already NP-complete, as are non-rotatable *vertical* dominoes alone.
- Monominoes combined with antimonominoes (09) are NP-complete.

So real hardness enters as soon as pieces bigger than 1 cell are allowed, well before tetrominoes.
At the panel sizes this mod would render (single-digit region dimensions, a handful of clues per
region), brute-force backtracking with pruning is fine in practice; only pathological/generated
puzzles need the dancing-links approach.

## Status in this mod

Not modelled. `Modifier` (`src/main/kotlin/com/xfastgames/witness/items/data/Edge.kt`) only has
`NONE, NORMAL, BREAK, DOT, START, END, HIDDEN`, no polyomino/region-symbol representation exists
in the data model at all. `PuzzleSolver`
(`src/main/kotlin/com/xfastgames/witness/screens/solver/PuzzleSolver.kt`) only implements
line-tracing mechanics (building the drawn path from pointer movement); it has no region
extraction or clue-satisfaction validation of any kind yet, so there is nothing downstream that
polyomino data could plug into without also adding region/clue validation in general.

## Sources

- [Abel, Bosboom, Demaine, Hamilton, Hesterberg, Kopinsky, Lynch, Rudoy, "Who witnesses The
  Witness? Finding witnesses in The Witness is hard and sometimes impossible" (FUN 2018)](https://erikdemaine.org/papers/Witness_FUN2018/paper.pdf)
  formal clue definitions (Table 2, Section 7) and the polyomino/domino/monomino hardness
  results used above.
- [SerGreen/TheWitnessPuzzles, Puzzle Rules Guide](https://raw.githubusercontent.com/SerGreen/TheWitnessPuzzles/master/Puzzle%20Rules%20Guide/RulesGuide.md)
  plain-language statement of the tetromino rule and the tilted-means-rotatable convention.
- Community explanations of rotation and non-flippability (Steam discussion boards, GameFAQs
  boards for *The Witness*), consistent with the paper's formal "rotatable by any multiple of 90°"
  wording and used only to corroborate, not as a primary source.
- `src/main/kotlin/com/xfastgames/witness/items/data/Edge.kt` and
  `src/main/kotlin/com/xfastgames/witness/screens/solver/PuzzleSolver.kt`, checked directly
  to confirm current implementation status.
