package com.xfastgames.witness.blocks.decorations

import com.xfastgames.witness.utils.blockSettings
import net.minecraft.block.AbstractBlock
import net.minecraft.block.BlockState
import net.minecraft.block.Fertilizable
import net.minecraft.block.PlantBlock
import net.minecraft.block.ShapeContext
import net.minecraft.item.ItemStack
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.random.Random
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.World
import net.minecraft.world.WorldView

/** Settings for a small ground plant/bush. Offset is now configured on the settings (getOffsetType was removed). */
fun bushSettings(id: Identifier): AbstractBlock.Settings =
    blockSettings(id)
        .nonOpaque()
        .sounds(BlockSoundGroup.GRASS)
        .offset(AbstractBlock.OffsetType.XZ)

abstract class FlowerBush(settings: AbstractBlock.Settings) : PlantBlock(settings), Fertilizable {
    override fun getOutlineShape(
        state: BlockState?,
        view: BlockView?,
        pos: BlockPos?,
        context: ShapeContext?
    ): VoxelShape = VoxelShapes.cuboid(0.3, 0.0, 0.3, 0.7, 0.5, 0.7)

    override fun getCollisionShape(
        state: BlockState?,
        view: BlockView?,
        pos: BlockPos?,
        context: ShapeContext?
    ): VoxelShape =
        VoxelShapes.empty()

    override fun isFertilizable(world: WorldView?, pos: BlockPos?, state: BlockState?): Boolean = true

    override fun canGrow(world: World?, random: Random?, pos: BlockPos?, state: BlockState?): Boolean = true

    override fun grow(world: ServerWorld?, random: Random?, pos: BlockPos?, state: BlockState?) {
        dropStack(world, pos, ItemStack(this))
    }

    override fun canPlantOnTop(floor: BlockState?, view: BlockView?, pos: BlockPos?): Boolean =
        floor?.isFullCube(view, pos) ?: false
}
