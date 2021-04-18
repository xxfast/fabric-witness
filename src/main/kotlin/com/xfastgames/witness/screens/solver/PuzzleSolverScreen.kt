package com.xfastgames.witness.screens.solver

import com.google.common.graph.EndpointPair
import com.google.common.graph.Graph
import com.google.common.graph.MutableGraph
import com.google.common.graph.Traverser
import com.xfastgames.witness.Witness
import com.xfastgames.witness.blocks.redstone.IronPuzzleFrameBlock
import com.xfastgames.witness.entities.PuzzleFrameBlockEntity
import com.xfastgames.witness.entities.renderer.PuzzleFrameBlockRenderer.Companion.PUZZLE_FRAME_SCALE
import com.xfastgames.witness.items.KEY_PANEL
import com.xfastgames.witness.items.PuzzlePanelItem
import com.xfastgames.witness.items.data.*
import com.xfastgames.witness.mixin.utils.MouseAccessorMixin
import com.xfastgames.witness.screens.solver.PuzzleSolverScreen.Sounds.Instances.FOCUS_MODE_DOING_INSTANCE
import com.xfastgames.witness.sounds.LoopingSoundInstance
import com.xfastgames.witness.utils.*
import com.xfastgames.witness.utils.guava.mutableGraph
import kotlinx.coroutines.FlowPreview
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.client.MinecraftClient
import net.minecraft.client.Mouse
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.network.ClientPlayerInteractionManager
import net.minecraft.client.util.NarratorManager
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.util.math.Vector3f
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.decoration.ItemFrameEntity
import net.minecraft.entity.projectile.ProjectileUtil
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.state.property.Properties
import net.minecraft.util.Identifier
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import org.lwjgl.glfw.GLFW

private const val BORDER_WIDTH = 14
private const val CLICK_PADDING = 0.4f

@Environment(EnvType.CLIENT)
@OptIn(FlowPreview::class)
@Suppress("UnstableApiUsage")
class PuzzleSolverScreen : Screen(NarratorManager.EMPTY) {

    object Sounds {
        val POINTLESS_CLICK: SoundEvent = registerSound(Identifier(Witness.IDENTIFIER, "pointless_click"))
        val FOCUS_MODE_ENTER: SoundEvent = registerSound(Identifier(Witness.IDENTIFIER, "focus_mode_enter"))
        val FOCUS_MODE_EXIT: SoundEvent = registerSound(Identifier(Witness.IDENTIFIER, "focus_mode_exit"))
        val FOCUS_MODE_DOING: SoundEvent = registerSound(Identifier(Witness.IDENTIFIER, "focus_mode_doing"))
        val FOCUS_MODE_BEING: SoundEvent = registerSound(Identifier(Witness.IDENTIFIER, "focus_mode_being"))
        val FOCUS_MODE_CONSIDERING_EXIT: SoundEvent =
            registerSound(Identifier(Witness.IDENTIFIER, "focus_mode_considering_exit"))
        val FOCUS_MODE_WONDERING: SoundEvent =
            registerSound(Identifier(Witness.IDENTIFIER, "focus_mode_wondering"))

        object Instances {
            val FOCUS_MODE_DOING_INSTANCE = LoopingSoundInstance(FOCUS_MODE_DOING, SoundCategory.AMBIENT)
        }
    }

    private val borderAlpha = Interpolator(.0f, .8f) { it.value += .05f }
    private val cursorShadowSize = Interpolator(BORDER_WIDTH * 4, BORDER_WIDTH / 2) { it.value -= 2 }
    private var startedBlockEntity: PuzzleFrameBlockEntity? = null

    private val domain = PuzzleSolverDomain()
    private val clientInstance: MinecraftClient by lazy { requireNotNull(client) }
    private val mouse: Mouse by lazy { clientInstance.mouse }
    private val mouseAccessor: MouseAccessorMixin by lazy { requireNotNull(clientInstance.mouse as? MouseAccessorMixin) }

    override fun init(client: MinecraftClient?, width: Int, height: Int) {
        super.init(client, width, height)
        mouse.hide()
        client?.player?.playSound(Sounds.FOCUS_MODE_ENTER, 0.5f, 1f)
        client?.options?.hudHidden = true
    }

    override fun isPauseScreen(): Boolean = false

    override fun render(matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) {
        borderAlpha.interpolate()
        cursorShadowSize.interpolate()
        val borderAlpha: Float = borderAlpha.value
        val cursorShadowSize: Int = cursorShadowSize.value
        fill(matrices, BORDER_WIDTH, 0, width - BORDER_WIDTH, BORDER_WIDTH, 1f, 1f, 1f, borderAlpha)
        fill(matrices, BORDER_WIDTH, height - BORDER_WIDTH, width - BORDER_WIDTH, height, 1f, 1f, 1f, borderAlpha)
        fill(matrices, 0, 0, BORDER_WIDTH, height, 1f, 1f, 1f, borderAlpha)
        fill(matrices, width - BORDER_WIDTH, 0, width, height, 1f, 1f, 1f, borderAlpha)

        // TODO: In the witness the cursor is still rendered
        if (!domain.isSolving) {
            circle(matrices, mouseX, mouseY, cursorShadowSize, 1f, 1f, 1f, .25f)
            circle(matrices, mouseX, mouseY, BORDER_WIDTH / 2, 1f, 1f, 1f, .9f)
        }
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        when (keyCode) {
            69 -> { // E 🙃
                client?.closeScreen()
                return true
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun tick() {
    }

    private var lastKnownValidX: Double = 0.0
    private var lastKnownValidY: Double = 0.0

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        if (!domain.isSolving) return

        val blockEntity: PuzzleFrameBlockEntity = startedBlockEntity ?: return

        // Only respond if the entity has an panel
        val puzzleStack: ItemStack = blockEntity.inventory.getStack(0)
        val tag: CompoundTag = puzzleStack.tag ?: return
        val puzzle: Panel? = tag.getPanel(KEY_PANEL)
        if (puzzleStack.item !is PuzzlePanelItem) return
        if (puzzle == null) return

        // Only respond if there is a start
        val start: Node? = puzzle.line.nodes().firstOrNull { it.modifier == Modifier.START }
        if (start == null) return

        val previousLine: Graph<Node> = puzzle.line

        val world: ClientWorld = requireNotNull(client?.world)

        val hitResult: PuzzlePanelHitResult = rayCastAtPanel(world, mouseX, mouseY) ?: return
        if (hitResult.blockEntity != startedBlockEntity) return

        val puzzlePanel: Panel = hitResult.puzzlePanel
        val (clickX, clickY) = hitResult.position

        val scale: Int = maxOf(puzzlePanel.width, puzzlePanel.height)

        // TODO: Sweet spot width seems to be between 3 and 4 sizes, anything bigger or smaller seems to be slightly off
        val scaledClickX: Double = (scale * clickX) / PUZZLE_FRAME_SCALE
        val scaledClickY: Double = (scale * clickY) / PUZZLE_FRAME_SCALE

        val clampedClickX: Float = (scaledClickX.toFloat() - (PUZZLE_FRAME_SCALE / 2))
            .coerceAtLeast(0f)
            .coerceAtMost(scale.toFloat())

        val clampedClickY: Float = (scaledClickY.toFloat() - (PUZZLE_FRAME_SCALE / 2))
            .coerceAtLeast(0f)
            .coerceAtMost(scale.toFloat())

        // Get the end
        val previousEnd: Node? = previousLine.nodes().firstOrNull { it.modifier == Modifier.END }

        // If there's no end, this means this is the first end that needs to be added
        val end: Node = previousEnd
            ?: previousLine.nodes().first { it.modifier == Modifier.START }
                .copy(modifier = Modifier.END)

        val maxDimension: Int = maxOf(width, height)
        val maxScale: Float = 1f / maxDimension

        val thickness = 1.pc
        val overNode: Node? = puzzle.graph.nodes().find { node ->
            clampedClickX in (node.x - thickness)..(node.x + thickness) &&
                    clampedClickY in (node.y - thickness)..(node.y + thickness)
        }

        val mouseXRange: ClosedFloatingPointRange<Float> =
            (clampedClickX - thickness)..(clampedClickX + thickness)

        val mouseYRange: ClosedFloatingPointRange<Float> =
            (clampedClickY - thickness)..(clampedClickY + thickness)

        var xEdgeIntersection: ClosedFloatingPointRange<Float>? = null
        var yEdgeIntersection: ClosedFloatingPointRange<Float>? = null

        val overEdge: EndpointPair<Node>? = puzzle.graph.edges().firstOrNull { endpointPair ->
            val edge: Edge? = puzzle.graph.edgeValue(endpointPair).value
            if (edge in listOf(Modifier.NONE, Modifier.HIDDEN)) return@firstOrNull false
            val u: Node = endpointPair.nodeU()
            val v: Node = endpointPair.nodeV()
            val edgeXRange: ClosedFloatingPointRange<Float> = u.x..v.x
            val edgeYRange: ClosedFloatingPointRange<Float> = u.y..v.y
            xEdgeIntersection = (mouseXRange intersection edgeXRange)
            yEdgeIntersection = (mouseYRange intersection edgeYRange)
            xEdgeIntersection != null && yEdgeIntersection != null
        }

        val xIntersection: Float? = xEdgeIntersection?.mid
        val yIntersection: Float? = yEdgeIntersection?.mid

        val line: List<Node> = Traverser.forGraph(previousLine).breadthFirst(start).toList()
        val lastNode: Node? = line.lastOrNull { it.modifier != Modifier.END }
        val nodeBeforeLastNode: Node? = lastNode?.let { line.getOrNull(line.indexOf(lastNode) - 1) }

        // TODO: Buggy! Can cause next edge to connect just because they share a node
        val edgeCanBeReachedFromLastNode: Boolean =
            overEdge?.nodeU() == lastNode || overEdge?.nodeV() == lastNode

        val validatedEnd: Node =
            if (xIntersection != null && yIntersection != null && edgeCanBeReachedFromLastNode)
                end.copy(x = xIntersection, y = yIntersection)
            else end

        val updatedLine: MutableGraph<Node> = mutableGraph(previousLine)
        updatedLine.removeNode(end)
        updatedLine.addNode(validatedEnd)

        // TODO: Remove debug logic
        val debugNode: Node? = updatedLine.nodes().find { node -> node.modifier == Modifier.DOT }
        debugNode?.let { updatedLine.removeNode(it) }
        updatedLine.addNode(Node(clampedClickX, clampedClickY, Modifier.DOT))

        val overNodeCanBeReached: Boolean = overNode != null && lastNode != null &&
                puzzle.graph.hasEdgeConnecting(overNode, lastNode)

        when {
            overNode != null && lastNode != null && overNode !in line && overNodeCanBeReached -> {
                updatedLine.addNode(overNode)
                updatedLine.putEdge(overNode, validatedEnd)
                updatedLine.putEdge(overNode, lastNode)
            }

            overNode != null && lastNode != null && overNode == lastNode && nodeBeforeLastNode != null -> {
                updatedLine.removeNode(lastNode)
                updatedLine.putEdge(nodeBeforeLastNode, validatedEnd)
            }

            lastNode != null -> {
                updatedLine.putEdge(lastNode, validatedEnd)
            }
        }

        if (overNode == null && overEdge == null) {
            val windowX: Double = (lastKnownValidX / this.width) * clientInstance.window.width
            val windowY: Double = (lastKnownValidY / this.height) * clientInstance.window.height
            mouse.setPosition(x = windowX, y = windowY, state = GLFW.GLFW_CURSOR_HIDDEN)
        } else {
            lastKnownValidX = mouseX
            lastKnownValidY = mouseY
        }

        updateLine(blockEntity, puzzle, updatedLine)
    }

    // TODO: Refactor this mess to a domain with a finite state
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        println("Mouse Click X: $mouseX")
        println("Mouse X: ${mouse.x}")
        println("Screen Width: ${this.width}")
        println("Window Width: ${clientInstance.window.width}")
        val client: MinecraftClient = requireNotNull(client)
        val player: ClientPlayerEntity = requireNotNull(client.player)
        val world: ClientWorld = requireNotNull(client.world)

        // If left button, close the screen
        if (button == 1) {
            client.closeScreen()
            return true
        }

        val hitResult: PuzzlePanelHitResult? = rayCastAtPanel(world, mouseX, mouseY)

        if (hitResult == null) return missClick(player)
        val puzzlePanel: Panel = hitResult.puzzlePanel
        val (clickX, clickY) = hitResult.position
        val blockEntity: PuzzleFrameBlockEntity = hitResult.blockEntity

        val scale: Int = maxOf(puzzlePanel.width, puzzlePanel.height)

        val scaledClickX: Double = scale * clickX
        val scaledClickY: Double = scale * clickY

        val clickXRange: ClosedFloatingPointRange<Double> =
            (scaledClickX - CLICK_PADDING)..(scaledClickX + CLICK_PADDING)

        val clickYRange: ClosedFloatingPointRange<Double> =
            (scaledClickY - CLICK_PADDING)..(scaledClickY + CLICK_PADDING)

        val clickedNode: Node? = puzzlePanel.graph.nodes()
            .find { node -> node.x in clickXRange && node.y in clickYRange }
            .takeIf { it?.modifier == Modifier.START }

        clickedNode?.let {
            val line: Graph<Node>? = domain.startTracingLine(puzzlePanel, clickedNode)
            if (line == null) return missClick(player)
            updateLine(blockEntity, puzzlePanel, line)
        }

        if (clickedNode != null) {
            player.playSound(IronPuzzleFrameBlock.Sounds.START_TRACING, 1f, 1f)
            FOCUS_MODE_DOING_INSTANCE.stop()
            client.soundManager.play(FOCUS_MODE_DOING_INSTANCE)
            startedBlockEntity = blockEntity
            lastKnownValidX = mouseX
            lastKnownValidY = mouseY
        } else missClick(player)

        return false
    }

    override fun onClose() {
        client?.options?.hudHidden = false
        client?.player?.playSound(Sounds.FOCUS_MODE_EXIT, 0.5f, 1f)
        domain.stopTrace()
        FOCUS_MODE_DOING_INSTANCE.stop()
        client?.mouse?.unlockCursor()
        super.onClose()
    }

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
        val fov: Double = client.options.fov
        val angleSize: Double = fov / height

        val verticalRotationAxis = Vector3f(cameraDirection)
        verticalRotationAxis.cross(Vector3f.POSITIVE_Y)

        //The camera is pointing directly up or down, you'll have to fix this one
        if (!verticalRotationAxis.normalize()) return null

        val horizontalRotationAxis = Vector3f(cameraDirection)
        horizontalRotationAxis.cross(verticalRotationAxis)
        horizontalRotationAxis.normalize()

        val cameraRotationAxis = Vector3f(cameraDirection)
        cameraRotationAxis.cross(horizontalRotationAxis)
        val anglePerPixel: Float = angleSize.toFloat()
        val horizontalRotation: Float = (mouseX.toFloat() - width / 2f) * anglePerPixel
        val verticalRotation: Float = (mouseY.toFloat() - height / 2f) * anglePerPixel

        val orignialCameraAxis = Vector3f(cameraDirection)
        orignialCameraAxis.rotate(cameraRotationAxis.getDegreesQuaternion(verticalRotation))
        orignialCameraAxis.rotate(horizontalRotationAxis.getDegreesQuaternion(horizontalRotation))
        val direction = Vec3d(orignialCameraAxis)

        val entity: Entity? = client.getCameraEntity()
        if (entity == null || client.world == null) return null
        val interactionManager: ClientPlayerInteractionManager = requireNotNull(client.interactionManager)
        var reachDistance: Double = interactionManager.reachDistance.toDouble() //Change this to extend the reach
        val end: Vec3d = entity.getCameraPosVec(tickDelta).add(direction.multiply(reachDistance))

        var target: HitResult? = entity.world.raycast(
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
        if (client.interactionManager!!.hasExtendedReach()) {
            extendedReach = 6.0 //Change this to extend the reach
            reachDistance = extendedReach
        } else {
            if (reachDistance > 3.0) {
                tooFar = true
            }
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

        val rayTracePredicate: (Entity) -> Boolean = { e: Entity -> !e.isSpectator && e.collides() }
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
        val puzzlePanel: Panel = puzzleStack.tag?.getPanel(KEY_PANEL) ?: return null
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
        val hitPosition: Pair<Double, Double> = when (facing) {
            Direction.EAST -> 1 - voxelCoordinate.z to voxelCoordinate.y
            Direction.WEST -> voxelCoordinate.z to voxelCoordinate.y
            Direction.NORTH -> 1 - voxelCoordinate.x to voxelCoordinate.y
            Direction.SOUTH -> voxelCoordinate.x to voxelCoordinate.y
            else -> return null
        }

        return PuzzlePanelHitResult(hitPosition, puzzlePanel, blockEntity)
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

        val updatedStack: ItemStack = stack.copy().apply { tag?.putPanel(KEY_PANEL, updatedPanel) }

        // TODO: Synchronise inventory
        blockEntity.inventory.setStack(0, updatedStack)
    }

    private fun missClick(player: ClientPlayerEntity): Boolean {
        player.playSound(Sounds.POINTLESS_CLICK, 0.5f, 1f)
        return false
    }
}
