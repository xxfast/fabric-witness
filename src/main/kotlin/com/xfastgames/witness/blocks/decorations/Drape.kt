package com.xfastgames.witness.blocks.decorations

import com.xfastgames.witness.utils.blockSettings
import com.xfastgames.witness.utils.neighbours
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.BonemealableBlock
import net.minecraft.world.level.block.VegetationBlock
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.resources.Identifier
import net.minecraft.util.StringRepresentable
import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import java.util.Locale

enum class DrapePart : StringRepresentable {
    TOP, MIDDLE, LOWER, LEAF;

    override fun getSerializedName(): String = this.name.lowercase(Locale.getDefault())
}

fun drapeSettings(id: Identifier): BlockBehaviour.Properties =
    blockSettings(id).noOcclusion().sound(SoundType.GRASS)

abstract class Drape(settings: BlockBehaviour.Properties) :
    VegetationBlock(settings),
    BonemealableBlock {

    companion object {
        val PART: EnumProperty<DrapePart> = EnumProperty.create("part", DrapePart::class.java)
    }

    init {
        registerDefaultState(stateDefinition.any().setValue(PART, DrapePart.LEAF))
    }

    override fun getCollisionShape(state: BlockState, world: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape =
        Shapes.empty()

    override fun getShape(state: BlockState, world: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape =
        when (state.getValue(PART)) {
            DrapePart.MIDDLE -> Shapes.box(0.2, 0.0, 0.2, 0.8, 1.0, 0.8)
            DrapePart.LOWER -> Shapes.box(0.2, 0.2, 0.2, 0.8, 1.0, 0.8)
            else -> Shapes.box(0.2, 0.0, 0.2, 0.8, 0.8, 0.8)
        }

    abstract fun isDrape(block: Block): Boolean

    override fun isValidBonemealTarget(world: LevelReader, pos: BlockPos, state: BlockState) = true

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(PART)
    }

    override fun performBonemeal(world: ServerLevel, random: RandomSource, pos: BlockPos, state: BlockState) {
        val positionBelow: BlockPos = pos.below(1)
        val blockStateBelow: BlockState = world.getBlockState(positionBelow)
        val blockBelow: Block = blockStateBelow.block

        when (state.getValue(PART)) {
            DrapePart.TOP, DrapePart.MIDDLE ->
                if (isDrape(blockBelow) && blockBelow is BonemealableBlock)
                    blockBelow.performBonemeal(world, random, positionBelow, blockStateBelow)

            DrapePart.LOWER -> {
                if (blockStateBelow.isAir) {
                    world.setBlock(pos, state.setValue(PART, DrapePart.MIDDLE), Block.UPDATE_ALL)
                    world.setBlock(positionBelow, state.setValue(PART, DrapePart.LOWER), Block.UPDATE_ALL)
                }
            }

            DrapePart.LEAF -> {
                world.setBlock(pos, state.setValue(PART, DrapePart.TOP), Block.UPDATE_ALL)
                if (blockStateBelow.isAir)
                    world.setBlock(positionBelow, state.setValue(PART, DrapePart.LOWER), Block.UPDATE_ALL)
            }

            else -> {
            }
        }
    }

    override fun isRandomlyTicking(state: BlockState): Boolean = true

    override fun isBonemealSuccess(world: Level, random: RandomSource, pos: BlockPos, state: BlockState): Boolean =
        random.nextBoolean()

    override fun canSurvive(state: BlockState, world: LevelReader, pos: BlockPos): Boolean {
        val blockAbove: Block = world.getBlockState(pos.above()).block
        return when (state.getValue(PART)) {
            DrapePart.LEAF -> {
                isDrape(blockAbove) ||
                        pos.neighbours
                            .map { position -> world.getBlockState(position) }
                            .any { neighbourState -> neighbourState.canOcclude() }
            }
            DrapePart.TOP ->
                pos.neighbours
                    .map { position -> world.getBlockState(position) }
                    .any { neighbourState -> neighbourState.canOcclude() }

            DrapePart.MIDDLE, DrapePart.LOWER -> isDrape(blockAbove)

            else -> false
        }
    }

    override fun setPlacedBy(world: Level, pos: BlockPos, state: BlockState, placer: LivingEntity?, itemStack: ItemStack) {
        val positionAbove: BlockPos = pos.above(1)
        val blockStateAbove: BlockState = world.getBlockState(positionAbove)
        val blockAbove: Block = blockStateAbove.block
        if (blockAbove is Drape) {
            world.setBlock(
                pos, state.setValue(
                    PART,
                    DrapePart.LOWER
                ),
                Block.UPDATE_ALL
            )
            when (blockStateAbove.getValue(PART)) {
                DrapePart.LOWER -> world.setBlock(
                    positionAbove, blockStateAbove.setValue(
                        PART,
                        DrapePart.MIDDLE
                    ),
                    Block.UPDATE_ALL
                )
                DrapePart.LEAF -> world.setBlock(
                    positionAbove, blockStateAbove.setValue(
                        PART,
                        DrapePart.TOP
                    ),
                    Block.UPDATE_ALL
                )
                else -> {
                }
            }
        } else super.setPlacedBy(world, pos, state, placer, itemStack)
    }

    override fun playerWillDestroy(world: Level, pos: BlockPos, state: BlockState, player: Player): BlockState {
        val positionAbove: BlockPos = pos.above(1)
        val blockAbove: BlockState = world.getBlockState(positionAbove)
        val positionRoot: BlockPos = positionAbove.above()
        val blockRoot: BlockState = world.getBlockState(positionRoot)

        if (isDrape(blockRoot.block) && isDrape(blockAbove.block))
            world.setBlock(positionAbove, blockAbove.setValue(PART, DrapePart.LOWER), Block.UPDATE_ALL)

        else if (isDrape(blockAbove.block))
            world.setBlock(positionAbove, blockAbove.setValue(PART, DrapePart.LEAF), Block.UPDATE_ALL)

        return super.playerWillDestroy(world, pos, state, player)
    }
}
