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
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.Identifier

@Serializable
enum class Direction { TOP, RIGHT, BOTTOM, LEFT }

@Serializable
enum class Line { FILLED, SHORTENED, END }

@Serializable
data class Tile(val lines: Map<Direction, Line>, val isStart: Boolean = false) {
    val left: Line? get() = lines[Direction.LEFT]
    val bottom: Line? get() = lines[Direction.BOTTOM]
    val right: Line? get() = lines[Direction.RIGHT]
    val top: Line? get() = lines[Direction.TOP]
    val center: Set<Direction> = lines.keys
}

private const val KEY_DATA = "tileData"

class PuzzleTile : Item(Settings().group(ItemGroup.REDSTONE)), Clientside {

    companion object {
        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "puzzle_tile")
        val ITEM: Item = registerItem(IDENTIFIER, PuzzleTile())
    }

    private val json = Json(JsonConfiguration.Stable)

    override fun onClient() {
        BuiltinItemRendererRegistry.INSTANCE.register(ITEM, PuzzleTileItemRenderer())
    }

    fun fromTag(tag: CompoundTag): Tile {
        val stringData: String = tag.getString(KEY_DATA)
        return json.parse(Tile.serializer(), stringData)
    }

    fun toTag(tile: Tile): CompoundTag = CompoundTag().apply {
        val dataString: String = json.stringify(Tile.serializer(), tile)
        putString(KEY_DATA, dataString)
    }
}

