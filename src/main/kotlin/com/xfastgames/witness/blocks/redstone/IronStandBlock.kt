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
import net.minecraft.resources.Identifier
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.level.BlockGetter

class IronStandBlock(settings: BlockBehaviour.Properties) : Block(settings) {

    companion object {
        val IDENTIFIER = Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, "iron_stand")
        val BLOCK: Block = registerBlock(
            IronStandBlock(blockSettings(IDENTIFIER).strength(2.5f).sound(SoundType.METAL)),
            IDENTIFIER
        )
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER)
    }

    init {
        registerDefaultState(stateDefinition.any()
            .setValue(HORIZONTAL_FACING, Direction.NORTH))
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState {
        return defaultBlockState().setValue(HORIZONTAL_FACING, ctx.horizontalDirection)
    }

    override fun createBlockStateDefinition(stateDefinition: StateDefinition.Builder<Block, BlockState>) {
        stateDefinition.add(HORIZONTAL_FACING)
    }

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
