package com.xfastgames.witness.items

import com.xfastgames.witness.Witness
import com.xfastgames.witness.items.renderer.PuzzlePanelItemRenderer
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.registerItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry
import net.minecraft.block.Block
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World

@Serializable
data class Panel(val tiles: List<List<Tile>>)

private const val KEY_DATA = "puzzleData"

class PuzzlePanel : Item(Settings().group(ItemGroup.REDSTONE)), Clientside {

    companion object {
        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "puzzle_panel")
        val ITEM = registerItem(IDENTIFIER, PuzzlePanel())

        fun generate(size: Int): Panel {
            val col = mutableListOf<List<Tile>>()
            repeat(size) { x ->
                val row = mutableListOf<Tile>()
                repeat(size) { y ->
                    row.add(PuzzleTile.generate())
                }
                col.add(row.toList())
            }
            return Panel(col.toList())
        }

        private val json = Json(JsonConfiguration.Stable)

        fun fromTag(tag: CompoundTag): Panel {
            val data: String = tag.getString(KEY_DATA)
            return json.parse(Panel.serializer(), data)
        }

        fun toTag(panel: Panel): CompoundTag = CompoundTag().apply {
            val data: String = json.stringify(Panel.serializer(), panel)
            putString(KEY_DATA, data)
        }
    }

    override fun onClient() {
        BuiltinItemRendererRegistry.INSTANCE.register(ITEM, PuzzlePanelItemRenderer())
    }

    override fun useOnBlock(context: ItemUsageContext): ActionResult {
        val block: Block = context.world.getBlockState(context.blockPos).block
        return super.useOnBlock(context)
    }

    override fun use(world: World?, user: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val itemStack: ItemStack = user.getStackInHand(hand)
        val data: CompoundTag = itemStack.orCreateTag
        return super.use(world, user, hand)
    }


}