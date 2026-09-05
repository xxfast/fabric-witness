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
| Square  | cell          | Place a black square; click again for white; again to remove. Refuses on a panel with no cells. See [../witness/06-colored-squares.md](../witness/06-colored-squares.md). |

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

## What each type's rail holds

| Tool | Grid | Tree | What it is on a tree |
|------|------|------|----------------------|
| Start | ● | ● | The trunk's foot, or a tip for an upside-down tree |
| End | ● | ● | A nub off a tip, or off the root; the two outermost tips are corners and square off left or right ([01-1](01-1-tree-panel.md#what-a-tree-panel-is)) |
| Break | ● | ● | A gap on a branch, as on any segment. The Orchard's broken limb is not this; it is a pruned limb with no end ([01-1](01-1-tree-panel.md#what-a-tree-panel-is)) |
| Hexagon | ● | **Apple** | The same mark, drawn as an apple here and not at all in the world: the author's note of the intended tip. Tips only, one per tree |
| Square | ● | absent | A tree has no cells |

The Modifiers rail is what a panel's type says it is, so the two rails are the two rows above and
nothing else is decided here. A third type brings its own column.

```
  Grid in the machine        Tree in the machine
  ┌────┬────┐               ┌────┬────┐
  │ ●  │ ⊣  │  start  end   │ ●  │ ⊣  │
  ├────┼────┤               ├────┼────┤
  │ ─  │ ⬡  │  break  hex   │ ─  │ 🍎 │  break  apple
  ├────┼────┤               └────┴────┘
  │ ■  │    │  square
  └────┴────┘
```

**The apple is the hexagon tool as the author's aid.** On a tree the mark is never drawn on the
block face or in a frame ([01-1](01-1-tree-panel.md#what-a-tree-panel-is)): the Orchard's panel
shows no apple, and the clue that points the player at the right tip is the map maker's to build in
the world. The composer is the one place the mark shows, as an apple on the tip it is on, so the
author can see which tip they have made the answer. Same tool, same rules, same refusal on a broken
segment.

**The editor draws what the frame will draw, plus what the author needs to see.** On every other
panel those are the same picture. A tree's apple is the one deliberate exception: shown here,
hidden there.

**The rail changes when the panel does, not when the tab does.** Put a tree in and the rail is the
tree's before you click anything; swap it for a grid and the square is back. If the tool you had
armed is not in the new rail, the start tool is armed instead: it exists on every type, and it is the
first thing anyone places on a fresh panel. An empty machine shows the grid's rail, since a grid is
what a panel is until something says otherwise.

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
- **The apple refuses everything but a tip, and there is only ever one.** On a tree the hexagon
  tool picks the answer tip: a click on a tip or on its nub moves the apple there, a click on the
  apple's tip or nub removes it, a click on a fork, the root, the root's nub or a branch is
  refused ([01-1](01-1-tree-panel.md#edge-cases)). The nub counts because that is where the apple
  is drawn, and a tool has to accept a click where it shows its result. On a grid the hexagon is
  unchanged.
- **Hexagon refuses a broken segment.** The line can never reach the middle of a gap, so a dot there
  would make the panel unsolvable by construction. A dot on either *end* of that segment is fine:
  the nodes are still reachable ([../witness/03-broken-edges.md](../witness/03-broken-edges.md)).
- **A hexagon does not disturb what it sits on.** It is held beside a node's role and a segment's
  traversal state, so a start point or an end nub can carry one. It is trivially satisfied there and
  therefore useless, but it is legal, and refusing it would be a rule this mod invented.
- **Marks do not survive their target.** Delete the node or segment underneath a mark on the Grid
  tab and the mark goes with it. Nothing is remembered if you put it back.
- **A tool armed on one type does not carry to a type without it.** Arm the square, put a tree in,
  and the start tool is armed. Put the grid back and the square is in the rail again, unarmed.
- **A tree's damage is made on the Grid tab, not here.** The Orchard's broken and shortened limbs
  are pruned limbs ([01-1](01-1-tree-panel.md#pruning-the-grid-tab-on-a-tree)), with or without an
  end; this tab only places the end.
- **A fresh tree arrives already marked.** Start on the root, an end on every tip
  ([01-1](01-1-tree-panel.md#what-a-tree-panel-is)); the tools remove or move them as on any panel.
  A fresh grid arrives blank. The rail is the same either way.

---

# Implementation

## Status in this mod

All five tools work, and the rail follows the panel's type
([what each type's rail holds](#what-each-types-rail-holds)), built 2026-09-05. `WModifiersRail` in
`PuzzleComposerScreen.kt` is one `WPlainPanel` that re-lays the same six button objects for the
output slot's type: the grid gets all six (the last still disabled), a tree gets start, end, break
and the hexagon button wearing `AppleIcon`. The buttons stay the same objects because the click
listener tells tools apart by identity. If the armed tool is not in the new rail the start tool is
armed. It lives on the Modifiers card of the composer's `WCardPanel` ([04](04-puzzle-composer.md)).
A side effect worth knowing: the rail's first layout arms the start tool, so the composer now opens
with a tool armed where it used to open with none and swallow the first click. That is what "exactly
one tool is selected" above always said.

**The rail re-checks the slot every client tick**, not on a listener. The output slot is filled by
vanilla slot sync on the client, which fires no `WItemSlot` change listener there; `tick()` is
the one hook that runs where the rail is drawn. The check is a type compare and a no-op when
unchanged. Do not move the refresh onto the slot listeners; they run on the server.

The editor draws a tree's hexagons as apples (`WPuzzleEditor.drawApple`); the world renderer
skips them on a tree (`renderSymbols(hidden = true)`). The apple hangs off the tip's **nub** when
it has one, not on the tip node the mark is stored on: the mark says "this end", and drawn at the
node it sat where the branch meets the nub and read as short of the end (seen 2026-09-05). A bare
stub, with no nub, wears it on the node.

`WRadioGroup` keeps exactly one member armed: clicking the armed tool re-arms it instead of leaving
the rail with nothing selected. That used to be a way to disarm the editor, which read as a dead
screen rather than a mode.

## Tool → panel transforms

| Tool | Code path |
|------|-----------|
| End | `Panel.withEndPointToggled(node)` (`items/data/EndPoints.kt`) |
| Hexagon | `Panel.withSymbolToggled(node, edgeNodePair)` (`items/data/Symbols.kt`) |
| Square | `Panel.withSquareCycled(x, y)` (`items/data/Symbols.kt`), from the click's panel position |
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

- **The tree rail and the editor's apples were seen 2026-09-05**: four buttons, apple icon,
  apples landing on the clicked tip and on a clicked fork, stem up. Not yet seen: swapping to a
  grid and back (square returns, armed tool falls back to start when it vanishes), and the empty
  machine (grid rail).
- **The apple at the nub's end is unseen in game.** Moved there 2026-09-05 after a shot showed it
  sitting where the branch meets the nub, covering the junction and reading as short of the end.
  Confirmed cosmetic by pixel comparison before the move: no node moves. Look for the fruit
  capping the nub, with the branch and nub both visible below it.
- **Hit testing on diagonal branches.** The Modifiers hit test uses a segment's axis-aligned
  bounding box, so on a tree a click near one branch can land on the sibling whose box overlaps
  it. Same "first, not nearest" gap as above, now with a shape that makes it likely. If the
  apple tool misfires in the shot, that is when `nearestEdge` gets wired in.
- **Only one region symbol.** Squares are in ([../witness/06-colored-squares.md](../witness/06-colored-squares.md));
  stars, polyominoes, negative polyominoes, triangles and eliminators are not. Cells now have a home
  (`Panel.symbols`, keyed by cell centre in panel units), so what remains is the rail:
  - **The rail does not have room.** Five tools plus five more region symbols is ten, and the rail
    is 2×3.
  - **Region symbols are not toggles.** The square gets away with a black/white cycle; stars need a
    real colour, polyominoes a shape and possibly a rotation. The rail has to grow a "tool plus
    parameter" model (a palette beside the selection) that a radio group cannot express.
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
