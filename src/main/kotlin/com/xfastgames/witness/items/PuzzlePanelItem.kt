package com.xfastgames.witness.items

import com.xfastgames.witness.Witness
import com.xfastgames.witness.items.data.Panel
import com.xfastgames.witness.items.data.getPanel
import com.xfastgames.witness.items.data.putPanel
import com.xfastgames.witness.items.renderer.PuzzlePanelRenderer
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.registerItem
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry
import net.minecraft.client.item.TooltipContext
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.world.World
import java.util.*

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
        stack?.nbt = NbtCompound().apply { putPanel(KEY_PANEL, Panel.TEST) }
    }

    // TODO: Use localised strings here
    override fun appendTooltip(
        stack: ItemStack,
        world: World?,
        tooltip: MutableList<Text>,
        context: TooltipContext?
    ) {
        val puzzle: Panel = stack.nbt?.getPanel(KEY_PANEL) ?: return

        val typeString: String =
            puzzle.type.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        val sizeString = when (puzzle) {
            is Panel.Grid -> "${puzzle.width - 1} x ${puzzle.height - 1}"
            is Panel.Tree -> "${puzzle.height - 1} Tall"
            is Panel.Freeform -> "${puzzle.width - 1} x ${puzzle.height - 1} Size"
        }

        val colorString: String = puzzle.backgroundColor.name
            .split("_")
            .joinToString(" ") {
                it.lowercase(Locale.getDefault())
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }

        tooltip.add(Text.of("($sizeString $colorString $typeString)"))
    }
}