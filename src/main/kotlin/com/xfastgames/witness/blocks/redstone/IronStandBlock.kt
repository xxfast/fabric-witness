package com.xfastgames.witness.blocks.redstone

import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.*
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING
import net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.redstone.Orientation
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.level.BlockGetter

/**
 * The post a frame stands on. It carries redstone: a signal into the base from any side except
 * the top comes out of the top, into the frame above (rules/minecraft/05-puzzle-frame.md), so a
 * row of frames can be fed by dust along the ground instead of a lever behind each one.
 */
class IronStandBlock(settings: BlockBehaviour.Properties) : Block(settings) {

    companion object {
        val IDENTIFIER = Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, "iron_stand")
        val BLOCK: Block = registerBlock(
            IronStandBlock(blockSettings(IDENTIFIER).strength(2.5f).requiresCorrectToolForDrops().sound(SoundType.METAL)),
            IDENTIFIER
        )
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER)

        private const val RELAYED_SIGNAL = 15

        /**
         * Redstone into the base from outside the network: every side but the top, which is where
         * it goes out. A cable underneath feeds the stand through the walk instead
         * ([RedstoneNetwork.refresh]), so a run lit by the frame on this stand can never feed it back.
         */
        fun hasVanillaInput(world: Level, pos: BlockPos): Boolean =
            RedstoneNetwork.vanillaSignal(world, pos, Direction.entries.filter { direction -> direction != Direction.UP })

        /** Writes the stand at [at] as powered or not, clients only; returns whether it changed. */
        fun write(world: Level, at: BlockPos, powered: Boolean): Boolean {
            val state: BlockState = world.getBlockState(at)
            if (state.getValue(POWERED) == powered) return false
            world.setBlock(at, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS)
            return true
        }
    }

    init {
        registerDefaultState(stateDefinition.any()
            .setValue(HORIZONTAL_FACING, Direction.NORTH)
            .setValue(POWERED, false))
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState =
        defaultBlockState()
            .setValue(HORIZONTAL_FACING, ctx.horizontalDirection)
            .setValue(POWERED, hasVanillaInput(ctx.level, ctx.clickedPos))

    override fun createBlockStateDefinition(stateDefinition: StateDefinition.Builder<Block, BlockState>) {
        stateDefinition.add(HORIZONTAL_FACING)
        stateDefinition.add(POWERED)
    }

    /**
     * Held as block state, written by the network walk together with the frame above it, so the
     * two never disagree; a lever on the far side of the base reaches the frame through the walk.
     */
    override fun neighborChanged(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        sourceBlock: Block,
        wireOrientation: Orientation?,
        notify: Boolean
    ) {
        // The network is already settled by whichever refresh wrote it; only the world around it can change it.
        if (RedstoneNetwork.isMember(sourceBlock)) return
        RedstoneNetwork.refresh(world, pos)
    }

    override fun onPlace(state: BlockState, world: Level, pos: BlockPos, oldState: BlockState, movedByPiston: Boolean) {
        if (oldState.block !is IronStandBlock) RedstoneNetwork.refresh(world, pos)
    }

    /** A broken stand splits its network: each member that was beside it walks what is left. */
    override fun affectNeighborsAfterRemoval(state: BlockState, world: ServerLevel, pos: BlockPos, movedByPiston: Boolean) {
        super.affectNeighborsAfterRemoval(state, world, pos, movedByPiston)
        RedstoneNetwork.membersBeside(world, pos).forEach { next -> RedstoneNetwork.refresh(world, next) }
    }

    override fun isSignalSource(state: BlockState): Boolean = true

    /** Out of the top only. [direction] runs from the asker to this block, so the frame above asks with DOWN. */
    override fun getSignal(state: BlockState, world: BlockGetter, pos: BlockPos, direction: Direction): Int =
        if (direction == Direction.DOWN && state.getValue(POWERED)) RELAYED_SIGNAL else 0

    override fun getShape(
        state: BlockState,
        world: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape {
        val standShape: VoxelShape = Shapes.box(6.pc.d, 1.pc.d, 6.pc.d, 10.pc.d, 16.pc.d, 8.pc.d)
        val baseShape: VoxelShape = Shapes.box(3.pc.d, 0.pc.d, 5.pc.d, 13.pc.d, 1.pc.d, 11.pc.d)
        val fullShape: VoxelShape = Shapes.or(standShape, baseShape)
        val direction: Direction = requireNotNull(state?.getValue(HORIZONTAL_FACING))
        return fullShape.rotateShape(to = direction)
    }
}
