package com.xfastgames.witness.blocks.decorations

import com.mojang.serialization.MapCodec
import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.BonemealableBlock
import net.minecraft.world.level.block.VegetationBlock
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.server.level.ServerLevel
import net.minecraft.resources.Identifier
import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader

open class Yucca(settings: BlockBehaviour.Properties) : VegetationBlock(settings), BonemealableBlock, Clientside {

    companion object {
        val IDENTIFIER = Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, "yucca")
        val CODEC: MapCodec<Yucca> = simpleCodec(::Yucca)
        val BLOCK = registerBlock(Yucca(bushSettings(IDENTIFIER)), IDENTIFIER)
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER)
    }

    override fun codec(): MapCodec<out VegetationBlock> = CODEC

    override fun onClient() {
    }

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
        if (state.block is Yucca) world.setBlock(pos, TallYucca.BLOCK.defaultBlockState(), Block.UPDATE_ALL)
    }

    override fun mayPlaceOn(floor: BlockState, view: BlockGetter, pos: BlockPos): Boolean =
        floor.isCollisionShapeFullBlock(view, pos)
}
