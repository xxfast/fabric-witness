package com.xfastgames.witness.items

import com.xfastgames.witness.Witness
import com.xfastgames.witness.items.data.Panel
import com.xfastgames.witness.items.data.PanelComponents
import com.xfastgames.witness.items.data.cost
import com.xfastgames.witness.items.data.panel
import com.xfastgames.witness.items.renderer.PuzzlePanelSpecialModelRenderer
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.itemSettings
import com.xfastgames.witness.utils.registerItem
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier
import net.minecraft.client.render.item.model.SpecialItemModel
import net.minecraft.component.type.TooltipDisplayComponent
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import java.util.Locale
import java.util.function.Consumer

const val KEY_COST = "cost"
const val KEY_PANEL = "panel"

class PuzzlePanelItem(settings: Settings) : Item(settings), Clientside {

    companion object {
        val IDENTIFIER = Identifier.of(Witness.IDENTIFIER, "puzzle_panel")
        val ITEM: Item = registerItem(
            IDENTIFIER,
            PuzzlePanelItem(itemSettings(IDENTIFIER).component(PanelComponents.PANEL, Panel.DEFAULT))
        )
    }

    override fun onClient() {
        ModelLoadingPlugin.register { pluginContext ->
            pluginContext.modifyItemModelBeforeBake().register(
                ModelModifier.OVERRIDE_PHASE,
                ModelModifier.BeforeBakeItem { model, context ->
                    if (context.itemId() == IDENTIFIER) {
                        SpecialItemModel.Unbaked(
                            Identifier.of(Witness.IDENTIFIER, "item/puzzle_panel_item"),
                            PuzzlePanelSpecialModelRenderer.Unbaked
                        )
                    } else {
                        model
                    }
                }
            )
        }
    }

    // TODO: Use localised strings here
    @Deprecated("Vanilla deprecates Item.appendTooltip in favour of component-driven tooltips")
    override fun appendTooltip(
        stack: ItemStack,
        context: TooltipContext,
        displayComponent: TooltipDisplayComponent,
        textConsumer: Consumer<Text>,
        type: TooltipType
    ) {
        val puzzle: Panel = stack.panel ?: return

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

        val cost: String = stack.cost?.let { cost -> "Costs $cost apt" }.orEmpty()

        if (type.isAdvanced) {
            textConsumer.accept(Text.of("($sizeString $colorString $typeString)"))
            textConsumer.accept(Text.of(cost))
        }
    }
}
