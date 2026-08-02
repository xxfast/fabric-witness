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
3. Commit with a message like `Prepare for relase vX.Y.Z`
4. Tag with `vX.Y.Z`
5. Push branch, and the tags

## License

CC0 1.0 Universal © Isuru Rajapakse
