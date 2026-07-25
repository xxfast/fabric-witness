package com.xfastgames.witness.recipes

import com.xfastgames.witness.Witness
import com.xfastgames.witness.items.AncientPuzzleTablet
import com.xfastgames.witness.items.PuzzlePanelItem
import com.xfastgames.witness.items.data.Panel
import com.xfastgames.witness.items.data.cost
import com.xfastgames.witness.items.data.panel
import net.minecraft.item.DyeItem
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.IngredientPlacement
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.recipe.SpecialCraftingRecipe
import net.minecraft.recipe.book.CraftingRecipeCategory
import net.minecraft.recipe.display.RecipeDisplay
import net.minecraft.recipe.display.ShapelessCraftingRecipeDisplay
import net.minecraft.recipe.display.SlotDisplay
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
        private val PANEL_INGREDIENT: Ingredient = Ingredient.ofItem(PuzzlePanelItem.ITEM)
        private val DYE_INGREDIENT: Ingredient =
            Ingredient.ofItems(DyeColor.entries.stream().map(DyeItem::byColor))
        private val INGREDIENTS: List<Ingredient> = listOf(PANEL_INGREDIENT, DYE_INGREDIENT)

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

    /*
     * SpecialCraftingRecipe opts out of the recipe book because most special recipes cannot
     * describe their inputs or output. This one can: the display uses the panel's default stack as
     * a representative output while craft() preserves the actual input panel and changes its dye.
     */
    override fun isIgnoredInRecipeBook(): Boolean = false

    override fun getIngredientPlacement(): IngredientPlacement =
        IngredientPlacement.forShapeless(INGREDIENTS)

    override fun getDisplays(): List<RecipeDisplay> = listOf(
        ShapelessCraftingRecipeDisplay(
            INGREDIENTS.map(Ingredient::toDisplay),
            SlotDisplay.ItemSlotDisplay(PuzzlePanelItem.ITEM),
            SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
        )
    )
}

/**
 * Replaces the nbtcrafting-based `puzzle_panel_grid_recycle(_compat)` recipes: crafting a lone
 * puzzle panel returns ancient puzzle tablets — as many as the panel's stored `cost`, or 4 for
 * legacy/compat panels without a cost component.
 */
class PanelRecycleRecipe(category: CraftingRecipeCategory) : SpecialCraftingRecipe(category) {

    companion object {
        private const val DEFAULT_RECYCLE_COUNT = 4
        private val PANEL_INGREDIENT: Ingredient = Ingredient.ofItem(PuzzlePanelItem.ITEM)

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
        val count: Int = panelStack.cost ?: DEFAULT_RECYCLE_COUNT
        return ItemStack(AncientPuzzleTablet.ITEM, count)
    }

    override fun getSerializer(): RecipeSerializer<out SpecialCraftingRecipe> = SERIALIZER

    override fun isIgnoredInRecipeBook(): Boolean = false

    override fun getIngredientPlacement(): IngredientPlacement =
        IngredientPlacement.forSingleSlot(PANEL_INGREDIENT)

    override fun getDisplays(): List<RecipeDisplay> = listOf(
        ShapelessCraftingRecipeDisplay(
            listOf(PANEL_INGREDIENT.toDisplay()),
            SlotDisplay.StackSlotDisplay(ItemStack(AncientPuzzleTablet.ITEM, DEFAULT_RECYCLE_COUNT)),
            SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
        )
    )
}
