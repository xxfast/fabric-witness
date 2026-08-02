package com.xfastgames.witness.blocks.decorations

import com.xfastgames.witness.utils.blockSettings
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.BonemealableBlock
import net.minecraft.world.level.block.VegetationBlock
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.item.ItemStack
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.SoundType
import net.minecraft.resources.Identifier
import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader

/** Settings for a small ground plant/bush. Offset is now configured on the settings (getOffsetType was removed). */
fun bushSettings(id: Identifier): BlockBehaviour.Properties =
    blockSettings(id)
        .noOcclusion()
        .sound(SoundType.GRASS)
        .offsetType(BlockBehaviour.OffsetType.XZ)

abstract class FlowerBush(settings: BlockBehaviour.Properties) : VegetationBlock(settings), BonemealableBlock {
    override fun getShape(
        state: BlockState,
        view: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape = Shapes.box(0.3, 0.0, 0.3, 0.7, 0.5, 0.7)

    override fun getCollisionShape(
        state: BlockState,
        view: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape =
        Shapes.empty()

    override fun isValidBonemealTarget(world: LevelReader, pos: BlockPos, state: BlockState): Boolean = true

    override fun isBonemealSuccess(world: Level, random: RandomSource, pos: BlockPos, state: BlockState): Boolean = true

    override fun performBonemeal(world: ServerLevel, random: RandomSource, pos: BlockPos, state: BlockState) {
        Block.popResource(world, pos, ItemStack(this))
    }

    override fun mayPlaceOn(floor: BlockState, view: BlockGetter, pos: BlockPos): Boolean =
        floor.isCollisionShapeFullBlock(view, pos)
}
