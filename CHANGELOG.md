# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- Puzzle panels can now be up to 10x10 cells, up from 8x8, so grids from the game fit
- Cables now carry power for 128 blocks from their nearest source, up from 64, since the game's
  cables often run far

## [0.13.0] - 2026-08-30

### Added

- Added colored squares, the first region symbol
  - Paint a cell black or white with the composer's square tool; every region the line carves out
    must keep its squares one colour or the solution is rejected
  - Tutorial panels flash the clashing squares

- Added a grid editor to the composer

- Puzzle frames are now redstone components
  - A frame needs redstone power to turn on, and once solved passes power out of the side its used
    end nub points to, so tutorial rows can be chained
  - Levers attach to the frame's back; a stand relays ground redstone up into its frame; a solved
    frame emits weak power for a repeater to pick up
  - Solutions are now judged on the server. Solved is sticky and resets when power is lost

- Added cables
  - A flat ribbon that carries a solved frame's power for 64 blocks without decay, lit in the
    powering panel's colour
  - Bends around floor corners, climbs walls, and lies over ledges; feeds a frame on any side without
    an end nub, and a stand from underneath
  - The recipe unlocks on picking up copper

- Blocks now have mineable tags and vanilla-style tool requirements
  - Stone and iron blocks need a pickaxe to drop (iron blocks need stone tier or better); cedar
    logs an axe; leaves a hoe

### Changed

- The solver snaps the cursor to the nearest start point

- Puzzle panels render on the text layer instead of the beacon beam layer
  - Shader packs no longer bloom the whole panel face; the panel keeps its lit-screen look and the
    traced line stays fully bright
  - Note: in vanilla, panels in dark rooms are now glowstone-dim rather than fullbright

- Stand-less frames on a wall draw a short bracket back to the wall

### Fixed

- Attacking an iron frame in creative mode drops its panel too

## [0.12.0] - 2026-08-03

### Added

- Puzzle panels can actually be solved
  - The line traces from a start point, refuses to cross itself, and won't cross broken edges
  - Right-click submits. Releasing anywhere but an end point aborts instead of failing
  - Hexagon dots must all be covered; miss one and the solution is rejected

- Added hexagon dots
  - Place them on nodes or edges in the composer

- Added tutorial panels
  - New per-panel tutorial flag, toggled from the composer
  - Tutorial panels pulse their start and end points, and flash the dots you missed on a rejection
  - Panels made before this version read as not-tutorial

- Added the full panel sound set
  - Tracing start/abort, path complete, success, failure, potential failure, and start/end point
    scintillation, plus composer menu sounds

- Added the grid upgrade recipe
  - Feed a panel and ancient puzzle tablets back into a crafting table to grow its grid
  - Keeps the panel's colour, its marks, and its accumulated cost

### Changed

- Support for **Minecraft 26.2** (up from 1.17.1)
  - Needs Java 25. See the README for the Fabric API, Fabric Language Kotlin, and LibGui versions

- Panel data now lives in a `witness:panel` data component instead of stack NBT
  - Note: panels crafted before 1.20.5 lose their puzzle on load. Blocks and world state are fine

- Crafting recipes collapsed from 76 JSONs to 23
  - Per-colour dye and per-size grid recipes are now component-aware recipes that carry the panel through

- The composer lost its dye slot; recolouring is the `panel_dye` recipe

- The composer's editor preview now matches how the panel renders in the world: same dyed backdrop,
  same node, endpoint, start, break, and solution styling

### Fixed

- Panels can be retrieved from an iron puzzle frame by attacking it again
- Puzzle panels render their live puzzle in hand, on the ground, and in the GUI again
- Dye and recycle recipes show up in the recipe book again
- Grid upgrade recipes no longer silently fail to load
- Click coordinates on a panel no longer drift from where the line actually goes
- `OakLeavesRunners` got its item tint back

## [0.11.0] - 2021-08-14

### Added

- Support for Minecraft 1.17.1
  - Note: Keep your axolotls away from panels :P

- Added puzzle panel solving (Experimental)
  - Note: Solutions are only client side for now

- Added new puzzle panel crafting recipes (Experimental)
  - Each tile approximately costs one ancient puzzle tablet
  - Expansion becomes progressively cheaper. More about this in the wiki

### Changed

- Changed puzzle panel item's data model
  - This means old panels would no longer be compatible with this version onwards.
  - You can still recover the cost of old panels with outdated models using a crafting table
  - This opens up possibility of panels of various shapes and forms, but more on these later ;)

- Changed puzzle composer layout to be more intuitive

### Removed

- Removed puzzle composer size sliders in favor of a more balanced crafting approach

## [0.10.1] - 2020-10-18

### Added

- Added puzzle composer recipe

### Fixed

- Issue with the background painters of item slots not rendering
- Improved puzzle panel item rendering in first person, third-person, and on the ground
- Setting puzzle panels to the puzzle frame from off-hand

## [0.10.0] - 2020-10-14
### Added
- Added puzzle panels  
- Added puzzle composer and composer screen
- Added puzzle frame and stand 
- Added support for Minecraft 1.16.3

## [0.9.0] - 2020-06-26
### Added
- Added yellow stained stone  
- Added yellow stained stone slabs 
- Added yellow stained stone stairs 
- Added yellow stained stone walls 

## [0.8.4] - 2020-06-26
### Fixed
- Continuous delivery workflow

## [0.8.0] - 2020-06-23
### Added
- Added support for Minecraft 1.16 Release Candidate

### Fixed
- Pink cedar trees leaves replacing parts of the trunk
- Yellow stained stone walls 

## [0.7.2] - 2020-06-17
### Added
- Add support for server

### Fixed
- All block items are in the right group in the creative menu

## [0.7.1] - 2020-06-07
### Fixed
- Oak leaves runners sounds when placed
- Made all vegetation blocks opaque

## [0.7.0] - 2020-06-03
### Added
- Added pink cedar trees
- Added pink cedar tree feature to flower forest biomes
- Added bougainvillea tree decorators
- Added tall yucca to yucca feature

### Fixed
- Small yucca will grow to tall yucca when grown

## [0.6.0] - 2020-05-31
### Added
- Added blue bougainvillea
- Added tall yucca block
- Added tall yucca to yucca feature

## [0.5.1] - 2020-05-25
### Fixed
- The purple bougainvillea 
  -  can now be grown with bone meal
  -  can now be placed on non-full solid blocks, and other purple bougainvillea
- Other minor bug fixes

## [0.5.0] - 2020-05-25
### Added
- Added mimosa block
- Added purple bougainvillea

## [0.4.1] - 2020-05-24
### Changed
- Renamed lilac bushes to jasmine bushes
- Changed jasmine bush model to be more bushy

### Added
- Added jasmine bush variants

## [0.4.0] - 2020-05-23
### Added
- Added lilac bushes
- Added lilac bushes generation feature to swamps

## [0.3.0] - 2020-05-23
### Added
- Added Yucca plants

### Changed
- Fixed oak leave runners not randomly growing like vines

## [0.2.0] - 2020-05-21
### Added
- Added Oak Leaves Runners

## [0.1.4] - 2020-05-11
### Changed
- Fixed Yellow Stained Stone Walls not connecting to each other or other wall types

## [0.1.3] - 2020-05-10
### Added
- Added Yellow Stained Stone Bricks 
- Added Yellow Stained Stone Bricks Slabs 
- Added Yellow Stained Stone Bricks Stairs 
- Added Yellow Stained Stone Bricks Walls 
- Added Yellow Stained Stone Bricks Buttons

### Changed
- The broken changelog

### Removed
- Broken changelog script

## [0.1.2] - 2020-05-10
### Added
- A broken changelog

## [0.1.1] - 2020-05-10
### Added
- A broken changelog

## [0.1.0] - 2020-05-10
### Added
- Base mod from the fork

[0.1.1]: https://github.com/xxfast/fabric-witness/releases/tag/v0.1.1
[0.1.0]: https://github.com/xxfast/fabric-witness/releases/tag/v0.1.0
