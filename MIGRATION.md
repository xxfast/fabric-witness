# Migration: Minecraft 1.17.1 → 1.21.11 (July 2026)

This documents the full modernization of the mod from MC 1.17.1 (2021 toolchain) to MC 1.21.11,
what changed by area, the judgment calls made, and what is known to be broken or unverified.

## Toolchain

| Dependency    | Before          | After                                                         |
|---------------|-----------------|---------------------------------------------------------------|
| Minecraft     | 1.17.1          | 1.21.11                                                       |
| Yarn mappings | 1.17.1+build.37 | 1.21.11+build.6                                               |
| Fabric Loader | 0.11.6          | 0.19.3                                                        |
| Fabric API    | 0.37.2+1.17     | 0.141.4+1.21.11                                               |
| Fabric Loom   | 0.8-SNAPSHOT    | 1.17.13                                                       |
| Kotlin / FLK  | 1.5.21 / 1.6.3  | 2.4.0 / 1.13.12                                               |
| LibGui        | 4.1.6           | 15.1.0                                                        |
| ModMenu       | 2.0.4           | 17.0.0                                                        |
| nbtcrafting   | 2.0.20          | **removed** (project archived 2024, no successor)             |
| Gradle        | 7.0.2           | 9.6.1                                                         |
| Java          | 16              | 21 (pinned via `org.gradle.java.home` in `gradle.properties`) |

Notes:
- Plugin versions are inlined in `build.gradle.kts`'s `plugins {}` block (Gradle 9 cannot read
  buildSrc constants there); everything else still lives in `buildSrc/src/main/kotlin/Dependencies.kt`.
- The CottonMC maven (`server.bbkr.space`) is dead; LibGui now resolves from
  `https://staging.alexiil.uk/maven/`.
- MC 26.2 (the actual latest) was deliberately deferred: it requires Java 25, Mojang mappings
  (Yarn is discontinued from 26.1), and ModMenu only has a beta there. The 1.21.11 → 26.2 hop is
  a much smaller follow-up.

## What was migrated

### Item data: NBT → data components
- Puzzle state used to live in raw ItemStack NBT (`stack.tag["panel"]`). MC 1.20.5 removed item
  NBT in favor of data components.
- New `items/data/Components.kt` registers `witness:panel` (`ComponentType<Panel>`) and
  `witness:cost` (`ComponentType<Int>`), with `ItemStack.panel` / `ItemStack.cost` extensions.
- `Panel.CODEC` wraps the original NBT serialization (`toPanel()` / `toNbt()`), so the stored
  shape matches the old `panel` tag. NBT readers were made tolerant of dynamic-ops numeric types
  (`utils/Nbt.kt`) so the same codec parses components written in recipe JSON.

### Recipes (nbtcrafting removal)
- 16 `puzzle_panel_color_*` JSONs → one `SpecialCraftingRecipe` (`witness:panel_dye`):
  panel + any dye → same panel with the new background color, all component data preserved.
- `puzzle_panel_grid_recycle(_compat)` → `witness:panel_recycle` special recipe: lone panel →
  puzzle tablets × `witness:cost` (default 4).
- ~30 shaped `puzzle_panel_grid_*` recipes: nbtcrafting `data` blobs → vanilla `components`.
- Vanilla JSON modernization: `{"item": X}` → `"X"` ingredients, `result.item` → `result.id`,
  stonecutting result object form. `test_recipe.json` (nbtcrafting bug repro) deleted.
- Datapack folder renames (MC 1.21 silently ignores the old names): `recipes/` → `recipe/`,
  `loot_tables/` → `loot_table/`, `tags/blocks/` → `tags/block/`; loot `match_tool` predicates
  `item` → `items`.

### Registration
- `utils/Registry.kt` rewritten on `net.minecraft.registry.Registries` / `RegistryKeys`.
  Block/item settings factories bake in the now-mandatory per-id `registryKey`; all 21 blocks
  take `Settings` constructor params; `BlockWithEntity`/`LeavesBlock`/`PlantBlock` subclasses
  implement the newly-abstract `getCodec()`.
- Creative tabs: `Item.Settings().group(...)` → `ItemGroupEvents.modifyEntriesEvent`.
  Remapping of removed vanilla tabs: DECORATIONS → NATURAL, MATERIALS → INGREDIENTS.
- Block entities: `FabricBlockEntityTypeBuilder`; NBT I/O via `ReadView`/`WriteView`
  (`readData`/`writeData`); client sync via `toUpdatePacket`/`toInitialChunkDataNbt` behind a
  small `Syncable` interface (replaces Fabric's removed `BlockEntityClientSerializable`).
- Sounds: all `SoundEvent`s now register during common init in `sounds/WitnessSounds.kt`
  (registries freeze after mod init since 1.19.3 — see Fixed issues below).
- Screen handler: `ExtendedScreenHandlerType<_, BlockPos>` registered during common init;
  client screen bound via `HandledScreens.register` in `WitnessClient`.

### Networking
- The raw `PacketByteBuf` C2S channel → typed `SynchronizePuzzleSlotPayload`
  (`CustomPayload` + `PacketCodec.tuple`) with `PayloadTypeRegistry.playC2S()` /
  `ServerPlayNetworking.registerGlobalReceiver`.

### Rendering
- Block entity renderers rewritten for the 1.21 render-state/command-queue system:
  `PuzzleFrameBlockRenderer` / `PuzzleComposerBlockRenderer` extract a `RenderState`
  (panel, facing, light) and submit geometry via `OrderedRenderCommandQueue.submitCustom`.
- Math moved to JOML (`org.joml.*`), `Vec3f`/`Matrix4f` (Minecraft's) are gone; helpers take
  `MatrixStack.Entry`; no more `VertexConsumer.next()`.
- GUI drawing (`fill`/`circle`/`hexagon` in `utils/DrawableHelper.kt`) reimplemented on
  `DrawContext` (the Tessellator immediate-mode GUI path no longer exists).
- Item model definitions added under `assets/witness/items/` for all 23 items (mandatory since
  1.21.4; without them items render as the missing-texture placeholder).
- Deleted as provably dead: `ExampleMixin`, `HeldItemRendererRegistry(+Impl)` and its
  `MixinHeldItemRenderer` (the registry was never populated). `MouseAccessorMixin` re-verified
  against 1.21.11 and kept.

### LibGui 4 → 15
- `paint(DrawContext, ...)` signatures, `onClick(Click, boolean)`, icon API, `WItemSlot`
  filter/modifiable builder setters, `PositionedSoundInstance.ui` (the `master` category is gone),
  `ExtendedScreenHandlerFactory<BlockPos>`.
- `WPuzzleEditor.paint` rebuilt as a flat 2D `DrawContext` painter — rendering the 3D panel into
  the GUI via `entityVertexConsumers` + Tessellator is impossible in the 1.21.6+ GUI pipeline.

### World gen
- The old `Feature` classes (`JasmineBushFeature`, `MimosaBushFeature`, `BiomeFeature`) were
  **never wired up in 1.17** (empty biome list, `register() = TODO()`) — nothing ever generated.
  They were replaced with equivalent data-driven `worldgen/configured_feature` +
  `placed_feature` JSONs (vanilla `random_patch`, rarity 10/5) that are intentionally **inert**
  (not injected into any biome) to preserve behavior. Enabling generation is a one-line
  `BiomeModifications.addFeature(...)` call plus a biome-selector decision.

### Entry points
- `Witness` split into `Witness` (main) + `WitnessClient` (client) — required so dedicated
  servers never classload client-only classes (renderers, screens).

## Known broken / not ported

2. **Old worlds don't keep crafted panel data.** Pre-1.20.5 stack NBT is migrated by vanilla
   into `minecraft:custom_data`, not the `witness:panel` component — panels crafted before the
   migration lose their puzzle when loaded in the new version. Blocks/world state are fine.
5. **`OakLeavesRunners` item tint dropped** (`ColorProviderRegistry.ITEM` no longer exists;
   needs a tint source in the item model JSON). `PinkCedarLeaves` has no leaf-fall particles.
6. Minor behavior swaps: solver raycast uses `player.blockInteractionRange` (the
   `reachDistance`/`hasExtendedReach` APIs are gone); solver screen cleanup moved
   `onClose` → `removed()`.

## Fixed issues found in play-testing

- **Dye/recycle recipes are visible in the recipe book.** Their component-aware crafting remains
  implemented as special recipes, but both now provide 1.21 recipe displays and placement
  ingredients and unlock when the player obtains a puzzle panel. The recycle display shows the
  default four-tablet return; crafting still uses the panel's stored `witness:cost`.
- **Custom puzzle-panel item rendering restored.** A `SpecialItemModel` installed through Fabric's
  item-model bake hook now delegates to `PuzzlePanelSpecialModelRenderer`, which snapshots and
  renders each stack's live `witness:panel` component in GUI, first-/third-person, and ground
  contexts. The custom pre-migration arm renderer remains gone; the vanilla item-holding pose is
  used.
- **Composer editor preview made faithful.** Kept the click-aligned 2D painter, but it now shares
  the world renderer's dyed backdrop and graph/solution textures and mirrors its node, endpoint,
  start, break, hidden, and solution styling. A real 3D GUI render is deferred unless playtesting
  shows that the faithful flat view is confusing.
- **Frozen-registry crash on panel click** (`Registry is already frozen ... witness:pointless_click`):
  sound events were registered lazily from `object`s nested in `PuzzleSolverScreen` /
  `IronPuzzleFrameBlock`, which only classload on first use — legal in 1.17, a crash since
  registries freeze after init (1.19.3+). All sounds now register eagerly in
  `sounds/WitnessSounds.kt` from `Witness.onInitialize()`.

## Still needs in-game verification

- Panel rendering in frames and the composer (new command-queue path: geometry, lighting,
  z-fighting).
- Composer screen end-to-end: slot-sync payload, dye tinting, editor clicks vs. the new 2D preview.
- Solver screen: raycasting (JOML rewrite), line tracing, sounds, mouse hide/unlock.
- All four recipe behaviors: grid crafting (component JSON decoding), panel dye, panel recycle,
  stonecutting.
- Worldgen JSONs validate on datapack load (inert until injected into biomes).
- Dedicated-server boot (client/server split, screen-handler registration timing).
