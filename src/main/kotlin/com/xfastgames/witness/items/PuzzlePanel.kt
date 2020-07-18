package com.xfastgames.witness.items

import com.xfastgames.witness.Witness
import com.xfastgames.witness.items.renderer.PuzzlePanelItemRenderer
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.jsonConfiguration
import com.xfastgames.witness.utils.registerItem
import kotlinx.serialization.Serializable
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.Identifier

@Serializable
data class Panel(val tiles: List<List<Tile>>) {

    constructor(size: Int) : this(generate(size, size))

    constructor(width: Int, height: Int) : this(generate(width, height))

    fun copy(x: Int, y: Int, copier: Tile.() -> Tile): Panel =
        copy(tiles = tiles.mapIndexed { xIndex, cols ->
            cols.mapIndexed { yIndex, tile ->
                if (xIndex == x && yIndex == y) copier(tile) else tile
            }
        })

    fun toTag(): CompoundTag = toTag(this)

    fun asItemStack(): ItemStack = ItemStack(PuzzlePanel.ITEM).apply { tag = this@Panel.toTag() }

    companion object Builder {
        val DEFAULT = Panel(3, 3)

        fun generate(width: Int, height: Int): List<List<Tile>> {
            val col = mutableListOf<List<Tile>>()
            repeat(width) { x ->
                val row = mutableListOf<Tile>()
                repeat(height) { y ->
                    row.add(Tile.DEFAULT)
                }
                col.add(row.toList())
            }
            return col.toList()
        }

        fun fromTag(tag: CompoundTag?): Panel {
            val dataString: String = tag?.getString(KEY_DATA) ?: return DEFAULT
            val data: Panel? = dataString.takeIf { it.isNotEmpty() }
                ?.let { jsonConfiguration.parse(serializer(), it) }
            return data ?: DEFAULT
        }

        fun toTag(panel: Panel): CompoundTag = CompoundTag().apply {
            val data: String = jsonConfiguration.stringify(serializer(), panel)
            putString(KEY_DATA, data)
        }
    }
}

private const val KEY_DATA = "puzzleData"

class PuzzlePanel : Item(Settings().group(ItemGroup.REDSTONE)), Clientside {

    companion object {
        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "puzzle_panel")
        val ITEM = registerItem(IDENTIFIER, PuzzlePanel())
    }

    override fun onClient() {
        BuiltinItemRendererRegistry.INSTANCE.register(ITEM, PuzzlePanelItemRenderer())
    }
}