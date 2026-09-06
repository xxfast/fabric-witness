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
import net.minecraft.client.renderer.item.SpecialModelWrapper
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.TooltipDisplay
import java.util.Locale
import java.util.Optional
import java.util.function.Consumer

const val KEY_COST = "cost"
const val KEY_PANEL = "panel"

class PuzzlePanelItem(settings: Properties) : Item(settings), Clientside {

    companion object {
        val IDENTIFIER = Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, "puzzle_panel")
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
                        SpecialModelWrapper.Unbaked(
                            Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, "item/puzzle_panel_item"),
                            Optional.empty(),
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
    override fun appendHoverText(
        stack: ItemStack,
        context: Item.TooltipContext,
        displayComponent: TooltipDisplay,
        textConsumer: Consumer<Component>,
        type: TooltipFlag
    ) {
        val puzzle: Panel = stack.panel ?: return

        val typeString: String =
            puzzle.type.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        // Crafting can't explain a failed craft, so the tooltip is the only place a player can
        // learn the grid won't grow any further.
        val maxedOut: Boolean = puzzle is Panel.Grid &&
            (puzzle.width >= Panel.Grid.MAX_NODES || puzzle.height >= Panel.Grid.MAX_NODES)

        val sizeString = when (puzzle) {
            is Panel.Grid -> "${puzzle.width - 1} x ${puzzle.height - 1}"
            is Panel.Tree -> "${puzzle.levels} Tall"
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
            textConsumer.accept(Component.literal("($sizeString $colorString $typeString)"))
            textConsumer.accept(Component.literal(cost))
            if (puzzle.tutorial) textConsumer.accept(Component.literal("Tutorial"))
            if (maxedOut) textConsumer.accept(Component.literal("Maximum size"))
        }
    }
}
