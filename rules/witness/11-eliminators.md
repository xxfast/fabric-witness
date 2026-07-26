# 11 — Eliminators

**Category:** region symbol

Also called erasers, antibodies, cancellation marks, jacks, or the "Y" symbol (an upside-down Y,
sometimes described as looking like the Mercedes-Benz logo).

## Rule

Each eliminator must cancel **exactly one** failing rule inside its own region. Not zero, not two.

If a region has an eliminator and everything else in it is already satisfied, the panel fails. The
eliminator needs an error to eat: a region with a lone, otherwise-valid eliminator is unsolvable as
drawn, and the line must be redrawn to introduce (or relocate) a violation for it to cancel.

Concretely: divide the grid so that one cell ends up on the wrong side (for example a black square
sharing a region with white squares), then place the eliminator in that same region. The eliminator
absorbs that one violation and the region as a whole reads as solved.

## What it can cancel

Any single unsatisfied element in its region, regardless of symbol type: an uncovered hexagon dot,
a wrongly-grouped colored square, an unpaired or over-paired star, a polyomino/negative-polyomino
that doesn't tile the region, a triangle with the wrong edge count. Confirmed by both the fandom
wiki and the SerGreen rules guide (see Sources): it is deliberately symbol-agnostic, which is why
players are told to "guess what to delete."

- **Hexagons:** yes, an eliminator can cancel an uncovered hexagon dot on the line. Confirmed
  (fandom wiki, jbzdarkid's `validate.js` treats an uncovered dot as exactly the kind of violation
  a negation symbol is allowed to consume).
- **Other eliminators:** yes, in the reference community implementation this is a real, named
  behavior: two eliminators in a region can pair off and cancel each other, gated behind a settings
  flag (`NEGATIONS_CANCEL_NEGATIONS`, default on) in jbzdarkid's engine. Treat this as verified for
  fan tooling; the base game's own source isn't public, but community guides agree eliminators
  "erase themselves and another clue" as a pair.
- **Itself:** unverified. No source describes a single, unpaired eliminator satisfying itself with
  no partner. The mechanism above always pairs two symbols (eliminator + violation, or eliminator +
  eliminator); a solo eliminator with nothing to cancel simply fails the region.

## Edge cases

- **"Cancels the rule, not the symbol" (disputed / unverified).** Several community rule guides
  repeat the claim that a colored square whose *rule* was cancelled by an eliminator still counts
  as a colored object for a same-colored star elsewhere in the region, i.e. the eliminator removes
  the violation but the object stays "on the board" for color-counting purposes. This is the classic
  trap as usually explained to players. However, the one detailed reference reimplementation this
  agent could inspect (jbzdarkid's `engine/validate.js`) does the opposite: it removes the
  cancelled cell from the grid entirely (`puzzle.setCell(target.x, target.y, null)`) before
  re-checking the region, which would also remove it from color counts. This agent could not find
  an authoritative source (official rules text, or the closed-source game engine itself) settling
  which behavior is correct. **Flagged as unverified/disputed** rather than asserted either way;
  don't assume the "still counts" trap without testing against the real game.
- The eliminator symbol is itself colored on some panels, and (per the fandom wiki and SerGreen
  guide) that color can interact with a same-colored star's pairing count while the eliminator is
  simultaneously busy cancelling something else in the region.
- A region can have more than one eliminator; each one needs its own distinct violation (or another
  eliminator) to pair with. Two eliminators sharing a single violation does not count as two
  satisfied eliminators.
- Not every violation is equally "available" to be cancelled. In jbzdarkid's implementation,
  uncovered hexagon dots, wrong-count triangles, and lone unpaired stars are classified as "very
  invalid" and are matched against eliminators first, greedily, before the remaining "invalid"
  violations (square-color collisions, over-paired stars, polyomino mismatches) are tried via
  backtracking search. This ordering is an implementation detail of that particular fan engine, not
  a confirmed rule of the original game; noted here as a plausible resolution strategy, not a fact
  about Jonathan Blow's engine.
- Wrong-count triangles, negative polyominoes, and eliminators can all coexist in one region; the
  eliminator does not care which rule type the error came from. (Four-triangle cells do not exist,
  see [10-triangles.md](10-triangles.md).)

## Interactions with other rules

- [07-stars.md](07-stars.md): an eliminator counts as a colored object for star pairing if it has a
  color, independent of whether it is currently cancelling something.
- [04-hexagon-dots.md](04-hexagon-dots.md): an uncovered hexagon dot is a valid target for
  cancellation.
- [10-triangles.md](10-triangles.md): triangles are evaluated per-cell, but a wrong-count triangle
  is still a region-level "error" an eliminator in that triangle's region can consume.
- [08-polyominoes.md](08-polyominoes.md) / [09-negative-polyominoes.md](09-negative-polyominoes.md):
  a tiling failure is a single collective violation; an eliminator can absorb it, but the involved
  poly/ylop pieces are handled as one unit, not per-cell.

## Implementation notes

Modeling eliminators is fundamentally an **assignment / matching problem**, not a simple per-region
predicate:

1. Evaluate the region once *without* eliminators to get the raw list of violated elements
   (unsatisfied stars, color collisions, bad triangles, uncovered dots, tiling failures).
2. Let `E` = eliminators in the region, `V` = raw violations in the region. A region is valid iff
   there exists an injective pairing `f: E -> V ∪ E'` (`E'` = other eliminators in `E`) such that:
   - every eliminator is paired with something (no eliminator left over), and
   - every element of `V` is either paired to an eliminator or otherwise satisfied, and
   - `|E|` accounts for exactly the violations removed, i.e. after removing all paired-off elements,
     the region re-evaluates as clean.
3. Because pairing choices can change what "clean" looks like (e.g. cancelling one bad square can
   fix a color collision that otherwise implicates several squares), this generally needs
   backtracking/search rather than a greedy one-pass match: try a pairing, re-run region validation
   with those elements removed, and backtrack if it doesn't resolve everything. jbzdarkid's engine
   does exactly this (recursive pairing with a stack of remaining eliminators and remaining
   violations), which is the shape to copy.
4. Complexity: with `k` eliminators and `m` violations in a region, naive backtracking is
   `O(m^k)`-ish per region before memoization; in practice both `k` and `m` are tiny (single digits)
   per region so this is not a performance concern. The wider puzzle-satisfiability problem (does
   *some* line exist that makes every region resolvable) is what the Demaine et al. "Who witnesses
   The Witness?" paper shows is NP-hard in general, independent of the eliminator matching sub-step.
5. Suggested representation: give each region-violation a stable identity distinct from the cell
   that produced it (a square-color collision can implicate multiple cells at once), so the matching
   step can pair an eliminator to "the collision" rather than to an arbitrary cell within it.

## Status in this mod

Not modelled. `Modifier` in `src/main/kotlin/com/xfastgames/witness/items/data/Edge.kt` is
`{ NONE, NORMAL, BREAK, DOT, START, END, HIDDEN }`, an edge-state enum with no eliminator/eraser
value, and no cell-level "region symbol" concept exists yet. `PuzzleSolver` in
`src/main/kotlin/com/xfastgames/witness/screens/solver/PuzzleSolver.kt` only implements line
tracing (`startTracingLine`/`move`/`buildLine`, geometric segment/collision logic) and has no region
partitioning or symbol-validation step at all, so there is nothing yet to hang eliminator logic off
of. Region validation (colors, stars, polyominoes, triangles) and eliminators would need to land
together.

## Sources

- [SerGreen/TheWitnessPuzzles Rules Guide](https://raw.githubusercontent.com/SerGreen/TheWitnessPuzzles/master/Puzzle%20Rules%20Guide/RulesGuide.md)
- [The Witness Wiki (fandom) - Puzzle elements](https://thewitness.fandom.com/wiki/Puzzle_elements)
- [epictrick.com - The Witness: guide to the rules of the various types of puzzles](https://www.epictrick.com/en/the-witness-guide-to-the-rules-of-the-various-types-of-puzzles)
- [jbzdarkid/jbzdarkid.github.io - engine/validate.js](https://github.com/jbzdarkid/jbzdarkid.github.io/blob/master/engine/validate.js) (fan reimplementation of the puzzle validation logic, including negation/eliminator pairing; used for the assignment-problem framing and the `NEGATIONS_CANCEL_NEGATIONS` behavior)
- [jbzdarkid/jbzdarkid.github.io - engine/puzzle.js](https://github.com/jbzdarkid/jbzdarkid.github.io/blob/master/engine/puzzle.js) (settings flag for eliminator-cancels-eliminator)
- Demaine, Hearn, et al., ["Who witnesses The Witness?"](https://erikdemaine.org/papers/Witness_TCS/paper.pdf) (referenced for the NP-hardness framing of overall puzzle satisfiability; not fetchable as text in this session, cited for context only)
