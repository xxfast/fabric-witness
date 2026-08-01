# 04 — Puzzle composer

**Category:** workstation (block + screen)

The block where a blank (or existing) puzzle panel becomes a puzzle. You put a panel in, paint
start/end/hexagon/break marks onto its grid, and take the edited panel out. The face of the block
shows the work-in-progress live.

This is composing, not solving. Solving happens on a frame (see the solver screen). Crafting sizes
and colours panels ([01](01-puzzle-panel-crafting.md), [02](02-panel-dye.md)); the composer only
edits the marks on a panel you already have.

---

# Design

## The rule

Right-click a placed Puzzle Composer to open it. A left rail of slots + tools, a large editor on the
right, player inventory under both.

```
  ┌─ Puzzle Composer ─────────────────┐
  │  [in]  │                          │
  │        │                          │
  │  ●  ⊣  │      [ editor grid ]     │
  │  ─  ◆  │                          │
  │  +  −  │                          │
  │        │                          │
  │  [out] │                          │
  │  ─── Inventory ─────────────────  │
  │  ████████████████████████████     │
  └───────────────────────────────────┘
```

- **Input** (top-left) — one slot, puzzle panels only. Empty look is a faint panel watermark.
- **Tools** (middle-left, 2×3) — radio group, one mode at a time. Top four live; bottom two
  (add / remove) render darker and do nothing.
- **Output** (bottom-left) — working copy. Take-only; you cannot put a panel into it.
- **Editor** (right, large square) — live drawing of the **output** panel. Clicks apply the
  selected tool.
- **Tutorial toggle** (top-right) — LibGui switch that sets `Panel.tutorial` on the working
  copy. Not a paint tool; panel-level authoring flag. Off by default; legacy panels without the
  key read as off.
- **Player inventory** — ordinary inventory strip under the workstation.

Putting a panel in the input clones it into the output (if the output is empty). Edits land on the
output only. Taking the output clears the input, so composing consumes the source panel: one in,
one out, no free duplication. Colour is not changed here; recolour is the crafting recipe
([02](02-panel-dye.md)).

## Tools

Exactly one tool is selected. Clicking a node or segment in the editor applies that tool:

| Tool    | Target        | Effect |
|---------|---------------|--------|
| Start   | node          | Toggle start disc on / off (`START` ↔ bare). Segments ignore this tool. |
| End     | border node   | Hang / cycle / remove an end-point nub. Interior nodes refuse. See [../witness/02-end-points.md](../witness/02-end-points.md). |
| Break   | segment       | Toggle the segment between normal and broken (a gap the line cannot cross). See [../witness/03-broken-edges.md](../witness/03-broken-edges.md). |
| Hexagon | node or segment | Toggle a hexagon dot on that node or edge. See [../witness/04-hexagon-dots.md](../witness/04-hexagon-dots.md). |
| Add     | —             | Disabled. Intended to grow the lattice; not wired. |
| Remove  | —             | Disabled. Intended to shrink the lattice; not wired. |

Each click commits immediately to the output panel. There is no undo beyond taking the panel out and
putting a fresh one in.

Start, end, break, and hexagon are pure marks: they do not spend tablets, change size, or change
`cost`. Growing a panel still goes through [crafting](01-puzzle-panel-crafting.md).

## Tutorial flag

A top-level boolean on the panel (`tutorial`, NBT key `tutorial`). Composer toggle at the top
right flips it on the working copy via `Panel.withTutorial`. It is authoring metadata only:
solvers, rendering, and crafting ignore it for now. Advanced item tooltip shows "Tutorial" when
set. Absent on old saves → false.

## Colour

The clone keeps the input panel's `backgroundColor` as-is. To recolour, use
[panel dye](02-panel-dye.md) at a crafting table (panel + any dye). There is no dye slot on the
composer; an earlier in-screen tint path was removed once that recipe landed.

## The block face

The composer renders the **output** panel on its top face in the world, facing the block's
horizontal direction. Empty output → no panel drawn. That makes the WIP visible without opening the
screen, and is what lets a play-area display a half-finished panel on a table rather than only in
a GUI.

## Cost

None for the edit itself. The panel keeps whatever `witness:cost` it already had. Tablets are only
spent at craft time ([01](01-puzzle-panel-crafting.md)); recycle still returns that cost
([03](03-panel-recycle.md)).

Crafting the block itself: 1 Ancient Puzzle Tablet + 8 iron ingots (shaped, tablet on top centre).

## Edge cases

- **Input only accepts puzzle panels.** Anything else is filtered out of the slot.
- **Empty input clears the output.** Pulling the input out (or taking the output, which also clears
  input) leaves the editor blank.
- **Output is take-only.** You cannot drop a panel into the output to "load" it; load goes through
  the input.
- **Taking output consumes input.** One panel in becomes one panel out. The composer is not a
  copier.
- **Legacy / creative panels without a panel component** still open in the editor as a default blank
  panel, matching how the world renderer treats them.
- **Re-inserting while output is still full does nothing.** The clone only runs when the output is
  empty, so a half-edited panel is not overwritten by jamming another panel into the input.
- **Breaking the block** drops every slot except the output. The working copy is discarded on
  break, so an unclaimed edit is lost (by design: it was a clone of the input, which was either
  still in the machine or already taken).
- **Add / Remove tools do nothing.** They render disabled. Size changes are crafting's job until
  those tools exist.
- **No validation of a "solvable" panel.** The composer will happily compose an isolated start, zero
  ends, or a lattice full of broken edges. Malformed panels are a content problem, not a UI reject.

---

# Implementation

## Status in this mod

Implemented and live as a block, block entity, LibGui screen, and block-entity renderer. Tools
start / end / break / hexagon work. Add / Remove tools exist as disabled radio buttons. Tutorial
toggle at the top right sets `Panel.tutorial`. Dye and side storage slots are gone; inventory is
input + output only.

## Inventory layout

`PuzzleComposerBlockEntity.INVENTORY_SIZE = 2`. Slot indices are constants on
`PuzzleComposerScreen`:

| Index | Role |
|-------|------|
| 0 | Input (`PUZZLE_INPUT_SLOT_INDEX`) |
| 1 | Output (`PUZZLE_OUTPUT_SLOT_INDEX`) |

Older worlds that still have a 10-slot composer NBT will drop anything that was in the removed dye /
storage indices on load; only slots 0 and 1 are read into the new size.

## Clone, edit, take

`PuzzleComposerScreenDescription`:

1. **Insert** (`inputSlot` change listener → `updateOutputFrom`): if input non-empty and output
   empty, copy the input stack (defaulting a missing panel component to `Panel.DEFAULT`) into the
   output via `updateInventory`.
2. **Edit** (`editor` click listener → tool branch → `commit`): rewrite the output stack's
   `witness:panel` component from the input stack's other components + the new `Panel`.
3. **Take** (`outputSlot` change listener): when output becomes empty, clear the input.

`updateInventory` is the side-aware write path:

- **Client** (editor clicks): `PuzzleComposerBlockEntity.syncInventorySlotTag` → C2S
  `SynchronizePuzzleSlotPayload` → server sets the slot.
- **Server** (slot change listeners on the authoritative handler): writes the inventory directly;
  vanilla slot sync pushes the result to the client.

That split exists because `WPuzzleEditor` clicks only fire client-side, while LibGui item-slot
listeners run on the server handler.

## Tool → panel transforms

| Tool | Code path |
|------|-----------|
| End | `Panel.withEndPointToggled(node)` (`items/data/EndPoints.kt`) |
| Hexagon | `Panel.withSymbolToggled(node, edgeNodePair)` (`items/data/Symbols.kt`) |
| Start | cycle node modifier `START` ↔ `NORMAL` via `nextIn`, then rebuild the graph node in place |
| Break | cycle edge modifier `BREAK` ↔ `NORMAL` via `nextIn`, then re-put the edge |

Start and break still do the graph rewrite by hand (copy graph, remove/re-add node or edge). End and
hexagon already live on `Panel` helpers with unit tests. The `when (Panel.Grid / Tree / Freeform)`
copy in `commit`'s call sites is the same sealed-type tax the dye recipe pays.

## Networking and persistence

- C2S payload `SynchronizePuzzleSlotPayload` registered in `PuzzleComposerBlockEntity` companion
  init (`PayloadTypeRegistry.playC2S` + global receiver).
- Block entity inventory persists through `readData` / `writeData` via `Inventories`.
- `sync()` / `toUpdatePacket` push block-entity state for the world renderer (output panel face).
- Screen opens as `ExtendedScreenHandlerFactory<BlockPos>` so the client gets the block position
  and rebuilds `ScreenHandlerContext` against the right entity.

## World render

`PuzzleComposerBlockRenderer` reads the output slot, falls back to `Panel.DEFAULT` when the stack
has no component, and draws via `PuzzlePanelRenderer` on the top face, rotated to
`HORIZONTAL_FACING`. Lighting still has a TODO (uses light above the block).

## Not done

- **Add / Remove tools disabled.** Lattice grow/shrink at the composer is intentionally off; size
  changes go through [01](01-puzzle-panel-crafting.md).
- **No compose-time validation** of solvable / well-formed panels (isolated starts, missing ends,
  etc.). Same stance as the witness rule docs: compose freely, solver / validation catches later.
- **No solution-line editing.** The editor draws an existing line if the panel carries one, but no
  tool clears or draws it. Growing a panel already drops the line in craft; the composer does not
  mirror that on edit.

## Sources

- `src/main/kotlin/com/xfastgames/witness/blocks/redstone/PuzzleComposerBlock.kt` — block, open
  screen, drop policy (output discarded on break).
- `src/main/kotlin/com/xfastgames/witness/entities/PuzzleComposerBlockEntity.kt` — inventory,
  screen factory, C2S slot sync payload.
- `src/main/kotlin/com/xfastgames/witness/screens/composer/PuzzleComposerScreen.kt` — screen,
  description, tools, clone / commit / take flow.
- `src/main/kotlin/com/xfastgames/witness/screens/widgets/WPuzzleEditor.kt` — editor paint + hit
  testing.
- `src/main/kotlin/com/xfastgames/witness/entities/renderer/PuzzleComposerBlockRenderer.kt` —
  world face.
- `src/main/kotlin/com/xfastgames/witness/items/data/EndPoints.kt`, `Symbols.kt` — end / hexagon
  panel transforms.
- `src/main/resources/data/witness/recipe/puzzle_composer.json` — block recipe.
- Witness rules this screen composes: [../witness/01-start-points.md](../witness/01-start-points.md),
  [../witness/02-end-points.md](../witness/02-end-points.md),
  [../witness/03-broken-edges.md](../witness/03-broken-edges.md),
  [../witness/04-hexagon-dots.md](../witness/04-hexagon-dots.md).
