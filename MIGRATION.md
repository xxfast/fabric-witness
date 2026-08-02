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
- MC 26.2 is now the target (see **Migration: 1.21.11 → 26.2** below). Yarn is gone; the jar is
  unobfuscated official names. Java 25 is required.

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
- The 9 literal “tablets → new grid” `puzzle_panel_grid_*` recipes use vanilla shaped
  `components`. The 29 grid-upgrade variants now share `witness:panel_grid`, a
  component-aware special recipe that restores NbtCrafting's input-size/cost checks, preserves
  the source tint and stack components, rebuilds the target grid, and adds the consumed tablets
  to `witness:cost`.
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

- **Grid-upgrade recipes load and preserve their dynamic components.** The initial migration left
  NbtCrafting expressions such as `$ i0.cost + 1` inside vanilla `witness:cost` component JSON,
  which only accepts an integer; it also dropped the old input-grid constraints and component-copy
  behavior. The 29 upgrade variants are now handled by one special recipe covering the legacy
  layouts, while the 9 literal base-grid recipes remain vanilla shaped recipes.
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

---

# Migration: Minecraft 1.21.11 → 26.2 (August 2026)

Compile + unit tests are green on this hop. In-game verification is still outstanding.

## Toolchain

| Dependency    | 1.21.11 (Yarn/mojmap) | 26.2                                      |
|---------------|-----------------------|-------------------------------------------|
| Minecraft     | 1.21.11               | 26.2                                      |
| Mappings      | Yarn → intermediate Mojmap | **none** (unobfuscated official names) |
| Fabric API    | 0.141.4+1.21.11       | 0.156.0+26.2                              |
| Fabric Loom   | 1.17.13               | 1.17.17 (`net.fabricmc.fabric-loom`)      |
| Kotlin / FLK  | 2.4.0 / 1.13.12       | 2.4.10 / 1.13.13+kotlin.2.4.10            |
| LibGui        | 15.x                  | 17.0.0+26.2                               |
| ModMenu       | 17.x                  | 20.0.1                                    |
| Java          | 21                    | 25 (Homebrew `openjdk@25`, pinned in `gradle.properties`) |

Notes:
- Loom plugin id is `net.fabricmc.fabric-loom` (not the legacy `fabric-loom` id alone).
- Dependencies use `implementation` rather than `modImplementation` for loader/API/FLK under Loom 1.17.
- No `mappings` line in `build.gradle.kts`.

## Yarn → Mojang names (on 1.21.11 first)

Loom's `migrateMappings` does not remap Kotlin sources. A custom Python remapper over Yarn/Mojmap
tiny + intermediate class lists, plus a compile-fix loop, rewrote ~80 sources. Pitfalls:
- Simple-name collisions (`Pair`→`Tuple`, `Player`→wrong type, package `world`→`level` over-eagerly).
- Own identifiers that looked like yarn names (e.g. `Interpolator`).
- Recovery: restore `src`, remapper v2 with curated renames, then file-level rewrites.

## API churn applied for 26.2

### Fabric
- `ItemGroupEvents` (`itemgroup.v1`) → `CreativeModeTabEvents.modifyOutputEvent` (`creativetab.v1`)
- `ExtendedScreenHandlerFactory` / `ExtendedScreenHandlerType` → `ExtendedMenuProvider` / `ExtendedMenuType` (`menu.v1`)
- `PayloadTypeRegistry.playC2S()` → `serverboundPlay()`
- `ColorProviderRegistry.BLOCK` → `BlockColorRegistry` + `BlockTintSources.constant(...)`
- `BlockRenderLayerMap` **removed** from Fabric rendering API for 26.x; plant cutout/translucent
  registration deleted. Models still need a data-driven layer path (open follow-up).

### Vanilla client
- `GuiGraphics` → `GuiGraphicsExtractor` (LibGui paint signatures match)
- `Screen.render` / `renderBackground` → `extractRenderState` / `extractBackground`
- `Minecraft.setScreen` / `.screen` → `Minecraft.gui.setScreen` / `gui.screen()`
- `Options.hideGui` → `Hud.toggle()` / `isHidden()` via `Minecraft.setHudHidden`
- BER camera package: `renderer.state.level.CameraRenderState`
- Light: `LevelRenderer.getLightColor` → `LightCoordsUtil.getLightCoords`
- Special item models: `SpecialModelWrapper.Unbaked(base, Optional.empty(), unbaked)`;
  `SpecialModelRenderer.submit` drops `ItemDisplayContext`; `Unbaked` is generic

### Recipes
- `CustomRecipe` is no-arg; category defaults via `category()`
- `assemble(CraftingInput)` (no `HolderLookup.Provider`)
- Serializers are `RecipeSerializer(MapCodec.unit(INSTANCE), StreamCodec.unit(INSTANCE))`
- Dyes: `DataComponents.DYE` / `Items.DYE` ColorCollection (no `DyeItem.byColor` / `.dyeColor`)
- Recipe book slot displays take `ItemStackTemplate`, not raw `ItemStack`

## Smoke test (2026-08-02)

`./gradlew runClient --args='--quickPlaySingleplayer "New World"'`:
- Client boots on MC 26.2 / Java 25; `witness 0.11.0` loads with Fabric API 0.156.0+26.2.
- First load failed on worldgen: `minecraft:random_patch` removed. Fixed by rewriting
  `jasmine_bush` / `mimosa_bush` configured features as `simple_block` (still not biome-injected).
- Second load: **player joined `WitnessPlayground`** and stayed in-world ~90s without crash.

Non-fatal log noise (addressed or expected):
- `#missing` texture refs on `iron_puzzle_frame` / `puzzle_composer` models → remapped to `#0`.
- `witness:puzzle_panel_grid` "can't be placed due to empty ingredients" (expected: `NOT_PLACEABLE`
  special recipe; recipe book uses `display()` instead).
- `pink_cedar_leaves` loot: `minecraft:alternative` → `any_of`.

Render layers: Fabric's `BlockRenderLayerMap` is gone. 26.2 picks cutout vs translucent from
texture alpha (`ChunkSectionLayer.byTransparency`); glass uses `"force_translucent": true` on the
texture material. Plant block models do not need a code-side layer map if their PNGs have alpha.

## Known follow-ups before calling 26.2 "done"
- In-game checklist: solver focus mode, composer GUI, panel craft/dye/recycle, frame BER.
- Confirm plant foliage looks cutout (not solid black boxes) in the play-test world.
- Data component compatibility for existing panels after the version hop.
