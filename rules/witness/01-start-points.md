# 01 — Start points

**Category:** line mechanic

A start point is where the line is picked up. Nothing else on a panel can be grabbed.

---

# Design

## The rule

Drawing begins at a start point, rendered as a filled black disc on a node (see
[00-line-and-path.md](00-line-and-path.md) for the path itself). The disc can sit on any node in
the lattice: a corner, a point along the border, or an interior intersection. Clicking or dragging
from it picks up the line; nothing else on the panel is a valid pickup point.

```
  o---o---o        o = lattice node
  |   |   |        ● = start point
  o---o---o
  |   |   |
  ●---o---o        2 ways out: up, right
```

- A panel can have multiple start points. Any one of them may be used, and choosing the right one
  is often the puzzle: the same panel can be unsolvable from one disc and trivial from another.
- Exactly one start point is consumed per line drawn. On symmetry panels (two lines at once), each
  line has its own start and the two are not interchangeable, see
  [05-symmetry.md](05-symmetry.md).
- A start point is otherwise a normal node: once picked up, the path leaves it along any segment
  running out of it, subject to the usual no-crossing, no-revisit rule.
- A start point carries no symbol of its own. It is not also a hexagon dot, a gap, or an end point;
  those are distinct node and segment roles.

A line picked up somewhere *else* may run straight through an unused start point and out the far
side, covering the disc as it goes. The disc stays visible either side of the line.

## Cost

None. Start points are a property of the panel, not something the player spends.

## Edge cases

- Multiple start points is a common source of apparently-impossible panels: the layout looks
  unsolvable until you notice a second disc elsewhere on the border.
- On symmetry panels the second line's start point can be invisible (never rendered), but it still
  exists, still moves, and still collides with the visible line. See
  [05-symmetry.md](05-symmetry.md).
- A start point placed at a true corner behaves identically to one placed along a border or mid
  grid; position doesn't change the rule, only which segments are reachable from it.
- Two start points that are both viable (either one solves the panel) are rare but not disallowed;
  the puzzle doesn't require a unique entry point, only a solution to exist from at least one.
- A start with nothing running out of it (isolated) is a malformed panel, not a puzzle variant.
- Unverified: whether the original game ever places a start point with only one way out (a dead-end
  start). Plausible on irregular lattices but not confirmed against a specific level.

---

# Implementation

## Status in this mod

Modelled. `Modifier.START` in `src/main/kotlin/com/xfastgames/witness/items/data/Edge.kt`.

- `PuzzleSolverScreen` (around the click handler) scans `puzzlePanel.graph.nodes()` for any node
  with `modifier == Modifier.START` under the cursor and calls
  `PuzzleSolver.startTracingLine(panel, start)` to begin the trace, so multiple start points
  per panel are already supported: any of them can be clicked to start.
- `PuzzleSolver` treats the chosen start like any other node once tracing begins; it doesn't
  special-case `START` beyond the initial pickup (`startTracingLine` rejects a node that isn't in
  the graph or isn't tagged `START`), so passing through a second, unused start point mid-path is
  allowed, subject to the usual no-revisit rule. Only the start point the line was picked up from
  is filled in the line colour (`PuzzlePanelRenderer.renderLine` sizes the disc off the node's
  degree in the line: 0 or 1 means it's the pickup); a start the line only travels over is covered
  by the line width alone and stays visible either side of it, as in the game.
- The composer (`PuzzleComposerScreen`, `WPuzzleEditor`) lets an author cycle a node's modifier to
  `START`, so authoring multiple start points on one panel is possible today.
- Symmetry (a second, simultaneous line from its own start) is not modelled; see
  [05-symmetry.md](05-symmetry.md).

## `START` is a node role only

`Modifier` doubles as the node modifier and the edge value (`typealias Edge = Modifier`), so
`START` on an *edge* is representable in the data model. It means nothing. The start tool ignores
segment clicks (`PuzzleComposerScreen`), and both renderers draw a `START` edge as a plain segment
(`PuzzlePanelRenderer.edge`, `WPuzzleEditor.drawGraph`), which is also how a legacy panel that
stored one degrades: the disc goes, the segment stays and traces normally via `edgeLimit`'s catch
all. Only `Node.modifier` is read when deciding what can be picked up.

## Not done

- Solution *validation* is still the line rule only (start to end, simple path). No region
  flood-fill, no symbols. See [00-line-and-path.md](00-line-and-path.md).
- Symmetry starts ([05-symmetry.md](05-symmetry.md)).
- Nothing rejects a malformed panel at authoring time: an isolated `START` node can be composed,
  and picking it up gives a line that cannot move.
- **Segment starts (a disc centred on a segment rather than on a node): descoped July 2026.** The
  edge value, the composer's segment branch and both renderers' disc-at-the-midpoint drawing were
  removed. What was there was authorable, persistent and drawn, but the solver only ever hit-tested
  nodes, so it could never be picked up.

  If it is ever picked back up, the notes that cost time to work out:

  - Nothing in the original game forbids it. Its panels are a list of dots at absolute float
    positions plus connections between them, and a start point is a flag on a dot, so a dot with
    two collinear neighbours draws as an unbroken straight run and a start flagged on it is
    indistinguishable from a disc centred on a segment. Whether any of the 523 campaign panels
    actually does this is unresolved: it needs per-dot data out of the running game (a `STARTPOINT`
    dot of degree 2 whose neighbours are collinear with it), and no published index carries panel
    geometry. See Sources.
  - The rule is worth stating in one line: a segment start is exactly *"start at either end of this
    segment, but you may not draw the segment itself"*, because leaving along one half makes the
    other half unreachable under no-revisit. It is never harder than the easier of the two node
    starts at its ends.
  - The implementation is a bisection: split the segment at its midpoint into a `START` node and
    two `NORMAL` halves, after which nothing else needs a special case. **Derive that split in the
    solver, do not store it.** `Panel.Grid.expandTo` indexes nodes by rounding their coordinates to
    grid indices, so a stored node at a half-integer coordinate collides with a real lattice node
    and `associateBy` silently drops one of the two on a grid upgrade. Edge values copy across
    untouched, which is why the storage was on the edge in the first place.

## Sources

- [SerGreen/TheWitnessPuzzles Rules Guide](https://raw.githubusercontent.com/SerGreen/TheWitnessPuzzles/master/Puzzle%20Rules%20Guide/RulesGuide.md) (checked for start-point coverage; the guide does not separately document start points, it assumes them)
- [The Witness Symmetry Island Walkthrough (Fandom)](https://thewitness.fandom.com/wiki/Symmetry_Island_(Walkthrough))
- [GameFAQs: Puzzles & Symbols walkthrough](https://gamefaqs.gamespot.com/pc/969704-the-witness/faqs/82392/puzzles-and-symbols)
- [Who witnesses The Witness? (Demaine et al., FUN 2018)](https://erikdemaine.org/papers/Witness_FUN2018/paper.pdf), formalizes puzzles as a simple path from a start vertex to a destination vertex on a grid graph
- [sigma144/witness-randomizer](https://github.com/sigma144/witness-randomizer), `Source/Panel.h` and `Source/Panel.cpp`: the original game's in-memory panel model, read live rather than from a data file. `dot_positions` (absolute float x,y per dot), `dot_flags` (`STARTPOINT = 0x2`, `ENDPOINT = 0x1`, `DOT = 0x20`, `GAP = 0x100000`), and `connections_a`/`connections_b` pairing dot indices into segments. The randomizer derives its own integer grid by rounding dot positions against measured bounds and clamping the strays, which is proof the game itself does not store panels on a lattice.
- [jbzdarkid/witness-randomizer `Source/Panels_.h`](https://github.com/jbzdarkid/witness-randomizer/blob/master/Source/Panels_.h), every campaign panel's hex id and name. Identity only, no geometry.
- [Steam: Allocation of 523 Campaign Puzzles and 49 Audiologs](https://steamcommunity.com/sharedfiles/filedetails/?id=619155228), the most complete public index. Counts per area and category, no geometry. Checked July 2026: no published index carries panel geometry.
