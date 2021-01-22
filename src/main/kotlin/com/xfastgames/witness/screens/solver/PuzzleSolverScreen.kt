package com.xfastgames.witness.screens.solver

import com.google.common.graph.MutableValueGraph
import com.google.common.graph.ValueGraph
import com.google.common.graph.ValueGraphBuilder
import com.xfastgames.witness.Witness
import com.xfastgames.witness.blocks.redstone.IronPuzzleFrameBlock
import com.xfastgames.witness.entities.PuzzleFrameBlockEntity
import com.xfastgames.witness.items.KEY_PANEL
import com.xfastgames.witness.items.PuzzlePanelItem
import com.xfastgames.witness.items.data.*
import com.xfastgames.witness.utils.*
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

private const val BORDER_WIDTH = 14
private const val CLICK_PADDING = 0.5f
private const val MAX_CURSOR_SPEED = 0.15f

@Environment(EnvType.CLIENT)
@OptIn(FlowPreview::class)
@Suppress("UnstableApiUsage")
class PuzzleSolverScreen : Screen(NarratorManager.EMPTY) {

    object Sounds {
        val POINTLESS_CLICK: SoundEvent = registerSound(Identifier(Witness.IDENTIFIER, "pointless_click"))
        val FOCUS_MODE_ENTER: SoundEvent = registerSound(Identifier(Witness.IDENTIFIER, "focus_mode_enter"))
        val FOCUS_MODE_EXIT: SoundEvent = registerSound(Identifier(Witness.IDENTIFIER, "focus_mode_exit"))
    }

    private val borderAlpha = Interpolator(.0f, .8f) { it.value += .05f }
    private val cursorShadowSize = Interpolator(BORDER_WIDTH * 4, BORDER_WIDTH / 2) { it.value -= 2 }
    private val mouseBuffer: MutableList<Pair<Double, Double>> = mutableListOf()
    private var targetBlockEntity: PuzzleFrameBlockEntity? = null

    private val domain = PuzzleSolverDomain()

    override fun init(client: MinecraftClient?, width: Int, height: Int) {
        super.init(client, width, height)
        val mouse: Mouse = requireNotNull(client?.mouse)
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

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        if (!domain.isSolving) return

        mouseBuffer.add(mouseX to mouseY)
        if (mouseBuffer.size > 2) mouseBuffer.removeAt(0)

        val mouseDeltas = mouseBuffer
            .zipSelf()
            .map { (previous, current) ->
                val (d1x, d1y) = previous
                val (d2x, d2y) = current
                (d2x - d1x) to (d2y - d1y)
            }

        if (mouseDeltas.isEmpty()) return

        val (dx: Double, dy: Double) = mouseDeltas.reduce { previous, current ->
            val (d1x, d1y) = previous
            val (d2x, d2y) = current
            (d2x + d1x) / 2 to (d2y + d1y) / 2
        }

        val blockEntity: PuzzleFrameBlockEntity = targetBlockEntity ?: return

        // Only respond if the entity has an panel
        val puzzleStack: ItemStack = blockEntity.inventory.getStack(0)
        val tag: CompoundTag = puzzleStack.tag ?: return
        val puzzle: Panel? = tag.getPanel(KEY_PANEL)
        if (puzzleStack.item !is PuzzlePanelItem) return
        if (puzzle == null) return

        // Only respond if there is a start
        val start: Node? = puzzle.line.nodes().firstOrNull { it.modifier == Modifier.START }
        if (start == null) return

        val relativeDX: Float =
            (-dx.toFloat() / puzzle.width / 2).coerceAtLeast(-MAX_CURSOR_SPEED).coerceAtMost(MAX_CURSOR_SPEED)
        val relativeDY: Float =
            (-dy.toFloat() / puzzle.height / 2).coerceAtLeast(-MAX_CURSOR_SPEED).coerceAtMost(MAX_CURSOR_SPEED)

        val updatedX: Float = (relativeDX + start.x).coerceAtLeast(0f).coerceAtMost(puzzle.width.toFloat())
        val updatedY: Float = (relativeDY + start.y).coerceAtLeast(0f).coerceAtMost(puzzle.height.toFloat())
        val movedStart: Node = start.copy(x = updatedX, y = updatedY)

        val updatedLine: MutableValueGraph<Node, Float> = ValueGraphBuilder.from(puzzle.line).build()
        updatedLine.removeNode(start)
        updatedLine.addNode(movedStart)

        updateLine(blockEntity, puzzleStack, puzzle, updatedLine)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val client: MinecraftClient = requireNotNull(client)
        val player: ClientPlayerEntity = requireNotNull(client.player)
        val world: ClientWorld = requireNotNull(client.world)

        // If left button, close the screen
        if (button == 1) {
            client.closeScreen()
            return true
        }

        val hit: HitResult? = rayCast(mouseX, mouseY)

        // Only respond to block hit results
        if (hit == null) return missClick(player)
        if (hit.type != HitResult.Type.BLOCK) return missClick(player)
        if (hit !is BlockHitResult) return missClick(player)

        val blockPos: BlockPos = hit.blockPos

        // Only respond if the block has an entity
        val blockEntity: BlockEntity = world.getBlockEntity(blockPos) ?: return missClick(player)
        if (blockEntity !is PuzzleFrameBlockEntity) return missClick(player)
        targetBlockEntity = blockEntity

        // Only respond if the entity has an panel
        val puzzleStack: ItemStack = blockEntity.inventory.getStack(0)
        val puzzlePanel: Panel? = puzzleStack.tag?.getPanel(KEY_PANEL) ?: return missClick(player)
        if (puzzleStack.item !is PuzzlePanelItem) return missClick(player)
        if (puzzlePanel == null) return missClick(player)

        // Only respond to block hit results for puzzle frames
        val blockState: BlockState = world.getBlockState(blockPos)
        val block: Block = blockState.block
        if (block !is IronPuzzleFrameBlock) return missClick(player)

        // Only respond if the hit is one the face of the puzzle frame
        val facing: Direction = blockState[Properties.HORIZONTAL_FACING]
        if (hit.side != facing.opposite) return missClick(player)

        // Transform to frame size
        val hitPos: Vec3d = hit.pos
        val voxelCoordinate = Vec3d(hitPos.x - blockPos.x, hitPos.y - blockPos.y, hitPos.z - blockPos.z)

        // TODO: Figure out why I cant do this transformation with rotation
        val (clickX, clickY) = when (facing) {
            Direction.EAST -> 1 - voxelCoordinate.z to voxelCoordinate.y
            Direction.WEST -> voxelCoordinate.z to voxelCoordinate.y
            Direction.NORTH -> 1 - voxelCoordinate.x to voxelCoordinate.y
            Direction.SOUTH -> voxelCoordinate.x to voxelCoordinate.y
            else -> return false
        }

        val scaledClickX: Double = puzzlePanel.width * clickX
        val scaledClickY: Double = puzzlePanel.height * clickY

        val clickXRange: ClosedFloatingPointRange<Double> =
            (scaledClickX - CLICK_PADDING)..(scaledClickX + CLICK_PADDING)

        val clickYRange: ClosedFloatingPointRange<Double> =
            (scaledClickY - CLICK_PADDING)..(scaledClickY + CLICK_PADDING)

        val clickedNode: Node? = puzzlePanel.graph.nodes()
            .find { node -> node.x in clickXRange && node.y in clickYRange }
            .takeIf { it?.modifier == Modifier.START }

        clickedNode?.let {
            val line: ValueGraph<Node, Float>? = domain.startTracingLine(puzzlePanel, clickedNode)
            if (line == null) return missClick(player)
            updateLine(blockEntity, puzzleStack, puzzlePanel, line)
        }

        if (clickedNode != null) player.playSound(IronPuzzleFrameBlock.Sounds.START_TRACING, 1f, 1f)
        else missClick(player)

        return false
    }

    override fun onClose() {
        client?.options?.hudHidden = false
        client?.player?.playSound(Sounds.FOCUS_MODE_EXIT, 0.5f, 1f)
        domain.stopTrace()
        super.onClose()
    }

    private fun rayCast(mouseX: Double, mouseY: Double, tickDelta: Float = 0.0f): HitResult? {
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

        return rayCastInDirection(client, tickDelta, direction)
    }

    /**
     * Refactor this
     */
    private fun rayCastInDirection(client: MinecraftClient, tickDelta: Float, direction: Vec3d): HitResult? {
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

        if (entityHitResult == null) return target

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
        return target
    }

    private fun updateLine(
        blockEntity: PuzzleFrameBlockEntity,
        stack: ItemStack,
        puzzlePanel: Panel,
        line: ValueGraph<Node, Float>
    ) {
        val updatedPanel: Panel = when (puzzlePanel) {
            is Panel.Grid -> puzzlePanel.copy(line = line)
            is Panel.Tree -> puzzlePanel.copy(line = line)
            is Panel.Freeform -> puzzlePanel.copy(line = line)
        }

        val updatedStack: ItemStack = stack.copy().apply { tag?.putPanel(KEY_PANEL, updatedPanel) }

        // TODO: Synchronise inventory
        blockEntity.inventory.setStack(0, updatedStack)
    }

    private fun missClick(player: ClientPlayerEntity): Boolean {
        player.playSound(Sounds.POINTLESS_CLICK, 0.5f, 1f)
        return false
    }
}
