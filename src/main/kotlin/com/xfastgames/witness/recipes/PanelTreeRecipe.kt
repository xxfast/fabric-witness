package com.xfastgames.witness.recipes

import com.mojang.serialization.MapCodec
import com.xfastgames.witness.Witness
import com.xfastgames.witness.items.AncientPuzzleTablet
import com.xfastgames.witness.items.PuzzlePanelItem
import com.xfastgames.witness.items.data.Panel
import com.xfastgames.witness.items.data.cost
import com.xfastgames.witness.items.data.panel
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.Identifier
import net.minecraft.tags.ItemTags
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemStackTemplate
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.CustomRecipe
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.display.RecipeDisplay
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay
import net.minecraft.world.item.crafting.display.SlotDisplay
import net.minecraft.world.level.Level

/**
 * The tree half of the panel-crafting rule (rules/minecraft/01-puzzle-panel-crafting.md#tree-panels).
 *
 * A sapling is the seed of a tree panel: stack tablets on top of it in a single column and each
 * tablet is one level of tree. The reading is disjoint from [PanelGridRecipe]'s by construction,
 * since no grid craft contains a sapling.
 *
 * Growing an existing tree (a tree panel at the bottom of the column instead of the sapling) is
 * designed but not built; this recipe only builds from scratch.
 */
class PanelTreeRecipe : CustomRecipe() {

    companion object {
        private val TABLET_INGREDIENT: Ingredient = Ingredient.of(AncientPuzzleTablet.ITEM)

        val INSTANCE: PanelTreeRecipe = PanelTreeRecipe()
        val MAP_CODEC: MapCodec<PanelTreeRecipe> = MapCodec.unit(INSTANCE)
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, PanelTreeRecipe> = StreamCodec.unit(INSTANCE)

        val IDENTIFIER: Identifier = Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, "panel_tree")
        val SERIALIZER: RecipeSerializer<PanelTreeRecipe> = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER,
            IDENTIFIER,
            RecipeSerializer(MAP_CODEC, STREAM_CODEC)
        )

        /** Referenced from common init to register this serializer before datapacks load. */
        fun init() {}
    }

    override fun matches(input: CraftingInput, world: Level): Boolean = plan(input) != null

    override fun assemble(input: CraftingInput): ItemStack {
        val levels: Int = plan(input) ?: return ItemStack.EMPTY
        return ItemStack(PuzzlePanelItem.ITEM).apply {
            panel = Panel.Tree.ofSize(levels)
            cost = levels
        }
    }

    override fun getSerializer(): RecipeSerializer<out CustomRecipe> = SERIALIZER

    override fun isSpecial(): Boolean = false

    override fun placementInfo(): net.minecraft.world.item.crafting.PlacementInfo =
        net.minecraft.world.item.crafting.PlacementInfo.NOT_PLACEABLE

    override fun recipeBookCategory(): net.minecraft.world.item.crafting.RecipeBookCategory =
        net.minecraft.world.item.crafting.RecipeBookCategories.CRAFTING_MISC

    /** One display per craftable height: the column with the sapling on the bottom row. */
    override fun display(): List<RecipeDisplay> = PanelTreeLayouts.seedColumns.map { levels ->
        ShapedCraftingRecipeDisplay(
            1,
            levels + 1,
            List(levels) { TABLET_INGREDIENT.display() } + saplingIngredient().display(),
            SlotDisplay.ItemStackSlotDisplay(treeTemplate(levels)),
            SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
        )
    }

    /**
     * Built per call rather than cached: tag contents only bind once datapacks load, well after
     * this class registers its serializer.
     */
    private fun saplingIngredient(): Ingredient =
        Ingredient.of(BuiltInRegistries.ITEM.getOrThrow(ItemTags.SAPLINGS))

    private fun treeTemplate(levels: Int): ItemStackTemplate =
        ItemStackTemplate.fromNonEmptyStack(
            ItemStack(PuzzlePanelItem.ITEM).apply {
                panel = Panel.Tree.ofSize(levels)
                cost = levels
            }
        )

    /**
     * The levels the layout describes, or null when it isn't a tree craft. Any stray item (neither
     * a sapling nor a tablet) rejects the whole layout; the column shape itself is
     * [PanelTreeLayouts.levels]'s business.
     */
    internal fun plan(input: CraftingInput): Int? {
        val occupied: List<PanelTreeLayouts.Slot> = buildList {
            for (y in 0 until input.height()) {
                for (x in 0 until input.width()) {
                    input.getItem(x, y).takeUnless(ItemStack::isEmpty)?.let { stack ->
                        val kind: PanelTreeLayouts.Kind = when {
                            stack.`is`(ItemTags.SAPLINGS) -> PanelTreeLayouts.Kind.SAPLING
                            stack.item is AncientPuzzleTablet -> PanelTreeLayouts.Kind.TABLET
                            else -> return null
                        }
                        add(PanelTreeLayouts.Slot(x, y, kind))
                    }
                }
            }
        }
        return PanelTreeLayouts.levels(occupied)
    }
}

/** Pure layout maths, kept separate so it can be regression-tested without Minecraft bootstrap. */
internal object PanelTreeLayouts {

    enum class Kind { SAPLING, TABLET }

    data class Slot(val x: Int, val y: Int, val kind: Kind)

    /** Every height a 3×3 crafting grid can seed: the column is at most 3 slots, one of them the sapling. */
    val seedColumns: List<Int> = listOf(1, 2)

    /**
     * The levels the occupied slots describe, or null when they are not a tree craft: a single
     * contiguous column, the sapling on the bottom row (crafting-grid y runs down, so the bottom is
     * the highest y), one tablet per level above it, within the height cap.
     */
    fun levels(occupied: List<Slot>): Int? {
        if (occupied.size < 2) return null

        val column: Int = occupied.first().x
        if (occupied.any { slot -> slot.x != column }) return null

        val rows: List<Int> = occupied.map(Slot::y).sorted()
        if (rows.last() - rows.first() != rows.size - 1) return null

        val bottom: Slot = occupied.maxBy(Slot::y)
        if (bottom.kind != Kind.SAPLING) return null

        // Exactly one sapling: anything else in the column has to be a tablet.
        val tablets: Int = occupied.count { slot -> slot.kind == Kind.TABLET }
        if (tablets != occupied.size - 1) return null

        if (tablets > Panel.Tree.MAX_LEVELS) return null
        return tablets
    }
}
