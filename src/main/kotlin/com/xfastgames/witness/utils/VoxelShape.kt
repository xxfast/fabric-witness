package com.xfastgames.witness.utils

import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes

/**
 * Optimised version of https://forums.minecraftforge.net/topic/74979-1144-rotate-voxel-shapes/
 */
fun VoxelShape.rotateShape(from: Direction = Direction.NORTH, to: Direction): VoxelShape {
    var pre: VoxelShape = this
    var after: VoxelShape = VoxelShapes.empty()
    val times: Int = (to.horizontal - from.horizontal + 4) % 4
    repeat(times) {
        pre.forEachBox { minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double ->
            after = VoxelShapes.union(after, VoxelShapes.cuboid(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX))
        }
        pre = after
        after = VoxelShapes.empty()
    }
    return pre
}