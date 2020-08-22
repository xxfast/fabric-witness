package com.xfastgames.witness.blocks.redstone

import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.pcD
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import com.xfastgames.witness.utils.rotateShape
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Material
import net.minecraft.block.ShapeContext
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
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

class IronStandBlock : Block(
    FabricBlockSettings.of(Material.METAL)
        .strength(2.5f)
        .sounds(BlockSoundGroup.METAL)
) {

    companion object {
        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "iron_stand")
        val BLOCK: Block = registerBlock(IronStandBlock(), IDENTIFIER)
        val BLOCK_ITEM: BlockItem = registerBlockItem(BLOCK, IDENTIFIER, Item.Settings().group(ItemGroup.REDSTONE))
    }

    init {
        defaultState = stateManager.defaultState
            .with(HORIZONTAL_FACING, Direction.NORTH)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? {
        return super.getPlacementState(ctx)?.with(HORIZONTAL_FACING, ctx.playerFacing)
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
        val standShape: VoxelShape = VoxelShapes.cuboid(6.pcD, 1.pcD, 6.pcD, 10.pcD, 16.pcD, 8.pcD)
        val baseShape: VoxelShape = VoxelShapes.cuboid(3.pcD, 0.pcD, 5.pcD, 13.pcD, 1.pcD, 11.pcD)
        val fullShape: VoxelShape = VoxelShapes.union(standShape, baseShape)
        val direction: Direction = requireNotNull(state?.get(HORIZONTAL_FACING))
        return fullShape.rotateShape(to = direction)
    }
}