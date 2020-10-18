package com.xfastgames.witness.blocks.redstone

import com.xfastgames.witness.Witness
import com.xfastgames.witness.entities.PuzzleFrameBlockEntity
import com.xfastgames.witness.items.PuzzlePanelItem
import com.xfastgames.witness.screens.PuzzleSolverScreen
import com.xfastgames.witness.utils.*
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.*
import net.minecraft.block.entity.BlockEntity
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.*
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.sound.SoundEvent
import net.minecraft.sound.SoundEvents
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.state.property.Properties.HORIZONTAL_FACING
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.World

class IronPuzzleFrameBlock : BlockWithEntity(
    FabricBlockSettings.of(Material.METAL)
        .strength(2.5f)
        .sounds(BlockSoundGroup.METAL)
        .lightLevel { state ->
            if (state[ENABLED]) 10 else 0
        }
) {
    object Sounds {
        val START_TRACING: SoundEvent = registerSound(Identifier(Witness.IDENTIFIER, "panel_start_tracing"))
    }

    companion object {
        val ENABLED: BooleanProperty = BooleanProperty.of("enabled")
        val TOP_CONNECTED: BooleanProperty = BooleanProperty.of("top_connected")
        val LEFT_CONNECTED: BooleanProperty = BooleanProperty.of("left_connected")
        val RIGHT_CONNECTED: BooleanProperty = BooleanProperty.of("right_connected")
        val BOTTOM_CONNECTED: BooleanProperty = BooleanProperty.of("bottom_connected")

        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "iron_puzzle_frame")
        val BLOCK: Block = registerBlock(IronPuzzleFrameBlock(), IDENTIFIER)
        val BLOCK_ITEM: BlockItem = registerBlockItem(BLOCK, IDENTIFIER, Item.Settings().group(ItemGroup.REDSTONE))
    }

    init {
        defaultState = stateManager.defaultState
            .with(HORIZONTAL_FACING, Direction.NORTH)
            .with(ENABLED, false)
            .with(TOP_CONNECTED, false)
            .with(LEFT_CONNECTED, false)
            .with(RIGHT_CONNECTED, false)
            .with(BOTTOM_CONNECTED, false)
    }

    override fun getRenderType(state: BlockState?): BlockRenderType = BlockRenderType.MODEL

    override fun createBlockEntity(world: BlockView?): BlockEntity? = PuzzleFrameBlockEntity()

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? {
        val blockBelow: Block = ctx.world.getBlockState(ctx.blockPos.below).block
        val blockAbove: Block = ctx.world.getBlockState(ctx.blockPos.above).block
        val blockNorth: Block = ctx.world.getBlockState(ctx.blockPos.north()).block
        val blockSouth: Block = ctx.world.getBlockState(ctx.blockPos.south()).block
        val blockEast: Block = ctx.world.getBlockState(ctx.blockPos.east()).block
        val blockWest: Block = ctx.world.getBlockState(ctx.blockPos.west()).block

        val blockLeft: Block =
            when (ctx.playerFacing.axis) {
                Direction.Axis.X -> when (ctx.playerFacing.direction) {
                    Direction.AxisDirection.POSITIVE -> blockNorth
                    // Direction.AxisDirection.NEGATIVE
                    else -> blockSouth
                }
                // Direction.Axis.Z
                else -> when (ctx.playerFacing.direction) {
                    Direction.AxisDirection.POSITIVE -> blockEast
                    // Direction.AxisDirection.NEGATIVE
                    else -> blockWest
                }
            }

        val blockRight: Block =
            when (ctx.playerFacing.axis) {
                Direction.Axis.X -> when (ctx.playerFacing.direction) {
                    Direction.AxisDirection.POSITIVE -> blockSouth
                    // Direction.AxisDirection.NEGATIVE
                    else -> blockNorth
                }
                // Direction.Axis.Z
                else -> when (ctx.playerFacing.direction) {
                    Direction.AxisDirection.POSITIVE -> blockWest
                    // Direction.AxisDirection.NEGATIVE
                    else -> blockEast
                }
            }

        val onTopOfFrameOrStand: Boolean = blockBelow is IronStandBlock || blockBelow is IronPuzzleFrameBlock
        val onBelowOfFrameOrStand: Boolean = blockAbove is IronStandBlock || blockAbove is IronPuzzleFrameBlock
        val onLeftOfFrame: Boolean = blockRight is IronPuzzleFrameBlock
        val onRightOfFrame: Boolean = blockLeft is IronPuzzleFrameBlock

        return super.getPlacementState(ctx)
            ?.with(HORIZONTAL_FACING, ctx.playerFacing)
            ?.with(BOTTOM_CONNECTED, onTopOfFrameOrStand)
            ?.with(TOP_CONNECTED, onBelowOfFrameOrStand)
            ?.with(LEFT_CONNECTED, onRightOfFrame)
            ?.with(RIGHT_CONNECTED, onLeftOfFrame)
    }

    override fun neighborUpdate(
        state: BlockState,
        world: World,
        pos: BlockPos,
        block: Block,
        fromPos: BlockPos,
        notify: Boolean
    ) {
        val blockUp: Block = world.getBlockState(pos.up()).block
        val blockDown: Block = world.getBlockState(pos.down()).block
        val blockEast: Block = world.getBlockState(pos.east()).block
        val blockWest: Block = world.getBlockState(pos.west()).block
        val blockNorth: Block = world.getBlockState(pos.north()).block
        val blockSouth: Block = world.getBlockState(pos.south()).block

        val isRightConnected: Boolean = when (state[HORIZONTAL_FACING]) {
            Direction.NORTH -> blockEast is IronPuzzleFrameBlock
            Direction.SOUTH -> blockWest is IronPuzzleFrameBlock
            Direction.WEST -> blockNorth is IronPuzzleFrameBlock
            Direction.EAST -> blockSouth is IronPuzzleFrameBlock
            else -> false
        }

        val isLeftConnected: Boolean = when (state[HORIZONTAL_FACING]) {
            Direction.NORTH -> blockWest is IronPuzzleFrameBlock
            Direction.SOUTH -> blockEast is IronPuzzleFrameBlock
            Direction.WEST -> blockSouth is IronPuzzleFrameBlock
            Direction.EAST -> blockNorth is IronPuzzleFrameBlock
            else -> false
        }

        val isTopConnected: Boolean = blockUp is IronPuzzleFrameBlock
        val isBottomConnected: Boolean = blockDown is IronPuzzleFrameBlock || blockDown is IronStandBlock

        world.setBlockState(
            pos, state
                .with(RIGHT_CONNECTED, isRightConnected)
                .with(LEFT_CONNECTED, isLeftConnected)
                .with(TOP_CONNECTED, isTopConnected)
                .with(BOTTOM_CONNECTED, isBottomConnected)
        )
    }

    override fun appendProperties(stateManager: StateManager.Builder<Block, BlockState>) {
        stateManager.add(HORIZONTAL_FACING)
        stateManager.add(ENABLED)
        stateManager.add(TOP_CONNECTED)
        stateManager.add(LEFT_CONNECTED)
        stateManager.add(RIGHT_CONNECTED)
        stateManager.add(BOTTOM_CONNECTED)
    }

    override fun getOutlineShape(
        state: BlockState?,
        world: BlockView?,
        pos: BlockPos?,
        context: ShapeContext?
    ): VoxelShape {
        val baseShape: VoxelShape =
            VoxelShapes.cuboid(.0, .0, .375, 1.0, 1.0, .625)
        val direction: Direction = requireNotNull(state?.get(HORIZONTAL_FACING))
        return baseShape.rotateShape(to = direction)
    }

    override fun onBreak(world: World, pos: BlockPos, state: BlockState, player: PlayerEntity) {
        val entity: BlockEntity? = world.getBlockEntity(pos)
        require(entity is PuzzleFrameBlockEntity)
        entity.inventory.items.forEach { stack -> dropStack(world, pos, stack) }
        super.onBreak(world, pos, state, player)
    }

    override fun onPlaced(
        world: World,
        pos: BlockPos?,
        state: BlockState?,
        placer: LivingEntity?,
        itemStack: ItemStack?
    ) {
        if (world.isClient) return
        val entity: BlockEntity = requireNotNull(world.getBlockEntity(pos))
        require(entity is PuzzleFrameBlockEntity)
        entity.sync()
        world.updateListeners(pos, state, state, 3)
    }

    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand?,
        hit: BlockHitResult?
    ): ActionResult {
        val entity: BlockEntity = requireNotNull(world.getBlockEntity(pos))
        require(entity is PuzzleFrameBlockEntity)
        val inventory: BlockInventory = entity.inventory
        when {
            // when there's no item in the frame, and player is holding a panel
            inventory.items[0].isEmpty &&
                    (player.mainHandStack.item is PuzzlePanelItem ||
                            player.offHandStack.item is PuzzlePanelItem) -> {
                // Only put one of the panel in the frame
                val holdingStack: ItemStack =
                    if (player.mainHandStack.item is PuzzlePanelItem) player.inventory.mainHandStack
                    else player.offHandStack

                val frameStack: ItemStack = holdingStack.split(1)
                // The rest goes back to players hand
                if (player.mainHandStack.item is PuzzlePanelItem)
                    player.setStackInHand(Hand.MAIN_HAND, holdingStack)
                else player.setStackInHand(Hand.OFF_HAND, holdingStack)

                inventory.items[0] = frameStack
                player.playSound(SoundEvents.ITEM_ARMOR_EQUIP_IRON, 1f, 1f)
            }

            // when there is an item in the frame and player is sneaking
            inventory.items[0].isNotEmpty && player.isInSneakingPose -> {
                val frameStack: ItemStack = inventory.items[0]
                when {
                    // If the player has and empty main hand
                    player.mainHandStack.isEmpty -> {
                        inventory.removeStack(0)
                        player.setStackInHand(Hand.MAIN_HAND, frameStack)
                    }

                    player.offHandStack.isEmpty -> {
                        inventory.removeStack(0)
                        player.setStackInHand(Hand.OFF_HAND, frameStack)
                    }

                    else -> ActionResult.FAIL
                }
            }

            // when there's a panel and player is not sneaking
            inventory.items[0].isNotEmpty -> {
                if (hit?.side == state[HORIZONTAL_FACING].opposite) {
                    if (world.isClient) MinecraftClient.getInstance().openScreen(PuzzleSolverScreen())
                    return ActionResult.CONSUME
                }
                return ActionResult.FAIL
            }

            else -> return ActionResult.FAIL

        }

        // Update block state
        if (inventory.items[0].isEmpty) world.setBlockState(pos, state.with(ENABLED, false))
        else world.setBlockState(pos, state.with(ENABLED, true))

        if (world.isClient) return ActionResult.SUCCESS
        entity.sync()
        world.updateListeners(pos, state, state, 3)
        return ActionResult.SUCCESS
    }
}