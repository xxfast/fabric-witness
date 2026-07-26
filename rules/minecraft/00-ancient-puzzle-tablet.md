# 00 — Ancient puzzle tablet

**Category:** ingredient (static recipe)

The raw currency of every panel recipe. Panels are built out of tablets, grown with more tablets,
and [recycled](04-panel-recycle.md) back into them, so a panel's whole economy is denominated in
tablets.

## Rule

One `minecraft:ancient_debris` crafts into **9** `witness:ancient_puzzle_tablet` (shapeless). That
is the only source; there is no reverse recipe from tablets back to debris.

## Edge cases

- Tablets are a plain stackable item — no data components, no per-stack state.
- The 1 → 9 ratio sets the exchange rate for the whole system: a 3×3-cell panel costs 9 tablets,
  i.e. exactly one ancient debris' worth.

## Cost accounting

Everything downstream measures value in tablets, tracked on a panel by its `witness:cost` component.
See [the shared cost model](README.md#cells-nodes-and-cost) for how tablets, cells, and nodes line
up. In short: a panel's `cost` is the number of tablets invested to build it, and
[recycle](04-panel-recycle.md) hands that many back.

## Status in this mod

Implemented as ordinary datapack JSON.

## Sources

- `src/main/resources/data/witness/recipe/ancient_puzzle_tablet.json` — the shapeless recipe.
- `src/main/kotlin/com/xfastgames/witness/items/AncientPuzzleTablet.kt` — the item.
