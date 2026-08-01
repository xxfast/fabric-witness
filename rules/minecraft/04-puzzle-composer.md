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

Right-click a placed Puzzle Composer to open it. The screen has four working areas:

1. **Input** — a single slot that accepts only a puzzle panel.
2. **Editor** — a live drawing of the panel currently being composed.
3. **Tools** — a radio group of edit modes (one active at a time).
4. **Output** — a single slot holding the working copy. You can take it; you cannot put anything
   into it.

Putting a panel in the input clones it into the output (if the output is empty). Edits land on the
output only. Taking the output clears the input, so composing consumes the source panel: one in,
one out, no free duplication.

Optional dye in a dye slot can recolour the working copy's background when the clone is made. Dye
is meant to be consumed when the colour of the taken panel differs from the input's, but that path
is currently broken (see [Not done](#not-done)).

```
  ┌──────────────────────────────────┐
  │           [ editor grid ]        │
  │  [in]                            │
  │  ●  ⊣   tools                    │
  │  ─  ◆                            │
  │  +  −                            │
  │  [out]                           │
  │  ─── player inventory ───        │
  └──────────────────────────────────┘
```

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

## Colour

If a dye is present when the input is cloned to the output, the working copy's background becomes
that dye's colour. If no dye is present, the input's colour is kept. Colour is the only field the
clone rewrites at insert time; graph, line, size, and cost come through unchanged.

This is the same outcome as [panel dye](02-panel-dye.md), just done at the workstation so you can
tint while composing instead of making a second trip to a crafting table.

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
start / end / break / hexagon work. Dye slot and side storage inventory are referenced in code but
not laid out in the GUI. Dye consumption on take is marked `TODO: This is currently broken`.
Add / Remove tools exist as disabled radio buttons.

## Inventory layout

`PuzzleComposerBlockEntity.INVENTORY_SIZE = 10`. Slot indices are constants on
`PuzzleComposerScreen`:

| Index | Role | In GUI today |
|-------|------|--------------|
| 0 | Input (`PUZZLE_INPUT_SLOT_INDEX`) | yes |
| 1 | Background dye (`PUZZLE_BACKGROUND_DYE_SLOT_INDEX`) | no (logic only) |
| 2 | unused | — |
| 3–? | Storage (`PUZZLE_INVENTORY_SLOT_INDEX = 3`, a 2×3 `WItemSlot` is constructed) | no (never added to root) |
| 7 | Output (`PUZZLE_OUTPUT_SLOT_INDEX`) | yes |

The unused storage range collides with the output index if it is ever laid out as written (2×3 from
3 covers 3..8, which includes 7). Re-enabling storage needs a non-overlapping index map first.

## Clone, edit, take

`PuzzleComposerScreenDescription`:

1. **Insert** (`inputSlot` change listener → `updateOutputFrom`): if input non-empty and output
   empty, copy the input stack, apply dye colour (or keep panel colour), write to output via
   `updateInventory`.
2. **Edit** (`editor` click listener → tool branch → `commit`): rewrite the output stack's
   `witness:panel` component from the input stack's other components + the new `Panel`.
3. **Take** (`outputSlot` change listener): when output becomes empty, clear the input. Dye
   consumption compares input vs output background colours and is currently broken (the emptied
   stack no longer carries the panel, so the colour compare is meaningless).

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

- **Dye slot is not in the layout.** Colour-on-clone only works if something external fills slot 1
  (hopper / creative), which nothing currently does from the GUI.
- **Dye consumption on take is broken** (`TODO` in the output change listener). Taking a recoloured
  panel does not spend the dye.
- **Side storage inventory is constructed but never shown**, and its index range collides with the
  output slot.
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
