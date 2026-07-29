# 04 — Hexagon dots

**Category:** line mechanic

A hexagon is a mark the line is obliged to cover. It is the only rule that adds an obligation to the
path itself rather than removing an option from it or constraining a region.

---

# Design

## The rule

The path must pass through every hexagon on the panel. Miss one and the panel fails.

A hexagon sits on one of the two things a line touches:

- on a **node**, satisfied when the path visits that node;
- on the **middle of an edge**, satisfied when the path traverses that edge, entering at one endpoint
  and leaving at the other.

Same rule, two placements. Formally, for a submitted path $P$ with nodes $N(P)$ and traversed edges
$E(P)$, the panel's hexagons are satisfied iff every hexagon-carrying node is in $N(P)$ and every
hexagon-carrying edge is in $E(P)$.

Order does not matter, and neither does direction. The line may reach a hexagon from either side and
at any point during the solve; the only question asked, once, is whether it was covered when the line
reached its end point.

Worked example, a 3×3-node panel with `S` a start, `E` an end, `*` a node hexagon and `·` an edge
hexagon:

```
o---o---o
|   |   |
o   *---o
|   |   |
S-·-o---E
```

The bottom edge out of `S` and the middle-right node are both obliged. `S → right → right → E` covers
the edge hexagon but misses the node, so it fails. `S → right → up → right → down → E` covers both,
and solves. Nothing forces the top row to be visited at all: unvisited grid is fine unless a hexagon
says otherwise.

## What it does to a panel

Hexagons are the first rule that can fail a line that legitimately reached an end point. Up to here a
panel is solved by arriving; with hexagons, arriving is necessary but no longer sufficient. That is
the whole point of the mechanic, and it is what makes a panel able to have a wrong answer that still
looks finished.

They compose with [broken edges](03-broken-edges.md) rather than duplicating them: a break subtracts
an option, a hexagon adds an obligation. A panel carrying both is constrained from both directions,
which is exactly how the base game uses them together on the Glass Factory plates.

## Colored variants

Only meaningful on symmetry panels ([05-symmetry.md](05-symmetry.md)):

- **Black**: either line may cross it.
- **Colored** (blue / yellow, matching the two lines): must be crossed by the line of that color
  specifically. The invisible line still has a color and still counts.

## Edge cases

- A hexagon is validated against the path, never against a region. It has no notion of being "inside"
  a colored area, so it is unaffected by how the finished line partitions the grid.
- A hexagon on a start or end node is trivially satisfied by starting or finishing there, since the
  line necessarily visits that node. Legal, but it carries no information.
- A hexagon cannot sit on a broken edge: the line would have to cover a point it can never reach, so
  the panel would be unsolvable by construction. A hexagon on one of that edge's *endpoints* is
  unaffected, since the node is still reachable by its other edges.
- A hexagon on the single edge into a dead-end node forces the line to enter that stub. On a simple
  path it can never come back out, so unless that dead end is an end point, the hexagon makes the
  panel unsolvable.
- [Eliminators](11-eliminators.md) can cancel one missed hexagon per region, the same as any other
  failing rule, but only if the hexagon falls inside the eliminator's region. A node-hexagon belongs
  to the regions touching that node; an edge-hexagon to the regions on both sides of that edge. Where
  that can't be pinned to one region unambiguously, treat it as **unverified**: guides describe
  eliminators eating "a missed dot" in general terms without spelling out region membership for a
  boundary-sitting hexagon.
- **Unverified / rare variants** described in secondary sources but not corroborated against a primary
  reference: "sound-based" hexagons whose size indicates pitch (the audio-log puzzles near the
  Jungle/Orchard), and "ordered" hexagons that must be crossed in sequence. Neither is part of the
  base rule above; do not implement either without further verification, they may be conflating
  hexagon dots with a different late-game mechanic.

---

# Implementation

## Status in this mod

**Data model, done.** A hexagon is `Symbol.HEXAGON` (`items/data/Edge.kt`), held on `Node.symbol`
beside the node's `modifier`, and on `Edge.symbol` beside the edge's `modifier`. `Edge` is a data class
rather than the old `typealias Edge = Modifier`, so a start point or a broken edge can carry a symbol
too. It round-trips through NBT, survives `Panel.Grid.expandTo`, and reads legacy `Modifier.DOT` back
into the new field from both positions. Pinned by `SymbolTests`.

**Authoring, done.** The composer's hexagon tool is enabled and carries `HexagonDotIcon`. Clicking
runs `Panel.withSymbolToggled` (`items/data/Symbols.kt`), which toggles the symbol on the clicked node
or, failing that, the clicked edge. A node wins over an edge when both match, since a node sits on the
edges that meet it and would otherwise be unclickable. A broken edge refuses one. Covered by
`SymbolTests.Authoring`.

**Validation, done.** `Panel.unsatisfiedHexagons(path)` (`items/data/Hexagons.kt`) returns the hexagons
a submitted path fails to cover, and `PuzzleSolver.isValidSolution` now rejects unless it is empty. It
returns the failures rather than a verdict so per-symbol feedback has something to build on. Covered by
`HexagonTests` and three cases in `PuzzleSolverTests`.

**Rendering: world done, composer unsettled.** Both `WPuzzleEditor` and `PuzzlePanelRenderer` draw
hexagons in a pass in front of the solution line, in the panel's backdrop colour. The world renderer
reuses `circle` at `resolution = 60.0`, whose six-step sweep is exactly a point-up hexagon; the
composer's 2D path uses a scanline `DrawableHelper.hexagon`.

Verified in game on a traced panel: hexagons stay visible on top of the drawn line, draw as clean
point-up hexagons, and leave line visible either side so they never sever it. The composer preview
does not hold up at its raster size, see the open question below.

Not modelled at all: colored hexagons (no color field, and no second line to attribute a crossing to).

Tracing needed no change. `edgeLimit` and `IMPASSABLE_EDGES` match on `modifier` alone, so a dotted
edge traces exactly like a plain one, pinned by `PuzzleSolverTests`.

## Data model

**A node's role and its symbol are separate fields.** `Node.modifier` used to be the only field, so a
node was `START` *or* `END` *or* `DOT`, never two at once. That collision was not acceptable even
though a hexagon on a start point is worthless (see Edge cases): the same collision blocks every region
symbol later, when a cell will need to carry a square *and* be a grid position. `Node` now carries
`symbol` alongside `modifier`, which stays the role.

The migration is additive. `putNode` writes a new key; `getNode` reads it with `getIntTolerant`, which
defaults a missing key to ordinal `0`, so panels saved before this change read back with no symbol.
`Panel.Grid.expandTo` carries the symbol across alongside the modifier; without that, growing a
composed puzzle would silently drop its hexagons.

**Traps.** `Node` is a data class used as a graph key, so equality covers every field: placing a
hexagon means removing the node from the graph and re-adding the updated copy with its edges re-linked,
not mutating it. That is what `Panel.withNodeReplaced` does. Any caller holding the old node must
re-look it up afterwards, or it is holding a node the graph no longer contains.

**An edge splits the same way.** The tempting shortcut is to leave a hexagon as an edge *value*
(`Modifier.DOT` in place of `NORMAL`) and split only the node. Don't: an edge value is one int per cell
of an `IntArray(n * n)` adjacency matrix, with nowhere to hang an attribute, so the shortcut is exactly
what makes the deferred variants (below) unimplementable later. An edge carries a traversal state
(`NORMAL`, `BREAK`, `HIDDEN`, absent) and, separately, a symbol, so `Edge` is a data class rather than
the old `typealias Edge = Modifier`.

That split was cheaper than it looked. No recipe JSON embeds a panel graph, every panel recipe is a
code recipe, so the CLAUDE.md warning about inline `components` does not apply. In NBT, edge symbols go
in `edgeSymbols`, a second int array parallel to the existing `edges` array in `Graph.kt`, read with the
same tolerant reader, so panels saved before this change come back with no symbols and the existing
`edges` array is untouched.

Traversal never needs to know a hexagon exists: `PuzzleSolver.edgeLimit` and `IMPASSABLE_EDGES` match
on state alone, and a dotted edge traces exactly like a plain one because it *is* a plain one wearing a
symbol.

## What this does to panels already in a world

Bounded, because nothing generates a panel. `data/witness/worldgen/` places bushes only, so a panel
enters a world exclusively by crafting or the creative menu, and lives in exactly three places: an
ItemStack's `witness:panel` component, a `PuzzleFrameBlockEntity`'s inventory, and the composer block
entity's slots. All three are the same component going through `Panel.CODEC`, so there is one read path
to get right. `PuzzlePanelItem` registers `Panel.DEFAULT` as the component's *default*, so a stack that
was never edited stores no component at all and can never be stale.

No panel anywhere can currently carry a hexagon: the composer's hexagon button is disabled with no
click branch, node modifiers only ever become `START` or `END`, and edge values only `NORMAL` or
`BREAK`. So there is no real data to migrate. Read a legacy `DOT` into the new symbol field anyway
(`DOT` on a node means symbol only, on an edge means `NORMAL` plus symbol); it is two lines and it keeps
a hand-edited or dev panel meaning what it said.

> **Do not remove `Modifier.DOT` from the enum when the split lands.** Nodes and edges both serialize as
> `.ordinal` and read back as `Modifier.values()[it]`, unguarded. Dropping `DOT` from
> `{NONE, NORMAL, BREAK, DOT, START, END, HIDDEN}` shifts every ordinal above it: saved `START` (4)
> reads back as `END`, saved `END` (5) as `HIDDEN`, and saved `HIDDEN` (6) is out of range and crashes
> the load. Every existing panel silently swaps its start point for an end point, or fails to load.
> `DOT` stays at ordinal 3 as a reserved legacy value, unused. The same applies to any future
> reordering: this enum is append-only.

The other two traps in the read path:

- **The new edge-symbol array will be absent on every existing panel.** `getIntListTolerant` returns an
  empty list for a missing key, and `chunked(nodes.size)` on that gives an empty matrix, so anything
  that walks the symbol matrix in step with the state matrix reads off the end or drops every edge.
  Default per cell, not per array.
- **Adding a field to `Node` changes `equals`/`hashCode`, and `Node` is the graph key.** `Panel.line`
  serializes its own copies of the nodes, separate from `Panel.graph`. Today they compare equal; once a
  hexagon can sit on a node, a stored line's copy of that node no longer equals the graph's. Nothing
  currently depends on it (`renderLine`, `drawSolution` and the solver all treat line nodes standalone),
  so this is latent rather than broken, but `line.nodes()` and `graph.nodes()` stop being guaranteed to
  intersect and should not be assumed to.

Forward-looking, `Panel.Grid.expandTo` must carry the symbol alongside the modifier, or growing a panel
through the grid recipe silently strips its hexagons.

## Room for the deferred variants

Colored and sound (size-varying) hexagons are deliberately deferred: they appear much later in the
game, and they depend on machinery this mod does not have. They are not to be designed around, but they
are not to be *precluded* either, which constrains the data model in one specific way:

**A symbol field holds symbol identity only. Every variant attribute is its own field.** Colour and
size are attributes of a hexagon, not different kinds of hexagon, so they never become enum values.
`HEXAGON_BLACK` / `HEXAGON_BLUE` / `HEXAGON_LARGE` is the shape to avoid: NBT stores these by ordinal,
so an enum can only ever be appended to, and a combinatorial one is unfixable once panels exist that
use it.

With identity and attributes kept apart, both variants land additively later: a node is an NBT compound
and takes new keys freely, and an edge takes one more parallel array per attribute, the same way the
symbol array is added now. Neither needs a migration.

## Validation

`Panel.unsatisfiedHexagons(path)` runs after the line rule passes: it walks the graph once, collecting
hexagon-carrying nodes and edges, and checks each against the submitted path. O(hexagons), no search.
Unlike the region symbols, hexagons need no flood-fill, which is why they land ahead of rules 06 on.

An edge counts whichever direction it was crossed, and order never matters, so consecutive path pairs
are compared as unordered pairs.

Two details fall out of the existing solver rather than needing special handling:

- A tip that committed onto an end point nub is snapped onto it by `finishLine` *before* validation, so
  a hexagon on that final nub edge counts as covered, matching what the player sees.
- A hexagon on a broken edge cannot be reached, and the panel is simply unsolvable. Left as an
  authoring error, not a guarded case, and the composer refuses to create one in the first place.

> **A path holds the graph's nodes, not equal-looking copies.** Marking a node produces a different
> `Node`, so a test (or any caller) that builds a path from its own fixtures compares against nodes
> the graph no longer contains and every hexagon reads as missed. Resolve path nodes from the panel.
> This cost real time twice; do not re-derive it.

## Rendering

**Hexagons draw in their own pass, in front of the drawn line.** In the game a hexagon stays visible
under the line as a dark mark; this mod draws the graph at `z = -0.01` and the solution line in front
of it at `z = -0.011`, so a hexagon emitted in the graph pass is covered the instant the line reaches
it. It needs a third `submitCustom` pass in front of both, with its own dark texture alongside
`lineFill` and `solutionFill`.

`utils/VertexConsumer.kt`'s `circle` takes a `resolution`, and at `60.0` its quad fan emits exactly six
arc points, so the world hexagon needed no new primitive: `RenderContext.hexagon` is that call. The arc
starts at the top, which puts a point up rather than a flat. The composer's 2D path has its own
scanline `DrawableHelper.hexagon`, since it draws through `DrawContext.fill` rather than geometry.

An edge hexagon sits at the midpoint of its edge. A node hexagon sits on the node, replacing the
junction disc `renderNode` would otherwise draw there.

> Do not derive the hexagon's on-screen orientation from the render transforms. The item icon, block
> entity and composer widget do not share a mirror convention, so reading the matrices predicts the
> wrong answer. Check it in game.

## Authoring

`hexagonDotButton` is enabled and its click handler delegates to `Panel.withSymbolToggled`, which
toggles the symbol on the clicked node or, failing that, the clicked edge. A node wins over an edge
when both match, since a node sits on the edges that meet it and would otherwise be unclickable.
Placing a hexagon on a broken edge is refused, since the rule makes it unsolvable by construction. A
start or end node accepts one: it carries no information, but refusing it would be a rule this mod
invented.

Authoring an unsolvable panel is otherwise allowed and intended. Deciding whether a panel with
hexagons is solvable at all is a Hamiltonian-path-flavoured search, and the composer does not attempt
it: an impossible panel is the author's problem, the same as in the base game.

## Open questions

- **A hexagon and a broken edge look the same in the composer preview.** A hexagon draws in the
  panel's background colour, so on an edge it cuts through the dark grid line, which is exactly what a
  break does. Confirmed in game, not predicted from the code: at the editor's line width the mark
  severs the line and protrudes past its edges rather than leaving line either side. Candidate fixes
  are half line width, a dark outline around the background fill, or reverting to a dark mark narrower
  than the line. Deferred, not resolved.

  **This is a composer-only problem.** The world renderer was checked separately and does not have it:
  at 3.pc across corners against a 4.pc line, line survives visibly on both sides of the hexagon, so
  it reads as a mark sitting on the line rather than a gap in it. The same ratio fails in the editor
  purely because `DrawableHelper` rasterises it into about five pixels, where the margin rounds away.
  Fix the editor, leave the world renderer alone.

## Not done

Deferred on purpose, and expected to arrive with the mechanics they depend on rather than as follow-up
work on this rule. The data model leaves room for each (see Room for the deferred variants); none of
them is blocked on anything decided here.

- **Colored hexagons**, which need [symmetry](05-symmetry.md) first. There is no second line to
  attribute a crossing to, so the rule cannot be stated, let alone checked.
- **Sound hexagons**, whose size encodes pitch. Still unverified against a primary source (see Edge
  cases), so the size attribute stays unmodelled until the variant itself is confirmed to exist.
- **Eliminator interaction**, which needs region flood-fill ([11-eliminators.md](11-eliminators.md)).
- **Per-symbol failure feedback.** A rejected solution clears the whole line; the game flashes the
  specific symbols that failed, so the player learns *which* hexagon was missed. Out of scope here, but
  this is the first rule where the difference is felt.

## Sources

- [SerGreen/TheWitnessPuzzles Rules Guide](https://raw.githubusercontent.com/SerGreen/TheWitnessPuzzles/master/Puzzle%20Rules%20Guide/RulesGuide.md)
- [GameFAQs: Puzzles & Symbols walkthrough](https://gamefaqs.gamespot.com/pc/969704-the-witness/faqs/82392/puzzles-and-symbols)
- [Gameranx: The Witness Puzzle Types And Rules Guide](https://gameranx.com/features/id/36898/article/the-witness-puzzle-types-and-rules-guide/)
- [Abel, Bosboom, Coulombe, Demaine et al., "Who witnesses The Witness?"](https://erikdemaine.org/papers/Witness_FUN2018/paper.pdf) (confirms hexagons are formally a "forced edge"-style clue, sufficient on their own for NP-hardness)
- `src/main/kotlin/com/xfastgames/witness/items/data/Edge.kt`, `Node.kt`, `Panel.kt`, `Graph.kt`
- `src/main/kotlin/com/xfastgames/witness/items/renderer/PuzzlePanelRenderer.kt`, `PuzzlePanelTextures.kt`
- `src/main/kotlin/com/xfastgames/witness/screens/solver/PuzzleSolver.kt`
- `src/main/kotlin/com/xfastgames/witness/screens/widgets/WPuzzleEditor.kt`,
  `screens/composer/PuzzleComposerScreen.kt`, `screens/widgets/icons/HexagonDotIcon.kt`
- `src/main/kotlin/com/xfastgames/witness/utils/VertexConsumer.kt`, `DrawableHelper.kt`
