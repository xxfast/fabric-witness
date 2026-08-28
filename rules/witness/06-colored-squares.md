# 06 — Colored squares

**Category:** region symbol

A square is a coloured mark inside a cell. It asks nothing of the line directly; it asks what the
line *encloses*. Once the path is finished it cuts the panel into regions, and every region may hold
squares of one colour only. This is the first rule that needs the grid partitioned, which is why it
lands ahead of stars, polyominoes and eliminators: it is the smallest thing the flood fill can carry.

---

# Design

## The rule

Squares of different colours must be separated. Every region cut out by the finished line may
contain squares of at most one colour.

A square sits in a **cell**, the space between four grid nodes, never on a node or an edge. The
finished line partitions the cells into regions: two neighbouring cells are in the same region
unless the line runs along the edge between them. Formally, for the submitted path $P$ the cells
form a graph whose edges join side-by-side cells not separated by $P$; the regions are its connected
components, and the panel's squares are satisfied iff, in every component, the set of square colours
has size $\le 1$.

Worked example, a 3×3-node panel (2×2 cells), `S` start, `E` end, `B` a black square, `W` a white
one:

```
o---o---E          o---o---E
| B | W |          | B ‖ W |
o---o---o          o---o---o
| B | W |          | B ‖ W |
S---o---o          S---o---o
```

`S → right → right → up → up → E` hugs the bottom and right border, so all four cells are one region
holding both colours: fails. `S → right → up → up → right → E` (right) runs up the middle: the left
region is all black, the right is all white, and it solves. Nothing forces every cell to be
enclosed on purpose; unvisited grid is fine, it just ends up in whichever region reaches it.

## What cuts a region

Only the line. A broken edge ([03](03-broken-edges.md)) is a gap the line cannot cross, but the
*cells* either side of it are still one region unless the line is drawn on the pieces of that edge.
Likewise a segment that is simply absent, on a panel carved down in the composer, separates
nothing: cells merge across it. The panel's own border is the outer boundary of every region, so a
cell in the far-left column and one in the far-right column are together only if the line fails to
come between them.

## Colours

The rule is colour-agnostic: it checks "same colour or not" and never cares which. The base game
confirms black, white, blue, green, purple and orange on panels; red, cyan, magenta and yellow are
reported by secondary sources only and stay unverified. This mod uses the 16 Minecraft dye colours
for a square, which covers every confirmed one and sidesteps the question of the exact vanilla
palette. Two squares are the same colour iff they carry the same dye.

## Cost

None. Placing a square is free at the composer, like every other mark
([../minecraft/04-1-puzzle-composer-modifiers.md](../minecraft/04-1-puzzle-composer-modifiers.md#cost)).
It spends no tablets and returns none on recycle, so there is no route through squares to a
cheaper panel, a bigger one, or a refund. Nothing existing is dominated: a panel with squares is
strictly *harder* to solve than the same panel without, and costs the same.

## Edge cases

- A region with no squares is fine; it satisfies the rule vacuously.
- Any number of same-coloured squares in one region is fine. There is no count constraint, only a
  separation constraint (contrast [stars](07-stars.md), which count).
- When a region fails, **every square in it is at fault**, the black ones and the white ones alike.
  Each square individually asks "is there a square of another colour in my region?", so all of them
  answer yes. Confirmed against the reference implementation (`ColoredSquareRule` returns an error
  for the owning block whenever any other-coloured block shares its sector). That is what the
  error flash shows.
- **A cell is a position, not a frame.** A square in a cell that has lost some of its corner nodes
  or segments on the Grid tab is still a square: the cell has merged with its neighbours, and the
  square sits in whatever region that merged cell ends up in. Carving the shared segment out of two
  cells is a legitimate way to author "these two must agree", and both cells can still hold a
  square. Unlike a hexagon, a square therefore survives Grid-tab edits; only the square tool
  removes one.
- Squares of one colour plus other symbol types in a region are fine as long as those validate on
  their own. Squares interact with [stars](07-stars.md) (a square is a coloured object for a star's
  pairing) and [eliminators](11-eliminators.md) (one can cancel a square's failure) and with nothing
  else.
- Two colours of squares plus broken edges is already enough to make solvability NP-complete
  (Abel, Demaine et al.). The composer does not try to decide it; an unsolvable panel is the
  author's problem, as with hexagons.
- Perspective puzzles in the base game recolour squares by viewing angle. World-drawing mechanic,
  out of scope.

## Authoring

One tool in the composer's Modifiers rail, the square. It is the first tool that carries a
parameter (a colour), and the rail has no palette yet, so the colour is chosen by cycling:

| Click | Effect |
|-------|--------|
| an empty cell | places a black square |
| a black square | turns it white |
| a white square | removes it |

Black and white are what the base game teaches with first, and two colours are enough to feel the
rule. Any other dye is a legal colour on the data side, a panel can carry one, but the composer
cannot author it until it grows a palette widget (see Not done).

The tool refuses a click on a panel type with no cells (a tree), consistent with
[04-1](../minecraft/04-1-puzzle-composer-modifiers.md#the-rail-is-a-function-of-the-panels-type):
the rail follows the type, and a tool with nothing to land on refuses rather than disappears.
Carving on the Grid tab never removes a square (see edge cases).

---

# Implementation

## Status in this mod

**Data model, done.** `CellSymbol` (`items/data/CellSymbol.kt`) on `Panel.symbols`, round-tripped
through NBT under `symbols`, carried through `Panel.Grid.expandTo` via the grid offsets. Pinned by
`SquareTests.Persistence`.

**Validation, done.** `Panel.regions(path)` and `Panel.clashingSquares(path)`
(`items/data/Regions.kt`); `PuzzleSolver.submit` rejects with them on
`SolutionRejected.clashingSquares`, and `failedPositions` feeds both rules into `PanelErrorFlash`
on a tutorial panel. Pinned by `SquareTests.Regions`, `SquareTests.Rule` and one case in
`PuzzleSolverTests`.

**Authoring, done.** The rail's former blank `addButton` is `squareButton` with `SquareIcon`;
clicking runs `Panel.withSquareCycled` (`items/data/Symbols.kt`), black → white → removed. The
editor's click listener now reports the click's panel position beside the node/edge hit. Pinned by
`SquareTests.Authoring`.

**World rendering, verified in game.** `PuzzlePanelRenderer.renderCellSymbols` draws a 0.4-unit
square in the cell in its dye colour on the symbols depth. Seen on a frame: a black square in the
bottom-left cell and a white one top-right, both centred in their cells and clear of the line, and
the outside line failing both. The error flash carries each failed symbol's shape
(`PanelErrorFlash.Mark`), so a square blinks as a red square at its own size; the first cut blinked a
hexagon there and was caught on screen. The flash draws at `z = -0.013`, one step in front of the
symbols: at the attract ring's depth it visibly floated off the face when viewed at an angle, also
caught on screen. Now verified flush. `WPuzzleEditor.drawSymbols` draws the composer preview with
`fill`; that view has not been checked on screen yet.

## Data model

**Cells are not in the graph.** A square is held in a list on `Panel`, `symbols`, beside `graph` and
`line`, rather than as a node or edge attribute:

```kotlin
enum class Figure { SQUARE }                       // identity only, append-only
data class CellSymbol(val x: Float, val y: Float, val figure: Figure, val color: DyeColor)
```

`x`/`y` is the cell's **centre in panel units**, the same coordinate space nodes use, so a 3×3-node
grid's bottom-left cell is at `(1.0, 1.0)` when the node offsets are `0.5`. Position rather than
grid index, because a grid index is meaningless on the other panel types while a centre is still a
point on the panel, and because every renderer and hit-test already speaks panel units.

Identity and attributes stay apart, exactly as for hexagons: `Figure` names the kind and `color`
is its own field. `SQUARE_BLACK` is the shape to avoid. Stars, polyominoes and eliminators later
add values to `Figure` and fields to `CellSymbol`, nothing else.

NBT: a `symbols` list of compounds on the panel tag, read with the tolerant readers, so every panel
saved before this reads back with no symbols. Absent list → empty, never an error. Colour is stored
as the `DyeColor` ordinal like `backgroundColor` already is.

> **Do not copy cell centres raw through `expandTo`.** `gridOffsets` recentres the lattice when the
> aspect ratio changes, so a square's centre has to be re-derived through the same source/target
> offset arithmetic the nodes go through, or growing a panel silently shifts its squares. Pinned by
> a test mirroring "Growing a grid carries a node's symbol across".

`Panel.cells()` in `Lattice.kt` is the type's third answer: for `Grid`, the `(width−1)×(height−1)`
lattice cell centres, present regardless of what has been carved; empty for `Tree` and `Freeform`.
`Panel.cellAt(x, y)` resolves a click. The Grid-tab transforms leave `symbols` untouched.

## Validation

`items/data/Regions.kt`:

- `Panel.regions(path): List<Set<Cell>>` — union-find over `cells()`. Two side-by-side cells join
  unless the path traversed the edge between them, i.e. the unordered pair of their two shared
  corner nodes appears in `path.zipWithNext()`. Only the path cuts; edge modifiers and absent edges
  are never consulted.
- `Panel.clashingSquares(path): List<CellSymbol>` — every square in a region whose square colours
  number more than one. Empty means the rule passes.

`PuzzleSolver.submit` runs it after `unsatisfiedHexagons`; `SolutionRejected` grows a
`clashingSquares` list beside `missedHexagons`, and `PanelErrorFlash.trigger` takes positions
rather than hexagons so both can flash on a tutorial panel.

## Rendering

A square is a rounded square of side about 0.4 panel units, centred in its cell, in its dye colour.
The world renderer draws it in the symbols pass in front of the line (same `text` layer, own colour
via the vertex tint on `solutionFill`); the composer preview draws it with `fill`. Corner rounding
is a nicety; a plain square ships first.

## Authoring

`squareButton` carries `SquareIcon`. The editor's click listener carries the click's panel position,
since a cell centre is outside `CLICK_PADDING` of every node and edge and would otherwise resolve to
nothing; the square branch runs *before* the "nothing was clicked" early return for that reason.
`Panel.withSquareCycled(x, y)` in `Symbols.kt` does the place / recolour / remove cycle above and
returns null when `(x, y)` is not a cell. `SQUARE_PALETTE` is the one list to extend.

> **The path is matched by position, not node identity.** A path holds the graph's marked copies
> (a `START` node is a different `Node` from the bare position), so `regions` compares corner
> *positions* against the path, never `Node` equality. Same trap as hexagons; do not re-derive it.

## Not done

- **A palette.** Cycling reaches black and white only. A real "tool plus parameter" rail is the
  same work stars and polyominoes need, so it waits for the second consumer.
- **Rounded corners** on the square, if the plain one reads wrong in game.
- **The rail still does not follow the panel type.** The square tool is always present and refuses
  on a tree ([04-1](../minecraft/04-1-puzzle-composer-modifiers.md#the-rail-is-a-function-of-the-panels-type)).
- **Eliminator and star interaction**, which need their own rules first.
- **Region-aware end points / expandTo on carved panels**, already listed in
  [04-2](../minecraft/04-2-puzzle-composer-grid.md#not-done).

## Sources

- [SerGreen/TheWitnessPuzzles rules guide](https://raw.githubusercontent.com/SerGreen/TheWitnessPuzzles/master/Puzzle%20Rules%20Guide/RulesGuide.md)
- [SerGreen/TheWitnessPuzzles `Puzzle.GetSectors`, `BlockRule.ColoredSquareRule`](https://github.com/SerGreen/TheWitnessPuzzles/tree/master/TWPBaseLib):
  sectors are built from the solution line only; a square errors whenever another colour shares its sector.
- [The Witness Wiki: puzzle elements](https://thewitness.fandom.com/wiki/Puzzle_elements)
- [Abel, Demaine et al., "Who witnesses The Witness?"](https://erikdemaine.org/papers/Witness_FUN2018/paper.pdf)
- `src/main/kotlin/com/xfastgames/witness/items/data/Panel.kt`, `Lattice.kt`, `Symbols.kt`
- `src/main/kotlin/com/xfastgames/witness/screens/solver/PuzzleSolver.kt`
- `src/main/kotlin/com/xfastgames/witness/screens/composer/PuzzleComposerScreen.kt`,
  `screens/widgets/WPuzzleEditor.kt`
