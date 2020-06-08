package com.xfastgames.witness.feature

import com.mojang.datafixers.Dynamic
import net.minecraft.block.Block
import net.minecraft.util.math.BlockPos
import net.minecraft.world.Heightmap
import net.minecraft.world.IWorld
import net.minecraft.world.gen.chunk.ChunkGenerator
import net.minecraft.world.gen.chunk.ChunkGeneratorConfig
import net.minecraft.world.gen.feature.DefaultFeatureConfig
import net.minecraft.world.gen.feature.Feature
import net.minecraft.world.gen.feature.FeatureConfig
import java.util.*
import java.util.function.Function
import kotlin.random.asKotlinRandom

abstract class PatchOfBlocksFeature(
    val blocks: List<Block>,
    private val amount: IntRange = 10..20,
    private val xWidth: Int = 5,
    private val zWidth: Int = 5
) :
    Feature<FeatureConfig>(Function { dynamic: Dynamic<*>? ->
        DefaultFeatureConfig.deserialize(dynamic)
    }) {

    override fun generate(
        world: IWorld,
        chunkGenerator: ChunkGenerator<out ChunkGeneratorConfig?>?,
        random: Random,
        pos: BlockPos?,
        config: FeatureConfig?
    ): Boolean {
        // Generate at surface
        val topPos: BlockPos = world.getTopPosition(Heightmap.Type.WORLD_SURFACE, pos)

        // Generate in pack of 20-50s in XZ directions
        repeat(random.asKotlinRandom().nextInt(amount.first, amount.last)) {
            var position: BlockPos =
                topPos.add(
                    random.asKotlinRandom().nextInt(-xWidth, +xWidth), 0,
                    random.asKotlinRandom().nextInt(-zWidth, +zWidth)
                )
            while (!world.getBlockState(position).isOpaque) position = position.down(1)
            val block = blocks.random(random.asKotlinRandom())
            world.setBlockState(position.up(1), block.defaultState, 3)
        }
        return true
    }
}