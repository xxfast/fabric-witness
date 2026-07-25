# 07 — Stars

**Category:** region symbol

Also called suns or octagrams.

## Rule

Each star has a color. In the region it ends up in, count every same-colored object: that count
must be **exactly two** (the star itself plus exactly one partner). Zero partners fails, two or
more partners fails.

Formally (per the Demaine et al. complexity analysis, "Who witnesses The Witness?"): if a region
contains a star of color `c`, the number of clues of color `c` in that region must equal exactly
two.

The partner can be any other colored object of the same color, not just another star:

- another star
- a colored square ([06-colored-squares.md](06-colored-squares.md))
- a colored polyomino, including negative/subtractive ones ([08](08-polyominoes.md),
  [09](09-negative-polyominoes.md)), a yellow tetromino satisfies a yellow star exactly like a
  yellow square would
- an eliminator ([11-eliminators.md](11-eliminators.md)), which is itself colored

Symbol *type* is irrelevant to this rule. Only color and region membership matter.

Triangles ([10-triangles.md](10-triangles.md)) are a partial exception in practice: every triangle
in the base game is drawn the same fixed orange, never a palette color that varies per panel. So a
triangle only ever satisfies an *orange* star, and orange stars paired against triangles are rare.
Hexagon dots and broken edges have no color at all and never count toward any star.

## Edge cases

- Two same-colored stars in a region pair with each other and are both satisfied.
- Three or more same-colored stars in one region always fails: the count for that color can never
  land on exactly two. No rearrangement of other same-colored objects fixes it, since adding more
  same-colored objects only makes the count worse, not better.
- A region can contain two different star colors at once (e.g. two black stars and two white
  stars) as two independent pairings, as long as each color's count is separately exactly two.
- Objects of a *different* color are invisible to a star's rule. They neither help nor hurt it,
  and impose no constraint of their own unless they belong to their own colored star.
- A same-colored eliminator counts as the star's partner even while the eliminator is simultaneously
  using its "cancel one failing rule" ability elsewhere in the same region ([11](11-eliminators.md)
  covers the trap: a cancelled rule's symbol still counts as a colored object here).
- An eliminator of *any* color, not just the star's color, can cancel a broken star-pairing rule as
  its one permitted cancellation. This is how a 3-star region can still validate: the eliminator
  doesn't fix the count, it erases the requirement that the count be right.
- A star with zero same-colored objects anywhere in its region (the only object of its color) always
  fails unless an eliminator cancels the star rule for that color.

## Interactions with other rules

- Region membership is decided entirely by the line ([00](00-line-and-path.md)) and any broken
  edges ([03](03-broken-edges.md)) that cut a region in two; stars must be evaluated per resulting
  region, not per whole panel.
- Squares ([06](06-colored-squares.md)) are validated independently (all squares in a region must
  share one color) and then, separately, participate in star counting for their own color.
- Polyominoes ([08](08-polyominoes.md), [09](09-negative-polyominoes.md)) are validated
  independently for tiling and, separately, participate in star counting if colored.
- Hexagon dots ([04-hexagon-dots.md](04-hexagon-dots.md)) and triangle edge-counts
  ([10](10-triangles.md)) are validated per-node/per-cell and don't participate in star color
  counting at all (dots have no color; triangles are effectively always one fixed color).

## Implementation notes

To validate computationally:

1. Model a region-symbol data type with at least `color: Color` and `kind: Star | Square |
   Polyomino | Eliminator | ...`, attached to a cell (not a node/edge, since stars live inside
   grid cells). None of this currently exists in this mod; see "Status in this mod" below.
2. After tracing the line, partition the grid into regions (flood fill across cells, blocked by
   the drawn line and by `BREAK` edges).
3. For each region, group its colored symbols by color. For every color `c` present on at least
   one star, assert `count(color == c) == 2`.
4. Feed eliminators in as a separate pass: an eliminator may cancel exactly one failing group
   count (of any color, including its own) per region, turning a false region-check into a pass.
   This needs a bipartite matching between eliminators and failing sub-rules in the same region
   (see [11-eliminators.md](11-eliminators.md) for the general algorithm), since eliminators are
   resolved last, after every non-eliminator rule.

## Status in this mod

Not modelled. `Modifier` in `src/main/kotlin/com/xfastgames/witness/items/data/Edge.kt` is
`{ NONE, NORMAL, BREAK, DOT, START, END, HIDDEN }`, applied to nodes/edges only; there is no
per-cell region-symbol type at all, so stars (and their color) have no representation yet.
`PuzzleSolver.kt` only traces the drawn line (`startTracingLine`, segment/intersection
checks) and never computes regions or evaluates any region-symbol rule.

## Sources

- [SerGreen/TheWitnessPuzzles, Puzzle Rules Guide](https://raw.githubusercontent.com/SerGreen/TheWitnessPuzzles/master/Puzzle%20Rules%20Guide/RulesGuide.md)
- [Demaine, Hesterberg, Ku, "Who witnesses The Witness? Finding witnesses in The Witness is hard and sometimes impossible" (FUN 2018 / arXiv:1804.10193)](https://erikdemaine.org/papers/Witness_FUN2018/paper.pdf)
- [r/theWitness and GameFAQs-derived summaries on star/tetromino color interaction](https://gamefaqs.gamespot.com/pc/969704-the-witness/faqs/82392/puzzles-and-symbols)
