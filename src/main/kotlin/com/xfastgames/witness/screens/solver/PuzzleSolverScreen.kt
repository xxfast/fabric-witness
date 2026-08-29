package com.xfastgames.witness.screens.solver

import com.google.common.graph.Graph
import com.xfastgames.witness.blocks.redstone.IronPuzzleFrameBlock
import com.xfastgames.witness.entities.PuzzleFrameBlockEntity
import com.xfastgames.witness.entities.SubmitSolutionPayload
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import com.xfastgames.witness.entities.renderer.PuzzleFrameBlockRenderer.Companion.PUZZLE_FRAME_SCALE
import com.xfastgames.witness.items.PuzzlePanelItem
import com.xfastgames.witness.items.data.*
import com.xfastgames.witness.items.renderer.PanelAttractPulse
import com.xfastgames.witness.items.renderer.PanelErrorFlash
import com.xfastgames.witness.sounds.LoopingSoundInstance
import com.xfastgames.witness.sounds.WitnessSound
import com.xfastgames.witness.sounds.WitnessSounds
import com.xfastgames.witness.sounds.play
import com.xfastgames.witness.utils.*
import com.xfastgames.witness.utils.Interpolator
import kotlinx.coroutines.FlowPreview
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.client.Minecraft
import net.minecraft.client.MouseHandler
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.chat.CommonComponents
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.item.ItemStack
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.util.Util
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.level.ClipContext
import net.minecraft.core.Direction
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.core.*
import net.minecraft.world.phys.*
import net.minecraft.util.*
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.hypot

private const val BORDER_WIDTH = 14
private const val DEBUG_SHOW_CURSOR_WHILE_TRACING = false
private const val CURSOR_WARP_EPSILON = 1.0
/** How close to a node's centre, in panel units, a click or a hover counts as being on it. */
private const val NODE_HIT_RADIUS = 0.25f
/**
 * How near a start point a click lands and still grabs it, in GUI pixels.
 *
 * About the radius of the cursor dot this screen draws, so anything the cursor visibly covers is
 * grabbable.
 */
private const val START_SNAP_RADIUS = 10.0
/** Three seconds, at 20 ticks a second: the idle bed creeps in rather than landing on the ear. */
private const val IDLE_AMBIENCE_FADE_IN_TICKS = 60
/** How long the panel goes untouched before attract cues start advertising themselves. */
private const val SCINT_ATTRACT_DELAY_MILLIS = 5_000L
/** Beat between start-point attract pings once the idle delay has elapsed. */
private const val SCINT_STARTPOINT_INTERVAL_MILLIS = 3_000L
/**
 * How many start-point attract pings fire before they fall silent. Volume steps down each time
 * (full, 2/3, 1/3) so the third is a whisper and nothing follows.
 */
private const val SCINT_STARTPOINT_MAX_PINGS = 3
// PuzzleFrameBlockRenderer (-0.034, -0.05) + PuzzlePanelRenderer line layer (-0.011).
private const val PUZZLE_LINE_DEPTH_FROM_BLOCK_CENTER = 0.095

@Environment(EnvType.CLIENT)
@OptIn(FlowPreview::class)
@Suppress("UnstableApiUsage")
class PuzzleSolverScreen(
    /**
     * Sample that opened focus mode. Attract / error cues stick to this pos (updated if the player
     * starts a trace on a different frame in the same session). Never raycast-for-focus again.
     */
    openedAt: BlockPos,
) : Screen(CommonComponents.EMPTY) {

    private val borderAlpha = Interpolator(.0f, .8f) { it.value += .05f }
    private val cursorShadowSize = Interpolator(BORDER_WIDTH * 4, BORDER_WIDTH / 2) { it.value -= 2 }
    private var startedBlockEntity: PuzzleFrameBlockEntity? = null
    private var tracingMousePosition: MousePosition? = null
    private var panelScreenBasis: PanelScreenBasis? = null
    private var pendingCursorWarp: MousePosition? = null
    private var tracingSound: LoopingSoundInstance? = null
    private var idleSound: LoopingSoundInstance? = null
    private var closing: Boolean = false
    private var lastInteractionMillis: Long = 0
    private var lastStartPointHintMillis: Long = 0
    private var startPointHintCount: Int = 0
    private var lastEndPointHintMillis: Long = 0
    /** Sticky focus frame; set at open, rebounds when a trace starts on another panel. */
    private var focusPos: BlockPos = openedAt.immutable()

    private val solver = PuzzleSolver()
    private val minecraft: Minecraft by lazy { Minecraft.getInstance() }
    private val mouse: MouseHandler by lazy { Minecraft.getInstance().mouseHandler }

    override fun init() {
        Minecraft.getInstance().mouseHandler.hide()
        minecraft?.player?.play(WitnessSounds.FOCUS_MODE_ENTER)
        startIdleAmbience()
        registerInteraction()
        minecraft?.setHudHidden(true)
    }

    /** Level BE at [focusPos], if it is still a puzzle frame with a panel. */
    private fun focusFrameEntity(): PuzzleFrameBlockEntity? {
        val world: ClientLevel = minecraft?.level ?: return null
        return world.getBlockEntity(focusPos) as? PuzzleFrameBlockEntity
    }

    private fun bindFocus(entity: PuzzleFrameBlockEntity) {
        focusPos = entity.blockPos.immutable()
    }

    /**
     * Marks the panel as touched. Clicks and a moving line count; drifting the cursor over the
     * panel does not, or the attract cue it gates would be reset by the very move that triggers it.
     * Resets the start-point ping counter so a later idle stretch gets the full three-ping fade.
     */
    private fun registerInteraction() {
        lastInteractionMillis = Util.getMillis()
        startPointHintCount = 0
    }

    private val isUnattended: Boolean
        get() = Util.getMillis() - lastInteractionMillis >= SCINT_ATTRACT_DELAY_MILLIS

    override fun isPauseScreen(): Boolean = false

    override fun extractBackground(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        // The solver is an in-world focus mode, so keep the panel and its surroundings visible.
        // Screen's default background blurs and darkens the world in newer Minecraft versions.
    }

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
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

    override fun keyPressed(input: KeyEvent): Boolean {
        when (input.key()) {
            69 -> { // E 🙃
                minecraft?.closeScreen()
                return true
            }
        }
        return super.keyPressed(input)
    }

    override fun tick() {
        hintStartPointWhileIdle()
        hintEndPointWhileStalled()
    }

    /**
     * Attract cue for where to begin. Once focus mode has been left alone for
     * [SCINT_ATTRACT_DELAY_MILLIS], pings every [SCINT_STARTPOINT_INTERVAL_MILLIS] while still idle
     * and not tracing. Volume steps down each ping and stops after [SCINT_STARTPOINT_MAX_PINGS].
     *
     * Always addresses [focusPos] (the frame that opened focus / last started a trace), never a
     * screen-centre raycast — that miss/hit noise was why pulses felt random.
     */
    private fun hintStartPointWhileIdle() {
        if (solver.isSolving || !isUnattended) return
        if (startPointHintCount >= SCINT_STARTPOINT_MAX_PINGS) return
        val now: Long = Util.getMillis()
        if (now - lastStartPointHintMillis < SCINT_STARTPOINT_INTERVAL_MILLIS) return
        val entity: PuzzleFrameBlockEntity = focusFrameEntity() ?: return
        val puzzle: Panel = entity.inventory.getItem(0).panel ?: return
        if (!puzzle.tutorial) return
        if (puzzle.graph.nodes().none { node -> node.modifier == Modifier.START }) return
        // full → 2/3 → 1/3 across the three pings, then the counter stops further plays.
        val volumeScale: Float =
            (SCINT_STARTPOINT_MAX_PINGS - startPointHintCount).toFloat() / SCINT_STARTPOINT_MAX_PINGS
        startPointHintCount++
        lastStartPointHintMillis = now
        minecraft?.player?.play(WitnessSounds.PANEL_SCINT_STARTPOINT, volumeScale)
        PanelAttractPulse.triggerStart(focusPos, volumeScale)
    }

    /**
     * Shimmers the way out when a trace stalls: a line that hasn't moved for
     * [SCINT_ATTRACT_DELAY_MILLIS] is a player who has stopped knowing where to draw, so the exit
     * says where it is. Keeps repeating on that beat until they move again.
     */
    private fun hintEndPointWhileStalled() {
        if (!solver.isSolving || !isUnattended) return
        val now: Long = Util.getMillis()
        if (now - lastEndPointHintMillis < SCINT_ATTRACT_DELAY_MILLIS) return
        val blockEntity: PuzzleFrameBlockEntity = startedBlockEntity ?: return
        val puzzle: Panel = blockEntity.inventory.getItem(0).panel ?: return
        // Attract cues are for tutorial panels only; everything else stays quiet.
        if (!puzzle.tutorial) return
        if (puzzle.graph.nodes().none { node -> node.modifier == Modifier.END }) return
        lastEndPointHintMillis = now
        minecraft?.player?.play(WitnessSounds.PANEL_SCINT_ENDPOINT)
        // Trace owner is always the focus frame once a line is started.
        PanelAttractPulse.triggerEnd(blockEntity.blockPos.immutable())
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        if (!solver.isSolving) return

        val blockEntity: PuzzleFrameBlockEntity = startedBlockEntity ?: return
        val puzzleStack: ItemStack = blockEntity.inventory.getItem(0)
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
        if (solver.isAtFinish && !wasAtFinish) minecraft?.player?.play(WitnessSounds.PANEL_PATH_COMPLETE)
        solver.tracingTip()?.let { tip -> lockCursorToLineTip(blockEntity, puzzle, tip, mousePosition) }
    }

    /** The node under a panel space position, restricted to the roles in [modifiers]. */
    private fun Panel.nodeAt(panelX: Float, panelY: Float, vararg modifiers: Modifier): Node? =
        graph.nodes().find { node ->
            node.modifier in modifiers &&
                    panelX in (node.x - NODE_HIT_RADIUS)..(node.x + NODE_HIT_RADIUS) &&
                    panelY in (node.y - NODE_HIT_RADIUS)..(node.y + NODE_HIT_RADIUS)
        }

    /**
     * The start point a click grabs: the nearest one drawn within [START_SNAP_RADIUS] pixels of the
     * cursor, or failing that whichever one the click landed dead on.
     *
     * Hit testing in panel units alone shrinks the target as the grid grows. A frame is always
     * `PUZZLE_FRAME_SCALE` wide however many cells it holds, so one panel unit is `1 / scale` of it
     * and a 9x9's start dot is half the size of a 4x4's. Measuring against the projected dot instead
     * (the exact inverse of the raycast that produced this click) keeps the grab the same size on
     * screen at any grid size and any viewing distance.
     *
     * While idle a start point is the only thing on a panel worth clicking, so snapping to the
     * nearest one can't steal a click from anything else. The panel space fallback covers a
     * projection that can't resolve, and never fires in practice for a panel the click just hit.
     */
    private fun startPointNear(
        blockEntity: PuzzleFrameBlockEntity,
        puzzle: Panel,
        mouseX: Double,
        mouseY: Double,
        panelX: Float,
        panelY: Float
    ): Node? {
        val snapped: Node? = puzzle.graph.nodes()
            .asSequence()
            .filter { node -> node.modifier == Modifier.START }
            .mapNotNull { node ->
                val position: MousePosition =
                    projectPanelPosition(blockEntity, puzzle, node.x, node.y) ?: return@mapNotNull null
                node to hypot(position.x - mouseX, position.y - mouseY)
            }
            .filter { (_, distance) -> distance <= START_SNAP_RADIUS }
            .minByOrNull { (_, distance) -> distance }
            ?.first
        return snapped ?: puzzle.nodeAt(panelX, panelY, Modifier.START)
    }

    override fun mouseClicked(click: MouseButtonEvent, doubled: Boolean): Boolean {
        val mc: Minecraft = Minecraft.getInstance()
        val player: LocalPlayer = requireNotNull(Minecraft.getInstance().player)
        val world: ClientLevel = requireNotNull(Minecraft.getInstance().level)

        val mouseX: Double = click.x()
        val mouseY: Double = click.y()
        val button: Int = click.button()
        registerInteraction()

        // Right-click closes the solver while idle. During a trace it releases and submits the
        // line, just like left-click, so both buttons accept only valid solutions.
        if (button == 1 && !solver.isSolving) {
            Minecraft.getInstance().closeScreen()
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

        val overNode: Node? = startPointNear(blockEntity, puzzlePanel, mouseX, mouseY, clickX, clickY)

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
            // Cues follow the frame the line is on, even if focus was opened on a neighbour.
            bindFocus(blockEntity)
            tracingMousePosition = MousePosition(mouseX, mouseY)
            // A new attempt supersedes the previous reject flash.
            PanelErrorFlash.clear()
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
        minecraft?.setHudHidden(false)
        stopTracing()
        idleSound?.stop()
        idleSound = null
        PanelAttractPulse.clear()
        PanelErrorFlash.clear()
        minecraft?.player?.play(WitnessSounds.FOCUS_MODE_EXIT)
        minecraft?.mouseHandler?.releaseMouse()
        super.removed()
    }

    /**
     * Releases the line. DripLandParticle on an end point is the finish gesture, and the verdict follows it;
     * releasing anywhere else drops the line without ever asking the panel.
     */
    private fun submitTrace(player: LocalPlayer) {
        val blockEntity: PuzzleFrameBlockEntity = startedBlockEntity ?: return stopTracing()
        val puzzle: Panel = blockEntity.inventory.getItem(0).panel ?: return stopTracing()
        val line: Graph<Node> = solver.submit(puzzle) ?: return stopTracing()
        updateLine(blockEntity, puzzle, line)
        val verdict: PuzzleSolverData = solver.state.value
        // The server judges the same path again before the frame, or anything downstream of it,
        // changes (rules/minecraft/05-puzzle-frame.md). The local write above is instant feedback.
        if (verdict is PuzzleSolverData.SolutionAccepted || verdict is PuzzleSolverData.SolutionRejected) {
            ClientPlayNetworking.send(SubmitSolutionPayload(blockEntity.blockPos.immutable(), solver.lastSubmittedPath))
        }
        when (verdict) {
            is PuzzleSolverData.SolutionAccepted -> {
                player.play(WitnessSounds.PANEL_FINISH_TRACING)
                player.play(WitnessSounds.PANEL_SUCCESS)
            }

            is PuzzleSolverData.SolutionRejected -> {
                player.play(WitnessSounds.PANEL_FINISH_TRACING)
                player.play(WitnessSounds.PANEL_FAILURE)
                // Tutorial only: flash the failed symbols on this frame, not every panel nearby.
                if (puzzle.tutorial && verdict.failedMarks.isNotEmpty()) {
                    PanelErrorFlash.trigger(blockEntity.blockPos.immutable(), verdict.failedMarks)
                }
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
            minecraft?.player?.play(cue)
        }
        solver.stopTrace()
        releaseTracing()
    }

    /** Swaps the focus mode ambience over to the actively-tracing layer. */
    private fun startTracingAmbience() {
        idleSound?.stop()
        idleSound = null
        tracingSound?.stop()
        tracingSound = LoopingSoundInstance(WitnessSounds.FOCUS_MODE_DOING, SoundSource.AMBIENT)
            .also { it.play() }
    }

    /** The focused-but-not-drawing layer, running whenever the screen is open and no line moves. */
    private fun startIdleAmbience() {
        if (closing) return
        idleSound?.stop()
        idleSound = LoopingSoundInstance(
            WitnessSounds.FOCUS_MODE_BEING,
            SoundSource.AMBIENT,
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
        // GUI coords → GLFW screen coords (not framebuffer; retina would 2× otherwise).
        Minecraft.getInstance().mouseHandler.setGuiPosition(lineTipPosition.x, lineTipPosition.y)
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
        val facing: Direction = blockEntity.blockState.getValue(BlockStateProperties.HORIZONTAL_FACING)
        val localPosition: Vec3 = when (facing) {
            Direction.EAST ->
                Vec3(0.5 - PUZZLE_LINE_DEPTH_FROM_BLOCK_CENTER, blockY, 1.0 - blockX)
            Direction.WEST ->
                Vec3(0.5 + PUZZLE_LINE_DEPTH_FROM_BLOCK_CENTER, blockY, blockX)
            Direction.NORTH ->
                Vec3(1.0 - blockX, blockY, 0.5 + PUZZLE_LINE_DEPTH_FROM_BLOCK_CENTER)
            Direction.SOUTH ->
                Vec3(blockX, blockY, 0.5 - PUZZLE_LINE_DEPTH_FROM_BLOCK_CENTER)
            else -> return null
        }
        val blockPos: BlockPos = blockEntity.blockPos
        val projected: Vec3 = Minecraft.getInstance().gameRenderer.projectPointToScreen(
            Vec3(
                blockPos.x + localPosition.x,
                blockPos.y + localPosition.y,
                blockPos.z + localPosition.z
            )
        )
        if (!projected.x.isFinite() || !projected.y.isFinite()) return null
        val window = Minecraft.getInstance().window
        return MousePosition(
            (projected.x + 1.0) * this.width / 2.0,
            (1.0 - projected.y) * this.height / 2.0
        )
    }

    private fun toPanelCoordinate(blockHit: Double, scale: Int): Float =
        (scale * ((blockHit - 0.5) / PUZZLE_FRAME_SCALE + 0.5)).toFloat()
            .coerceIn(0f, scale.toFloat())

    private fun rayCastAtPanel(
        world: ClientLevel,
        mouseX: Double,
        mouseY: Double,
        tickDelta: Float = 0.0f
    ): PuzzlePanelHitResult? {
        val mc: Minecraft = Minecraft.getInstance()
        val width: Int = this.width
        val height: Int = this.height
        val camera: Entity = requireNotNull(Minecraft.getInstance().cameraEntity)
        val cameraDirection: Vec3 = camera.getViewVector(tickDelta)
        val fov: Double = Minecraft.getInstance().options.fov().get().toDouble()
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
        val direction = Vec3(orignialCameraAxis)

        val entity: Entity? = Minecraft.getInstance().getCameraEntity()
        if (entity == null || Minecraft.getInstance().level == null) return null
        // The old MultiPlayerGameMode.reachDistance/hasExtendedReach were replaced by
        // entity attributes in 1.20.5+.
        var reachDistance: Double = requireNotNull(Minecraft.getInstance().player).blockInteractionRange()
        val end: Vec3 = entity.getEyePosition(tickDelta).add(direction.scale(reachDistance))

        var target: HitResult? = entity.level().clip(
            ClipContext(
                entity.getEyePosition(tickDelta),
                end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                entity
            )
        )

        var tooFar = false
        var extendedReach = reachDistance
        if (reachDistance > 3.0) {
            tooFar = true
        }
        val cameraPos: Vec3 = entity.getEyePosition(tickDelta)
        extendedReach *= extendedReach
        if (target != null) {
            extendedReach = target.location.distanceToSqr(cameraPos)
        }
        val vec3d3 = cameraPos.add(direction.scale(reachDistance))
        val box: AABB = entity
            .boundingBox
            .expandTowards(entity.getViewVector(1.0f).scale(reachDistance))
            .inflate(1.0)

        val rayTracePredicate: (Entity) -> Boolean = { e: Entity -> !e.isSpectator && e.isPickable }
        val entityHitResult: EntityHitResult? =
            ProjectileUtil.getEntityHitResult(entity, cameraPos, vec3d3, box, rayTracePredicate, extendedReach)

        // TODO: Maybe some bs that i dont actually need
        if (entityHitResult != null) {
            val entity2: Entity = entityHitResult.entity
            val vec3d4 = entityHitResult.location
            val g = cameraPos.distanceToSqr(vec3d4)
            if (tooFar && g > 9.0) {
                return null
            } else if (g < extendedReach || target == null) {
                target = entityHitResult
                if (entity2 is LivingEntity || entity2 is ItemFrame) {
                    Minecraft.getInstance().crosshairPickEntity = entity2
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
        val puzzleStack: ItemStack = blockEntity.inventory.getItem(0)
        val puzzlePanel: Panel = puzzleStack.panel ?: return null
        if (puzzleStack.item !is PuzzlePanelItem) return null

        // Only respond to block hit results for puzzle frames
        val blockState: BlockState = world.getBlockState(blockPos)
        val block: Block = blockState.block
        if (block !is IronPuzzleFrameBlock) return null

        // Only respond if the hit is one the face of the puzzle frame
        val facing: Direction = blockState.getValue(BlockStateProperties.HORIZONTAL_FACING)
        if (hit.direction != facing.opposite) return null

        // Transform to frame size
        val hitPos: Vec3 = hit.location
        val voxelCoordinate = Vec3(hitPos.x - blockPos.x, hitPos.y - blockPos.y, hitPos.z - blockPos.z)

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

        val stack: ItemStack = blockEntity.inventory.getItem(0)

        val updatedStack: ItemStack = stack.copy().apply { panel = updatedPanel }

        // TODO: Synchronise inventory
        blockEntity.inventory.setItem(0, updatedStack)
    }

    private fun missClick(player: LocalPlayer): Boolean {
        player.play(WitnessSounds.POINTLESS_CLICK)
        return false
    }
}
