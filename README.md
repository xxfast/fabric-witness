<img src="src/main/resources/assets/witness/icon.png" align="right" width="100" height="100">

# fabric-witness

![Gradle build](https://github.com/xxfast/fabric-witness/workflows/Gradle%20build/badge.svg)
[![Release](https://img.shields.io/github/v/release/xxfast/fabric-witness.svg?include_prereleases&logo=mojang-studios)](https://github.com/xxfast/fabric-witness/releases)

<img src="https://user-images.githubusercontent.com/13775137/95962402-0e99ce00-0e52-11eb-87a4-a8959bb7aefe.png" align="center">

## What is it?

The fabric mod that adds puzzle frames from The Witness to Minecraft

This is also a companion mod for The Witness Minecraft [youtube](https://www.youtube.com/channel/UCrLikF1yl6dqz0N9OaJlAcA) series - and adds a variety of decoration blocks to the game to recreate the island

## How do I get it?

- Head over to [releases](https://github.com/xxfast/fabric-witness/releases) and download the jar in the assets
- Load it up with [fabric mod loader](https://fabricmc.net/)

## Requirements
This build targets **Minecraft 26.2** (Java 25, official Mojang names).

For the migration notes (1.17.1 → 1.21.11 → 26.2), see [`MIGRATION.md`](MIGRATION.md).

| Dependency                                                                | Version                        | Required             |
|---------------------------------------------------------------------------|--------------------------------|----------------------|
| [Fabric Loader](https://fabricmc.net/)                                    | `>=0.19.3`                     | yes                  |
| [Fabric API](https://modrinth.com/mod/fabric-api)                         | `0.156.0+26.2`                 | yes                  |
| [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin) | `1.13.13+kotlin.2.4.10`        | yes                  |
| [LibGui](https://github.com/CottonMC/LibGui)                              | `17.0.0+26.2`                  | bundled (jar-in-jar) |
| [ModMenu](https://modrinth.com/mod/modmenu)                               | `20.0.1`                       | optional             |

Versions are pinned in `buildSrc/src/main/kotlin/Dependencies.kt`.

## Releasing

1. Update the mod version in `buildSrc/src/main/kotlin/Info.kt`
2. Add a changelog entry in `CHANGELOG.md`
3. Run `./gradlew clean build` and check `fabric.mod.json` in the jar reports the new version.
   `clean` is not optional: `processResources` doesn't track `Info.kt` as an input, so a plain
   `build` after a version bump ships a jar with the old version in it.
4. Commit with a message like `Prepare for release vX.Y.Z`
5. Tag with `vX.Y.Z`. The tag always goes on the version-bump commit, so anything else that belongs
   in the release (CI changes, unrelated fixes) is reordered to land *before* it. A tag push runs
   `.github/workflows/release.yml` as it exists in that tag's tree, so a workflow fix that isn't an
   ancestor of the tag doesn't run.
6. Push the branch first, then the tag, so the release build has the history it needs

## License

CC0 1.0 Universal © Isuru Rajapakse
