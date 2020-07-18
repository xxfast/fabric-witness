package com.xfastgames.witness.items

import com.xfastgames.witness.Witness
import com.xfastgames.witness.items.renderer.PuzzleTileItemRenderer
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.registerItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.Identifier

@Serializable
enum class Direction { TOP, RIGHT, BOTTOM, LEFT }

@Serializable
enum class Line { FILLED, SHORTENED, END }

@Serializable
data class Tile(val isStart: Boolean, val lines: Map<Direction, Line>) {

    val left: Line? get() = lines[Direction.LEFT]
    val bottom: Line? get() = lines[Direction.BOTTOM]
    val right: Line? get() = lines[Direction.RIGHT]
    val top: Line? get() = lines[Direction.TOP]
    val center: Set<Direction> = lines.keys

    constructor(start: Boolean, top: Line?, left: Line?, bottom: Line?, right: Line?) : this(
        isStart = start,
        lines = mapOf(
            Direction.TOP to top,
            Direction.LEFT to left,
            Direction.RIGHT to right,
            Direction.BOTTOM to bottom
        ).mapNotNull {
            it.value?.let { value -> it.key to value }
        }.toMap()
    )

    fun toTag(): CompoundTag = toTag(this)

    fun asItemStack(): ItemStack = ItemStack(PuzzleTile.ITEM).apply { tag = this@Tile.toTag() }

    companion object Builder {
        val DEFAULT = Tile(false, Line.FILLED, Line.FILLED, Line.FILLED, Line.FILLED)

        fun generate() = Tile(
            isStart = false,
            lines = mapOf(
                Direction.TOP to Line.values().random(),
                Direction.RIGHT to Line.values().random(),
                Direction.BOTTOM to Line.values().random(),
                Direction.LEFT to Line.values().random()
            )
        )

        private val json = Json(JsonConfiguration.Stable)

        fun fromTag(tag: CompoundTag): Tile {
            val dataString: String = tag.getString(KEY_DATA)
            val data: Tile? = dataString.takeIf { it.isNotEmpty() }?.let { json.parse(serializer(), dataString) }
            return data ?: DEFAULT
        }

        fun toTag(tile: Tile): CompoundTag = CompoundTag().apply {
            val data: String = json.stringify(serializer(), tile)
            putString(KEY_DATA, data)
        }
    }
}

private const val KEY_DATA = "tileData"

class PuzzleTile : Item(Settings().group(ItemGroup.REDSTONE)), Clientside {

    companion object {
        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "puzzle_tile")
        val ITEM: Item = registerItem(IDENTIFIER, PuzzleTile())
        val RENDERER = PuzzleTileItemRenderer()


    }

    override fun onClient() {
        BuiltinItemRendererRegistry.INSTANCE.register(ITEM, RENDERER)
    }
}

