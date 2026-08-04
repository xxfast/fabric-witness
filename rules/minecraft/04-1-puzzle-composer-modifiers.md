# 04-1 Puzzle composer: Modifiers tab

**Category:** workstation tab. The screen it lives in is [04](04-puzzle-composer.md).

The tab that decides **what a panel means**: which node starts, which one ends, which segment is
broken, where the hexagons are. Every one of those is a statement about a node or a segment that is
already there. Which nodes and segments exist at all is the other tab
([04-2](04-2-puzzle-composer-grid.md)).

---

# Design

## The rule

Exactly one tool is selected. Clicking a node or segment in the editor applies that tool:

| Tool    | Target        | Effect |
|---------|---------------|--------|
| Start   | node          | Toggle start disc on / off (`START` ↔ bare). Segments ignore this tool. |
| End     | border node   | Hang / cycle / remove an end-point nub. Interior nodes refuse. See [../witness/02-end-points.md](../witness/02-end-points.md). |
| Break   | segment       | Toggle the segment between normal and broken (a gap the line cannot cross). See [../witness/03-broken-edges.md](../witness/03-broken-edges.md). |
| Hexagon | node or segment | Toggle a hexagon dot on that node or edge. See [../witness/04-hexagon-dots.md](../witness/04-hexagon-dots.md). |

Each click commits immediately to the output panel. There is no undo beyond taking the panel out and
putting a fresh one in.

## The rail is a function of the panel's type

Those four mark up nodes and segments, so every panel gets them. Every other rule in
[../witness/](../witness/README.md) that is not modelled yet is a **region** symbol: squares, stars,
polyominoes, negative polyominoes, triangles and eliminators all sit *inside a cell*. A panel type
with no cells can never take one, so those tools are not in the rail at all when such a panel is in
the machine. Put a tree panel in and the tetromino tool is not greyed out, it is absent: a tree
closes no faces, so there is nowhere on that panel it could ever land.

Cells are one of the three questions a panel type answers
([04-2](04-2-puzzle-composer-grid.md#the-rule)), and this is what that third answer is for.

The rail follows the **type**, not the panel in front of you. Carve a grid down to something with no
closed cells and the region tools stay in the rail; they just have nothing to land on, and say so by
refusing the click. That is how the end tool already behaves on an interior node. A rail that
reshuffled itself while you edited the grid would be worse than a tool that occasionally refuses.

## Not every rule is a tool

Symmetry ([../witness/05-symmetry.md](../witness/05-symmetry.md)) is a statement about the whole
panel rather than about any node or segment on it, so it is not a paint tool whatever else it turns
out to be. Current thinking is that a symmetry panel becomes its own **panel type**, which the rail
already knows how to handle: it would bring its own controls (an axis, and the coloured hexagons
that only mean something once there are two lines) and they would appear only when such a panel is
in the machine.

Worth noting for whoever picks that up: a symmetry panel answers all three anchor questions exactly
the way a grid does ([04-2](04-2-puzzle-composer-grid.md#the-rule)), same lattice, same joinable
pairs, same cells. It differs in how it is *solved*, not in what can be drawn on it, so it inherits
the Grid tab unchanged.

## Edge cases

- **Start only marks nodes.** A start point is a node's role, so clicking a segment with it selected
  does nothing. A legacy panel that stored `START` on an *edge* draws as a plain segment and traces
  like one ([../witness/01-start-points.md](../witness/01-start-points.md)).
- **End refuses interior nodes.** A nub has to point out of the panel, so only a node on the border
  can carry one. A corner cycles through its three orientations (diagonal, then squared off along
  each of its two borders) before going bare; everything else is a plain on/off toggle.
- **Hexagon refuses a broken segment.** The line can never reach the middle of a gap, so a dot there
  would make the panel unsolvable by construction. A dot on either *end* of that segment is fine:
  the nodes are still reachable ([../witness/03-broken-edges.md](../witness/03-broken-edges.md)).
- **A hexagon does not disturb what it sits on.** It is held beside a node's role and a segment's
  traversal state, so a start point or an end nub can carry one. It is trivially satisfied there and
  therefore useless, but it is legal, and refusing it would be a rule this mod invented.
- **Marks do not survive their target.** Delete the node or segment underneath a mark on the Grid
  tab and the mark goes with it. Nothing is remembered if you put it back.

---

# Implementation

## Status in this mod

All four tools work. The rail is a fixed 2×3 radio group with the bottom two buttons disabled; it
does **not** yet vary with the panel's type, because no type-dependent tool exists to vary. It now
lives on the Modifiers card of the composer's `WCardPanel` ([04](04-puzzle-composer.md)) rather than
directly on the window.

`WRadioGroup` keeps exactly one member armed: clicking the armed tool re-arms it instead of leaving
the rail with nothing selected. That used to be a way to disarm the editor, which read as a dead
screen rather than a mode.

## Tool → panel transforms

| Tool | Code path |
|------|-----------|
| End | `Panel.withEndPointToggled(node)` (`items/data/EndPoints.kt`) |
| Hexagon | `Panel.withSymbolToggled(node, edgeNodePair)` (`items/data/Symbols.kt`) |
| Start | cycle node modifier `START` ↔ `NORMAL` via `nextIn`, then rebuild the graph node in place |
| Break | cycle edge modifier `BREAK` ↔ `NORMAL` via `nextIn`, then re-put the edge |

Start and break still do the graph rewrite by hand in `PuzzleComposerScreenDescription`'s click
listener (copy graph, remove and re-add the node or edge). End and hexagon already live on `Panel`
helpers with unit tests, which is where the other two belong too. The
`when (Panel.Grid / Tree / Freeform)` copy at those call sites is the same sealed-type tax the dye
recipe pays.

A `Node` is a data class and the graph's key, so changing any field makes a *different* node: it has
to be removed and re-added with its whole neighbourhood re-linked rather than mutated in place. That
is what `Panel.withNodeReplaced` exists for.

## Hit testing

`WPuzzleEditor.onClick` maps the click into panel coordinates, then takes the first node whose
position falls inside a ±`CLICK_PADDING` box and the first edge whose bounding box intersects it.
Node wins over edge when both match, since a node sits on every edge that meets it and would
otherwise be unclickable.

**First, not nearest.** A click within the padding of a node's centre intersects all four of its
incident edges, and iteration order decides which one comes back. Break and hexagon have always
lived with that. `Graph.kt` already has `nearestEdge` doing proper point-to-segment distance, which
is what this should use.

## Not done

- **No region symbols.** Squares, stars, polyominoes, negative polyominoes, triangles and
  eliminators are all unmodelled ([../witness/](../witness/README.md)), and the blocker is not the
  rail:
  - **A cell has nowhere to store a symbol.** `Node` carries a `symbol: Atom` and `Edge` carries a
    `symbol: Atom`. Cells carry nothing, because cells are not in the data model at all. Keying them
    by grid index is trivial for `Grid` and meaningless for every other type; keying them by face of
    the planar embedding is general and real work. That decision comes with the first region symbol.
  - **The rail does not have room.** Four node/edge tools plus six region symbols is ten, and the
    rail is 2×3.
  - **Region symbols are not toggles.** Squares and stars need a colour, polyominoes need a shape
    and possibly a rotation. The rail has to grow a "tool plus parameter" model (a palette beside
    the selection) that a radio group cannot express.
- **Hit testing picks the first match, not the nearest** (above).
- **No solution-line editing.** The editor draws an existing line if the panel carries one, but no
  tool clears or draws it. Growing a panel already drops the line at craft time; the composer does
  not mirror that on edit.

## Sources

- `src/main/kotlin/com/xfastgames/witness/screens/composer/PuzzleComposerScreen.kt`: tools, click
  listener, commit.
- `src/main/kotlin/com/xfastgames/witness/screens/widgets/WPuzzleEditor.kt`: editor paint + hit
  testing.
- `src/main/kotlin/com/xfastgames/witness/screens/widgets/icons/`: the tool icons, drawn in code.
- `src/main/kotlin/com/xfastgames/witness/items/data/EndPoints.kt`, `Symbols.kt`: end / hexagon
  panel transforms.
- Witness rules this tab composes: [../witness/01-start-points.md](../witness/01-start-points.md),
  [../witness/02-end-points.md](../witness/02-end-points.md),
  [../witness/03-broken-edges.md](../witness/03-broken-edges.md),
  [../witness/04-hexagon-dots.md](../witness/04-hexagon-dots.md).
