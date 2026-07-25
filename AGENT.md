# AGENT.md

This file provides guidance to coding agents working with code in this repository.

## What this is

A Fabric mod (Kotlin, MC 1.21.11, Yarn mappings, Java 21) that adds puzzle panels from The Witness
to Minecraft, plus decoration blocks. Recently migrated from MC 1.17.1 — see `MIGRATION.md` for
what changed, known-broken items, and the in-game verification checklist before assuming a bug is new.

## Commands

Java 21 is pinned via `org.gradle.java.home` in `gradle.properties` — no JAVA_HOME setup needed.

- `./gradlew build` — full build + tests; mod jar lands at `build/libs/fabric-witness.jar`
- `./gradlew runClient` / `./gradlew runServer` — launch a dev client/server with the mod (working dir `run/`)
- `./gradlew test` — unit tests only (pure JVM: puzzle graph/panel logic, no Minecraft bootstrap)
- `./gradlew test --tests "com.xfastgames.witness.items.data.GraphTests"` — single test class

## Optional development aids

The project has an IntelliJ run configuration named **`Minecraft Client`**. When the IntelliJ IDEA
MCP is connected, useful tools include:

- `build_project` for compiler errors from IntelliJ's project model.
- `lint_files` and `get_file_problems` for IDE inspections.
- `execute_run_configuration` for launching `Minecraft Client` and capturing its build/runtime
  output. The repository root can be supplied as `projectPath` if IntelliJ has multiple projects
  open.

For tasks that need the existing play-test world, its display name is **`WitnessPlayground`** and
its save-directory identifier is **`New World`**. Minecraft's quick-play flag takes the directory
identifier:

```shell
./gradlew runClient --args='--quickPlaySingleplayer "New World"'
```

The save is at `run/saves/New World`. In-game F2 screenshots are written to `run/screenshots/` and
can be inspected directly. On macOS, F2 can optionally be sent to the active Minecraft window with
System Events (`key code 120`) when GUI automation permission is available.

Versions live in `buildSrc/src/main/kotlin/Dependencies.kt` (mod version/metadata in `Info.kt`),
**except** plugin versions, which are inlined in `build.gradle.kts`'s `plugins {}` block — Gradle 9
can't read buildSrc constants there. Keep the two Kotlin versions in sync (plugin block vs.
`Jetbrains.Kotlin.version`, which must match what fabric-language-kotlin bundles).

## Architecture

**Entry points** (declared in `src/main/resources/fabric.mod.json`):
- `Witness` (main) — common init. `WitnessClient` (client) — renderers, screens, client networking.
  Never reference client-only classes (`@Environment(EnvType.CLIENT)`) from common code paths;
  dedicated servers crash on classload.

**Registration pattern — the #1 gotcha in this codebase.** Game objects are registered as side
effects of `val`s in companion/`object` initializers (e.g. `IronPuzzleFrameBlock.Companion.BLOCK`
calls `registerBlock(...)`), using helpers from `utils/Registry.kt`. Registries freeze after
`onInitialize` returns, so `Witness.onInitialize()` force-classloads everything with registration
side effects (`PanelComponents.init()`, `WitnessSounds.init()`, `BLOCKS.size`, ...). If you add a
registered object, make sure something in `onInitialize` reaches it — a lazy first-use classload
in-game throws `Registry is already frozen`. Sounds specifically must go in `sounds/WitnessSounds.kt`.

**Puzzle data model.** A puzzle is a `Panel` (`items/data/Panel.kt`) holding a Guava `ValueGraph`
of `Node`s (`items/data/Graph.kt`, `Node.kt`) — the grid, the drawn line, and modifiers
(start/end/hexagon/break). It's attached to ItemStacks via the `witness:panel` data component
(`items/data/Components.kt`; use the `ItemStack.panel` extension, never raw NBT). `Panel.CODEC`
wraps the legacy NBT serialization (`toPanel()`/`toNbt()`), and recipe JSONs embed the same shape
under `components` — the NBT readers tolerate dynamic-ops numeric types for this reason.

**Puzzle rules.** `rules/` catalogs the Witness line-puzzle rules this mod replicates, one file per
rule, indexed by `rules/README.md`. Check it before touching puzzle logic.

**Puzzle flow across the mod:**
- `PuzzlePanelItem` stacks carry the component → placed in a `PuzzleFrameBlockEntity`
  (`IronPuzzleFrameBlock`) → rendered live by `PuzzleFrameBlockRenderer`.
- Solving: clicking a frame opens `screens/solver/PuzzleSolverScreen` (a full-screen overlay, not a
  ScreenHandler screen) which raycasts back at the physical panel and traces the line on the graph
  via `PuzzleSolver` (tracing only, no solution validation yet).
- Composing: `PuzzleComposerBlock` opens a LibGui `SyncedGuiDescription`
  (`screens/composer/`, `WPuzzleEditor` widget) with slot changes synced C2S via
  `SynchronizePuzzleSlotPayload` (typed `CustomPayload`).
- Crafting: two code recipes in `recipes/` (`witness:panel_dye`, `witness:panel_recycle` —
  `SpecialCraftingRecipe`s that carry the component through), plus vanilla JSON grid recipes in
  `data/witness/recipe/` with inline `components`.

**Rendering.** Block entity renderers use the 1.21 render-state/command-queue system: extract state
in `updateRenderState`, submit geometry via `OrderedRenderCommandQueue.submitCustom`. Shared vertex
helpers in `utils/VertexConsumer.kt` / `RenderContext.kt` (JOML math). GUI drawing helpers in
`utils/DrawableHelper.kt` are `DrawContext`-based.

**Assets/data specifics that silently fail:** every item needs a model definition JSON under
`assets/witness/items/` (missing → purple-black placeholder); datapack folders use 1.21 singular
names (`recipe/`, `loot_table/`, `tags/block/`). Worldgen JSONs under `data/witness/worldgen/` are
intentionally inert — registered but not injected into any biome (matches pre-migration behavior).

**Mixins:** `witness.mixins.json`; only `MouseAccessorMixin` (cursor lock for the solver screen).
Prefer Fabric API events over new mixins.

Release process: see README.md.
