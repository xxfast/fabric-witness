package com.xfastgames.witness.items.renderer

import com.google.common.graph.EndpointPair
import com.google.common.graph.Graph
import com.google.common.graph.ValueGraph
import com.xfastgames.witness.items.data.*
import com.xfastgames.witness.utils.*
import com.xfastgames.witness.utils.guava.edgeValueOf
import com.xfastgames.witness.utils.guava.incidentEdges
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.util.LightCoordsUtil
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.DyeColor
import net.minecraft.core.BlockPos
import org.joml.Vector3f
import kotlin.math.*

/**
 * Renders puzzle panels in the world (puzzle frames / composer table).
 *
 * Migration notes (1.17 -> 1.21.11):
 * - The old `BuiltinItemRendererRegistry.DynamicItemRenderer` (custom in-hand / GUI / ground item
 *   rendering incl. the custom arm pose) was removed with the 1.21.4 item model rework. See
 *   TODO(migration) in [com.xfastgames.witness.items.PuzzlePanelItem].
 * - Level rendering now goes through the ordered render command queue instead of a
 *   MultiBufferSource; geometry is submitted per render layer via `submitCustom`.
 */
@Suppress("UnstableApiUsage")
object PuzzlePanelRenderer {

    /**
     * Attract / error-cue draw path.
     *
     * - `false` (default): opaque quads on the `text` layer, same pass as the solution line. Fade
     *   is white → panel backdrop (no grey). Reliable on the face.
     * - `true`: `entityTranslucentEmissive` + vertex alpha. Softer look; broken on-face today —
     *   see TODO(panel-cues) below before turning on.
     */
    private const val USE_TRANSLUCENT_PANEL_CUES: Boolean = false

    /**
     * Slight forward bias past the lattice (lattice ~-.01, symbols ~-.012). Keep this *small* so
     * the cue stays on the glass; large values peel the ring into free space.
     */
    private const val CUE_Z_BIAS: Double = -0.02

    /** Error flash depth: just in front of the symbols pass (-.012), so it reads as the symbol itself turning red. */
    private const val ERROR_FLASH_Z: Double = -0.013

    // TODO(panel-cues): Proper translucent attract / error rings (Witness soft white alpha on glass).
    //
    // Failure mode when USE_TRANSLUCENT_PANEL_CUES is true today (consistent, not random):
    //   The ring is only visible where it falls *outside* the panel bezel. On the face it vanishes.
    //   Opaque lattice/backdrop already wrote the depth buffer for the whole face; translucent is
    //   drawn later, depth-tests, and loses. Escaping that depth footprint (huge CUE_Z_BIAS, or a
    //   start on the edge that peels into free space) is the only reason translucent ever "works" —
    //   it is not on-glass. Multi-start panels show the same: only the start that projects clear of
    //   the face reads.
    //
    // What a real fix needs (not more Z bias):
    //   1. A dedicated RenderType / RenderPipeline for these cues that either
    //        - depth-tests with a small reliable polygon/Z offset that stays coplanar under
    //          perspective, or
    //        - disables depth test (or ALWAYS) for this overlay only, depth-write off, so on-face
    //          fragments still composite without fighting the lattice.
    //   2. Keep sticky focus + x/y/z frame matching (PanelAttractPulse / PanelErrorFlash); do not
    //      reintroduce screen-centre raycast for "which panel".
    //   3. Fade via vertex alpha on pure white (red for error), not RGB dim toward black.
    //   4. Verify multi-start panels and steep camera angles: every START/END on the focused frame
    //      must read on the glass, not only nodes that project outside the bezel.
    //   5. Prefer submitCustom on that layer in-world (panel perspective), not a 2D screen overlay.
    //
    // Until then leave USE_TRANSLUCENT_PANEL_CUES false and CUE_Z_BIAS small.

    /**
     * Panels read as lit screens: their block light is floored at this emission (glowstone-ish) so
     * they stay readable in the dark — but only via the vanilla lightmap. Never draw panel
     * geometry on a fullbright layer (`beaconBeam` & co) to get this effect: shader packs route
     * those layers through their beacon/emissive programs and bloom the whole face into a
     * blinding light source. The `text` layer is the flat, lightmap-lit pass vanilla uses for
     * maps in item frames, which is exactly what a panel face is.
     */
    private const val PANEL_GLOW: Int = 12

    /**
     * Centre to point, so a hexagon is 3.pc across corners against the 4.pc line it marks. Narrower
     * than the line on purpose; see the open question in rules/witness/04-hexagon-dots.md.
     */
    private val HEXAGON_RADIUS: Float = 1.5f.pc

    /** Side of a coloured square in panel units: well inside its one-unit cell, clear of the line. */
    private const val SQUARE_SIDE: Float = 0.4f

    /** Corner radius of a square: the line's own cap radius (half its 4.pc width), so the two read as one family. */
    private val SQUARE_CORNER_RADIUS: Float = 2.pc

    fun renderPanel(
        stack: ItemStack,
        matrices: PoseStack,
        queue: SubmitNodeCollector,
        light: Int,
        overlay: Int,
        framePos: BlockPos? = null,
    ) {
        val puzzle: Panel = stack.panel ?: Panel.DEFAULT
        renderPanel(puzzle, matrices, queue, light, overlay, framePos)
    }

    /**
     * @param framePos world position of the puzzle frame this panel is mounted on. Attract and
     * error flashes only draw when [framePos] matches the frame that owns the live effect, so a
     * reject never lights every tutorial panel nearby. Item / composer renders leave it null.
     */
    fun renderPanel(
        puzzle: Panel,
        matrices: PoseStack,
        queue: SubmitNodeCollector,
        light: Int,
        overlay: Int,
        framePos: BlockPos? = null,
    ) {
        val panelLight: Int = LightCoordsUtil.lightCoordsWithEmission(light, PANEL_GLOW)

        renderBackground(puzzle.backgroundColor, matrices, queue, panelLight, overlay)
        renderGraph(puzzle.graph, puzzle.width, puzzle.height, matrices, queue, panelLight, overlay)
        // The traced line caps the lightmap instead of the glow floor: it should pop against the
        // backdrop like the lit line in The Witness, and a maxed lightmap does that without
        // tripping shader-pack bloom.
        renderLine(puzzle.line, puzzle.width, puzzle.height, matrices, queue, LightCoordsUtil.FULL_BRIGHT, overlay)
        // Symbols go in front of both, so a hexagon stays visible once the line covers it: that is
        // the only way a player can tell it was crossed (rules/witness/04-hexagon-dots.md).
        renderSymbols(
            puzzle.graph, puzzle.backgroundColor, puzzle.width, puzzle.height,
            matrices, queue, panelLight, overlay
        )
        renderCellSymbols(puzzle.symbols, puzzle.width, puzzle.height, matrices, queue, panelLight, overlay)
        // Cue drawing is gated by the effect's own frame-pos match (set at trigger), not by
        // re-reading tutorial here — trigger already required tutorial, and a missing/stale
        // component must not silently drop an in-flight flash.
        if (framePos != null) {
            renderAttractPulse(puzzle, framePos, matrices, queue, panelLight, overlay)
            renderErrorFlash(puzzle, framePos, matrices, queue, panelLight, overlay)
        }
    }

    /**
     * Expanding white ring on start discs / end nubs ([PanelAttractPulse]). See
     * [USE_TRANSLUCENT_PANEL_CUES] for the two draw paths.
     */
    private fun renderAttractPulse(
        puzzle: Panel,
        framePos: BlockPos,
        matrices: PoseStack,
        queue: SubmitNodeCollector,
        light: Int,
        overlay: Int
    ) {
        val frame: PanelAttractPulse.Sample = PanelAttractPulse.sample(framePos) ?: return
        val nodes: List<Node> = when (frame.kind) {
            PanelAttractPulse.Kind.START ->
                puzzle.graph.nodes().filter { it.modifier == Modifier.START }
            PanelAttractPulse.Kind.END ->
                puzzle.graph.nodes().filter { it.modifier == Modifier.END }
        }
        if (nodes.isEmpty()) return

        val baseRadius: Float = when (frame.kind) {
            PanelAttractPulse.Kind.START -> 4.pc
            PanelAttractPulse.Kind.END -> 2.pc
        }
        val t: Float = frame.progress
        val expand: Float = 1f - (1f - t) * (1f - t)
        val midRadius: Float = baseRadius * (0.30f + 1.25f * expand)
        val stroke: Float = baseRadius * 0.18f
        val innerRadius: Float = (midRadius - stroke).coerceAtLeast(0f)
        val outerRadius: Float = midRadius
        // Shared envelope: remaining intensity of the ring (1 = full, 0 = gone).
        val intensity: Float = frame.strength * (1f - t) * (1f - t)
        if (intensity <= 0.04f) return

        val r: Float
        val g: Float
        val b: Float
        val a: Float
        if (USE_TRANSLUCENT_PANEL_CUES) {
            // Pure white; fade is vertex alpha.
            r = 1f; g = 1f; b = 1f; a = intensity
        } else {
            // Opaque path: lerp white → panel backdrop so the ring dissolves without greying.
            val (br, bg, bb) = dyeRgb(puzzle.backgroundColor)
            r = br + (1f - br) * intensity
            g = bg + (1f - bg) * intensity
            b = bb + (1f - bb) * intensity
            a = 1f
        }

        val maxDimension: Int = maxOf(puzzle.width, puzzle.height)
        val maxScale: Float = 1f / maxDimension

        matrices.pushPose()
        matrices.scale(maxScale, maxScale, 1f)
        matrices.translate(.0, .0, CUE_Z_BIAS)

        val layerLight: Int = if (USE_TRANSLUCENT_PANEL_CUES) LightCoordsUtil.FULL_BRIGHT else light
        queue.submitCustomGeometry(matrices, panelCueLayer()) { entry, consumer ->
            withRenderContext(entry, consumer, layerLight, overlay) {
                nodes.forEach { node ->
                    ring(
                        Vector3f(node.x, node.y, 0f),
                        innerRadius,
                        outerRadius,
                        r = r,
                        g = g,
                        b = b,
                        a = a
                    )
                }
            }
        }

        matrices.popPose()
    }

    /**
     * Red blink on missed hexagon dots ([PanelErrorFlash]). Same layer switch as the attract ring.
     */
    private fun renderErrorFlash(
        puzzle: Panel,
        framePos: BlockPos,
        matrices: PoseStack,
        queue: SubmitNodeCollector,
        light: Int,
        overlay: Int
    ) {
        val frame: PanelErrorFlash.Sample = PanelErrorFlash.sample(framePos) ?: return
        if (frame.alpha <= 0.02f || frame.marks.isEmpty()) return

        val maxDimension: Int = maxOf(puzzle.width, puzzle.height)
        val maxScale: Float = 1f / maxDimension

        matrices.pushPose()
        matrices.scale(maxScale, maxScale, 1f)
        // One step in front of the symbols it recolours (-.012), the same spacing the lattice, line
        // and symbol passes already use. Seen in game: at the attract ring's depth the red square
        // visibly floated above the panel face when viewed from an angle.
        matrices.translate(.0, .0, ERROR_FLASH_Z)

        val layerLight: Int = if (USE_TRANSLUCENT_PANEL_CUES) LightCoordsUtil.FULL_BRIGHT else light
        // Translucent: alpha from blink. Opaque: solid on/off (alpha ignored).
        val a: Float = if (USE_TRANSLUCENT_PANEL_CUES) frame.alpha else 1f
        queue.submitCustomGeometry(matrices, panelCueLayer()) { entry, consumer ->
            withRenderContext(entry, consumer, layerLight, overlay) {
                // Each symbol blinks in its own shape and at its own size, so the flash reads as
                // the symbol turning red rather than a marker dropped on top of it.
                frame.marks.forEach { mark ->
                    when (mark.shape) {
                        PanelErrorFlash.Shape.HEXAGON -> hexagon(
                            Vector3f(mark.x, mark.y, 0f),
                            HEXAGON_RADIUS,
                            r = 1f, g = 0.12f, b = 0.08f, a = a
                        )

                        PanelErrorFlash.Shape.SQUARE -> roundedSquare(
                            Vector3f(mark.x, mark.y, 0f),
                            SQUARE_SIDE,
                            SQUARE_CORNER_RADIUS,
                            r = 1f, g = 0.12f, b = 0.08f, a = a
                        )
                    }
                }
            }
        }

        matrices.popPose()
    }

    private fun panelCueLayer() =
        if (USE_TRANSLUCENT_PANEL_CUES) {
            RenderTypes.entityTranslucentEmissive(PuzzlePanelTextures.solutionFill)
        } else {
            RenderTypes.text(PuzzlePanelTextures.solutionFill)
        }

    /** Dye entity RGB as 0..1 floats. */
    private fun dyeRgb(color: DyeColor): Triple<Float, Float, Float> {
        val rgb: Int = color.getTextureDiffuseColor()
        return Triple(
            ((rgb shr 16) and 0xFF) / 255f,
            ((rgb shr 8) and 0xFF) / 255f,
            (rgb and 0xFF) / 255f,
        )
    }

    /**
     * Draws each hexagon in the panel's backdrop colour, so it reads as a notch punched through
     * whatever covers it.
     *
     * A hexagon is drawn narrower than the line it marks, since at full width it would sever the
     * line and read as a broken edge. See the open question in rules/witness/04-hexagon-dots.md:
     * this is not settled, and it is the one number to change when it is.
     */
    fun renderSymbols(
        graph: ValueGraph<Node, Edge>,
        backgroundColor: DyeColor,
        width: Int,
        height: Int,
        matrices: PoseStack,
        queue: SubmitNodeCollector,
        light: Int,
        overlay: Int
    ) {
        val hexagons: List<Vector3f> = symbolPositions(graph)
        if (hexagons.isEmpty()) return
        val maxDimension: Int = maxOf(width, height)
        val maxScale: Float = 1f / maxDimension

        matrices.pushPose()
        matrices.scale(maxScale, maxScale, 1f)
        matrices.translate(.0, .0, -.012)

        val backdrop = PuzzlePanelTextures.backdrop(backgroundColor)
        queue.submitCustomGeometry(matrices, RenderTypes.text(backdrop)) { entry, consumer ->
            withRenderContext(entry, consumer, light, overlay) {
                hexagons.forEach { position -> hexagon(position, HEXAGON_RADIUS) }
            }
        }

        matrices.popPose()
    }

    /**
     * A square (rules/witness/06-colored-squares.md) is a [SQUARE_SIDE] block of its own dye colour
     * centred in its cell. It sits in the cell, never under the line, so it shares the symbols'
     * depth rather than needing one of its own; the tint rides on the white solution texture.
     */
    fun renderCellSymbols(
        symbols: List<CellSymbol>,
        width: Int,
        height: Int,
        matrices: PoseStack,
        queue: SubmitNodeCollector,
        light: Int,
        overlay: Int
    ) {
        if (symbols.isEmpty()) return
        val maxDimension: Int = maxOf(width, height)
        val maxScale: Float = 1f / maxDimension

        matrices.pushPose()
        matrices.scale(maxScale, maxScale, 1f)
        matrices.translate(.0, .0, -.012)

        queue.submitCustomGeometry(matrices, RenderTypes.text(PuzzlePanelTextures.solutionFill)) { entry, consumer ->
            withRenderContext(entry, consumer, light, overlay) {
                symbols.forEach { symbol ->
                    val (r, g, b) = dyeRgb(symbol.color)
                    when (symbol.figure) {
                        Figure.SQUARE -> roundedSquare(
                            Vector3f(symbol.x, symbol.y, 0f),
                            SQUARE_SIDE,
                            SQUARE_CORNER_RADIUS,
                            r = r, g = g, b = b
                        )
                    }
                }
            }
        }

        matrices.popPose()
    }

    /** Where every hexagon on [graph] sits: on its node, or at the midpoint of its edge. */
    private fun symbolPositions(graph: ValueGraph<Node, Edge>): List<Vector3f> {
        val nodes: List<Vector3f> = graph.nodes()
            .filter { node -> node.symbol == Atom.HEXAGON }
            .map { node -> Vector3f(node.x, node.y, 0f) }
        val edges: List<Vector3f> = graph.edges()
            .filter { side -> graph.edgeValueOf(side)?.symbol == Atom.HEXAGON }
            .map { side ->
                val u: Node = side.nodeU()
                val v: Node = side.nodeV()
                Vector3f((u.x + v.x) / 2, (u.y + v.y) / 2, 0f)
            }
        return nodes + edges
    }

    fun renderBackground(
        dyeColor: DyeColor,
        matrices: PoseStack,
        queue: SubmitNodeCollector,
        light: Int,
        overlay: Int
    ) {
        matrices.pushPose()
        val backdropTexture = PuzzlePanelTextures.backdrop(dyeColor)
        queue.submitCustomGeometry(matrices, RenderTypes.text(backdropTexture)) { entry, consumer ->
            consumer.square(entry, Vector3f(0.pc, 0.pc, 0.pc), 16.pc, light, overlay)
        }
        matrices.popPose()
    }

    private fun numberOfEdgesVisible(graph: ValueGraph<Node, Edge>, node: Node): Int =
        graph.incidentEdges(node).count { endpointPair ->
            graph.edgeValueOf(endpointPair)?.modifier !in listOf(Modifier.NONE, Modifier.HIDDEN)
        }

    private fun RenderContext.renderNode(graph: ValueGraph<Node, Edge>, node: Node): Unit = when {
        node.modifier == Modifier.START -> circle(Vector3f(node.x, node.y, 0f), 4.pc)
        // The nub's edge is drawn like any other; the node just rounds off its tip.
        node.modifier == Modifier.END -> circle(Vector3f(node.x, node.y, 0f), 2.pc)
        numberOfEdgesVisible(graph, node) > 1 -> circle(Vector3f(node.x, node.y, 0f), 2.pc)
        numberOfEdgesVisible(graph, node) == 1 -> square(Vector3f(node.x - 2.pc, node.y - 2.pc, 0f), 4.pc)
        else -> Unit
    }

    private fun RenderContext.renderEdge(graph: ValueGraph<Node, Edge>, side: EndpointPair<Node>) {
        val edge: Edge = graph.edgeValueOf(side) ?: return
        val startNode: Node = side.nodeU()
        val endNode: Node = side.nodeV()
        val start = Vector3f(startNode.x, startNode.y, 0f)
        val end = Vector3f(endNode.x, endNode.y, 0f)
        edge(start, end, 4.pc, edge)
    }

    fun renderGraph(
        graph: ValueGraph<Node, Edge>,
        width: Int,
        height: Int,
        matrices: PoseStack,
        queue: SubmitNodeCollector,
        light: Int,
        overlay: Int
    ) {
        if (graph.nodes().isEmpty()) return
        val maxDimension: Int = maxOf(width, height)
        val maxScale: Float = 1f / maxDimension

        matrices.pushPose()
        matrices.scale(maxScale, maxScale, 1f)
        matrices.translate(.0, .0, -.01)

        queue.submitCustomGeometry(matrices, RenderTypes.text(PuzzlePanelTextures.lineFill)) { entry, consumer ->
            withRenderContext(entry, consumer, light, overlay) {
                graph.nodes().forEach { node -> renderNode(graph, node) }
                graph.edges().forEach { side -> renderEdge(graph, side) }
            }
        }

        matrices.popPose()
    }

    fun renderLine(
        line: Graph<Node>,
        width: Int,
        height: Int,
        matrices: PoseStack,
        queue: SubmitNodeCollector,
        light: Int,
        overlay: Int
    ) {
        matrices.pushPose()
        if (line.nodes().isEmpty()) return matrices.popPose()
        val maxDimension: Int = maxOf(width, height)
        val maxScale: Float = 1f / maxDimension

        matrices.scale(maxScale, maxScale, 1f)
        matrices.translate(.0, .0, -.011)

        queue.submitCustomGeometry(matrices, RenderTypes.text(PuzzlePanelTextures.solutionFill)) { entry, consumer ->
            withRenderContext(entry, consumer, light, overlay) {
                line.nodes().forEach { node ->
                    // Only the start point the line was picked up from fills its whole circle. A
                    // start the line merely travels over keeps its own disc and is covered by the
                    // line width alone, so it stays visible either side of the line.
                    val pickedUp: Boolean = node.modifier == Modifier.START && line.degree(node) <= 1
                    circle(Vector3f(node.x, node.y, 0f), if (pickedUp) 4.pc else 2.pc)
                }

                line.edges().forEach { side ->
                    val startNode: Node = side.nodeU()
                    val endNode: Node = side.nodeV()
                    val start = Vector3f(startNode.x, startNode.y, 0f)
                    val end = Vector3f(endNode.x, endNode.y, 0f)
                    edge(start, end, 4.pc, Edge.NORMAL)
                }
            }
        }

        return matrices.popPose()
    }

    private fun RenderContext.edge(start: Vector3f, end: Vector3f, thickness: Float, edge: Edge) {
        fun RenderContext.`break`(
            start: Vector3f,
            end: Vector3f
        ) {
            val max: Vector3f = maxOf(start, end)
            val theta: Float = atan2(start.y - end.y, start.x - end.x)
            val halfThickness: Float = thickness / 2
            val lengthX: Float = start.x - end.x
            val lengthY: Float = start.y - end.y
            val length: Float = sqrt(lengthX.pow(2) + lengthY.pow(2)) + thickness
            val halfLength: Float = (length / 2)

            val vertices: List<Vector3f> = listOf(
                Vector3f(max).add(0f, -halfThickness, 0f),
                Vector3f(max).add(0f, halfThickness, 0f),
                Vector3f(max).add(halfLength - thickness, +halfThickness, 0f),
                Vector3f(max).add(halfLength - thickness, -halfThickness, 0f),
                Vector3f(max).add(halfLength, -halfThickness, 0f),
                Vector3f(max).add(halfLength, +halfThickness, 0f),
                Vector3f(max).add(length - thickness, +halfThickness, 0f),
                Vector3f(max).add(length - thickness, -halfThickness, 0f)
            ).map { corner ->
                val tempX: Float = corner.x - max.x
                val tempY: Float = corner.y - max.y
                val rotatedX: Float = tempX * cos(theta) - tempY * sin(theta)
                val rotatedY: Float = tempX * sin(theta) + tempY * cos(theta)
                Vector3f(rotatedX + max.x, rotatedY + max.y, max.z)
            }

            vertices.forEach { position ->
                vertexConsumer.addVertex(entry.pose(), position.x, position.y, position.z)
                    .setColor(1f, 1f, 1f, 1f)
                    .setUv(0f, 1f)
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(entry, .5f, .5f, .5f)
            }
        }

        when (edge.modifier) {
            Modifier.NONE -> {
            }
            // A start point is a node role, never an edge value. A legacy panel that stored one on
            // an edge draws as a plain segment: the disc goes, the segment stays, and it traces
            // like any other (rules/witness/01-start-points.md).
            Modifier.NORMAL, Modifier.START -> line(start, end, thickness)
            Modifier.BREAK -> `break`(start, end)
            Modifier.DOT -> {
            }
            Modifier.END -> {
            }
            Modifier.HIDDEN -> {
            }
        }
    }
}
