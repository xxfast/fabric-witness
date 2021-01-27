package com.xfastgames.witness.items

import com.xfastgames.witness.Witness
import com.xfastgames.witness.items.data.Panel
import com.xfastgames.witness.items.data.getPanel
import com.xfastgames.witness.items.renderer.PuzzlePanelRenderer
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.registerItem
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry
import net.minecraft.client.item.TooltipContext
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.world.World

const val KEY_PANEL = "panel"

class PuzzlePanelItem : Item(Settings().group(ItemGroup.REDSTONE)), Clientside {

    companion object {
        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "puzzle_panel")
        val ITEM: Item = registerItem(IDENTIFIER, PuzzlePanelItem())
    }

    override fun onClient() {
        BuiltinItemRendererRegistry.INSTANCE.register(ITEM, PuzzlePanelRenderer)
    }

    override fun onCraft(stack: ItemStack?, world: World?, player: PlayerEntity?) {
//        stack?.tag = null
//        val tag = CompoundTag().apply { putPanel(KEY_PANEL, Panel.Grid.ofSize(4, 2)) }
//        println(tag)
//        stack?.tag = tag
    }

    // TODO: Use localised strings here
    override fun appendTooltip(
        stack: ItemStack,
        world: World?,
        tooltip: MutableList<Text>,
        context: TooltipContext?
    ) {
        val puzzle: Panel = stack.tag?.getPanel(KEY_PANEL) ?: return

        val typeString: String = puzzle.type.name.capitalize()

        val sizeString = when (puzzle) {
            is Panel.Grid -> "${puzzle.width - 1} x ${puzzle.height - 1}"
            is Panel.Tree -> "${puzzle.height - 1} Tall"
            is Panel.Freeform -> TODO()
        }

        val colorString: String = puzzle.backgroundColor.name
            .split("_")
            .joinToString(" ") { it.toLowerCase().capitalize() }

        tooltip.add(Text.of("($sizeString $colorString $typeString)"))
    }
}