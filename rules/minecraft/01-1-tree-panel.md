# 01-1 Tree panel

**Category:** special recipe (code), and a panel type. The crafting rule it is a variant of is
[01](01-puzzle-panel-crafting.md).

A tree panel is the orchard puzzle from the game: a trunk that forks into branches, a line that runs
from the root to one tip, and an apple that says which tip. It is grown from a **sapling**, its size
is in **levels**, and it is the one panel type with no cells at all.

Sizes below are in **levels** (branch steps from the root to a tip) and costs are in tablets.

---

# Design

## In the original game

The Orchard is a row of five posts, each carrying a green panel drawn as the silhouette of the tree
standing beside it. The panel and the tree fork the same way. Every panel is traced from the base
of the trunk, and the trace can end at any branch tip; what tells the tips apart is the real tree,
which has **one apple hanging from one branch**. Trace the trunk out to the apple's branch and the
panel solves. The five panels then complicate how you read the tree, not the panel:

1. **Plain.** Mark the branch with the apple on it.
2. **A broken branch.** One limb on the real tree is snapped, and the panel shows the same limb
   broken. The natural reading is that the broken limb is not the answer but the key that says
   which way round the panel is.
3. **Mirrored.** The guides say to read the tree "as if you were looking at the tree in a mirror";
   the natural reading is that the panel faces away from its tree.
4. **More broken branches**, with the perspective twist on top.
5. **No apple.** The tree has four broken limbs and the panel shows three. The fourth lies on the
   ground nearby with an eaten apple; so, reading it the only way it can be read, the branch that
   is whole on the panel but gone from the tree is the answer.

Everything past "one apple, trace to it" is reconstructed from walkthroughs, not from the game,
except the shape of the damage: a shot of the second panel (read 2026-09-05) shows the broken
branch as a limb that stops short of the crown with no tip, and the first panel's tree has a limb
that never takes its last fork. See the [open questions](#open-questions) for the rest.

That splits cleanly into two halves. On the **panel side** a tree is a tiny vocabulary: a start at
the root, an end at every tip, a limb cut short with no end where one is snapped, and one path per
tip so the whole puzzle is *which tip*. On the **world side** the clue lives outside the panel: the apple, the
viewing angle, the missing limb. The mod keeps the panel side whole and leaves the world side to
the map maker: the panel validates the apple's tip without showing it, and whatever in the world
points the player at that tip is theirs to build. Perspective, mirroring and the eaten apple are
environmental puzzles and stay out of scope, the same stance as [../witness/](../witness/README.md)
takes on every world-drawn puzzle.

## The rule

The [crafting schematic](01-puzzle-panel-crafting.md#the-rule) with a different seed. A **sapling is
the seed of a tree panel**: stack tablets on top of it in a single column, and each tablet is one
level of tree.

```
┌───┬───┬───┐
│ T │   │   │      S: a sapling, T: a tablet.
├───┼───┼───┤      the column is read bottom-up,
│ T │   │   │      and can sit in any of the three
├───┼───┼───┤      crafting-grid columns
│ S │   │   │
└───┴───┴───┘
```

$$S \;+\; h\,T \;=\; \text{Tree}_{h} \quad (\text{cost: } h\,T)$$

What comes out is a full binary tree: a root at the bottom centre with a trunk above it, every
branch splitting in two on the way up, $2^h$ tips in a row along the crown
([layout](#layout)).

```
  o   o   o   o        Tree_2: 2 levels, 4 tips.
   \ /     \ /         Every parent sits centred
    o       o          under its pair of children,
     \     /           the levels shorten toward
      \   /            the crown, and the root
        o              alone at the foot of the
        |              trunk.
        o
```

| Column | Levels | Tips | Cost |
|--------|--------|------|------|
| S + T  | 1      | 2    | 1    |
| S + TT | 2      | 4    | 2    |

Three and four levels are grow-only ([below](#growing-a-tree)): a column has room for two rows of
tablets above the sapling.

A tree craft is accepted when:

1. The occupied slots form a single column with the sapling at the bottom.
2. Every slot above the sapling holds a tablet, at least one.
3. The result is within the [height cap](#the-height-cap).

The tree reading and the [grid reading](01-puzzle-panel-crafting.md#the-rule) are disjoint by
construction: no grid craft contains a sapling, so no layout ever matches both. **Any sapling
works** and the wood doesn't matter; colour comes from [dye](02-panel-dye.md).

## Growing a tree

Put a tree panel at the bottom of the column instead of a sapling and it gains one level per tablet:

$$\text{Tree}_{h} \;+\; n\,T \;=\; \text{Tree}_{h+n} \quad (\text{cost: } c + n\,T)$$

```
┌───┬───┬───┐
│ T │   │   │      Tree_1 + 2T = Tree_3, and Tree_2 + 2T = Tree_4,
├───┼───┼───┤      the cap. A column is at most 3 slots, so a seed
│ T │   │   │      craft tops out at 2 levels and the Orchard's
├───┼───┼───┤      16-tip tree is two crafts away from a sapling
│ P │   │   │
└───┴───┴───┘
```

Marks travel by **branch position**, the way a grid's marks travel with the anchor. A branch
position is the turn sequence from the root (left, right, left …), not a place on the panel: growing
re-spaces every row, so nothing is where it was, but the root is still the root and the
left-left-right branch is still the left-left-right branch. The old tree becomes the bottom $h$
levels of the new one, and its marks go with it.

The exception is ends on the old tips. Those tips become forks, an end can only hang off a tip,
so they drop, and the tips the growth adds get fresh ends like a fresh tree's. Growing a composed
tree keeps its starts, breaks and apples. A nub on the root survives, so an upside-down tree grows
upside down.

Pruning survives too. A limb that was not there stays not there, and a stub keeps whatever the
author gave it: a stub with an end grows into the same short branch with the same end, a bare stub
stays the broken branch. Only the crown grows.

## Layout

The Orchard's first panel is the yardstick (IGN's shot of it, read 2026-09-05): a start dot at the
bottom centre, a short **trunk**, then a two-way fork and three more two-way forks above it, so
**16 tips** in a row across the crown. Each level is shorter than the one below it, the top forks
are tiny V's, and the line is about half as wide as the gap between neighbouring tips. Four
levels on one panel is the target, and the layout is what makes it legible:

- **The line is one width everywhere, a quarter of a unit**, and every panel is drawn at one over
  its size in units. So a tree gets a thinner line the same way a big grid does: by being more
  units across, never by a special line.
- **The proportions are the Orchard panel's, measured.** Crown 77% of the panel wide, tree 65%
  tall from root to tips and centred both ways, neighbouring tips 1.4 line widths apart at the
  least, and level heights trunk : first : second : third : fourth = 0.75 : 1 : 1 : 0.6 : 0.45. A
  shorter tree uses the first of those, so its crown is the same shape.
- **The root is the foot of the trunk, not the first fork.** The start sits there like the
  Orchard's, and it is the one node with a single branch. An upside-down tree puts its end there.
- **The panel is square**, sized so the crown fits at that tip spacing: 3 units up to 4 tips, 4
  units for 8, 7 units for 16.

| Levels | Tips | Panel (units) | Tip gap, in line widths | Line, as a fraction of the panel |
|--------|------|---------------|-------------------------|----------------------------------|
| 1 | 2 | 3 | 9.2 | 0.083 |
| 2 | 4 | 3 | 3.1 | 0.083 |
| 3 | 8 | 4 | 1.8 | 0.063 |
| **4** | **16** | **7** | **1.4** | **0.036** |
| 5 | 32 | 13 | 1.4 | 0.019 |

The Orchard panel's line is 3.7% of its width and its tips 1.4 lines apart, so the four-level tree
is drawn at the game's own density. For scale, the 10×10 grid at its cap has a line 2.3% wide.

Two layouts were seen in game on 2026-09-05 before this one and both were wrong on sight: levels
halving toward the crown (every branch at one angle) put half the tree into the first fork with a
fringe on top, and a tree filling its panel edge to edge with tips two lines apart was airy and
square-shouldered where the game's is compact with room around it. Measure, don't derive.

## The height cap

**4 levels (16 tips)**, the Orchard's own maximum. Five would be 32 tips on a 13-unit panel with a
line under 2% of the panel wide, a 12×12 grid's density, past anything the game asks a player to
trace. The cap is on levels because, with tips packed to a fixed floor, levels are what decide the
panel's size.

## What a tree panel is

The three questions every panel type answers ([04-2](04-2-puzzle-composer-grid.md#the-rule)):

| Question | A tree's answer |
|----------|-----------------|
| Where may a node sit | At the branch positions of a full binary tree of its levels: the root, and every fork and tip above it |
| Which two may be joined | A branch and the branch it hangs from |
| What are its cells | **None.** A tree closes no faces, so no region symbol can ever sit on one |

So a tree's content is the line vocabulary and nothing else: starts, ends, breaks and hexagons,
placed at the composer like on any panel ([04-1](04-1-puzzle-composer-modifiers.md)). The region
tools are absent from the rail for a tree rather than greyed, because there is nowhere on it they
could land.

- **One path per tip.** A tree has no cycles, so picking the exit picks the whole line. Solving one
  is reading, not searching: the tip the world points at, whose branch line carries no break.
  That puts a hard ceiling on difficulty, and that is the role, exactly as it was in the Orchard:
  tutorial rows, flavour, and routing.
- **A fresh tree is an orchard tree: a start on the root, an end straight up off every tip.**
  That is what every Orchard panel is, and it is what a tree is for; a blank tree is nothing until
  a start and every end are clicked in. The composer removes or moves any of them as on any
  panel. This is the one place a crafted panel is not blank, and it is deliberate.
- **The apple is the author's mark, and the panel never shows it.** The Orchard's panel is a bare
  silhouette; the apple hangs on the real tree beside it. So a hexagon on a tree is drawn only at
  the composer, as an apple on the tip the author means, and on the block face and in a frame it
  is invisible. It still validates exactly as a hexagon does
  ([../witness/04-hexagon-dots.md](../witness/04-hexagon-dots.md)): the line has to reach it. The
  world-side clue that tells the player which tip is the map maker's to build, as the game built
  an orchard, and the mod does not try to draw it on the panel. Decided 2026-09-05, after a frame
  full of red apples looked nothing like the game.
- **A broken limb is a pruned limb with no end.** The Orchard draws a broken branch as a stub: it
  leaves its fork, stops short of the crown, and has no tip. That is a fork whose branches were
  [pruned](#pruning-the-grid-tab-on-a-tree) and which was given no end: it sits a level below the
  crown, so it draws short by itself, and the line can enter it but never finish there. Nothing
  tree-specific is needed, and the [break](../witness/03-broken-edges.md) stays what it is on
  every panel, a gap on a segment. A break on a tree branch is legal and makes every tip above it
  unreachable, but it is not how the Orchard's damage looks.
- **Routing.** Every tip sits on the top border, so tip ends point up. A tree with one end feeds
  every joined frame like any one-end panel; a tree with a choice of tips feeds the frame the used
  tip points at ([05](05-puzzle-frame.md#where-the-power-goes)). The two outermost tips are
  corners and can be squared off left or right at the composer, which makes a tree a left/right
  selector in a frame chain.
- **Upside down works.** The root sits on the bottom border, so it can take a downward end. Starts
  on the tips and the end on the root is a legal panel, traced downward.

## Pruning: the Grid tab on a tree

The Orchard's trees are not symmetric. Some limbs fork three times, some never fork, and the panel
draws the tree it has. The full binary tree the craft produces is the **template**, and the
[Grid tab](04-2-puzzle-composer-grid.md) is how a tree becomes a particular tree:

- The **eraser** on a branch removes it and everything above it. The fork it hung from becomes a
  tip, so it is now on the border and can take an end.
- The **pencil** on a missing branch position restores that branch, one step, with blank tips.

Pruning is how both of the Orchard's irregularities are made. A limb that never forks again, with
an end on its tip, is a shorter branch and the tree is lopsided. A limb that never forks again,
with **no** end, is the Orchard's broken branch: a stub the line can enter and never finish on,
whose job is to be seen. In the game it is the clue that orients the panel to the tree. The two
differ by one nub, and that nub is placed with the end tool like any other.

**A tree's border is its tips.** On a grid, "the border" is the rectangle's edge; a tree has no
rectangle, and a tip is on the edge of the tree by construction, whatever row it sits on. So a fork
that becomes a tip by pruning can take an end there and then, pointing away from the branch it
hangs from, and a pruned tree keeps the Orchard reading: every tip an end, and the puzzle still
*which tip*. Without this, every shortened limb would be a dead end that can never be the answer,
and pruning would only ever make trees that are not orchard trees.

An end on a lower tip points where its branch points, up and outward, so on a frame it feeds the
top side like any other tip unless the composer squares it off. The two outermost tips of any row
are the only ones that can be squared sideways.

## Cost

$$\text{new cost} = \text{old cost} + \text{tablets placed}$$

A column is thin by definition, so a tree never pays [the
premium](01-puzzle-panel-crafting.md#the-convenience-premium): $h$ tablets for an $h$-level tree on
every route, seeded in one craft or grown one level at a time. [Recycle](03-panel-recycle.md)
returns `cost` as usual. The sapling is spent, like dye in [02](02-panel-dye.md), and is not
refunded.

| Route to Tree_3 | Tablets | Saplings | Crafts | Recycle returns |
|-----------------|---------|----------|--------|-----------------|
| S + TT, then + T | 3 | 1 | 2 | 3 |
| S + T, then + TT | 3 | 1 | 2 | 3 |
| S + T, + T, + T | 3 | 1 | 3 | 3 |

Every route is the same price, so there is nothing to discover and no dupe on any route: the most
a tree can ever return is the 3 tablets it took. The only asymmetry is the sapling, which is a
consumable in a game where saplings are free.

**As a pure selector, the tree is the pretty option, not the cheap one.** A 1×1-cell grid panel
with two squared corner ends routes left/right for 1 tablet and no sapling. Tree_1 does the same
job for 1 tablet and a sapling. The tree buys the look and the orchard reading.

## Edge cases

- **One apple, on a tip.** The apple tool on a tree goes on tips only and moves rather than
  multiplies: click a tip, or the nub hanging off it, and the apple is there and nowhere else;
  click it again and it is gone; click a fork, the root or a branch and nothing happens. Paths are unique, so a second
  apple could only make the panel unsolvable, and an apple below a tip only ever said "one of
  these", which is not what the author's mark is for. A tree authored before this rule keeps its
  stray apples until the next click, which clears them.
- **A tree with no end on any tip is a panel with no solution.** Prune everything back and leave
  the stubs bare and the line has nowhere to finish. Same category as the apple off the line:
  allowed, and wrong.
- **Growing resets the ends, and only the ends.** A player who removed some tip ends at the
  composer, grew the tree, and finds an end on every new tip has hit the rule, not a bug. The tips
  they pruned ends from are forks now, and the new tips come as a fresh tree's do.
- **A tree in a grid craft is a stray item**, and a sapling in a grid craft is one too. Neither
  layout crafts, and nothing says why: the [same silence](01-puzzle-panel-crafting.md#edge-cases)
  as every failed special craft.
- **The recipe book shows the two seed columns and one grow.** Growing is one rule over every
  tree, and a finite preview can't list it; the single grow shown is Tree_2 plus two tablets, the
  one that reaches the cap.

## Open questions

- **Whether every Orchard tip carries an end nub**, or only the candidates. The guides describe
  the trace as going "to where the fruit is", which reads as every tip being a legal end, and that
  is what the design above assumes. Unverified.
- **The tree has no economy.** Every route costs the same and there is no premium to find. That is
  the consequence of a one-dimensional size, not a decision, and it is probably fine for a panel
  type whose role is flavour. Noted so it is not mistaken for an oversight when the grid's
  premium gets tuned.

---

# Implementation

## Status in this mod

Seeding and growing are both built. `PanelTreeRecipe` matches the column with either a sapling (any
`#minecraft:saplings` item) or a tree panel on the bottom row; `PanelTreeLayouts.levels` is the pure
column check (`PanelTreeRecipeTests`), a `TREE` slot carrying the levels the panel already has, and
the result is `Panel.Tree.ofSize(levels)` for a seed or `source.expandTo(levels)` for a grow, at
cost = previous cost + tablets. A grow copies the source stack, so dye and any other component
travel, as on a grid. `generateTree` builds the full binary tree with the grid's half unit border
margin and every parent centred under its pair of children, pinned in `GraphTests`. `ofSize` takes
**levels**, stored as `Tree.levels`; `width`/`height` are the crown's panel size
([geometry](#geometry)), not a function of levels a reader should count on.

`Panel.Tree.expandTo(levels)` is the transplant: it generates the bigger tree, walks source and
target from the root in parallel, and copies each source node's role and symbol and each branch's
edge onto the node and edge at the same turn sequence. A source child is matched to the target
child on the same **side** of its parent, not by order, so a lone surviving limb lands where it
was; a target limb with no source counterpart below the source's crown is pruned along with
everything above it. Nubs are re-hung on the root and on any matched node that is still a tip;
fresh ends go only on the tips the template added (`withTipEnds(except = authored)`).
Pinned in `GraphTests` (`growing carries marks by branch position and drops the tip nubs`, `growing
keeps a nub on the root`). Same size or smaller returns the instance untouched, like the grid.

Hexagon marks on a tree are not drawn in the world (`PuzzlePanelRenderer.renderSymbols(hidden =
true)`, on the item, the block face and the frame alike); `WPuzzleEditor.drawApple` draws them as
apples in the composer. Same data, same validation (`Panel.unsatisfiedHexagons`); only where they
show differs.

Seen in game 2026-09-05: a fresh Tree_4 in a frame, 16 tips, trunk, start on the root and a nub
on every tip, matching the Orchard shot side by side. "This is perfect" was the sign-off, after
two layouts that were not ([layout](#layout)).

## Geometry

The [layout](#layout) as designed, built 2026-09-05. The measured proportions are constants on
`Panel.Tree`: `CROWN_WIDTH` (0.77), `TREE_HEIGHT` (0.65), `TIP_GAP_IN_LINES` (1.4),
`LEVEL_WEIGHTS` (0.75, 1, 1, 0.6, 0.45) and `MIN_SIZE` (3). `sizeFor(levels)` is the square panel
in units: the smallest whose crown fits the tips at that spacing. `generateTree(levels)` is blank:
it lays the tips out first, evenly across the crown, pairs children into parents level by level
with the weights scaled to the tree's height, and adds the root last at the foot of the trunk.
`ofSize` puts the Orchard's marks on it (`withTipEnds`, a start on the root), and `expandTo`
calls `withTipEnds` on the transplanted result. Pinned in `GraphTests` (`Test generate tree 1
tall` for coordinates, `4 tall` for the 16-tip crown at 1.44 lines, the level heights and the
centring, `ofSize …` for the marks and that `endSides` reads them all as `TOP`).

`levels` is its own field, written to NBT as `levels`; a tree saved before that key existed reads
it as `height - 1`, which was true of every tree then. Such a tree keeps its old graph and old
panel size until grown, at which point `expandTo` matches root and fork by role
(`firstFork`): a two-child root is the fork it used to be, so its marks land on the new fork and
the new foot stays blank. Pinned in `growing a tree laid out before the trunk existed`.

The renderer's `maxOf(width, height)` scaling needs no tree case; a wider panel is a thinner line.

**Node positions are recomputed on every size change.** The x of every node depends on `levels`,
so a Tree_2 node has no coordinate in common with the Tree_3 node at the same branch position.
Growth therefore cannot transplant by coordinate the way `Panel.Grid.expandTo` copies through
`gridOffsets()` in index space; it has to walk both trees from the root by turn sequence and copy
modifiers across. Do not try to match nodes by position, it matches nothing.

## Not done

- **Growth of a composed tree is unseen in game.** The fresh Tree_4 with its default marks was
  seen in a frame on 2026-09-05 and signed off against the Orchard shot. Not yet seen: a composed
  Tree_2 with an apple and two breaks grown to Tree_3 (marks on the same branches, ends on the
  new tips); a dyed tree grown (colour kept); Tree_3 plus two tablets (must not craft); tracing a
  Tree_4 to a tip with the solver, which has never selected among tips 1.4 lines apart.
- **The composer on a tree is unseen in game.** Pruning, the tree rail and the editor's apples
  were all built 2026-09-05 and unit-tested only ([04-1](04-1-puzzle-composer-modifiers.md#not-done),
  [04-2](04-2-puzzle-composer-grid.md#not-done)). What to look at on a Tree_4: erase a level-3 fork
  and see the stub; give the stub an end with the end tool; grow it and see the stub survive.
- **Not yet seen in game:** tracing along the diagonal branches, and a tree in a frame with its
  hexagons hidden. Apples in a frame were seen 2026-09-05 and are what prompted hiding them.
  `PuzzleSolver` is graph-generic so both should follow, but the solver's segment selection has
  only ever been exercised on axis-aligned edges.

## Sources

- `src/main/kotlin/com/xfastgames/witness/recipes/PanelTreeRecipe.kt` — the sapling column,
  `PanelTreeLayouts.levels`.
- `src/test/kotlin/com/xfastgames/witness/recipes/PanelTreeRecipeTests.kt` — accepted and rejected
  columns.
- `src/main/kotlin/com/xfastgames/witness/items/data/Panel.kt` — `Panel.Tree`, `ofSize`,
  `generateTree`, `MAX_LEVELS`.
- `src/test/kotlin/com/xfastgames/witness/items/data/GraphTests.kt` — the tree geometry.
- `src/main/kotlin/com/xfastgames/witness/items/renderer/PuzzlePanelRenderer.kt` — `renderSymbols`,
  `apple`.
- [Gosu Noob: Pink Trees puzzle solutions](https://www.gosunoob.com/witness/pink-trees-puzzle-solutions/)
  — the five Orchard panels in order; the only walkthrough that could be retrieved in full.
- [The Witness Wiki: Orchard (Walkthrough)](https://thewitness.fandom.com/wiki/Orchard_(Walkthrough))
  — not retrievable at time of writing; listed for the reader, not relied on above.
