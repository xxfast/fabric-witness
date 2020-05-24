package com.xfastgames.witness.blocks.flowers

import com.xfastgames.witness.utils.neighbours
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.*
import net.minecraft.entity.EntityContext
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.state.StateManager
import net.minecraft.state.property.EnumProperty
import net.minecraft.util.StringIdentifiable
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.World
import net.minecraft.world.WorldView
import java.util.*

enum class DrapePart : StringIdentifiable {
    TOP, MIDDLE, LOWER, LEAF;

    override fun asString(): String = this.name.toLowerCase()
}

class BougainvilleaDrape :
    PlantBlock(FabricBlockSettings.of(Material.LEAVES).nonOpaque()),
    Fertilizable {

    companion object {
        val BLOCK by lazy { BougainvilleaDrape() }
        val PART: EnumProperty<DrapePart> = EnumProperty.of("part", DrapePart::class.java)
    }

    init {
        defaultState = stateManager.defaultState.with(PART, DrapePart.LEAF)
    }

    override fun getCollisionShape(
        state: BlockState?,
        view: BlockView?,
        pos: BlockPos?,
        context: EntityContext?
    ): VoxelShape =
        VoxelShapes.empty()

    override fun getOutlineShape(
        state: BlockState?,
        view: BlockView?,
        pos: BlockPos?,
        context: EntityContext?
    ): VoxelShape =
        when (state?.get(PART)) {
            DrapePart.MIDDLE -> VoxelShapes.cuboid(0.2, 0.0, 0.2, 0.8, 1.0, 0.8)
            DrapePart.LOWER -> VoxelShapes.cuboid(0.2, 0.2, 0.2, 0.8, 1.0, 0.8)
            else -> VoxelShapes.cuboid(0.2, 0.0, 0.2, 0.8, 0.8, 0.8)
        }

    override fun isFertilizable(world: BlockView?, pos: BlockPos?, state: BlockState?, isClient: Boolean) = true

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>?) {
        builder?.add(PART)
    }

    override fun grow(world: ServerWorld, random: Random, pos: BlockPos, state: BlockState) {
        world.setBlockState(pos, state.with(PART, DrapePart.TOP))
        val positionBelow: BlockPos = pos.down(1)
        val blockBelow: BlockState = world.getBlockState(positionBelow)
        if (blockBelow.isAir) {
            world.setBlockState(positionBelow, state.with(PART, DrapePart.LOWER))
        }
        val positionAbove: BlockPos = pos.up(1)
        val blockAbove: BlockState = world.getBlockState(positionAbove)
        if (blockAbove.block is BougainvilleaDrape) {
            world.setBlockState(pos, blockAbove.with(PART, DrapePart.MIDDLE))
        }
    }

    override fun hasRandomTicks(state: BlockState?): Boolean = true

    override fun canGrow(world: World?, random: Random?, pos: BlockPos?, state: BlockState?): Boolean =
        random?.nextBoolean() ?: false

    override fun canPlaceAt(state: BlockState, world: WorldView, pos: BlockPos): Boolean =
        world.getBlockState(pos.up()).block is BougainvilleaDrape || pos.neighbours
            .map { position -> position to world.getBlockState(position) }
            .any { (position, state) -> state.isSideSolidFullSquare(world, position, Direction.UP) }

    override fun onBreak(world: World, pos: BlockPos, state: BlockState, player: PlayerEntity) {
        val positionAbove: BlockPos = pos.up(1)
        val blockAbove: BlockState = world.getBlockState(positionAbove)
        val positionRoot: BlockPos = positionAbove.up()
        val blockRoot: BlockState = world.getBlockState(positionRoot)

        // If there's a root, then set above to LOWER
        if (blockRoot.block is BougainvilleaDrape && blockAbove.block is BougainvilleaDrape)
            world.setBlockState(positionAbove, blockAbove.with(PART, DrapePart.LOWER))

        // If there's no root, then set above to LEAF
        else if (blockAbove.block is BougainvilleaDrape)
            world.setBlockState(positionAbove, blockAbove.with(PART, DrapePart.LEAF))

        super.onBreak(world, pos, state, player)
    }
}