package com.xfastgames.witness.screens.solver

import com.google.common.graph.Graph
import com.xfastgames.witness.blocks.redstone.IronPuzzleFrameBlock
import com.xfastgames.witness.entities.PuzzleFrameBlockEntity
import com.xfastgames.witness.entities.renderer.PuzzleFrameBlockRenderer.Companion.PUZZLE_FRAME_SCALE
import com.xfastgames.witness.items.PuzzlePanelItem
import com.xfastgames.witness.items.data.*
import com.xfastgames.witness.sounds.LoopingSoundInstance
import com.xfastgames.witness.sounds.WitnessSound
import com.xfastgames.witness.sounds.WitnessSounds
import com.xfastgames.witness.sounds.play
import com.xfastgames.witness.utils.*
import com.xfastgames.witness.utils.Interpolator
import kotlinx.coroutines.FlowPreview
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.client.MinecraftClient
import net.minecraft.client.Mouse
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.input.KeyInput
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.util.NarratorManager
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.decoration.ItemFrameEntity
import net.minecraft.entity.projectile.ProjectileUtil
import net.minecraft.item.ItemStack
import net.minecraft.sound.SoundCategory
import net.minecraft.state.property.Properties
import net.minecraft.util.Util
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.*
import net.minecraft.world.RaycastContext
import org.joml.Quaternionf
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW.GLFW_CURSOR_HIDDEN
import kotlin.math.hypot

private const val BORDER_WIDTH = 14
private const val DEBUG_SHOW_CURSOR_WHILE_TRACING = false
private const val CURSOR_WARP_EPSILON = 1.0
/** How close to a node's centre, in panel units, a click or a hover counts as being on it. */
private const val NODE_HIT_RADIUS = 0.25f
/** Three seconds, at 20 ticks a second: the idle bed creeps in rather than landing on the ear. */
private const val IDLE_AMBIENCE_FADE_IN_TICKS = 60
/** How long the panel goes untouched before start points start advertising themselves. */
private const val SCINT_ATTRACT_DELAY_MILLIS = 5_000L
// PuzzleFrameBlockRenderer (-0.034, -0.05) + PuzzlePanelRenderer line layer (-0.011).
private const val PUZZLE_LINE_DEPTH_FROM_BLOCK_CENTER = 0.095

@Environment(EnvType.CLIENT)
@OptIn(FlowPreview::class)
@Suppress("UnstableApiUsage")
class PuzzleSolverScreen : Screen(NarratorManager.EMPTY) {

    private val borderAlpha = Interpolator(.0f, .8f) { it.value += .05f }
    private val cursorShadowSize = Interpolator(BORDER_WIDTH * 4, BORDER_WIDTH / 2) { it.value -= 2 }
    private var startedBlockEntity: PuzzleFrameBlockEntity? = null
    private var tracingMousePosition: MousePosition? = null
    private var panelScreenBasis: PanelScreenBasis? = null
    private var pendingCursorWarp: MousePosition? = null
    private var tracingSound: LoopingSoundInstance? = null
    private var idleSound: LoopingSoundInstance? = null
    private var hoveredNode: Node? = null
    private var closing: Boolean = false
    private var lastInteractionMillis: Long = 0
    private var lastEndPointHintMillis: Long = 0

    private val solver = PuzzleSolver()
    private val clientInstance: MinecraftClient by lazy { requireNotNull(client) }
    private val mouse: Mouse by lazy { clientInstance.mouse }

    override fun init() {
        mouse.hide()
        client?.player?.play(WitnessSounds.FOCUS_MODE_ENTER)
        startIdleAmbience()
        registerInteraction()
        client?.options?.hudHidden = true
    }

    /**
     * Marks the panel as touched. Clicks and a moving line count; drifting the cursor over the
     * panel does not, or the attract cue it gates would be reset by the very move that triggers it.
     */
    private fun registerInteraction() {
        lastInteractionMillis = Util.getMeasuringTimeMs()
    }

    private val isUnattended: Boolean
        get() = Util.getMeasuringTimeMs() - lastInteractionMillis >= SCINT_ATTRACT_DELAY_MILLIS

    override fun shouldPause(): Boolean = false

    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // The solver is an in-world focus mode, so keep the panel and its surroundings visible.
        // Screen's default background blurs and darkens the world in newer Minecraft versions.
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        borderAlpha.interpolate()
        cursorShadowSize.interpolate()
        val borderAlpha: Float = borderAlpha.value
        val cursorShadowSize: Int = cursorShadowSize.value
        fill(context, BORDER_WIDTH, 0, width - BORDER_WIDTH, BORDER_WIDTH, 255f, 255f, 255f, borderAlpha)
        fill(context, BORDER_WIDTH, height - BORDER_WIDTH, width - BORDER_WIDTH, height, 255f, 255f, 255f, borderAlpha)
        fill(context, 0, 0, BORDER_WIDTH, height, 255f, 255f, 255f, borderAlpha)
        fill(context, width - BORDER_WIDTH, 0, width, height, 255f, 255f, 255f, borderAlpha)
        if (!solver.isSolving || DEBUG_SHOW_CURSOR_WHILE_TRACING) {
            circle(context, mouseX, mouseY, cursorShadowSize, 255f, 255f, 255f, .25f)
            circle(context, mouseX, mouseY, BORDER_WIDTH / 2, 255f, 255f, 255f, .9f)
        }
    }

    override fun keyPressed(input: KeyInput): Boolean {
        when (input.key()) {
            69 -> { // E 🙃
                client?.closeScreen()
                return true
            }
        }
        return super.keyPressed(input)
    }

    override fun tick() {
        hintEndPointWhileStalled()
    }

    /**
     * Shimmers the way out when a trace stalls: a line that hasn't moved for
     * [SCINT_ATTRACT_DELAY_MILLIS] is a player who has stopped knowing where to draw, so the exit
     * says where it is. Keeps repeating on that beat until they move again.
     */
    private fun hintEndPointWhileStalled() {
        if (!solver.isSolving || !isUnattended) return
        val now: Long = Util.getMeasuringTimeMs()
        if (now - lastEndPointHintMillis < SCINT_ATTRACT_DELAY_MILLIS) return
        val puzzle: Panel = startedBlockEntity?.inventory?.getStack(0)?.panel ?: return
        if (puzzle.graph.nodes().none { node -> node.modifier == Modifier.END }) return
        lastEndPointHintMillis = now
        client?.player?.play(WitnessSounds.PANEL_SCINT_ENDPOINT)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        if (!solver.isSolving) return scintillateStartPointUnderCursor(mouseX, mouseY)

        val blockEntity: PuzzleFrameBlockEntity = startedBlockEntity ?: return
        val puzzleStack: ItemStack = blockEntity.inventory.getStack(0)
        if (puzzleStack.item !is PuzzlePanelItem) return
        val puzzle: Panel = puzzleStack.panel ?: return
        val mousePosition = MousePosition(mouseX, mouseY)
        if (consumePendingCursorWarp(mousePosition)) return
        val previousMousePosition: MousePosition =
            tracingMousePosition ?: mousePosition.also { tracingMousePosition = it }
        tracingMousePosition = mousePosition
        val basis: PanelScreenBasis = panelScreenBasis ?: return
        val panelDelta: Pair<Float, Float> =
            basis.toPanelDelta(mouseX - previousMousePosition.x, mouseY - previousMousePosition.y)
                ?: return
        val wasAtFinish: Boolean = solver.isAtFinish
        val line: Graph<Node> = solver.move(puzzle, panelDelta.first, panelDelta.second) ?: return
        updateLine(blockEntity, puzzle, line)
        registerInteraction()
        // Reaching the exit is geometric, not a verdict: it chirps the same whether or not the
        // path would survive validation.
        if (solver.isAtFinish && !wasAtFinish) client?.player?.play(WitnessSounds.PANEL_PATH_COMPLETE)
        solver.tracingTip()?.let { tip -> lockCursorToLineTip(blockEntity, puzzle, tip, mousePosition) }
    }

    /**
     * Shimmers the start point under the cursor, but only once the panel has been left alone for
     * [SCINT_ATTRACT_DELAY_MILLIS]: this is an attract cue for a player who hasn't found where to
     * begin, not running commentary on the cursor. Fires on entering a node, not on every move
     * across it.
     */
    private fun scintillateStartPointUnderCursor(mouseX: Double, mouseY: Double) {
        val world: ClientWorld = client?.world ?: return
        val hitResult: PuzzlePanelHitResult? = rayCastAtPanel(world, mouseX, mouseY)
        val node: Node? = hitResult?.let { hit ->
            val (panelX: Float, panelY: Float) = hit.position
            hit.puzzlePanel.nodeAt(panelX, panelY, Modifier.START)
        }
        if (node == hoveredNode) return
        hoveredNode = node
        if (node == null || !isUnattended) return
        client?.player?.play(WitnessSounds.PANEL_SCINT_STARTPOINT)
    }

    /** The node under a panel space position, restricted to the roles in [modifiers]. */
    private fun Panel.nodeAt(panelX: Float, panelY: Float, vararg modifiers: Modifier): Node? =
        graph.nodes().find { node ->
            node.modifier in modifiers &&
                    panelX in (node.x - NODE_HIT_RADIUS)..(node.x + NODE_HIT_RADIUS) &&
                    panelY in (node.y - NODE_HIT_RADIUS)..(node.y + NODE_HIT_RADIUS)
        }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        val client: MinecraftClient = requireNotNull(client)
        val player: ClientPlayerEntity = requireNotNull(client.player)
        val world: ClientWorld = requireNotNull(client.world)

        val mouseX: Double = click.x()
        val mouseY: Double = click.y()
        val button: Int = click.button()
        registerInteraction()

        // Right-click closes the solver while idle. During a trace it releases and submits the
        // line, just like left-click, so both buttons accept only valid solutions.
        if (button == 1 && !solver.isSolving) {
            client.closeScreen()
            return true
        }

        // Left- or right-click while tracing releases the line and submits it for validation.
        if (solver.isSolving) {
            submitTrace(player)
            return true
        }

        val panelHitResult: PuzzlePanelHitResult? = rayCastAtPanel(world, mouseX, mouseY)
        if (panelHitResult == null) return missClick(player)

        val puzzlePanel: Panel = panelHitResult.puzzlePanel
        val (clickX, clickY) = panelHitResult.position
        val blockEntity: PuzzleFrameBlockEntity = panelHitResult.blockEntity

        val overNode: Node? = puzzlePanel.nodeAt(clickX, clickY, Modifier.START)

        overNode?.let {
            panelScreenBasis = calculatePanelScreenBasis(blockEntity, puzzlePanel, overNode)
                ?: return missClick(player)
            val line: Graph<Node>? = solver.startTracingLine(puzzlePanel, overNode)
            if (line == null) return missClick(player)
            updateLine(blockEntity, puzzlePanel, line)
        }

        if (overNode != null) {
            player.play(WitnessSounds.PANEL_START_TRACING)
            startTracingAmbience()
            startedBlockEntity = blockEntity
            tracingMousePosition = MousePosition(mouseX, mouseY)
            solver.tracingTip()?.let { tip ->
                lockCursorToLineTip(
                    blockEntity,
                    puzzlePanel,
                    tip,
                    MousePosition(mouseX, mouseY)
                )
            }
        } else missClick(player)

        return false
    }

    override fun removed() {
        closing = true
        client?.options?.hudHidden = false
        stopTracing()
        idleSound?.stop()
        idleSound = null
        client?.player?.play(WitnessSounds.FOCUS_MODE_EXIT)
        client?.mouse?.unlockCursor()
        super.removed()
    }

    /**
     * Releases the line. Landing on an end point is the finish gesture, and the verdict follows it;
     * releasing anywhere else drops the line without ever asking the panel.
     */
    private fun submitTrace(player: ClientPlayerEntity) {
        val blockEntity: PuzzleFrameBlockEntity = startedBlockEntity ?: return stopTracing()
        val puzzle: Panel = blockEntity.inventory.getStack(0).panel ?: return stopTracing()
        val line: Graph<Node> = solver.submit(puzzle) ?: return stopTracing()
        updateLine(blockEntity, puzzle, line)
        when (solver.state.value) {
            is PuzzleSolverData.SolutionAccepted -> {
                player.play(WitnessSounds.PANEL_FINISH_TRACING)
                player.play(WitnessSounds.PANEL_SUCCESS)
            }

            is PuzzleSolverData.SolutionRejected -> {
                player.play(WitnessSounds.PANEL_FINISH_TRACING)
                player.play(WitnessSounds.PANEL_FAILURE)
            }

            else -> player.play(WitnessSounds.PANEL_ABORT_TRACING)
        }
        // The verdict stays on the solver's state; only the screen's tracing state is dropped.
        releaseTracing()
    }

    /** Throws away a trace in progress: cancelling on the end point has its own cue. */
    private fun stopTracing() {
        if (solver.isSolving) {
            val cue: WitnessSound =
                if (solver.isAtFinish) WitnessSounds.PANEL_ABORT_FINISH_TRACING
                else WitnessSounds.PANEL_ABORT_TRACING
            client?.player?.play(cue)
        }
        solver.stopTrace()
        releaseTracing()
    }

    /** Swaps the focus mode ambience over to the actively-tracing layer. */
    private fun startTracingAmbience() {
        idleSound?.stop()
        idleSound = null
        tracingSound?.stop()
        tracingSound = LoopingSoundInstance(WitnessSounds.FOCUS_MODE_DOING, SoundCategory.AMBIENT)
            .also { it.play() }
    }

    /** The focused-but-not-drawing layer, running whenever the screen is open and no line moves. */
    private fun startIdleAmbience() {
        if (closing) return
        idleSound?.stop()
        idleSound = LoopingSoundInstance(
            WitnessSounds.FOCUS_MODE_BEING,
            SoundCategory.AMBIENT,
            fadeInTicks = IDLE_AMBIENCE_FADE_IN_TICKS
        ).also { it.play() }
    }

    private fun releaseTracing() {
        tracingSound?.stop()
        tracingSound = null
        startIdleAmbience()
        startedBlockEntity = null
        tracingMousePosition = null
        panelScreenBasis = null
        pendingCursorWarp = null
        hoveredNode = null
    }

    private fun consumePendingCursorWarp(position: MousePosition): Boolean {
        val pending: MousePosition = pendingCursorWarp ?: return false
        pendingCursorWarp = null
        if (hypot(position.x - pending.x, position.y - pending.y) > CURSOR_WARP_EPSILON) return false
        tracingMousePosition = position
        return true
    }

    private fun lockCursorToLineTip(
        blockEntity: PuzzleFrameBlockEntity,
        puzzle: Panel,
        tip: Node,
        currentPosition: MousePosition
    ) {
        val lineTipPosition: MousePosition =
            projectPanelPosition(blockEntity, puzzle, tip.x, tip.y) ?: return
        tracingMousePosition = lineTipPosition
        if (hypot(
                currentPosition.x - lineTipPosition.x,
                currentPosition.y - lineTipPosition.y
            ) <= CURSOR_WARP_EPSILON
        ) {
            pendingCursorWarp = null
            return
        }

        pendingCursorWarp = lineTipPosition
        val window = clientInstance.window
        mouse.setPosition(
            lineTipPosition.x * window.width / window.scaledWidth,
            lineTipPosition.y * window.height / window.scaledHeight,
            GLFW_CURSOR_HIDDEN
        )
    }

    private data class PanelScreenBasis(
        val xAxisX: Double,
        val xAxisY: Double,
        val yAxisX: Double,
        val yAxisY: Double
    ) {
        fun toPanelDelta(screenX: Double, screenY: Double): Pair<Float, Float>? {
            val determinant: Double = xAxisX * yAxisY - yAxisX * xAxisY
            if (!determinant.isFinite() || kotlin.math.abs(determinant) < 0.0001) return null
            val panelX: Double = (screenX * yAxisY - yAxisX * screenY) / determinant
            val panelY: Double = (xAxisX * screenY - screenX * xAxisY) / determinant
            return panelX.toFloat() to panelY.toFloat()
        }
    }

    private fun calculatePanelScreenBasis(
        blockEntity: PuzzleFrameBlockEntity,
        puzzle: Panel,
        origin: Node
    ): PanelScreenBasis? {
        val screenOrigin: MousePosition = projectPanelPosition(blockEntity, puzzle, origin.x, origin.y) ?: return null
        val screenX: MousePosition =
            projectPanelPosition(blockEntity, puzzle, origin.x + 1f, origin.y) ?: return null
        val screenY: MousePosition =
            projectPanelPosition(blockEntity, puzzle, origin.x, origin.y + 1f) ?: return null
        return PanelScreenBasis(
            xAxisX = screenX.x - screenOrigin.x,
            xAxisY = screenX.y - screenOrigin.y,
            yAxisX = screenY.x - screenOrigin.x,
            yAxisY = screenY.y - screenOrigin.y
        )
    }

    private fun projectPanelPosition(
        blockEntity: PuzzleFrameBlockEntity,
        puzzle: Panel,
        panelX: Float,
        panelY: Float
    ): MousePosition? {
        val scale: Int = maxOf(puzzle.width, puzzle.height)
        val blockX: Double = 0.5 + PUZZLE_FRAME_SCALE * (panelX / scale - 0.5)
        val blockY: Double = 0.5 + PUZZLE_FRAME_SCALE * (panelY / scale - 0.5)
        val facing: Direction = blockEntity.cachedState[Properties.HORIZONTAL_FACING]
        val localPosition: Vec3d = when (facing) {
            Direction.EAST ->
                Vec3d(0.5 - PUZZLE_LINE_DEPTH_FROM_BLOCK_CENTER, blockY, 1.0 - blockX)
            Direction.WEST ->
                Vec3d(0.5 + PUZZLE_LINE_DEPTH_FROM_BLOCK_CENTER, blockY, blockX)
            Direction.NORTH ->
                Vec3d(1.0 - blockX, blockY, 0.5 + PUZZLE_LINE_DEPTH_FROM_BLOCK_CENTER)
            Direction.SOUTH ->
                Vec3d(blockX, blockY, 0.5 - PUZZLE_LINE_DEPTH_FROM_BLOCK_CENTER)
            else -> return null
        }
        val blockPos: BlockPos = blockEntity.pos
        val projected: Vec3d = clientInstance.gameRenderer.project(
            Vec3d(
                blockPos.x + localPosition.x,
                blockPos.y + localPosition.y,
                blockPos.z + localPosition.z
            )
        )
        if (!projected.x.isFinite() || !projected.y.isFinite()) return null
        val window = clientInstance.window
        return MousePosition(
            (projected.x + 1.0) * window.scaledWidth / 2.0,
            (1.0 - projected.y) * window.scaledHeight / 2.0
        )
    }

    private fun toPanelCoordinate(blockHit: Double, scale: Int): Float =
        (scale * ((blockHit - 0.5) / PUZZLE_FRAME_SCALE + 0.5)).toFloat()
            .coerceIn(0f, scale.toFloat())

    private fun rayCastAtPanel(
        world: ClientWorld,
        mouseX: Double,
        mouseY: Double,
        tickDelta: Float = 0.0f
    ): PuzzlePanelHitResult? {
        val client: MinecraftClient = requireNotNull(client)
        val width: Int = client.window.scaledWidth
        val height: Int = client.window.scaledHeight
        val camera: Entity = requireNotNull(client.cameraEntity)
        val cameraDirection: Vec3d = camera.getRotationVec(tickDelta)
        val fov: Double = client.options.fov.value.toDouble()
        val angleSize: Double = fov / height

        val verticalRotationAxis = cameraDirection.toVector3f()
        verticalRotationAxis.cross(Vector3f(0f, 1f, 0f))

        //The camera is pointing directly up or down, you'll have to fix this one
        if (verticalRotationAxis.lengthSquared() == 0f) return null
        verticalRotationAxis.normalize()

        val horizontalRotationAxis = cameraDirection.toVector3f()
        horizontalRotationAxis.cross(verticalRotationAxis)
        horizontalRotationAxis.normalize()

        val cameraRotationAxis = cameraDirection.toVector3f()
        cameraRotationAxis.cross(horizontalRotationAxis)
        val anglePerPixel: Float = angleSize.toFloat()
        val horizontalRotation: Float = (mouseX.toFloat() - width / 2f) * anglePerPixel
        val verticalRotation: Float = (mouseY.toFloat() - height / 2f) * anglePerPixel

        val orignialCameraAxis = cameraDirection.toVector3f()
        orignialCameraAxis.rotate(Quaternionf().rotationAxis(Math.toRadians(verticalRotation.toDouble()).toFloat(), cameraRotationAxis))
        orignialCameraAxis.rotate(Quaternionf().rotationAxis(Math.toRadians(horizontalRotation.toDouble()).toFloat(), horizontalRotationAxis))
        val direction = Vec3d(orignialCameraAxis)

        val entity: Entity? = client.getCameraEntity()
        if (entity == null || client.world == null) return null
        // The old ClientPlayerInteractionManager.reachDistance/hasExtendedReach were replaced by
        // entity attributes in 1.20.5+.
        var reachDistance: Double = requireNotNull(client.player).blockInteractionRange
        val end: Vec3d = entity.getCameraPosVec(tickDelta).add(direction.multiply(reachDistance))

        var target: HitResult? = entity.entityWorld.raycast(
            RaycastContext(
                entity.getCameraPosVec(tickDelta),
                end,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                entity
            )
        )

        var tooFar = false
        var extendedReach = reachDistance
        if (reachDistance > 3.0) {
            tooFar = true
        }
        val cameraPos: Vec3d = entity.getCameraPosVec(tickDelta)
        extendedReach *= extendedReach
        if (target != null) {
            extendedReach = target.pos.squaredDistanceTo(cameraPos)
        }
        val vec3d3 = cameraPos.add(direction.multiply(reachDistance))
        val box: Box = entity
            .boundingBox
            .stretch(entity.getRotationVec(1.0f).multiply(reachDistance))
            .expand(1.0, 1.0, 1.0)

        val rayTracePredicate: (Entity) -> Boolean = { e: Entity -> !e.isSpectator && e.canHit() }
        val entityHitResult: EntityHitResult? =
            ProjectileUtil.raycast(entity, cameraPos, vec3d3, box, rayTracePredicate, extendedReach)

        // TODO: Maybe some bs that i dont actually need
        if (entityHitResult != null) {
            val entity2: Entity = entityHitResult.entity
            val vec3d4 = entityHitResult.pos
            val g = cameraPos.squaredDistanceTo(vec3d4)
            if (tooFar && g > 9.0) {
                return null
            } else if (g < extendedReach || target == null) {
                target = entityHitResult
                if (entity2 is LivingEntity || entity2 is ItemFrameEntity) {
                    client.targetedEntity = entity2
                }
            }
        }

        val hit: HitResult? = target
        // Only respond to block hit results
        if (hit == null) return null
        if (hit.type != HitResult.Type.BLOCK) return null
        if (hit !is BlockHitResult) return null

        val blockPos: BlockPos = hit.blockPos

        // Only respond if the block has an entity
        val blockEntity: BlockEntity = world.getBlockEntity(blockPos) ?: return null
        if (blockEntity !is PuzzleFrameBlockEntity) return null

        // Only respond if the entity has an panel
        val puzzleStack: ItemStack = blockEntity.inventory.getStack(0)
        val puzzlePanel: Panel = puzzleStack.panel ?: return null
        if (puzzleStack.item !is PuzzlePanelItem) return null

        // Only respond to block hit results for puzzle frames
        val blockState: BlockState = world.getBlockState(blockPos)
        val block: Block = blockState.block
        if (block !is IronPuzzleFrameBlock) return null

        // Only respond if the hit is one the face of the puzzle frame
        val facing: Direction = blockState[Properties.HORIZONTAL_FACING]
        if (hit.side != facing.opposite) return null

        // Transform to frame size
        val hitPos: Vec3d = hit.pos
        val voxelCoordinate = Vec3d(hitPos.x - blockPos.x, hitPos.y - blockPos.y, hitPos.z - blockPos.z)

        // TODO: Figure out why I cant do this transformation with rotation
        val blockHitPosition: Pair<Double, Double> = when (facing) {
            Direction.EAST -> 1 - voxelCoordinate.z to voxelCoordinate.y
            Direction.WEST -> voxelCoordinate.z to voxelCoordinate.y
            Direction.NORTH -> 1 - voxelCoordinate.x to voxelCoordinate.y
            Direction.SOUTH -> voxelCoordinate.x to voxelCoordinate.y
            else -> return null
        }

        val (blockHitX, blockHitY) = blockHitPosition

        val scale: Int = maxOf(puzzlePanel.width, puzzlePanel.height)

        // Inverse of [projectPanelPosition]: the panel is drawn centred on the block face at
        // PUZZLE_FRAME_SCALE, so block = 0.5 + PUZZLE_FRAME_SCALE * (panel / scale - 0.5).
        val clampedClickX: Float = toPanelCoordinate(blockHitX, scale)
        val clampedClickY: Float = toPanelCoordinate(blockHitY, scale)

        val position: Pair<Float, Float> = clampedClickX to clampedClickY

        return PuzzlePanelHitResult(position, puzzlePanel, blockEntity)
    }

    private fun updateLine(
        blockEntity: PuzzleFrameBlockEntity,
        puzzlePanel: Panel,
        line: Graph<Node>
    ) {
        val updatedPanel: Panel = when (puzzlePanel) {
            is Panel.Grid -> puzzlePanel.copy(line = line)
            is Panel.Tree -> puzzlePanel.copy(line = line)
            is Panel.Freeform -> puzzlePanel.copy(line = line)
        }

        val stack: ItemStack = blockEntity.inventory.getStack(0)

        val updatedStack: ItemStack = stack.copy().apply { panel = updatedPanel }

        // TODO: Synchronise inventory
        blockEntity.inventory.setStack(0, updatedStack)
    }

    private fun missClick(player: ClientPlayerEntity): Boolean {
        player.play(WitnessSounds.POINTLESS_CLICK)
        return false
    }
}
