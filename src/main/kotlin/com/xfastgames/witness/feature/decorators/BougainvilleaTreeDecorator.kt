package com.xfastgames.witness.feature.decorators

import com.google.common.collect.ImmutableMap
import com.mojang.datafixers.Dynamic
import com.mojang.datafixers.types.DynamicOps
import com.xfastgames.witness.blocks.drapes.BlueBougainvilleaDrape
import com.xfastgames.witness.blocks.drapes.Drape
import com.xfastgames.witness.blocks.drapes.DrapePart
import com.xfastgames.witness.blocks.drapes.PurpleBougainvilleaDrape
import com.xfastgames.witness.utils.neighbours
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockBox
import net.minecraft.util.math.BlockPos
import net.minecraft.util.registry.Registry
import net.minecraft.world.IWorld
import net.minecraft.world.gen.decorator.TreeDecorator
import net.minecraft.world.gen.decorator.TreeDecoratorType
import java.util.*
import kotlin.random.asKotlinRandom

class BougainvilleaTreeDecorator : TreeDecorator(TreeDecoratorType.TRUNK_VINE) {

    override fun generate(
        world: IWorld,
        random: Random,
        logPositions: MutableList<BlockPos>,
        leavesPositions: MutableList<BlockPos>,
        set: MutableSet<BlockPos>,
        box: BlockBox
    ) {
        logPositions.takeLast(1).forEach { logPosition ->
            // Choose random color
            val drape: Block = listOf(PurpleBougainvilleaDrape.BLOCK, BlueBougainvilleaDrape.BLOCK)
                .random(random.asKotlinRandom())

            logPosition.neighbours.dropLast(2) // don't consider neighbours above and below
                .filter { neighbour -> neighbour !in logPositions }
                .forEach { neighbour ->
                    // Generate 75% of the time
                    if (random.nextDouble() > 0.75) return

                    // Start with top
                    world.setBlockState(
                        neighbour,
                        drape.defaultState.with(Drape.PART, DrapePart.TOP),
                        0
                    )

                    // Grow the root randomly upto length of 3
                    var downPos: BlockPos = neighbour
                    var downBlockState: BlockState
                    repeat(random.nextInt(5)) {
                        downPos = neighbour.down()
                        downBlockState = world.getBlockState(downPos)
                        if (downBlockState.isAir) world.setBlockState(
                            downPos,
                            drape.defaultState.with(Drape.PART, DrapePart.MIDDLE),
                            0
                        )
                    }

                    val positionAboveLowest: BlockPos = downPos.up()
                    val blockStateAboveLowest: BlockState = world.getBlockState(positionAboveLowest)

                    // Change lowers to the right part, if the one above is a drape
                    if (blockStateAboveLowest.block is Drape)
                        world.setBlockState(
                            downPos,
                            drape.defaultState.with(Drape.PART, DrapePart.LOWER),
                            0
                        )

                    // If lowest part is the same as top, it must be a leaf
                    if (neighbour == downPos)
                        world.setBlockState(
                            downPos,
                            drape.defaultState,
                            0
                        )
                }
        }
    }

    override fun <T : Any> serialize(ops: DynamicOps<T>): T? {
        return Dynamic(
            ops,
            ops.createMap(
                ImmutableMap.of(
                    ops.createString("type"),
                    ops.createString(
                        Registry.TREE_DECORATOR_TYPE.getId(type).toString()
                    )
                )
            )
        ).value
    }
}