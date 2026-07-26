# 04 — Hexagon dots

**Category:** line mechanic

## Rule

The path must pass through every hexagon on the panel. Miss one and the panel fails.

Hexagons sit either on a **node** (the path must visit that node) or on the middle of an **edge**
(the path must traverse that edge, entering from one endpoint and exiting the other). Same rule,
different placement. In the formal literature this is called a "forced edge"/"forced vertex"
clue: node-hexagons and edge-hexagons are the same constraint applied to the two kinds of graph
elements a solution line touches.

Order does not matter. The line can arrive at a hexagon from either direction and at any point
during the solve; only "was it crossed by the time the line reaches its endpoint" is checked.

## Colored variants

Only meaningful on symmetry panels ([05-symmetry.md](05-symmetry.md)):

- **Black**: either line may cross it.
- **Colored** (blue / yellow, matching the two lines): must be crossed by the line of that
  color specifically. The invisible line still has a color and still counts.

## Edge cases

- Hexagons are the only symbol validated against the path itself rather than against a region.
  Regions are irrelevant to them; a hexagon has no notion of "inside" a colored area.
- A hexagon on a start or end node is trivially satisfied by starting/finishing there, since the
  line necessarily visits that node.
- A hexagon on the single edge leading into a dead-end node forces the line to enter that
  dead-end and (since it's a dead end) come back out the same edge in some layouts, or simply
  forces that stub to be part of the route if it's also an endpoint.
- Eliminators ([11-eliminators.md](11-eliminators.md)) can cancel one missed hexagon per region,
  the same as any other failing rule, but only if the hexagon falls inside the eliminator's
  region. A node-hexagon belongs to the regions touching that node; an edge-hexagon belongs to
  the regions on both sides of that edge. If the eliminator's region can't be pinned down
  unambiguously (hexagon sits on a boundary touching more than one candidate region with more
  than one available error), treat it as **unverified** here: known guides describe eliminators
  eating "a missed dot" in general terms but don't spell out the region-membership rule for
  boundary-sitting hexagons in detail.
- **Unverified / rare variants** seen described in secondary sources but not corroborated against
  a primary rules reference: "sound-based" hexagons whose size indicates pitch (used in the
  audio-log puzzles near the Jungle/Orchard), and "ordered" hexagons that must be crossed in a
  specific sequence. Neither is part of the base line-and-hexagon rule above; do not implement
  either without further verification, they may be conflating hexagon dots with a different
  late-game mechanic.

## Implementation notes

Data model: a hexagon is a boolean-ish tag on either a `Node` (`Modifier.DOT` on the node itself)
or on an `Edge` between two adjacent nodes (`Modifier.DOT` as the edge's value in the
`ValueGraph<Node, Edge>`). Both are already representable with a single `Modifier` enum since it's
shared between node and edge positions in this codebase (`Edge.kt`: `typealias Edge = Modifier`).

Validation sketch, given the solved path as an ordered list of nodes (or equivalently a
sub-`Graph<Node>` of the panel's line):

1. Collect all node-hexagons: nodes in `panel.graph.nodes()` with `modifier == Modifier.DOT`.
   Each is satisfied iff it appears in `panel.line.nodes()` (or in the traced node list).
2. Collect all edge-hexagons: edges in `panel.graph.edges()` whose value is `Modifier.DOT`.
   Each is satisfied iff both endpoints are adjacent in the traced line, i.e. the endpoint pair
   also exists as an edge in `panel.line` (or consecutive in the ordered path list).
3. Panel fails if any hexagon (node or edge) is unsatisfied, subject to eliminator cancellation
   per-region (see `11-eliminators.md`).
4. For colored variants, track which of the two lines (player line vs. mirrored line, see
   `05-symmetry.md`) actually traversed the hex, and compare against the hex's stored color
   before applying the crossed/uncrossed check.

This is a cheap O(number of hexagons) check once the final line graph is known; no full-graph
search is needed, unlike region symbols which require flood-filling the panel into regions first.

## Status in this mod

Data model only. `Modifier.DOT` exists in `src/main/kotlin/com/xfastgames/witness/items/data/Edge.kt`
and can be placed on either a `Node` or an `Edge` (same enum, shared type alias). The composer
widget (`WPuzzleEditor.kt`) treats `Modifier.DOT` as a non-interactive modifier when cycling edge
state. The panel renderer (`PuzzlePanelRenderer.kt`) has an explicit `Modifier.DOT -> {}` branch
for edges that draws nothing, and node rendering (`renderNode`) has no case for a `DOT` node
modifier either, so a hexagon is not currently visually distinguished from a plain connector node.
`PuzzleSolver.kt` only implements the *drawing* mechanics (tracing the line, waypoint
snapping, collision with the line itself); it has no win-condition validation at all, so hexagons
(colored or otherwise) are not checked against the solved path. Colored hexagons are entirely
unmodelled, there's no per-node/edge color field and no second (mirrored) line to check them
against.

## Sources

- [SerGreen/TheWitnessPuzzles Rules Guide](https://raw.githubusercontent.com/SerGreen/TheWitnessPuzzles/master/Puzzle%20Rules%20Guide/RulesGuide.md)
- [GameFAQs: Puzzles & Symbols walkthrough](https://gamefaqs.gamespot.com/pc/969704-the-witness/faqs/82392/puzzles-and-symbols)
- [Gameranx: The Witness Puzzle Types And Rules Guide](https://gameranx.com/features/id/36898/article/the-witness-puzzle-types-and-rules-guide/)
- [Abel, Bosboom, Coulombe, Demaine et al., "Who witnesses The Witness?"](https://erikdemaine.org/papers/Witness_FUN2018/paper.pdf) (abstract/summary via search; confirms hexagons are formally a "forced edge"-style clue sufficient on their own for NP-hardness)
- `src/main/kotlin/com/xfastgames/witness/items/data/Edge.kt`, `Node.kt`, `Panel.kt`
- `src/main/kotlin/com/xfastgames/witness/items/renderer/PuzzlePanelRenderer.kt`
- `src/main/kotlin/com/xfastgames/witness/screens/solver/PuzzleSolver.kt`
- `src/main/kotlin/com/xfastgames/witness/screens/widgets/WPuzzleEditor.kt`
