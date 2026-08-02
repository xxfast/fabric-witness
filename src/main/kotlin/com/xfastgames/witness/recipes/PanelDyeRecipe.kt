package com.xfastgames.witness.recipes

import com.mojang.serialization.MapCodec
import com.xfastgames.witness.Witness
import com.xfastgames.witness.items.AncientPuzzleTablet
import com.xfastgames.witness.items.PuzzlePanelItem
import com.xfastgames.witness.items.data.Panel
import com.xfastgames.witness.items.data.cost
import com.xfastgames.witness.items.data.panel
import net.minecraft.core.Registry
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.Identifier
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemStackTemplate
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.CustomRecipe
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.PlacementInfo
import net.minecraft.world.item.crafting.RecipeBookCategories
import net.minecraft.world.item.crafting.RecipeBookCategory
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.display.RecipeDisplay
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay
import net.minecraft.world.item.crafting.display.SlotDisplay
import net.minecraft.world.level.Level

/**
 * Replaces the dead nbtcrafting-based `puzzle_panel_color_*` recipes (1.17): crafting a puzzle
 * panel together with any dye produces the same panel with its background colour changed, keeping
 * all other puzzle data (now stored in the `witness:panel` data component).
 *
 * Dyes carry colour via [DataComponents.DYE] in 26.2 (no DyeItem.byColor).
 */
class PanelDyeRecipe : CustomRecipe() {

    companion object {
        private val PANEL_INGREDIENT: Ingredient = Ingredient.of(PuzzlePanelItem.ITEM)
        private val DYE_INGREDIENT: Ingredient =
            Ingredient.of(Items.DYE.asList().stream())
        private val INGREDIENTS: List<Ingredient> = listOf(PANEL_INGREDIENT, DYE_INGREDIENT)

        val INSTANCE: PanelDyeRecipe = PanelDyeRecipe()
        val MAP_CODEC: MapCodec<PanelDyeRecipe> = MapCodec.unit(INSTANCE)
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, PanelDyeRecipe> = StreamCodec.unit(INSTANCE)

        val IDENTIFIER: Identifier = Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, "panel_dye")
        val SERIALIZER: RecipeSerializer<PanelDyeRecipe> = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER,
            IDENTIFIER,
            RecipeSerializer(MAP_CODEC, STREAM_CODEC)
        )

        /** Referenced from mod init to force registration of the recipe serializers. */
        fun init() {
            PanelRecycleRecipe.SERIALIZER
            PanelGridRecipe.SERIALIZER
        }
    }

    override fun matches(input: CraftingInput, world: Level): Boolean {
        var panels = 0
        var dyes = 0
        input.items().forEach { stack ->
            when {
                stack.isEmpty -> Unit
                stack.item is PuzzlePanelItem -> panels++
                stack.has(DataComponents.DYE) -> dyes++
                else -> return false
            }
        }
        return panels == 1 && dyes == 1
    }

    override fun assemble(input: CraftingInput): ItemStack {
        val panelStack: ItemStack = input.items().firstOrNull { it.item is PuzzlePanelItem } ?: return ItemStack.EMPTY
        val dyeStack: ItemStack = input.items().firstOrNull { it.has(DataComponents.DYE) } ?: return ItemStack.EMPTY
        val updatedColor: DyeColor = dyeStack.get(DataComponents.DYE) ?: return ItemStack.EMPTY

        val puzzle: Panel = panelStack.panel ?: return ItemStack.EMPTY
        val tintedPanel: Panel = when (puzzle) {
            is Panel.Grid -> puzzle.copy(backgroundColor = updatedColor)
            is Panel.Tree -> puzzle.copy(backgroundColor = updatedColor)
            is Panel.Freeform -> puzzle.copy(backgroundColor = updatedColor)
        }

        return panelStack.copyWithCount(1).apply { panel = tintedPanel }
    }

    override fun getSerializer(): RecipeSerializer<out CustomRecipe> = SERIALIZER

    override fun isSpecial(): Boolean = false

    override fun placementInfo(): PlacementInfo = PlacementInfo.create(INGREDIENTS)

    override fun display(): List<RecipeDisplay> = listOf(
        ShapelessCraftingRecipeDisplay(
            INGREDIENTS.map(Ingredient::display),
            SlotDisplay.ItemSlotDisplay(PuzzlePanelItem.ITEM),
            SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
        )
    )

    override fun recipeBookCategory(): RecipeBookCategory = RecipeBookCategories.CRAFTING_MISC
}

/**
 * Replaces the nbtcrafting-based `puzzle_panel_grid_recycle(_compat)` recipes: crafting a lone
 * puzzle panel returns ancient puzzle tablets, as many as the panel's stored `cost`, or 4 for
 * legacy/compat panels without a cost component.
 */
class PanelRecycleRecipe : CustomRecipe() {

    companion object {
        private const val DEFAULT_RECYCLE_COUNT = 4
        private val PANEL_INGREDIENT: Ingredient = Ingredient.of(PuzzlePanelItem.ITEM)

        val INSTANCE: PanelRecycleRecipe = PanelRecycleRecipe()
        val MAP_CODEC: MapCodec<PanelRecycleRecipe> = MapCodec.unit(INSTANCE)
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, PanelRecycleRecipe> = StreamCodec.unit(INSTANCE)

        val IDENTIFIER: Identifier = Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, "panel_recycle")
        val SERIALIZER: RecipeSerializer<PanelRecycleRecipe> = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER,
            IDENTIFIER,
            RecipeSerializer(MAP_CODEC, STREAM_CODEC)
        )
    }

    override fun matches(input: CraftingInput, world: Level): Boolean {
        val stacks: List<ItemStack> = input.items().filterNot { it.isEmpty }
        return stacks.size == 1 && stacks.single().item is PuzzlePanelItem
    }

    override fun assemble(input: CraftingInput): ItemStack {
        val panelStack: ItemStack = input.items().firstOrNull { it.item is PuzzlePanelItem } ?: return ItemStack.EMPTY
        val count: Int = panelStack.cost ?: DEFAULT_RECYCLE_COUNT
        return ItemStack(AncientPuzzleTablet.ITEM, count)
    }

    override fun getSerializer(): RecipeSerializer<out CustomRecipe> = SERIALIZER

    override fun isSpecial(): Boolean = false

    override fun placementInfo(): PlacementInfo = PlacementInfo.create(PANEL_INGREDIENT)

    override fun display(): List<RecipeDisplay> = listOf(
        ShapelessCraftingRecipeDisplay(
            listOf(PANEL_INGREDIENT.display()),
            SlotDisplay.ItemStackSlotDisplay(
                ItemStackTemplate(AncientPuzzleTablet.ITEM, DEFAULT_RECYCLE_COUNT)
            ),
            SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
        )
    )

    override fun recipeBookCategory(): RecipeBookCategory = RecipeBookCategories.CRAFTING_MISC
}
