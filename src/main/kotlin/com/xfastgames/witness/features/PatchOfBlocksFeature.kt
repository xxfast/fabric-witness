package com.xfastgames.witness.features

import com.mojang.serialization.Codec
import net.minecraft.block.Block
import net.minecraft.util.math.BlockPos
import net.minecraft.world.Heightmap
import net.minecraft.world.ServerWorldAccess
import net.minecraft.world.gen.StructureAccessor
import net.minecraft.world.gen.chunk.ChunkGenerator
import net.minecraft.world.gen.feature.Feature
import net.minecraft.world.gen.feature.FeatureConfig
import java.util.*
import kotlin.random.asKotlinRandom

abstract class PatchOfBlocksFeature(
    val blocks: List<Block>,
    private val amount: IntRange = 10..20,
    private val xWidth: Int = 5,
    private val zWidth: Int = 5
) :
    Feature<FeatureConfig>(Codec.unit(FeatureConfig.DEFAULT)) {

    override fun generate(
        serverWorldAccess: ServerWorldAccess,
        accessor: StructureAccessor?,
        generator: ChunkGenerator?,
        random: Random,
        pos: BlockPos?,
        config: FeatureConfig?
    ): Boolean {
        // Generate at surface
        val topPos: BlockPos = serverWorldAccess.getTopPosition(Heightmap.Type.WORLD_SURFACE, pos)

        // Generate in pack of 20-50s in XZ directions
        repeat(random.asKotlinRandom().nextInt(amount.first, amount.last)) {
            var position: BlockPos =
                topPos.add(
                    random.asKotlinRandom().nextInt(-xWidth, +xWidth), 0,
                    random.asKotlinRandom().nextInt(-zWidth, +zWidth)
                )
            while (!serverWorldAccess.getBlockState(position).isOpaque) position = position.down(1)
            val block = blocks.random(random.asKotlinRandom())
            serverWorldAccess.setBlockState(position.up(1), block.defaultState, 3)
        }
        return true
    }
}