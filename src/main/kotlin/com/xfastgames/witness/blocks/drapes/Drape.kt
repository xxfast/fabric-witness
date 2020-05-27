package com.xfastgames.witness.blocks.drapes

import com.xfastgames.witness.utils.above
import com.xfastgames.witness.utils.neighbours
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.*
import net.minecraft.entity.EntityContext
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.state.StateManager
import net.minecraft.state.property.EnumProperty
import net.minecraft.util.StringIdentifiable
import net.minecraft.util.math.BlockPos
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

abstract class Drape :
    PlantBlock(FabricBlockSettings.of(Material.LEAVES).nonOpaque()),
    Fertilizable {

    companion object {
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

    abstract fun isDrape(block: Block): Boolean

    override fun isFertilizable(world: BlockView?, pos: BlockPos?, state: BlockState?, isClient: Boolean) = true

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>?) {
        builder?.add(PART)
    }

    override fun grow(world: ServerWorld, random: Random, pos: BlockPos, state: BlockState) {
        val positionBelow: BlockPos = pos.down(1)
        val blockStateBelow: BlockState = world.getBlockState(positionBelow)
        val blockBelow: Block = blockStateBelow.block

        when (state[PART]) {
            // When the top or middle is grown,
            DrapePart.TOP, DrapePart.MIDDLE ->
                // and if the block below a drape
                if (isDrape(blockBelow) && blockBelow is Fertilizable)
                // relay growth the lower part
                    blockBelow.grow(world, random, positionBelow, blockStateBelow)

            // When the lower is grown
            DrapePart.LOWER -> {
                // and if the block below is air
                if (blockStateBelow.isAir) {
                    // this itself become a MIDDLE, and grow another LOWER below it
                    world.setBlockState(pos, state.with(PART, DrapePart.MIDDLE))
                    world.setBlockState(positionBelow, state.with(PART, DrapePart.LOWER))
                }
            }

            // When a leaf is grown
            DrapePart.LEAF -> {
                // this itself become a top
                world.setBlockState(pos, state.with(PART, DrapePart.TOP))
                // and if the block below is air
                if (blockStateBelow.isAir)
                // grow another LOWER below it
                    world.setBlockState(positionBelow, state.with(PART, DrapePart.LOWER))
            }

            // Because java
            else -> {
            }
        }
    }

    override fun hasRandomTicks(state: BlockState?): Boolean = true

    override fun canGrow(world: World?, random: Random?, pos: BlockPos?, state: BlockState?): Boolean =
        random?.nextBoolean() ?: false

    override fun canPlaceAt(state: BlockState, world: WorldView, pos: BlockPos): Boolean {
        val blockAbove: Block = world.getBlockState(pos.above).block
        return when (state[PART]) {
            // If it is a LEAF, (i.e when placed by the user, most of the time)
            DrapePart.LEAF -> {
                // can be placed if the top is a drape
                isDrape(blockAbove) ||
                        // or any of its neighbours are opaque
                        pos.neighbours
                            .map { position -> world.getBlockState(position) }
                            .any { neighbourState -> neighbourState.isOpaque }
            }
            // TOP can be only be placed next to non opaque blocks
            DrapePart.TOP ->
                pos.neighbours
                    .map { position -> world.getBlockState(position) }
                    .any { neighbourState -> neighbourState.isOpaque }

            // MIDDLE and LOWER can only be set if the top is MIDDLE or a TOP
            DrapePart.MIDDLE, DrapePart.LOWER -> isDrape(blockAbove)

            else -> false
        }
    }

    override fun onPlaced(
        world: World,
        pos: BlockPos,
        state: BlockState,
        placer: LivingEntity?,
        itemStack: ItemStack?
    ) {
        val positionAbove: BlockPos = pos.up(1)
        val blockStateAbove: BlockState = world.getBlockState(positionAbove)
        val blockAbove: Block = blockStateAbove.block
        if (blockAbove is Drape) {
            world.setBlockState(
                pos, state.with(
                    PART,
                    DrapePart.LOWER
                )
            )
            when (blockStateAbove[PART]) {
                DrapePart.LOWER -> world.setBlockState(
                    positionAbove, blockStateAbove.with(
                        PART,
                        DrapePart.MIDDLE
                    )
                )
                DrapePart.LEAF -> world.setBlockState(
                    positionAbove, blockStateAbove.with(
                        PART,
                        DrapePart.TOP
                    )
                )
                else -> {
                }
            }
        } else super.onPlaced(world, pos, state, placer, itemStack)
    }

    override fun onBreak(world: World, pos: BlockPos, state: BlockState, player: PlayerEntity) {
        val positionAbove: BlockPos = pos.up(1)
        val blockAbove: BlockState = world.getBlockState(positionAbove)
        val positionRoot: BlockPos = positionAbove.up()
        val blockRoot: BlockState = world.getBlockState(positionRoot)

        // If there's a root, then set above to LOWER
        if (isDrape(blockRoot.block) && isDrape(blockAbove.block))
            world.setBlockState(positionAbove, blockAbove.with(PART, DrapePart.LOWER))

        // If there's no root, then set above to LEAF
        else if (isDrape(blockAbove.block))
            world.setBlockState(positionAbove, blockAbove.with(PART, DrapePart.LEAF))

        super.onBreak(world, pos, state, player)
    }

    override fun getSoundGroup(state: BlockState?): BlockSoundGroup = BlockSoundGroup.GRASS
}