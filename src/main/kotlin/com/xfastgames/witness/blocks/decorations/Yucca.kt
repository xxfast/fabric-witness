package com.xfastgames.witness.blocks.decorations

import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.minecraft.block.*
import net.minecraft.client.render.RenderLayer
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.World

open class Yucca : PlantBlock(FabricBlockSettings.of(Material.LEAVES).nonOpaque()), Fertilizable, Clientside {

    companion object {
        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "yucca")
        val BLOCK = registerBlock(Yucca(), IDENTIFIER)
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER, Item.Settings().group(ItemGroup.DECORATIONS))
    }

    override fun onClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(BLOCK, RenderLayer.getCutout())
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

    override fun getOffsetType(): OffsetType =
        OffsetType.XZ

    override fun getSoundGroup(state: BlockState?): BlockSoundGroup = BlockSoundGroup.GRASS

    override fun isFertilizable(world: BlockView?, pos: BlockPos?, state: BlockState?, isClient: Boolean): Boolean =
        true

    override fun canGrow(world: World?, random: java.util.Random?, pos: BlockPos?, state: BlockState?): Boolean = true

    override fun grow(world: ServerWorld, random: java.util.Random?, pos: BlockPos, state: BlockState) {
        if (state.block is Yucca) world.setBlockState(pos, TallYucca.BLOCK.defaultState)
    }

    override fun canPlantOnTop(floor: BlockState?, view: BlockView?, pos: BlockPos?): Boolean =
        floor?.isFullCube(view, pos) ?: false
}