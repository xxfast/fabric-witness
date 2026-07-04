package com.xfastgames.witness.blocks.redstone

import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.*
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.ShapeContext
import net.minecraft.item.ItemPlacementContext
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.state.StateManager
import net.minecraft.state.property.Properties.HORIZONTAL_FACING
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView

class IronStandBlock(settings: AbstractBlock.Settings) : Block(settings) {

    companion object {
        val IDENTIFIER = Identifier.of(Witness.IDENTIFIER, "iron_stand")
        val BLOCK: Block = registerBlock(
            IronStandBlock(blockSettings(IDENTIFIER).strength(2.5f).sounds(BlockSoundGroup.METAL)),
            IDENTIFIER
        )
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER)
    }

    init {
        defaultState = stateManager.defaultState
            .with(HORIZONTAL_FACING, Direction.NORTH)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? {
        return super.getPlacementState(ctx)?.with(HORIZONTAL_FACING, ctx.horizontalPlayerFacing)
    }

    override fun appendProperties(stateManager: StateManager.Builder<Block, BlockState>) {
        stateManager.add(HORIZONTAL_FACING)
    }

    override fun getOutlineShape(
        state: BlockState?,
        world: BlockView?,
        pos: BlockPos?,
        context: ShapeContext?
    ): VoxelShape {
        val standShape: VoxelShape = VoxelShapes.cuboid(6.pc.d, 1.pc.d, 6.pc.d, 10.pc.d, 16.pc.d, 8.pc.d)
        val baseShape: VoxelShape = VoxelShapes.cuboid(3.pc.d, 0.pc.d, 5.pc.d, 13.pc.d, 1.pc.d, 11.pc.d)
        val fullShape: VoxelShape = VoxelShapes.union(standShape, baseShape)
        val direction: Direction = requireNotNull(state?.get(HORIZONTAL_FACING))
        return fullShape.rotateShape(to = direction)
    }
}
