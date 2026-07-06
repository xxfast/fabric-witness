# GOALS.md — what fabric-witness is trying to become

This is the north star. It exists so Claude Code (and any contributor) can judge whether a
proposed change moves the mod toward its end goal or sideways. `ROADMAP.md` sequences the work;
this file says where the work is headed and why.

## North star

**A puzzle-first Minecraft mod that faithfully recreates The Witness's line-drawing puzzles as
interactive blocks.**

Puzzles are the point. The decoration blocks the mod already ships (foliage, stained stone, drapes)
are supporting flavor — they exist to make a solved panel feel like it belongs somewhere and to
serve the companion [YouTube series](https://www.youtube.com/channel/UCrLikF1yl6dqz0N9OaJlAcA). They
are not the goal, and the goal is not to reproduce any particular environment. When a decision trades
puzzle fidelity against decoration breadth, puzzle fidelity wins every time.

## The two pillars

1. **Faithful puzzles.** A panel should solve the way The Witness's panels solve — the same rules,
   the same feel, the same "click" when the line snaps to a node. Fidelity to the source game is
   the measure of success, not novelty.
2. **Puzzles as world mechanics.** Panels gate doors, bridges, and contraptions. Composing custom
   puzzles (the composer block) and solving found ones (frames) are both first-class. Puzzles are
   diegetic Minecraft blocks, not a minigame menu.

## Puzzle-rule fidelity — the end state

The Witness's puzzle language is the long-term target. Ordered roughly by how foundational each is;
`ROADMAP.md` turns this into phases.

| Rule                                                                     | Lives on                     | Status today                                                 |
|--------------------------------------------------------------------------|------------------------------|--------------------------------------------------------------|
| Continuous line: single start → endpoint, no self-intersection           | nodes + edges                | partial (traces + reaches END; self-intersection unenforced) |
| Broken edges (gaps the line can't cross)                                 | edges (`Modifier.BREAK`)     | modifier exists, **not enforced**                            |
| Hexagon dots (line must pass over)                                       | nodes/edges (`Modifier.DOT`) | modifier exists, **not enforced**                            |
| Squares (separation by color)                                            | **faces**                    | not modeled                                                  |
| Stars (exactly two of a color per region)                                | **faces**                    | not modeled                                                  |
| Tetris / polyominoes (region must tile to the shapes; incl. subtractive) | **faces**                    | not modeled                                                  |
| Triangles (line borders exactly N sides of the cell)                     | **faces**                    | not modeled                                                  |
| Elimination marks (cancel one error in a region)                         | **faces**                    | not modeled                                                  |
| Symmetry (two mirrored lines) / colored lines                            | line(s)                      | not modeled                                                  |

### The one architectural fact that governs all of this

Witness rules fall into two kinds:

- **Line-path rules** — validated purely against the drawn line: start→end continuity, no
  self-intersection, broken edges, hexagon dots. No notion of enclosed area is needed.
- **Region rules** — a symbol sits in an area of the panel and constrains it: squares, stars,
  tetris, triangles, elimination. These need to know how the line partitions the panel.

The general primitive for the second kind is a **face** of the planar panel graph — a bounded area
enclosed by edges — together with the **regions** the drawn line partitions those faces into. On a
square grid the faces are the familiar unit *cells*; but The Witness has plenty of panels that don't
conform to a grid, and **this codebase already encodes that**: `Panel` is a sealed
`Grid | Tree | Freeform` (`items/data/Panel.kt`). A cell is just the grid's face. The model must not
assume one.

The current `Panel` is a `ValueGraph<Node, Edge>` — nodes and edges only. There is no representation
of a face, a region the line has enclosed, or a symbol attached to one.

So "full fidelity" is not a pile of independent features. It has a **prerequisite**: a
**topology-agnostic** face/region concept in the data model — one that works the same for grid, tree,
and freeform panels — plus a solver that partitions the panel into regions and validates region rules
over them, and line-path rules over the line. Everything from squares onward depends on it. This is
the single most important architectural decision the roadmap makes; make it topology-agnostic, and
make it once.

## Non-goals (for now)

- **Environmental / perspective puzzles** (the ones drawn by the landscape itself). Out of scope —
  they don't map to a panel block.
- **Recreating The Witness's island.** Not a goal of the mod. A *companion map / datapack* that
  rebuilds the island using these blocks is a plausible far-future project, but it's separate content
  built on top of the mod — not something the mod jar or this roadmap owns. Too far out to plan.
- **Broadening the decoration set for its own sake.** New decoration blocks earn their place by
  serving a puzzle scene, not by chasing environmental coverage.
- **Backward save compatibility with pre-1.20.5 crafted panels** (`MIGRATION.md` item 2). Not worth
  the effort for a mod at this stage.

## How to use this file

Before adding a feature, ask: *which pillar does this serve, and does it move a row of the fidelity
table from "not modeled" toward "enforced"?* If it serves none of them, it probably belongs in a
datapack or a different mod. Link roadmap items back to the pillar or table row they advance.
