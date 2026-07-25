# 05 — Symmetry lines

**Category:** line mechanic

## Rule

The panel has two start points and two end points. You draw one line from your start; a second
line is drawn automatically and simultaneously from the other start, following a fixed geometric
relationship to yours (a mirror reflection, or a 180° rotation). Both lines advance in lockstep:
every move you make produces a corresponding move on the other line at the same instant.

The puzzle is solved only when both lines simultaneously reach a valid end point. Neither line may
cross or touch itself, and the two lines may never cross or touch each other, at any point during
the trace, not only at the final state. A move that would make the mirrored line collide with
anything is illegal and simply can't be made, exactly as if your own line had hit an obstacle.

## Variants

- **Horizontal mirror**: the second line is reflected across the panel's vertical center axis
  (left-right flip). This is the baseline case introduced first and used most often.
- **Rotational / both-axes mirror**: the second line is reflected across both axes at once, which
  is equivalent to a 180° rotation about the panel's center point. The base game's own rules guide
  describes only these two cases: "mirrored just horizontally or both horizontally and vertically."
- **Vertical-only mirror** (top-bottom flip, without also flipping left-right), not confirmed to
  exist as a distinct vanilla variant. Community rules guides describe only the two cases above;
  treat a pure vertical-only mirror as **unverified** for the base game, it may only appear in fan
  puzzle editors/randomizers.
- **Translational** (the second line is offset by a fixed vector rather than mirrored), **not
  present in the base game**. It shows up as a custom symmetry option in fan tooling (e.g. the
  Sigma Randomizer, which lists translational, flipped-translational, diagonal, and 90° rotational
  symmetry as randomizer-only additions on top of vanilla's two mirror types). Treat any prior claim
  that translational symmetry is a rare vanilla variant as corrected: it isn't vanilla at all.
- **Invisible second line**: one of the two lines isn't rendered, but it still exists: it still
  has to obey every rule (collision, reaching a valid endpoint), and it still carries a color for
  colored-hexagon purposes. The game also stages this at the level-design layer in the Symmetry
  area: physically separate panel pairs face each other, you solve the visible (yellow) side, and
  the same trace is required to be valid, sight unseen, on the paired panel with an invisible line.
  That's a presentation choice, not a third rule variant; the underlying constraint is identical to
  a single panel with one invisible line.

## Edge cases

- Collision is the dominant failure mode here, more than any placed symbol. "Crossing" includes
  the two lines merely touching at a shared vertex, not just perpendicular crossings; per community
  rules descriptions the lines are "forced to keep both your lines from meeting at any point."
- On an odd-width/odd-height grid with rotational (180°) symmetry, the panel's center node is
  fixed by the symmetry transform (it maps to itself). Whether vanilla ever allows both lines to
  legally occupy that fixed point at the same traced step is **unverified**; anecdotally, puzzle
  designers appear to sidestep the question by choosing grid dimensions where no such fixed node
  exists, or by placing an obstacle there. Don't assume a "both lines share the center" case is
  legal without a concrete verified example.
- Both lines partition the panel together: region symbols (colored squares, hexagon dots, stars,
  polyominoes) are validated against the regions cut by the union of both lines' edges, not by
  either line alone. A region that only your visible line encloses can still be invalid if the
  mirrored line's edge cuts through it.
- Colored hexagons pair constantly with symmetry: the convention is blue hexagons belong to your
  own (player) line, yellow to the mirrored line, and black/neutral hexagons can be satisfied by
  either. This lets a single dot mean different things depending on which line passes over it.
- A dead end for either line (no legal move keeps both lines simultaneously non-colliding) ends the
  attempt exactly like a normal collision, even if your own line alone still has room to move.
- Symmetry combines with every other rule (broken edges, hexagons, squares, stars, polyominoes,
  triangles, eliminators): those symbols are evaluated once against the merged two-line partition,
  they don't get special-cased for symmetry panels.

## Implementation notes

Data model: a symmetric panel needs a second graph (or a transform function over the first) plus a
transform descriptor, e.g. `enum SymmetryTransform { MIRROR_H, MIRROR_ROTATIONAL }` and a mapping
`Node -> Node` derived from panel dimensions (reflect `x`, reflect both `x` and `y`, etc). The second
start/end pair is just the transform applied to the first pair's `Node`s; it does not need separate
authoring if the transform and the primary graph are known.

Algorithm sketch, keyed off the existing single-line tracer in
`PuzzleSolverDomain.kt` (`chooseSegment`/`traceLimit`/`intersectionParameter`):

1. On every candidate move of the primary line, compute the corresponding candidate move of the
   mirrored line by applying the transform to the primary's current node and target node.
2. Run the existing collision/segment-limit logic (`traceLimit`, `intersectionParameter`) twice:
   once for the primary line against itself, once for the mirrored line against itself, and twice
   more cross-checking each line's new segment against the other line's already-traced path.
3. Clamp the move to the tightest of the resulting limits (self-collision on either line, or
   cross-collision between the two), same pattern as the existing `limit.coerceAtLeast(0f)` logic.
4. Solved state requires both traced paths to terminate at a `Modifier.END` node (their own end,
   respecting the transform) at the same call to `arriveAt`/`buildLine`.
5. Region computation: build one merged `MutableGraph<Node>` containing both lines' edges before
   running the existing region-flood-fill / symbol-validation pass, so colored-square/hexagon/star
   checks see a single combined partition.

## Status in this mod

Not modelled. `Modifier` (`src/main/kotlin/com/xfastgames/witness/items/data/Edge.kt`) has no
symmetry-related case (`NONE, NORMAL, BREAK, DOT, START, END, HIDDEN`), `Panel` has no second
start/end or transform field, and `PuzzleSolverDomain.kt` traces exactly one line: `path` is a
single `MutableList<Node>`, and `chooseSegment`/`traceLimit` only check the traced line against
itself, never against a second line. Adding this rule means extending `Panel`'s data model with an
optional symmetry transform and running the tracer twice per move as sketched above.

## Sources

- [SerGreen/TheWitnessPuzzles Rules Guide](https://raw.githubusercontent.com/SerGreen/TheWitnessPuzzles/master/Puzzle%20Rules%20Guide/RulesGuide.md) (also mirrored at [Steam Community](https://steamcommunity.com/sharedfiles/filedetails/?id=614554253))
- [Symmetry Island (Walkthrough), The Witness Wiki](https://thewitness.fandom.com/wiki/Symmetry_Island_(Walkthrough))
- [02 - Symmetry, GameFAQs Walkthrough](https://gamefaqs.gamespot.com/pc/969704-the-witness/faqs/82392/02-symmetry)
- [Symmetry Puzzle Solutions Guide, Gosunoob](https://www.gosunoob.com/witness/symmetry-puzzle-solutions-guide/)
- [Sigma Randomizer, The Witness Speedrunning wiki](https://thewitness.miraheze.org/wiki/Sigma_Randomizer) (custom symmetry types: translational, flipped-translational, diagonal, 90° rotational, as additions on top of vanilla's two mirror types)
- `src/main/kotlin/com/xfastgames/witness/items/data/Edge.kt`, `src/main/kotlin/com/xfastgames/witness/screens/solver/PuzzleSolverDomain.kt` (in-repo, confirms current single-line-only implementation)
