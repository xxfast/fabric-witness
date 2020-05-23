package com.xfastgames.witness.feature

import com.mojang.datafixers.Dynamic
import com.xfastgames.witness.blocks.yucca.Yucca
import com.xfastgames.witness.utils.BiomeFeature
import net.minecraft.util.math.BlockPos
import net.minecraft.world.Heightmap
import net.minecraft.world.IWorld
import net.minecraft.world.biome.Biome
import net.minecraft.world.biome.Biomes
import net.minecraft.world.gen.chunk.ChunkGenerator
import net.minecraft.world.gen.chunk.ChunkGeneratorConfig
import net.minecraft.world.gen.feature.DefaultFeatureConfig
import net.minecraft.world.gen.feature.Feature
import java.util.*
import java.util.function.Function
import kotlin.random.asKotlinRandom


class YuccaFeature :
    Feature<DefaultFeatureConfig?>(Function { dynamic: Dynamic<*>? ->
        DefaultFeatureConfig.deserialize(dynamic)
    }),
    BiomeFeature {
    override fun generate(
        world: IWorld,
        chunkGenerator: ChunkGenerator<out ChunkGeneratorConfig?>?,
        random: Random,
        pos: BlockPos?,
        config: DefaultFeatureConfig?
    ): Boolean {
        // Generate at surface
        val topPos: BlockPos = world.getTopPosition(Heightmap.Type.WORLD_SURFACE, pos)

        // Generate in pack of 20-50s in XZ directions
        repeat(random.asKotlinRandom().nextInt(20, 50)) {
            var position: BlockPos =
                topPos.add(
                    random.asKotlinRandom().nextInt(-5, +5), 0,
                    random.asKotlinRandom().nextInt(-5, +5)
                )
            while (!world.getBlockState(position).isOpaque) position = position.down(1)
            world.setBlockState(position.up(1), Yucca.defaultState, 3)
        }
        return true
    }

    override val biomes: List<Biome> = listOf(
        Biomes.SAVANNA,
        Biomes.SAVANNA_PLATEAU,
        Biomes.SHATTERED_SAVANNA,
        Biomes.SHATTERED_SAVANNA_PLATEAU
    )

}