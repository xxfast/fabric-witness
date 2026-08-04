# 04: Puzzle composer

**Category:** workstation (block + screen)

The block where a blank (or existing) puzzle panel becomes a puzzle. You put a panel in, shape its
grid and mark it up, and take the edited panel out. The face of the block shows the work-in-progress
live.

This file is the workstation: the block, the slots, and how a panel gets in and out. The editing
itself is split across two tabs, one file each:

- **[04-1](04-1-puzzle-composer-modifiers.md) Modifiers**, what the panel *means*: start, end,
  break, hexagon.
- **[04-2](04-2-puzzle-composer-grid.md) Grid**, what the panel *is*: which nodes exist and which of
  them are joined.

This is composing, not solving. Solving happens on a frame (see the solver screen). Crafting sizes
and colours panels ([01](01-puzzle-panel-crafting.md), [02](02-panel-dye.md)); the composer only
edits a panel you already have.

---

# Design

## The rule

Right-click a placed Puzzle Composer to open it. Two tabs down the left edge, a rail of slots and
tools, a large editor on the right, player inventory under both.

```
      ┌─ Puzzle Composer ─────────────────┐
 ┌──┐ │  [in]  │                          │
 │● │ │        │                          │
 ├──┤ │  ●  ⊣  │      [ editor grid ]     │
 │⋮⋮│ │  ─  ◆  │                          │
 └──┤ │  ·  ·  │                          │
      │        │                          │
      │  [out] │                          │
      │  ─── Inventory ─────────────────  │
      │  ████████████████████████████     │
      └───────────────────────────────────┘
```

- **Tabs** (left edge): **Modifiers** and **Grid**, hanging off the outside of the window the way
  vanilla's do. The selected one is joined to the panel, the other is closed off and darker. They
  swap the tool rail and what a click in the editor does. Everything else stays put.
- **Input** (top-left): one slot, puzzle panels only. Empty look is a faint panel watermark.
- **Tools** (middle-left): radio group, one mode at a time. Contents depend on the tab.
- **Output** (bottom-left): working copy. Take-only; you cannot put a panel into it.
- **Editor** (right, large square): live drawing of the **output** panel, on both tabs.
- **Tutorial toggle** (top-right): LibGui switch that sets `Panel.tutorial` on the working
  copy. Not a paint tool; panel-level authoring flag. Off by default; legacy panels without the
  key read as off.
- **Player inventory**: ordinary inventory strip under the workstation.

Putting a panel in the input clones it into the output (if the output is empty). Edits land on the
output only. Taking the output clears the input, so composing consumes the source panel: one in,
one out, no free duplication.

## Why two tabs

The split is what a click is talking about:

- **Grid** decides **what exists**: which nodes are on the panel and which of them are joined.
- **Modifiers** decides **what it means**: which node starts, which one ends, which segment is
  broken, where the hexagons are.

Every modifier is a statement about a node or a segment that is already there, so in practice the
two are ordered: shape the grid, then mark it up. Nothing stops you going back and forth, and
re-shaping a marked-up panel is fine; the marks on whatever you deleted go with it.

The tabs are also what lets each side stay simple. A tool that paints meaning and a gesture that
adds or removes geometry want different hit targets and different things drawn under the cursor. A
finished panel cannot show you a node that is not there, so cramming both into one rail means
faking the missing half on top of a preview that is trying to be accurate.

## Tutorial flag

A top-level boolean on the panel (`tutorial`, NBT key `tutorial`). The toggle at the top right flips
it on the working copy via `Panel.withTutorial`. It is authoring metadata only: solvers, rendering,
and crafting ignore it for now. Advanced item tooltip shows "Tutorial" when set. Absent on old saves
→ false.

It sits outside the tabs on purpose: it is a statement about the whole panel rather than about
anything on it, and neither tab is about the panel as a whole.

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

None for the edit itself, on either tab. The panel keeps whatever `witness:cost` it already had.
Tablets are only spent at craft time ([01](01-puzzle-panel-crafting.md)); recycle still returns that
cost ([03](03-panel-recycle.md)). Shape is free and size is paid for, which is what keeps this block
from competing with the crafting table
([04-2](04-2-puzzle-composer-grid.md#cost)).

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
- **No validation of a "solvable" panel.** Neither tab checks anything. The composer will happily
  produce an isolated start, zero ends, a lattice full of broken edges, or a panel cleared to
  nothing. Malformed panels are a content problem, not a UI reject.

---

# Implementation

## Status in this mod

Implemented and live as a block, block entity, LibGui screen, and block-entity renderer. Tutorial
toggle at the top right sets `Panel.tutorial`. Dye and side storage slots are gone; inventory is
input + output only.

**Both tabs exist.** `WSideTab` paints the vanilla advancements tab sprites down the outside left
edge, and a `WCardPanel` swaps the rail beneath them. Modifiers holds the four tools
([04-1](04-1-puzzle-composer-modifiers.md#status-in-this-mod)); Grid holds the pencil and the eraser
([04-2](04-2-puzzle-composer-grid.md#status-in-this-mod)).

The root panel is 32px wider than the window body so the tabs have a gutter to protrude into.
That means `setUseDefaultRootBackground(false)`, with `BackgroundPainter.VANILLA` on the body
instead: left on, LibGui paints the window across the whole root and swallows the gutter.

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
listeners run on the server handler. Any new tab's gestures land on the same path.

## Networking and persistence

- C2S payload `SynchronizePuzzleSlotPayload` registered in `PuzzleComposerBlockEntity` companion
  init (`PayloadTypeRegistry.playC2S` + global receiver).
- Block entity inventory persists through `readData` / `writeData` via `Inventories`.
- `sync()` / `toUpdatePacket` push block-entity state for the world renderer (output panel face).
- Screen opens as `ExtendedScreenHandlerFactory<BlockPos>` so the client gets the block position
  and rebuilds `ScreenHandlerContext` against the right entity.

Which tab is selected is pure client state. It never leaves the screen, so it needs no payload and
no persistence.

## World render

`PuzzleComposerBlockRenderer` reads the output slot, falls back to `Panel.DEFAULT` when the stack
has no component, and draws via `PuzzlePanelRenderer` on the top face, rotated to
`HORIZONTAL_FACING`. Lighting still has a TODO (uses light above the block).

## Not done

- **No undo, on either tab.** Every gesture commits straight to the output panel. See
  [04-2](04-2-puzzle-composer-grid.md#not-done) for why the eraser stands in for it and what real
  undo would cost.
- Per-tab gaps are listed in [04-1](04-1-puzzle-composer-modifiers.md#not-done) and
  [04-2](04-2-puzzle-composer-grid.md#not-done).
- **No compose-time validation** of solvable / well-formed panels (isolated starts, missing ends,
  etc.). Same stance as the witness rule docs: compose freely, solver / validation catches later.

## Sources

- `src/main/kotlin/com/xfastgames/witness/blocks/redstone/PuzzleComposerBlock.kt`: block, open
  screen, drop policy (output discarded on break).
- `src/main/kotlin/com/xfastgames/witness/entities/PuzzleComposerBlockEntity.kt`: inventory,
  screen factory, C2S slot sync payload.
- `src/main/kotlin/com/xfastgames/witness/screens/composer/PuzzleComposerScreen.kt`: screen,
  description, clone / commit / take flow.
- `src/main/kotlin/com/xfastgames/witness/entities/renderer/PuzzleComposerBlockRenderer.kt`: world
  face.
- `src/main/resources/data/witness/recipe/puzzle_composer.json`: block recipe.
