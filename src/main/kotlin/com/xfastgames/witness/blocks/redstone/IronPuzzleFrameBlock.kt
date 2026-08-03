package com.xfastgames.witness.blocks.redstone

import com.mojang.serialization.MapCodec
import com.xfastgames.witness.Witness
import com.xfastgames.witness.entities.PuzzleFrameBlockEntity
import com.xfastgames.witness.items.PuzzlePanelItem
import com.xfastgames.witness.screens.solver.PuzzleSolverScreen
import com.xfastgames.witness.utils.BlockInventory
import com.xfastgames.witness.utils.blockSettings
import com.xfastgames.witness.utils.d
import com.xfastgames.witness.utils.pc
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import com.xfastgames.witness.utils.rotateShape
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

class IronPuzzleFrameBlock(settings: BlockBehaviour.Properties) : BaseEntityBlock(settings) {
    companion object {
        val ENABLED: BooleanProperty = BooleanProperty.create("enabled")
        val TOP_CONNECTED: BooleanProperty = BooleanProperty.create("top_connected")
        val LEFT_CONNECTED: BooleanProperty = BooleanProperty.create("left_connected")
        val RIGHT_CONNECTED: BooleanProperty = BooleanProperty.create("right_connected")
        val BOTTOM_CONNECTED: BooleanProperty = BooleanProperty.create("bottom_connected")

        val IDENTIFIER = Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, "iron_puzzle_frame")
        val CODEC: MapCodec<IronPuzzleFrameBlock> = simpleCodec(::IronPuzzleFrameBlock)
        val BLOCK: Block = registerBlock(
            IronPuzzleFrameBlock(
                blockSettings(IDENTIFIER)
                    .strength(2.5f)
                    .sound(SoundType.METAL)
                    .lightLevel { state: BlockState -> if (state.getValue(ENABLED)) 10 else 0 }
            ),
            IDENTIFIER
        )
        val BLOCK_ITEM: BlockItem = registerBlockItem(BLOCK, IDENTIFIER)

        /**
         * Left-click retrieval, mirroring vanilla item frames: attacking a loaded frame pops the
         * panel out and leaves the frame standing, so the frame only starts breaking once it is
         * empty. Wired to `AttackBlockCallback` in [com.xfastgames.witness.Witness.onInitialize].
         */
        fun retrieveOnAttack(player: Player, world: Level, pos: BlockPos): InteractionResult {
            // Cheap state check first, this runs for every block the player left-clicks.
            val state: BlockState = world.getBlockState(pos)
            if (state.block !is IronPuzzleFrameBlock) return InteractionResult.PASS

            val entity: BlockEntity = world.getBlockEntity(pos) ?: return InteractionResult.PASS
            if (entity !is PuzzleFrameBlockEntity) return InteractionResult.PASS
            // An empty frame breaks normally, same as an empty item frame.
            if (entity.inventory.items[0].isEmpty) return InteractionResult.PASS

            // removeItemNoUpdate hands back the stack itself. removeItem(slot, amount) would
            // split it, which empties the stack still sitting in the slot list.
            val frameStack: ItemStack = entity.inventory.removeItemNoUpdate(0)

            // ItemFrame.dropItem swallows the drop for creative players. popResource is already
            // server-side only and respects the blockDrops gamerule.
            if (!player.hasInfiniteMaterials())
                Block.popResourceFromFace(world, pos, state.getValue(HORIZONTAL_FACING).opposite, frameStack)

            world.playSound(player, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1f, 1f)
            world.setBlock(pos, state.setValue(ENABLED, false), Block.UPDATE_ALL)
            return InteractionResult.SUCCESS
        }
    }

    init {
        registerDefaultState(stateDefinition.any()
            .setValue(HORIZONTAL_FACING, Direction.NORTH)
            .setValue(ENABLED, false)
            .setValue(TOP_CONNECTED, false)
            .setValue(LEFT_CONNECTED, false)
            .setValue(RIGHT_CONNECTED, false)
            .setValue(BOTTOM_CONNECTED, false))
    }

    override fun codec(): MapCodec<out BaseEntityBlock> = CODEC

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        PuzzleFrameBlockEntity(pos, state)

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState? {
        val blockBelow: Block = ctx.level.getBlockState(ctx.clickedPos.below()).block
        val blockAbove: Block = ctx.level.getBlockState(ctx.clickedPos.above()).block
        val blockNorth: Block = ctx.level.getBlockState(ctx.clickedPos.north()).block
        val blockSouth: Block = ctx.level.getBlockState(ctx.clickedPos.south()).block
        val blockEast: Block = ctx.level.getBlockState(ctx.clickedPos.east()).block
        val blockWest: Block = ctx.level.getBlockState(ctx.clickedPos.west()).block

        val blockLeft: Block =
            when (ctx.horizontalDirection.axis) {
                Direction.Axis.X -> when (ctx.horizontalDirection.axisDirection) {
                    Direction.AxisDirection.POSITIVE -> blockNorth
                    else -> blockSouth
                }
                else -> when (ctx.horizontalDirection.axisDirection) {
                    Direction.AxisDirection.POSITIVE -> blockEast
                    else -> blockWest
                }
            }

        val blockRight: Block =
            when (ctx.horizontalDirection.axis) {
                Direction.Axis.X -> when (ctx.horizontalDirection.axisDirection) {
                    Direction.AxisDirection.POSITIVE -> blockSouth
                    else -> blockNorth
                }
                else -> when (ctx.horizontalDirection.axisDirection) {
                    Direction.AxisDirection.POSITIVE -> blockWest
                    else -> blockEast
                }
            }

        val onTopOfFrameOrStand: Boolean = blockBelow is IronStandBlock || blockBelow is IronPuzzleFrameBlock
        val onBelowOfFrameOrStand: Boolean = blockAbove is IronStandBlock || blockAbove is IronPuzzleFrameBlock
        val onLeftOfFrame: Boolean = blockRight is IronPuzzleFrameBlock
        val onRightOfFrame: Boolean = blockLeft is IronPuzzleFrameBlock

        return super.getStateForPlacement(ctx)
            ?.setValue(HORIZONTAL_FACING, ctx.horizontalDirection)
            ?.setValue(BOTTOM_CONNECTED, onTopOfFrameOrStand)
            ?.setValue(TOP_CONNECTED, onBelowOfFrameOrStand)
            ?.setValue(LEFT_CONNECTED, onRightOfFrame)
            ?.setValue(RIGHT_CONNECTED, onLeftOfFrame)
    }

    override fun neighborChanged(state: BlockState, world: Level, pos: BlockPos, sourceBlock: Block, wireOrientation: net.minecraft.world.level.redstone.Orientation?, notify: Boolean) {
        val blockUp: Block = world.getBlockState(pos.above()).block
        val blockDown: Block = world.getBlockState(pos.below()).block
        val blockEast: Block = world.getBlockState(pos.east()).block
        val blockWest: Block = world.getBlockState(pos.west()).block
        val blockNorth: Block = world.getBlockState(pos.north()).block
        val blockSouth: Block = world.getBlockState(pos.south()).block

        val isRightConnected: Boolean = when (state.getValue(HORIZONTAL_FACING)) {
            Direction.NORTH -> blockEast is IronPuzzleFrameBlock
            Direction.SOUTH -> blockWest is IronPuzzleFrameBlock
            Direction.WEST -> blockNorth is IronPuzzleFrameBlock
            Direction.EAST -> blockSouth is IronPuzzleFrameBlock
            else -> false
        }

        val isLeftConnected: Boolean = when (state.getValue(HORIZONTAL_FACING)) {
            Direction.NORTH -> blockWest is IronPuzzleFrameBlock
            Direction.SOUTH -> blockEast is IronPuzzleFrameBlock
            Direction.WEST -> blockSouth is IronPuzzleFrameBlock
            Direction.EAST -> blockNorth is IronPuzzleFrameBlock
            else -> false
        }

        val isTopConnected: Boolean = blockUp is IronPuzzleFrameBlock
        val isBottomConnected: Boolean = blockDown is IronPuzzleFrameBlock || blockDown is IronStandBlock

        world.setBlock(
            pos, state
                .setValue(RIGHT_CONNECTED, isRightConnected)
                .setValue(LEFT_CONNECTED, isLeftConnected)
                .setValue(TOP_CONNECTED, isTopConnected)
                .setValue(BOTTOM_CONNECTED, isBottomConnected),
            Block.UPDATE_ALL
        )
    }

    override fun createBlockStateDefinition(stateDefinition: StateDefinition.Builder<Block, BlockState>) {
        stateDefinition.add(HORIZONTAL_FACING)
        stateDefinition.add(ENABLED)
        stateDefinition.add(TOP_CONNECTED)
        stateDefinition.add(LEFT_CONNECTED)
        stateDefinition.add(RIGHT_CONNECTED)
        stateDefinition.add(BOTTOM_CONNECTED)
    }

    override fun getShape(
        state: BlockState,
        world: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape {
        val shape: VoxelShape = Shapes.or(
            Shapes.box(1.pc.d, 1.pc.d, 6.pc.d, 15.pc.d, 15.pc.d, 9.pc.d),
            Shapes.box(0.pc.d, 1.pc.d, 8.pc.d, 1.pc.d, 16.pc.d, 10.pc.d),
            Shapes.box(0.pc.d, 0.pc.d, 8.pc.d, 16.pc.d, 1.pc.d, 10.pc.d),
            Shapes.box(0.pc.d, 15.pc.d, 8.pc.d, 16.pc.d, 16.pc.d, 10.pc.d),
            Shapes.box(15.pc.d, 1.pc.d, 8.pc.d, 16.pc.d, 16.pc.d, 10.pc.d),
        )

        val direction: Direction = requireNotNull(state.getValue(HORIZONTAL_FACING))
        return shape.rotateShape(to = direction)
    }

    override fun playerWillDestroy(world: Level, pos: BlockPos, state: BlockState, player: Player): BlockState {
        val entity: BlockEntity? = world.getBlockEntity(pos)
        require(entity is PuzzleFrameBlockEntity)
        entity.inventory.items.forEach { stack -> Block.popResource(world, pos, stack) }
        return super.playerWillDestroy(world, pos, state, player)
    }

    override fun setPlacedBy(
        world: Level,
        pos: BlockPos,
        state: BlockState,
        placer: LivingEntity?,
        itemStack: ItemStack
    ) {
        if (world.isClientSide) return
        val entity: BlockEntity = requireNotNull(world.getBlockEntity(pos))
        require(entity is PuzzleFrameBlockEntity)
        entity.sync()
        world.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL)
    }

    override fun useWithoutItem(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
        hit: BlockHitResult
    ): InteractionResult = interact(state, world, pos, player, hit)

    override fun useItemOn(
        stack: ItemStack,
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult = interact(state, world, pos, player, hit)

    private fun interact(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
        hit: BlockHitResult?
    ): InteractionResult {
        val entity: BlockEntity = requireNotNull(world.getBlockEntity(pos))
        require(entity is PuzzleFrameBlockEntity)
        val inventory: BlockInventory = entity.inventory
        when {
            // when there's no item in the frame, and player is holding a panel
            inventory.items[0].isEmpty &&
                    (player.mainHandItem.item is PuzzlePanelItem ||
                            player.offhandItem.item is PuzzlePanelItem) -> {
                val holdingStack: ItemStack =
                    if (player.mainHandItem.item is PuzzlePanelItem) player.mainHandItem
                    else player.offhandItem

                val frameStack: ItemStack = holdingStack.split(1)
                if (player.mainHandItem.item is PuzzlePanelItem)
                    player.setItemInHand(InteractionHand.MAIN_HAND, holdingStack)
                else player.setItemInHand(InteractionHand.OFF_HAND, holdingStack)

                inventory.items[0] = frameStack
                player.playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1f, 1f)
            }

            // when there is an item in the frame and player is sneaking
            !inventory.items[0].isEmpty && player.isShiftKeyDown -> {
                val freeHand: InteractionHand = when {
                    player.mainHandItem.isEmpty -> InteractionHand.MAIN_HAND
                    player.offhandItem.isEmpty -> InteractionHand.OFF_HAND
                    else -> return InteractionResult.FAIL
                }

                // removeItemNoUpdate hands back the stack itself. removeItem(slot, amount) would
                // split it, which empties the stack still sitting in the slot list.
                val frameStack: ItemStack = inventory.removeItemNoUpdate(0)
                player.setItemInHand(freeHand, frameStack)
            }

            // when there's a panel and player is not sneaking
            !inventory.items[0].isEmpty -> {
                if (hit?.direction == state.getValue(HORIZONTAL_FACING).opposite) {
                    // Pass the frame pos so focus-mode cues (attract / error flash) stick to
                    // this panel instead of whatever screen-centre raycast happens to hit.
                    if (world.isClientSide) {
                        Minecraft.getInstance().gui.setScreen(PuzzleSolverScreen(pos.immutable()))
                    }
                    return InteractionResult.CONSUME
                }
                return InteractionResult.FAIL
            }

            else -> return InteractionResult.FAIL
        }

        // Update block state
        if (inventory.items[0].isEmpty) world.setBlock(pos, state.setValue(ENABLED, false), Block.UPDATE_ALL)
        else world.setBlock(pos, state.setValue(ENABLED, true), Block.UPDATE_ALL)

        if (world.isClientSide) return InteractionResult.SUCCESS
        entity.sync()
        world.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL)
        return InteractionResult.SUCCESS
    }
}
