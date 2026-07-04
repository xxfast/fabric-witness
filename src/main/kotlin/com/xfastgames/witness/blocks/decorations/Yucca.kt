package com.xfastgames.witness.blocks.decorations

import com.mojang.serialization.MapCodec
import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap
import net.minecraft.block.AbstractBlock
import net.minecraft.block.BlockState
import net.minecraft.block.Fertilizable
import net.minecraft.block.PlantBlock
import net.minecraft.block.ShapeContext
import net.minecraft.client.render.BlockRenderLayer
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.random.Random
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.World
import net.minecraft.world.WorldView

open class Yucca(settings: AbstractBlock.Settings) : PlantBlock(settings), Fertilizable, Clientside {

    companion object {
        val IDENTIFIER = Identifier.of(Witness.IDENTIFIER, "yucca")
        val CODEC: MapCodec<Yucca> = createCodec(::Yucca)
        val BLOCK = registerBlock(Yucca(bushSettings(IDENTIFIER)), IDENTIFIER)
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER)
    }

    override fun getCodec(): MapCodec<out PlantBlock> = CODEC

    override fun onClient() {
        BlockRenderLayerMap.putBlock(BLOCK, BlockRenderLayer.CUTOUT)
    }

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

    override fun grow(world: ServerWorld, random: Random?, pos: BlockPos, state: BlockState) {
        if (state.block is Yucca) world.setBlockState(pos, TallYucca.BLOCK.defaultState)
    }

    override fun canPlantOnTop(floor: BlockState?, view: BlockView?, pos: BlockPos?): Boolean =
        floor?.isFullCube(view, pos) ?: false
}
