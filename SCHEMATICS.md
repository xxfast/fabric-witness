# Island reference schematic

A reference copy of The Witness's island, extracted from the game's own data, used to see where
areas sit relative to each other while building. It is two things generated from the same voxels:

- **`run/schematics/witness_island.litematic`**, a Litematica schematic to overlay on any world.
- **`run/saves/Witness Island`**, a void world with the island written into it, to walk around.

Both are **reference only**. Never paste the schematic into a world.

Neither is in git, and neither should be: see [Version control](#version-control). The tools that
make them are in `tools/witness-island/`, which exists only on this machine. The steps to regenerate
them are below.

## What is in it

One block per metre. Everything is a hollow shell: ground is a one-block surface, and buildings are
their walls.

| Layer | Schematic region | In the world | Blocks |
|---|---|---|---|
| Terrain | `terrain` | yes | grass; sand for beaches and desert; stone for rock and cliffs; gravel below sea level |
| Vegetation | `vegetation` | **no** (removed on request) | oak leaves, oak logs for trunks |
| Buildings | `buildings` | yes | a block per area and material, see [Building colours](#building-colours) |
| Puzzles | `puzzles` | yes | one yellow concrete block per panel |
| Perimeter | `perimeter` | **no** (removed on request) | red concrete ring on the ground along the coast and around lakes |
| Water | none | yes | still water from the sea and lake beds up to sea level |

The schematic keeps vegetation and the perimeter as regions you can toggle off; the world leaves them out.
The world has water; the schematic does not.

## Coordinates

The Witness is Z-up. Minecraft coordinates in the Witness Island world map directly:

```
world x = witness x      world y = witness z + 64      world z = -witness y
```

Sea level is Witness z ≈ −0.5, so the sea surface is world y 63. The mapping is a rotation, not a
mirror; area positions check out against the island map (Desert Ruins NW, Symmetry Island W, entry SW,
Quarry and Keep N, Shipwreck NE, Marsh E).

Useful spots in the Witness Island world: the Orchard is around `/tp -115 75 -16`, Desert Ruins
`/tp -176 80 -166`, the Keep `/tp 48 90 -138`, the Mountain `/tp 155 140 53`.

**Schematic origin:** the ground block at the centre of the starting courtyard (the Entry Yard's
walls), Witness (−141.6, −149.4, z 5). To line the schematic up with the Witness Island world, place its
origin at **(−142, 69, 149)**. In any other world, put the origin on the block where the courtyard
centre should go. The regions sit at negative offsets from that origin.

## Using it in Litematica

Litematica 0.28.8 and MaLiLib 0.29.6 are dev-run dependencies (`localRuntime` in `build.gradle.kts`,
versions in `buildSrc/.../Dependencies.kt`). They are not bundled into the mod jar, and Litematica
is client-only, so a server never needs it.

- `M` opens the menu. The tool item is a stick; Ctrl + scroll cycles its modes.
- Load: `M` → Load Schematics → `witness_island.litematic`. Set the origin in Schematic Placements → Configure, then **Lock** it.
- Layers: toggle sub-regions in the placement's Configure screen. Render layers (Page Up/Down)
  slice by height.
- `M` + `R` toggles all Litematica rendering.
- The whole island loads fine at once (~730 KB); Litematica only renders chunks in view.

**Keeping it reference-only:** blocks are only placed by the paste operation or Easy Place.
Unbind the execute-operation hotkey, leave Easy Place off and unbind its toggle, and consider
moving the tool item off the stick. On a server without op, pasting fails anyway.

**The overlay colours** show where the schematic and the world disagree. Pink means extra
(the world has a block, the schematic has air), light blue missing, red wrong block, orange wrong
state. In the Witness Island world, expect pink over water, light blue for trees and red along the
coast. If terrain and buildings light up too, the placement is misaligned. Turn a mismatch type off
in Configuration → Visuals, or set it to ignore existing fluids.

## How it is made

```
data-pc.zip ─┬─ save.entities ── Unpack.java ── run_parse.py ──► nodes.pkl (every entity)
             └─ *.pkg / *.mesh ───────────────── export.py ────► meshes.bin, instances.bin, puzzles.bin
                                  nodes.pkl ──── mask.py ──────► drop.bin, drop_panels.bin (ending sets)
                                  nodes.pkl ──── groups.py ────► groups.txt (editor group per instance)
             everything above ── Voxelize.java ─┬► witness_island.litematic
                                                 └► WorldWriter.java ─► a new world folder
```

- **The game archive**, `C:\Program Files (x86)\Steam\steamapps\common\The Witness\data-pc.zip`,
  is a plain zip. `.pkg` files inside it are zips too. `save.entities` holds every placed object in
  the world and is LZ4-block compressed behind a 12-byte header (u32, u32, raw size).
- **Parsing** uses [TheWitnessExplorer](https://github.com/ClementSparrow/TheWitnessExplorer)
  (Python), cloned into the work directory as `twe/`. It parses all 49,485 entities exactly (no bytes
  left over) and reads meshes. It has **no license**, so it is not vendored here. Clone it yourself.
- **Python runs under WSL** (`wsl -e python3`, 3.12, no numpy needed). Windows has only the Store stub.
  `meshlib.py` reads the archive from its `/mnt/c/...` path.
- **Entity basics:** position is a Z-up vector, rotation a quaternion `(x, y, z, w)`, then a uniform
  scale. Terrain meshes are already in world space, with the entity at the origin. Each entity names
  its editor Group (`node_group_id`), and group names (`Keep`, `Entry_Yard_Walls`, `exit_hall` …)
  are the most reliable way to tell areas apart.
- **Classification** (`export.py`, by mesh name) sorts instances into terrain, vegetation or
  buildings, and skips collision, occluder (`ocl`) and audio helper meshes. Doors come from
  `Door` entities.
- **Voxelizing** (`Voxelize.java`) samples every triangle at 0.5 m into a 1 m grid. It keeps each
  voxel as a short with bits for each layer, then derives the perimeter and water.
- **Writing:** the `.litematic` is written by hand (NBT, schematic Version 7, Minecraft data version
  4903 for 26.2). The world is written by hand as 26.2 region files. It borrows `level.dat`, data
  packs and world-gen settings from `run/saves/New World`, read-only. World-gen lives in
  `data/minecraft/world_gen_settings.dat` in 26.2, not in `level.dat`. Chunks are written with
  `isLightOn = 0` so the game lights them.

### Regenerating

Work in a folder **outside the repo** (the output is game data). With `twe/` cloned there, the
scripts from `tools/witness-island/` copied in, and the game closed:

```shell
java Unpack.java save.entities entities.bin
wsl -e python3 run_parse.py          # nodes.pkl
wsl -e python3 export.py             # meshes.bin, instances.bin, puzzles.bin (about 2.5 minutes)
wsl -e python3 mask.py               # drop masks; checks alignment with instances.bin
wsl -e python3 groups.py             # groups.txt
javac -d build *.java
java -Xmx6g -cp build Voxelize . witness_island.litematic 1.0 topdown.png \
    "<repo>/run/saves/New World" "./Witness Island" --no-vegetation --no-perimeter
```

The last command writes the schematic, a top-down PNG for checking by eye, and a world folder.
`WorldWriter` refuses to write into a folder that exists. To replace `run/saves/Witness Island`,
close the game, delete that folder, and move the new one in. **Never touch the other saves.**

`Floating.java` is a diagnostic. It lists non-terrain meshes hanging well above the ground, which is
how the ending sets were found.

## Decisions made so far

- **The ending sequences are removed.** The credits' floating viewing rooms (hotel, airport, cave,
  TheRoom) and the secret speedrun ending are gone. Removal goes by editor group (`exit_hall*`,
  `speedrun`, `cavecap*`) plus names (`loc_end2*`, `loc_secret_speedrun*`, `end2_*`, `template_*`).
  Groups alone missed a large cave-exit slab near the start. Removing everything within the ending
  sets' bounding boxes also deleted real island content (Orchard walls, panels).
- **Editor test planes** (`vs_laketest*`, `vs_octopus`, `*refplane`) are dropped, and `ter_*` ground
  that the name rules put in buildings (the treehouse ground) is moved to terrain.
- **Trees are a stopgap.** `*branch*` meshes count as leaves. Whole-tree meshes (`multiTrunk`,
  `wisteria_trunk`) split geometrically: a central column below 40% of the height is log, and the
  rest is leaves.
- **Buildings are hand-coloured**, not sampled from textures. "No need to be too fancy."
- **The perimeter** replaces the top ground block of each land column that borders the sea or a
  lake. At sea level it was buried about 6 blocks deep and invisible. Lake rings were kept, and later
  the ring was dropped from the world entirely.
- The Mountain uses plain deepslate, not deepslate bricks.

### Building colours

`BuildingPalette.java`. Material words in the mesh name win: glass/window → glass, rope → brown
wool, rust → brown terracotta, brick → bricks, roof → terracotta, metal/rebar/pipe/rail/catwalk →
iron block, rubble/debris/rock → cobblestone, cement/concrete → light grey concrete, and wood words →
oak planks (or the area's wood). Otherwise the area decides: Keep and Entry Yard stone bricks, Desert
Ruins smooth sandstone, Bunker white concrete, Treehouse and forests spruce, Shipwreck dark oak,
Mountain deepslate, Challenge polished blackstone, Marsh brown terracotta, Monastery calcite, Town
white terracotta, Orchard mossy stone bricks, Quarry cobblestone, Symmetry Island smooth quartz,
farm and windmill oak, vaults polished deepslate. Anything else is light grey concrete.

## Open work

- **Split trees by material.** The exact fix for trunks and leaves is to keep each mesh's material
  parts separate and classify bark against leaf textures. `export.py` currently joins the parts.
  This needs a re-export.
- **Sample building colours from textures**, if hand colours stop being enough. It needs material
  names from the meshes and a DXT texture decoder (TheWitnessExplorer has
  `parse_TheWitness_texture.py`).
- **Running TheWitnessExplorer from Claude Code** was refused by the auto-mode permission check
  partway through ("code from external"). Allow it with a Bash permission rule, or port the entity and
  mesh readers to Java so no outside code runs.
- **Per-area schematics** (Orchard, Keep, Town …) sharing the one origin, so only the area being
  worked on is loaded. Offered, not built.
- **The schematic origin inside the island** (negative region offsets) had not been confirmed in game
  when this was written.
- `export.py`'s skip pattern has `lodd` where `lod\d` was meant. It is kept as-is so the output
  reproduces exactly.

## Version control

- **The schematic and the world stay out of git.** Both are derived from The Witness's level data,
  which is Thekla's copyrighted content, and this repository is public. They regenerate in seconds
  from anyone's own copy of the game. `run/` is already gitignored, and
  `tools/witness-island/.gitignore` keeps intermediate files out as well.
- **The tools stay on this machine too.** `tools/witness-island/` is excluded locally in
  `.git/info/exclude`, not in `.gitignore`, so it never shows up as untracked. They contain no game
  data, but they serve only this reference, so there is no reason to publish them.
