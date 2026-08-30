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

        /** Redstone into the base: every side but the top, which is where it goes out. */
        private fun hasInput(world: Level, pos: BlockPos): Boolean = Direction.entries.any { direction ->
            direction != Direction.UP && world.getSignal(pos.relative(direction), direction) > 0
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
            .setValue(POWERED, hasInput(ctx.level, ctx.clickedPos))

    override fun createBlockStateDefinition(stateDefinition: StateDefinition.Builder<Block, BlockState>) {
        stateDefinition.add(HORIZONTAL_FACING)
        stateDefinition.add(POWERED)
    }

    /**
     * Held as block state rather than computed on demand so a change notifies the frame above
     * through `UPDATE_ALL`; a lever on the far side of the base would not reach it otherwise.
     */
    override fun neighborChanged(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        sourceBlock: Block,
        wireOrientation: Orientation?,
        notify: Boolean
    ) {
        if (world.isClientSide) return
        val powered: Boolean = hasInput(world, pos)
        if (powered != state.getValue(POWERED)) world.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL)
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
