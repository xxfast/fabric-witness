package com.xfastgames.witness.recipes

import com.xfastgames.witness.Witness
import com.xfastgames.witness.items.AncientPuzzleTablet
import com.xfastgames.witness.items.PuzzlePanelItem
import com.xfastgames.witness.items.data.Panel
import com.xfastgames.witness.items.data.cost
import com.xfastgames.witness.items.data.panel
import net.minecraft.item.DyeItem
import net.minecraft.item.ItemStack
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.recipe.SpecialCraftingRecipe
import net.minecraft.recipe.book.CraftingRecipeCategory
import net.minecraft.recipe.input.CraftingRecipeInput
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryWrapper
import net.minecraft.util.DyeColor
import net.minecraft.util.Identifier
import net.minecraft.world.World

/**
 * Replaces the dead nbtcrafting-based `puzzle_panel_color_*` recipes (1.17): crafting a puzzle
 * panel together with any dye produces the same panel with its background colour changed, keeping
 * all other puzzle data (now stored in the `witness:panel` data component).
 */
class PanelDyeRecipe(category: CraftingRecipeCategory) : SpecialCraftingRecipe(category) {

    companion object {
        val IDENTIFIER: Identifier = Identifier.of(Witness.IDENTIFIER, "panel_dye")
        val SERIALIZER: RecipeSerializer<PanelDyeRecipe> = Registry.register(
            Registries.RECIPE_SERIALIZER,
            IDENTIFIER,
            SpecialRecipeSerializer(::PanelDyeRecipe)
        )

        /** Referenced from mod init to force registration of the recipe serializers. */
        fun init() {
            PanelRecycleRecipe.SERIALIZER
        }
    }

    override fun matches(input: CraftingRecipeInput, world: World): Boolean {
        var panels = 0
        var dyes = 0
        input.stacks.forEach { stack ->
            when {
                stack.isEmpty -> Unit
                stack.item is PuzzlePanelItem -> panels++
                stack.item is DyeItem -> dyes++
                else -> return false
            }
        }
        return panels == 1 && dyes == 1
    }

    override fun craft(input: CraftingRecipeInput, registries: RegistryWrapper.WrapperLookup): ItemStack {
        val panelStack: ItemStack = input.stacks.firstOrNull { it.item is PuzzlePanelItem } ?: return ItemStack.EMPTY
        val dyeItem: DyeItem = input.stacks.firstOrNull { it.item is DyeItem }?.item as? DyeItem
            ?: return ItemStack.EMPTY

        val puzzle: Panel = panelStack.panel ?: return ItemStack.EMPTY
        val updatedColor: DyeColor = dyeItem.color
        val tintedPanel: Panel = when (puzzle) {
            is Panel.Grid -> puzzle.copy(backgroundColor = updatedColor)
            is Panel.Tree -> puzzle.copy(backgroundColor = updatedColor)
            is Panel.Freeform -> puzzle.copy(backgroundColor = updatedColor)
        }

        return panelStack.copyWithCount(1).apply { panel = tintedPanel }
    }

    override fun getSerializer(): RecipeSerializer<out SpecialCraftingRecipe> = SERIALIZER
}

/**
 * Replaces the nbtcrafting-based `puzzle_panel_grid_recycle(_compat)` recipes: crafting a lone
 * puzzle panel returns ancient puzzle tablets — as many as the panel's stored `cost`, or 4 for
 * legacy/compat panels without a cost component.
 */
class PanelRecycleRecipe(category: CraftingRecipeCategory) : SpecialCraftingRecipe(category) {

    companion object {
        val IDENTIFIER: Identifier = Identifier.of(Witness.IDENTIFIER, "panel_recycle")
        val SERIALIZER: RecipeSerializer<PanelRecycleRecipe> = Registry.register(
            Registries.RECIPE_SERIALIZER,
            IDENTIFIER,
            SpecialRecipeSerializer(::PanelRecycleRecipe)
        )
    }

    override fun matches(input: CraftingRecipeInput, world: World): Boolean {
        val stacks: List<ItemStack> = input.stacks.filterNot { it.isEmpty }
        return stacks.size == 1 && stacks.single().item is PuzzlePanelItem
    }

    override fun craft(input: CraftingRecipeInput, registries: RegistryWrapper.WrapperLookup): ItemStack {
        val panelStack: ItemStack = input.stacks.firstOrNull { it.item is PuzzlePanelItem } ?: return ItemStack.EMPTY
        val count: Int = panelStack.cost ?: 4
        return ItemStack(AncientPuzzleTablet.ITEM, count)
    }

    override fun getSerializer(): RecipeSerializer<out SpecialCraftingRecipe> = SERIALIZER
}
