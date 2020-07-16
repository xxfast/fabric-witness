package com.xfastgames.witness.blocks.redstone

import com.xfastgames.witness.Witness
import com.xfastgames.witness.entities.PuzzleFrameBlockEntity
import com.xfastgames.witness.items.PuzzleTile
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import com.xfastgames.witness.utils.rotateShape
import net.minecraft.block.*
import net.minecraft.block.entity.BlockEntity
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
import net.minecraft.world.World

class PuzzleFrameBlock :
    BlockWithEntity(
        Settings
            .of(Material.METAL)
            .strength(2.5f)
            .sounds(BlockSoundGroup.METAL)
            .lightLevel { 5 }
    ) {

    init {
        defaultState = stateManager.defaultState.with(HORIZONTAL_FACING, Direction.NORTH)
    }

    companion object {
        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "puzzle_frame")
        val BLOCK: Block = registerBlock(PuzzleFrameBlock(), IDENTIFIER)
        val BLOCK_ITEM: BlockItem = registerBlockItem(BLOCK, IDENTIFIER, Item.Settings().group(ItemGroup.REDSTONE))
    }

    override fun getRenderType(state: BlockState?): BlockRenderType = BlockRenderType.MODEL

    override fun createBlockEntity(world: BlockView?): BlockEntity? = PuzzleFrameBlockEntity()

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
        val baseShape: VoxelShape =
            VoxelShapes.cuboid(.0, .0, .375, 1.0, 1.0, .625)
        val direction: Direction = requireNotNull(state?.get(HORIZONTAL_FACING))
        return baseShape.rotateShape(to = direction)
    }

    fun putPuzzle(world: World, pos: BlockPos, puzzleTile: PuzzleTile) {
        val entity: BlockEntity = requireNotNull(world.getBlockEntity(pos))
        require(entity is PuzzleFrameBlockEntity)
    }
}