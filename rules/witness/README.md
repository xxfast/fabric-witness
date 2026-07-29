# The Witness: line puzzle rules

Catalog of every rule that can appear on a drawable line panel in The Witness. Environmental
puzzles (perspective/world-drawing puzzles that feed the obelisks) are deliberately out of scope.

Each rule gets one file. Files are shallow by design: what the rule is, how it validates, the
edge cases that bite, and where it stands in this mod.

## Two categories

**Line mechanics**: properties of the grid and the path itself. They constrain how you draw.

- [x] **00** [Line and path basics](00-line-and-path.md): tracing + line rule validated on submit
- [x] **01** [Start points](01-start-points.md): authored, multiple starts, only `START` picks up
- [x] **02** [End points](02-end-points.md): authored as nubs, terminates and validates the line
- [x] **03** [Broken edges (gaps)](03-broken-edges.md): traceable to the gap, never across
- [x] **04** [Hexagon dots](04-hexagon-dots.md): authored, rendered, checked on submit; look unsettled
- [ ] **05** [Symmetry lines](05-symmetry.md): not modelled

**Region symbols**: drawn inside cells. The finished line partitions the grid into regions, and
each symbol is validated against the region that contains it.

None are modelled. All of them are blocked on region flood-fill, except triangles.

- [ ] **06** [Colored squares](06-colored-squares.md)
- [ ] **07** [Stars](07-stars.md)
- [ ] **08** [Polyominoes](08-polyominoes.md)
- [ ] **09** [Negative polyominoes](09-negative-polyominoes.md)
- [ ] **10** [Triangles](10-triangles.md)
- [ ] **11** [Eliminators](11-eliminators.md)

Triangles are the odd one out: filed under region symbols because they are drawn in cells, but
validated per cell against their own four edges, ignoring the region partition entirely.

## How validation composes

1. Player draws a path from a start point to an end point.
2. Line mechanics are checked directly against the path (gaps, hexagons, symmetry).
3. The path cuts the grid into regions. Every symbol is checked against its own region only,
   never across region boundaries. Triangles skip this step.
4. Eliminators run last: each one cancels exactly one otherwise-failing rule in its region. This
   is a matching problem, not a per-region predicate, since a pairing choice changes what counts
   as clean.
5. Puzzle solves iff nothing is left failing.

## Where the mod actually stands

The line mechanics that don't need regions (00 through 03) are done. `PuzzleSolver` traces from a
`START` node with segment selection, geometric self-collision prevention, backtracking and
`BREAK` stubs (`edgeLimit`), and `submit` validates the line rule via `isValidSolution`, moving the
state into `SolutionAccepted` / `SolutionRejected` (`PuzzleSolverViewModels`). Covered by
`PuzzleSolverTests`.

What's left is everything that needs the grid partitioned:

- **Region flood-fill is the shared blocker.** Every region symbol (06 through 09, 11) needs it,
  and none of them can be started without it. There is no flood-fill and no symbol validation
  anywhere in `src/main`, so a panel carrying symbols is accepted on the line rule alone.
- **Symmetry (05)** is the last unmodelled line mechanic. Hexagons (04) are done: authored in the
  composer, drawn in front of the line, and validated on submit via `Panel.unsatisfiedHexagons`.
  Their appearance is still open, a hexagon currently reads too much like a broken edge.

## Open questions

Claims that could not be settled against a primary source, flagged in their own files:

- Whether an eliminator cancels the *rule* or removes the *symbol*. The widely-repeated community
  trap says a cancelled square still counts for star pairing; the one inspectable reference
  implementation deletes the cell outright. Unresolved, see
  [11-eliminators.md](11-eliminators.md).
- Whether vertical-only mirror symmetry exists in vanilla ([05-symmetry.md](05-symmetry.md)).
- The full set of square colors used in the base game ([06-colored-squares.md](06-colored-squares.md)).
- Reported "sound" and "ordered" hexagon variants, found only in secondary sources
  ([04-hexagon-dots.md](04-hexagon-dots.md)).

Note that the shipped game has a documented zero-sum bug in negative polyomino validation, so
"match vanilla" and "match the formal rule" are not the same target. See
[09-negative-polyominoes.md](09-negative-polyominoes.md).

## Sources

- [SerGreen/TheWitnessPuzzles rules guide](https://github.com/SerGreen/TheWitnessPuzzles/blob/master/Puzzle%20Rules%20Guide/RulesGuide.md)
- [The Witness Wiki: puzzle elements](https://thewitness.fandom.com/wiki/Puzzle_elements)
- [GameFAQs: puzzles & symbols](https://gamefaqs.gamespot.com/pc/969704-the-witness/faqs/82392/puzzles-and-symbols)
- [Gameranx: puzzle types and rules guide](https://gameranx.com/features/id/36898/article/the-witness-puzzle-types-and-rules-guide/)
- [Demaine et al., "Who witnesses The Witness?"](https://erikdemaine.org/papers/Witness_FUN2018/paper.pdf) (formal treatment of the constraints)
