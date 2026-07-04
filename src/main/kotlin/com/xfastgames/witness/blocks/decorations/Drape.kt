package com.xfastgames.witness.blocks.decorations

import com.xfastgames.witness.utils.above
import com.xfastgames.witness.utils.blockSettings
import com.xfastgames.witness.utils.neighbours
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Fertilizable
import net.minecraft.block.PlantBlock
import net.minecraft.block.ShapeContext
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.state.StateManager
import net.minecraft.state.property.EnumProperty
import net.minecraft.util.Identifier
import net.minecraft.util.StringIdentifiable
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.random.Random
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.World
import net.minecraft.world.WorldView
import java.util.Locale

enum class DrapePart : StringIdentifiable {
    TOP, MIDDLE, LOWER, LEAF;

    override fun asString(): String = this.name.lowercase(Locale.getDefault())
}

fun drapeSettings(id: Identifier): AbstractBlock.Settings =
    blockSettings(id).nonOpaque().sounds(BlockSoundGroup.GRASS)

abstract class Drape(settings: AbstractBlock.Settings) :
    PlantBlock(settings),
    Fertilizable {

    companion object {
        val PART: EnumProperty<DrapePart> = EnumProperty.of("part", DrapePart::class.java)
    }

    init {
        defaultState = stateManager.defaultState.with(PART, DrapePart.LEAF)
    }

    override fun getCollisionShape(
        state: BlockState?,
        world: BlockView?,
        pos: BlockPos?,
        context: ShapeContext?
    ): VoxelShape =
        VoxelShapes.empty()

    override fun getOutlineShape(
        state: BlockState?,
        view: BlockView?,
        pos: BlockPos?,
        context: ShapeContext?
    ): VoxelShape =
        when (state?.get(PART)) {
            DrapePart.MIDDLE -> VoxelShapes.cuboid(0.2, 0.0, 0.2, 0.8, 1.0, 0.8)
            DrapePart.LOWER -> VoxelShapes.cuboid(0.2, 0.2, 0.2, 0.8, 1.0, 0.8)
            else -> VoxelShapes.cuboid(0.2, 0.0, 0.2, 0.8, 0.8, 0.8)
        }

    abstract fun isDrape(block: Block): Boolean

    override fun isFertilizable(world: WorldView?, pos: BlockPos?, state: BlockState?) = true

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>?) {
        builder?.add(PART)
    }

    override fun grow(world: ServerWorld, random: Random, pos: BlockPos, state: BlockState) {
        val positionBelow: BlockPos = pos.down(1)
        val blockStateBelow: BlockState = world.getBlockState(positionBelow)
        val blockBelow: Block = blockStateBelow.block

        when (state[PART]) {
            DrapePart.TOP, DrapePart.MIDDLE ->
                if (isDrape(blockBelow) && blockBelow is Fertilizable)
                    blockBelow.grow(world, random, positionBelow, blockStateBelow)

            DrapePart.LOWER -> {
                if (blockStateBelow.isAir) {
                    world.setBlockState(pos, state.with(PART, DrapePart.MIDDLE))
                    world.setBlockState(positionBelow, state.with(PART, DrapePart.LOWER))
                }
            }

            DrapePart.LEAF -> {
                world.setBlockState(pos, state.with(PART, DrapePart.TOP))
                if (blockStateBelow.isAir)
                    world.setBlockState(positionBelow, state.with(PART, DrapePart.LOWER))
            }

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
            DrapePart.LEAF -> {
                isDrape(blockAbove) ||
                        pos.neighbours
                            .map { position -> world.getBlockState(position) }
                            .any { neighbourState -> neighbourState.isOpaque }
            }
            DrapePart.TOP ->
                pos.neighbours
                    .map { position -> world.getBlockState(position) }
                    .any { neighbourState -> neighbourState.isOpaque }

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

    override fun onBreak(world: World, pos: BlockPos, state: BlockState, player: PlayerEntity): BlockState {
        val positionAbove: BlockPos = pos.up(1)
        val blockAbove: BlockState = world.getBlockState(positionAbove)
        val positionRoot: BlockPos = positionAbove.up()
        val blockRoot: BlockState = world.getBlockState(positionRoot)

        if (isDrape(blockRoot.block) && isDrape(blockAbove.block))
            world.setBlockState(positionAbove, blockAbove.with(PART, DrapePart.LOWER))

        else if (isDrape(blockAbove.block))
            world.setBlockState(positionAbove, blockAbove.with(PART, DrapePart.LEAF))

        return super.onBreak(world, pos, state, player)
    }
}
