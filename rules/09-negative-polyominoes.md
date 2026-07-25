# 09 — Negative polyominoes

**Category:** region symbol

Blue hollow shapes, also called antipolyominoes. Split out from
[08-polyominoes.md](08-polyominoes.md) because they invert the arithmetic. In vanilla The Witness
they're introduced in Swamp and otherwise rare (Desert Vault, a Treehouse bridge panel, one UTM
cave-in panel).

## Rule

Formally (Demaine et al., "Who witnesses The Witness?"): for each region, it must be possible to
place all polyomino and antipolyomino clues belonging to that region (not necessarily within the
region's own cells) so that, for some single `i ∈ {0, 1}` chosen for the whole region:

- every cell **inside** the region ends up covered by exactly `i` more polyomino layers than
  antipolyomino layers (positives count `+1`, negatives count `-1`, net summed per cell), and
- every cell **outside** the region ends up covered by an equal number of polyomino and
  antipolyomino layers (net `0`).

`i = 1` is the ordinary case: positives minus negatives exactly tile the region once over. `i = 0`
is the "zero-sum" case: the region's positives and negatives cancel completely, leaving the region
with no net shape constraint at all (see Zero-sum below).

Because a negative's own layer only ever subtracts, satisfying it always requires a positive layer
sitting on the same cells somewhere in the plane. That positive must come from a polyomino clue
that belongs to the *same region* as the negative (each clue's constraint scope is its own region's
clue set, not the whole board), a region with only negative clues and no positive clues can never
reach `i ∈ {0, 1}`, since there is nothing to cancel the negative layer wherever it lands.

## Edge cases

- **Placement outside the region is allowed for both signs.** A shape may hang partly or wholly
  over the region boundary, as long as whatever spills outside is exactly cancelled cell-for-cell
  by an opposite-signed layer there. This is how you get positive/negative pairs that look like
  they don't fit the region shape in isolation but combine correctly once overlapped.
- **Overlap is required, not incidental.** Two shapes only ever overlap in this rule because a
  negative is cancelling a positive (or vice versa); nothing else licenses one positive tile
  covering ground another positive tile already covers.
- **Zero-sum regions.** If a region's positive and negative clue areas are equal and can be
  arranged to exactly coincide (`i = 0` everywhere in the region), the region has no net shape
  requirement at all: it's fully "eaten." Example: a 4-cell region with one straight tromino
  negative (3 cells) is impossible to zero out alone, but a straight tromino negative plus a
  1-cell monomino positive covering the same 3+1 footprint can zero-sum a 4-cell region trivially
  (both fully cancel, `i=0`, so the region's actual shape is unconstrained by these clues).
- **Vanilla zero-sum bug (game-implementation quirk, not the formal rule).** Per community
  documentation (see Sources), the shipped game only checks that total positive area minus total
  negative area sums to zero for a region; it does *not* verify the shapes actually coincide
  cell-for-cell. So a straight tromino negative can "cancel" a bent tromino positive of the same
  area even though they can't literally overlap into a net-zero footprint. This is a documented
  engine bug in the original release, not part of the intended puzzle logic. Flag this explicitly
  if a solver is meant to match player-observed vanilla behaviour rather than the formal rule.
- **Rotatable negatives exist.** Like positive polyominoes, a negative clue drawn upright is fixed
  orientation; drawn tilted ~15°, it may be rotated in 90° steps before placement. Nothing about
  being hollow changes this independently of the rotatable/fixed distinction.
- **Area arithmetic is necessary but not sufficient.** `2 positives × 4 cells − 1 negative × 4
  cells = 4-cell region` is a fast filter, but the shapes still have to actually fit together
  (with the required overlap) to realize that count. This is the same NP-hardness source as plain
  polyominoes (08); negative clues don't make the search easier, and monomino-plus-antimonomino
  alone is already enough to make solving NP-complete (Theorem 10, Demaine et al.).

## Interactions with other rules

- Combine with plain polyominoes per [08-polyominoes.md](08-polyominoes.md); a region's full
  polyomino constraint is over the union of its positive and negative clues.
- Combine with an antibody (see the dot/break/antibody family, out of scope for this file):
  an antibody can eliminate a negative polyomino clue like any other clue in its region, which is
  one of the two configurations the Demaine et al. paper shows pushes solving to
  Σ2-completeness (witnesses may provably not exist), rather than merely NP-hard.

## Implementation notes

- **Why area subtraction alone fails as a check:** area arithmetic only verifies the necessary
  condition `Σ(positive areas) − Σ(negative areas) == region area` (for the `i=1` case) or `== 0`
  (for `i=0`). It says nothing about whether the actual polygons interlock, so it accepts false
  positives (mismatched shapes with matching total area) that a shape-aware checker must reject
  (modulo the vanilla zero-sum bug noted above, which *does* accept exactly this).
- **Search formulation:** model each positive clue as a `+1`-weighted polyomino (one orientation
  per allowed rotation if rotatable) and each negative clue as a `-1`-weighted polyomino, and search
  for placements of all of them on an unbounded plane (or a padded grid a few cells larger than the
  region in every direction, which suffices since nothing benefits from going further out) such
  that summed weight equals a fixed `i` on every region cell and `0` on every non-region cell. This
  is an exact-cover-with-signed-multiplicities problem; encode it as an integer/boolean SAT or ILP
  instance (placement variables, one linear constraint per cell) rather than hand-rolled
  backtracking once negatives are involved, since naive backtracking on overlapping placements
  blows up quickly.
- **Complexity:** packing with only positive polyominoes is already NP-hard in general (rotatable
  dominoes alone suffice, per 08's Sources); adding negatives does not change the complexity class,
  but does add the extra existential dimension of *where* each negative cancels, which is why exact
  cover / SAT solvers, not manual case analysis, are the practical implementation route.
- Any solver needs a canonical shape representation (list of rotations as cell-offset sets) shared
  with `08-polyominoes.md`, plus a sign field, rather than a separate parallel type.

## Status in this mod

Not modelled. `Modifier` in `src/main/kotlin/com/xfastgames/witness/items/data/Edge.kt` is
`{ NONE, NORMAL, BREAK, DOT, START, END, HIDDEN }`: no polyomino or antipolyomino concept exists on
edges, nodes, or as a region/cell clue. `PuzzleSolver.kt`'s solving logic has no region,
polyomino, or shape-packing logic at all; it only validates line-drawing constraints from the
modifiers above. Region-based symbols (squares, stars, polyominoes, negative polyominoes) would
need a new data model layer before any of this rule could be implemented.

## Sources

- [Who witnesses The Witness? Finding witnesses in The Witness is hard and sometimes impossible](https://erikdemaine.org/papers/Witness_FUN2018/paper.pdf) (Abel, Bosboom, Demaine, Hamilton, Hesterberg, Kopinsky, Lynch, Rudoy; FUN 2018), Table 2's antipolyomino row gives the formal `i ∈ {0,1}` layering definition used above; Section 7 states the "not necessarily within the region" placement clause; Theorem 10 covers monomino+antimonomino NP-hardness.
- [Polyominoes, The Witness Speedrunning wiki](https://thewitness.miraheze.org/wiki/Polyominoes), source for the Zero-Sum and Zero-Sum Exception (engine bug) sections, and vanilla-panel location notes.
- [SerGreen/TheWitnessPuzzles Rules Guide](https://raw.githubusercontent.com/SerGreen/TheWitnessPuzzles/master/Puzzle%20Rules%20Guide/RulesGuide.md), plain-language statement that hollow tetrominoes are subtractive and must nullify squares of other tetrominoes.
