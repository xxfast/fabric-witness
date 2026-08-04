# 04-2 Puzzle composer: Grid tab

**Category:** workstation tab. The screen it lives in is [04](04-puzzle-composer.md).

The tab that decides **what a panel is**: which nodes sit on it and which of them are joined. A
panel does not have to be the full rectangle it was crafted as. What those nodes and segments then
*mean* is the other tab ([04-1](04-1-puzzle-composer-modifiers.md)).

---

# Design

## The rule

Anchors are the general form. **A panel type answers three questions and nothing else: where may a
node sit, which two nodes may be joined, and what are its cells.** That is all that differs between
one panel type and the next. This tab is one editor over the first two answers; the third decides
which region symbols the [Modifiers tab](04-1-puzzle-composer-modifiers.md) can offer.

| Type | Where a node may sit | Which nodes may be joined | Cells |
|------|----------------------|---------------------------|-------|
| Grid | The `width` × `height` lattice points inside the panel | Neighbours along a row or a column | The (`width`−1) × (`height`−1) squares between them |
| Tree | The branch positions of a tree of this depth | A branch and the branch it hangs from | None. A tree closes no faces, so no region symbol can ever go on one |
| Freeform | Anywhere on the panel | Any two nodes | Whatever faces the drawing happens to close |

This tab draws the same live picture of the output panel as the other one, plus the two things a
finished panel cannot show you. Both are editing affordances for this tab only: the item, the block
face, and the panel in the world never draw them.

| Drawn | Means |
|-------|-------|
| Faint dot, at **every** anchor | A node can go here |
| Solid dot on top of one | A node is here, with nothing joined to it |
| Junction and segments | An ordinary node |

**A dot means a node can sit here, not that one is missing.** The faint dot goes at every anchor,
occupied or not, and whatever is really there paints over it. Marking only the *empty* anchors would
invert the panel's own language, where a dot has always meant a node: you would be reading a mark
that means "nothing here", which is exactly backwards from everywhere else in the mod. Drawing the
whole lattice also keeps the tab usable on a panel carved down to almost nothing, where the
remaining segments are no longer enough to infer where the anchors sit.

The solid dot exists because a node with nothing joined to it is otherwise invisible: the panel
renderer draws nothing for it, correctly, since no line can reach it. Without a mark of its own it
would be indistinguishable from a bare anchor while answering the tools completely differently, since
the pencil places a node on an empty anchor and does nothing on one that is already there.

You can only reach that state on purpose now, by pencilling a node onto an anchor with no neighbours.
[The eraser cleans up after itself](#edge-cases), so erasing can no longer strand one.

## Pencil and eraser

Two tools in the rail, exactly one armed, the same shape of rail as the
[Modifiers tab](04-1-puzzle-composer-modifiers.md). The tool says whether a gesture builds or
destroys; where the gesture lands says whether it is talking about a node or a segment.

| Tool | Click a node | Click a segment | Drag |
|--------|--------------|-----------------|------|
| Pencil | Places a node, joined to every neighbour already present | Joins that pair | Draws the stroke: lays a node on every anchor it crosses and joins each to the one before |
| Eraser | Removes the node, and every segment hanging off it | Lifts that segment, and either endpoint it leaves bare | Lifts every segment the stroke crosses, same rule |

**A click hits a node first, then a segment.** A position near a node is near all four segments
meeting it, so the node has to win or it becomes unclickable; only a click that missed every anchor
falls through to the segment under it. That is the same order the Modifiers tab's hit testing
already uses, and it is what makes clicking an edge here mean what clicking an edge means there.

Segment hit testing works off the *lattice*, not off what is drawn, so the pencil can click a
segment that is not there yet. Nothing else could add one back without dragging.

**Drag is the fast path, not the only path.** An earlier pass made segments drag-only, on the
reasoning that a click is about nodes and a drag is about segments. That was wrong in practice:
clicking an edge did nothing at all, which reads as broken rather than as a mode, and it
contradicted the tab next door where clicking an edge is how break and hexagon are applied. Drag
earns its place by painting a run of segments in one gesture, not by being the only way to reach
one.

**Why tools rather than gestures that toggle.** An earlier pass had no tools: clicking an anchor
added or removed depending on what was there, and dragging flipped the segment between two nodes.
Two things were wrong with it.

A toggle cannot paint. Sweeping across a row where some segments are present and some are not
flips each one it meets, so the stroke leaves an alternating mess rather than a line. Drawing a
shape is the main thing this tab is for, and it needs a gesture whose result does not depend on
what it happens to pass over.

A toggle also makes the destructive half of the tab reachable by accident. Clicking a node takes
it *and every segment on it*, and there is no undo anywhere in the composer. Arming the eraser
first is the cheap version of a confirmation: a stray click with the pencil out lands on a node
that is already there and does nothing.

**A stroke does not weld.** A node the pencil places with a *click* joins to every present
neighbour, because a click means "this cell belongs here", and that makes restoring a chunk of
carved grid one click per node rather than one per segment. A node laid down by a *stroke* joins
only along the stroke, because a stroke means "this line belongs here". Without that split, drawing
a line alongside existing geometry would fuse the two, and a deliberate gap between two parallel
runs could not be drawn at all.

**A fast drag draws a staircase.** A drag reports a cursor position per frame, so a quick sweep
jumps several anchors at once. Rather than drop those steps and tear a hole in the stroke, the path
between the last anchor and the current one is walked a unit at a time, along the x axis first and
then the y. A diagonal sweep therefore lays a staircase, which is the only thing a stroke
constrained to a lattice can mean.

**No Fill or Clear.** An earlier pass had both as buttons here. Fill only ever reached the state a
freshly crafted panel is already in, so it was an undo for carving wearing a tool's clothes, and on
a small panel it was slower than carving: on a 3x3, clearing and redrawing a five-node shape is six
clicks against four deletions. Clear has the one argument the eraser does not answer, since
emptying a large panel is one click against one per node, so it may come back. Neither is in the
rail today.

## What it does not do

It changes the shape of a panel, never its extent. A `Grid` panel stays a `Grid` panel of the same
`width` × `height` no matter what you delete: the anchors stay exactly where they were, which is
what makes every deletion reversible. Size and type both come from crafting
([01](01-puzzle-panel-crafting.md)), and this tab cannot place a node outside the extent the panel
was crafted at.

```
  ●───●───●        ●───●   ·        ·   ·   ·
  │   │   │        │   │            │       │
  ●───●───●   →    ●───●───●   →    ●───●───●
  │   │   │        │   │   │            │
  ●───●───●        ●───●───●        ·   ●   ·

     crafted         a corner          drawn from
                     deleted           empty
```

All three are 3×3 panels and all three cost the same.

**Editing never changes a panel's type.** The type plus the size fields *are* the anchor set; the
graph is only what is currently sitting on it. If carving a grid demoted it to a freeform panel it
would silently lose the ability to be grown at a crafting table, which is value destroyed by an edit
that looks cosmetic.

## Cost

Free in both directions, and it refunds nothing. Clearing a panel to nothing does not make it
cheaper to recycle ([03](03-panel-recycle.md)), and filling it back costs nothing, because
`witness:cost` tracks tablets invested, not nodes present.

**Shape is free; size is paid for.** A carved panel is worth exactly what its bounding grid cost, so
an L carved out of an 8×8 is still an 8×8 panel. That is the boundary between this tab and
[crafting](01-puzzle-panel-crafting.md), and it is what keeps the two from competing: there is no
route here that produces a bigger panel, or a cheaper one, than the crafting table does.

## Edge cases

- **Deleting takes the marks with it.** A start disc, a hexagon, a break, an end point nub: gone
  with whatever carried it. Put the node or segment back and it comes back plain. Nothing is
  remembered.
- **Deleting a node deletes its end point.** A nub is a node plus the one segment holding it on, so
  it cannot outlive the node it hangs from. Use the end tool on the other tab if you only want to
  move it.
- **A missing segment is not a broken one.** Both stop the line, and they are different. A break is
  still a segment: it is drawn, and the line can be pushed into it up to the gap
  ([../witness/03-broken-edges.md](../witness/03-broken-edges.md)). A segment that is not there has
  nothing to push into and nothing to see. Deleting is the stronger of the two.
- **Shape survives growing.** Grow a carved panel at a crafting table and the holes stay holes. The
  new ring of grid around them comes in complete, because that part was paid for. Not true yet:
  `expandTo` restores deleted nodes ([below](#not-done)).
- **End points follow the shape you can see.** Delete a panel's whole top row and the row below
  becomes the border, so nubs hang off the visible shape rather than off the rectangle the panel was
  crafted at. Not true yet either, and for the same reason ([below](#not-done)).
- **The eraser never leaves a node joined to nothing.** Lift a node's last segment and the node
  goes with it. Such a node is inert everywhere downstream, invisible on a finished panel and
  unreachable by any line, so leaving one behind would be depositing residue rather than erasing.
  Whichever endpoint keeps a segment stays exactly as it was, marks included.
- **A node with no segments is still legal, just never accidental.** The pencil can place one on a
  bare anchor, and this tab draws it as a solid dot so it is visible where it matters and invisible
  where it does not. What changed is that you can no longer arrive at one by erasing.
- **A nub counts as a segment.** Strip a border node's last grid segment while an end point still
  hangs off it and the node stays, because the nub is holding it on
  ([04-1](04-1-puzzle-composer-modifiers.md)). Only the end tool removes that.
- **Erasing a panel down to nothing is allowed.** Same stance as everywhere else here: compose
  freely, a bad panel is a content problem, not a UI reject.

---

# Implementation

## Status in this mod

**Built for `Grid`.** The tab exists, with the pencil and the eraser, click and drag on both, faint
dots at empty anchors, and the anchor walk that keeps a fast sweep from tearing its own stroke.
`Tree` and `Freeform` answer "no anchors" and are therefore not editable here, which is the
intended MVP boundary rather than a gap to fill in.

Deleting a node or a segment had been impossible anywhere in the mod until this tab. The pre-graph
tile model could do half of it: a segment cycled `FILLED → SHORTENED → null`, and `null` was a
segment that was not there. That was lost in the move to a `ValueGraph` (`dcaaf7f`), where break
became a two-state toggle and nothing wrote absence any more.

## Where the answers live

`items/data/Lattice.kt` holds the type's two answers and the editor built on them. It is deliberately
panel-level rather than screen-level, so the operations are unit-testable without a client:

| | |
|---|---|
| `anchors()` | Every position a node may occupy. `Grid` enumerates its lattice; `Tree` and `Freeform` return nothing |
| `canJoin(a, b)` | Whether two positions may be joined. Compares position only, never node identity |
| `nodeAt(x, y)` | The node actually present at a position, if any |
| `withNodeAdded` / `withNodeRemoved` | The pencil's and the eraser's click. Removing takes the node's segments and its end point nub |
| `withSegmentAdded` / `withSegmentRemoved` | The pencil's and the eraser's stroke, a unit step at a time |
| `nearestJoinablePair(x, y, tol)` | The segment a click landed on, drawn or not |
| `anchorPathBetween(from, to)` | The anchors a fast drag skipped over, so every step gets painted |

`WPuzzleEditor` reports *where a gesture landed on the lattice*, never what it should mean;
`PuzzleComposerScreenDescription` reads the armed tool and picks the transform. That keeps the
widget free of any notion of a pencil, which is what lets the Modifiers tab share it unchanged.

## Absence needs no new data

A node or segment that is simply absent from the panel's `ValueGraph` already means what the design
says, everywhere downstream:

- Nothing is rendered. `PuzzlePanelRenderer` and `WPuzzleEditor` both iterate `graph.nodes()` /
  `graph.edges()`, and a node with no visible incident edges draws nothing at all.
- The solver has no adjacency to offer. `PuzzleSolver.chooseSegment` walks `graph.adjacentNodes`.
- It survives a grow. `Panel.Grid.expandTo` deliberately carries a missing **edge** through rather
  than filling it in (`PanelExpansionTests`, "deliberately missing edges").
- It round-trips. `putValueGraph` writes the current node list plus an n×n adjacency built from the
  same iteration order, so a graph with fewer nodes just writes a shorter list. A carved panel is
  held to that by `LatticeTests`, "carving survives an NBT round trip".

Deleting therefore removes the node or edge outright. `Edge.NONE` would not have worked as a stored
"absent" marker: `putValueGraph` writes `Modifier.NONE` for absent edges and `getValueGraph` reads
`NONE` back as no edge, so an edge carrying that value disappears on the next world load anyway.
`Modifier.HIDDEN` is not it either, since hidden edges are invisible but still traceable
(`PuzzleSolver`).

## Not done

Three things this tab makes newly wrong. None is reachable from inside the composer, which is why
the tab ships without them, but a carved panel can now walk out of the machine and reach all three.

- **`expandTo` fills in missing nodes.** It builds every target node unconditionally and only
  carries edge absence through. A carved panel grown at a crafting table comes back with its deleted
  nodes restored, joined by plain segments, since one end of each counts as "new". Growing has to
  respect an absent node the way it already respects an absent edge. This is the one a player hits
  first, and it silently undoes their work.
- **End point bounds ignore shape.** `EndPoints.borderSigns` measures the panel from every non-nub
  node, so on a carved panel it hangs nubs off deleted geometry. It has to measure the nodes that
  are actually there.
- **`Panel.Type` is decoded without a fallback.** `Type.values()[getIntTolerant(...)]` in `toPanel`
  throws on an ordinal it does not know, failing the decode of the whole item component.
  `Modifier` and `Atom` both have `getOrElse` fallbacks and a documented append-only contract for
  exactly this reason (`DyeColor.values()[...]` on the next line has the same hole). Adding a panel
  type is safe going forward, but a panel written by a newer version hard-fails instead of
  degrading, and this tab is what makes the type load-bearing.

Deliberately out of scope:

- **Size and type still change at the crafting table only.** This tab touches neither `width` /
  `height` nor `type` ([01](01-puzzle-panel-crafting.md)).
- **Tree and Freeform are not editable.** Freeform has `width` / `height` but "anywhere" anchors, so
  there is no finite set to draw or hit-test, and `resize` is undefined; nothing crafts one, only
  `Panel.TEST` constructs one. Tree's anchors are the branch positions of its tree, which is real
  work of its own. Both answer "no anchors" until someone needs them, so the tab draws nothing and
  refuses every gesture rather than guessing.
- **No undo.** Every edit commits straight to the output panel, on both tabs. Arming the eraser is
  what stands in for it: the destructive gesture cannot be reached with the pencil out. Real undo
  would be a client-side stack of `Panel` snapshots in the screen description, since `commit` is the
  single funnel every edit passes through, and nothing about it needs to touch the panel or the
  block entity.
- **No compose-time validation** of solvable or well-formed panels.

## Sources

- `src/main/kotlin/com/xfastgames/witness/items/data/Lattice.kt`: anchors, `canJoin`, and the
  pencil / eraser transforms.
- `src/test/kotlin/com/xfastgames/witness/items/data/LatticeTests.kt`: what those are held to.
- `src/main/kotlin/com/xfastgames/witness/screens/composer/PuzzleComposerScreen.kt`: the rail, and
  which transform each gesture resolves to.
- `src/main/kotlin/com/xfastgames/witness/items/data/Panel.kt`: the sealed type, `expandTo`,
  `generateGrid` / `generateTree`, NBT round trip.
- `src/main/kotlin/com/xfastgames/witness/items/data/Graph.kt`: `putValueGraph` / `getValueGraph`,
  `nearestNode`, `nearestEdge`.
- `src/main/kotlin/com/xfastgames/witness/items/data/EndPoints.kt`: `borderSigns`, nub geometry.
- `src/main/kotlin/com/xfastgames/witness/screens/widgets/WPuzzleEditor.kt`: editor paint + hit
  testing.
- `src/test/kotlin/com/xfastgames/witness/items/data/PanelExpansionTests.kt`: what a grow is
  already required to preserve.
