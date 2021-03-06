package com.xfastgames.witness.blocks.decorations

import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.*
import net.minecraft.item.ItemStack
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.util.math.BlockPos
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.World

abstract class FlowerBush : PlantBlock(FabricBlockSettings.of(Material.LEAVES).nonOpaque()), Fertilizable {
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

    override fun grow(world: ServerWorld?, random: java.util.Random?, pos: BlockPos?, state: BlockState?) {
        dropStack(world, pos, ItemStack(this))
    }

    override fun canPlantOnTop(floor: BlockState?, view: BlockView?, pos: BlockPos?): Boolean =
        floor?.isFullCube(view, pos) ?: false
}