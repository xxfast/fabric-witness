package com.xfastgames.witness.items

import com.xfastgames.witness.Witness
import com.xfastgames.witness.items.renderer.PuzzlePanelItemRenderer
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.registerItem
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World

private const val KEY_PANEL = "panel"
private const val KEY_SIZE = "width"
private const val KEY_TILES = "tiles"

data class Panel(val tiles: List<List<Tile>>) {
    constructor(size: Int) : this(generate(size).tiles)
    constructor(size: Int, tiles: List<Tile>) : this(
        tiles.takeIf { it.isNotEmpty() }?.chunked(size).orEmpty()
    )

    fun copy(x: Int, y: Int, copier: Tile.() -> Tile): Panel =
        copy(tiles = tiles.mapIndexed { xIndex, cols ->
            cols.mapIndexed { yIndex, tile ->
                if (xIndex == x && yIndex == y) copier(tile) else tile
            }
        })
}

private fun generate(size: Int): Panel {
    val col = mutableListOf<List<Tile>>()
    repeat(size) { x ->
        val row = mutableListOf<Tile>()
        repeat(size) { y ->
            row.add(Tile())
        }
        col.add(row.toList())
    }
    return Panel(col.toList())
}

fun CompoundTag.getPanel(): Panel =
    getCompound(KEY_PANEL).let { tag ->
        Panel(
            tag.getInt(KEY_SIZE),
            tag.getList(KEY_TILES, 10) // TODO: Why 10? 🤷‍
                .filterIsInstance<CompoundTag>()
                .map { it.getTile() }
        )
    }

fun CompoundTag.putPanel(panel: Panel) {
    put(KEY_PANEL, CompoundTag().apply {
        putInt(KEY_SIZE, panel.tiles.size)
        put(KEY_TILES, ListTag().apply {
            panel.tiles.flatten().map { tile ->
                add(CompoundTag().apply { putTile(tile) })
            }
        })
    })
}

class PuzzlePanel : Item(Settings().group(ItemGroup.REDSTONE)), Clientside {

    companion object {
        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "puzzle_panel")
        val ITEM = registerItem(IDENTIFIER, PuzzlePanel())
    }

    override fun onClient() {
        BuiltinItemRendererRegistry.INSTANCE.register(ITEM, PuzzlePanelItemRenderer())
    }

    override fun use(world: World?, user: PlayerEntity, hand: Hand?): TypedActionResult<ItemStack> {
        val panel = Panel(5)
        val tag: CompoundTag = user.mainHandStack.orCreateTag
        tag.putPanel(panel)
        return TypedActionResult(
            ActionResult.SUCCESS,
            if (hand === Hand.MAIN_HAND) user.mainHandStack else user.offHandStack
        )
    }
}