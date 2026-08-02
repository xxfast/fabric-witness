package com.xfastgames.witness.utils

import net.minecraft.core.Direction
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.world.phys.shapes.Shapes

/**
 * Optimised version of https://forums.minecraftforge.net/topic/74979-1144-rotate-voxel-shapes/
 */
fun VoxelShape.rotateShape(from: Direction = Direction.NORTH, to: Direction): VoxelShape {
    var pre: VoxelShape = this
    var after: VoxelShape = Shapes.empty()
    val times: Int = (to.get2DDataValue() - from.get2DDataValue() + 4) % 4
    repeat(times) {
        pre.forAllBoxes { minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double ->
            after = Shapes.or(after, Shapes.box(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX))
        }
        pre = after
        after = Shapes.empty()
    }
    return pre
}
