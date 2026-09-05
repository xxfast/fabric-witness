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
 * The tree panel craft (rules/minecraft/01-1-tree-panel.md#the-rule).
 *
 * A sapling is the seed of a tree panel: stack tablets on top of it in a single column and each
 * tablet is one level of tree. Put a tree panel at the bottom instead and it grows by one level per
 * tablet, its marks carried by branch position ([Panel.Tree.expandTo]). The reading is disjoint
 * from [PanelGridRecipe]'s by construction, since no grid craft contains a sapling or a tree.
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

    /** A matched column: the levels it produces, and the tree it grows if it is not seeding one. */
    internal data class Plan(val levels: Int, val cost: Int, val source: ItemStack?)

    override fun matches(input: CraftingInput, world: Level): Boolean = plan(input) != null

    override fun assemble(input: CraftingInput): ItemStack {
        val planned: Plan = plan(input) ?: return ItemStack.EMPTY
        // Copying the source stack carries its other components forward; a seed has none to carry.
        val result: ItemStack = planned.source?.copyWithCount(1) ?: ItemStack(PuzzlePanelItem.ITEM)
        val sourceTree: Panel.Tree? = planned.source?.panel as? Panel.Tree
        return result.apply {
            panel = sourceTree?.expandTo(planned.levels) ?: Panel.Tree.ofSize(planned.levels)
            cost = planned.cost
        }
    }

    override fun getSerializer(): RecipeSerializer<out CustomRecipe> = SERIALIZER

    override fun isSpecial(): Boolean = false

    override fun placementInfo(): net.minecraft.world.item.crafting.PlacementInfo =
        net.minecraft.world.item.crafting.PlacementInfo.NOT_PLACEABLE

    override fun recipeBookCategory(): net.minecraft.world.item.crafting.RecipeBookCategory =
        net.minecraft.world.item.crafting.RecipeBookCategories.CRAFTING_MISC

    /**
     * One display per seedable height, the column with the sapling on the bottom row, plus the
     * one grow that reaches the cap. Illustrative: growing is one rule over every tree.
     */
    override fun display(): List<RecipeDisplay> {
        val seeds: List<RecipeDisplay> = PanelTreeLayouts.seedColumns.map { levels ->
            column(levels, saplingIngredient().display(), treeTemplate(levels, cost = levels))
        }
        val seedMax: Int = PanelTreeLayouts.seedColumns.max()
        val grown: RecipeDisplay = column(
            tablets = Panel.Tree.MAX_LEVELS - seedMax,
            bottom = SlotDisplay.ItemStackSlotDisplay(treeTemplate(levels = seedMax, cost = seedMax)),
            result = treeTemplate(levels = Panel.Tree.MAX_LEVELS, cost = Panel.Tree.MAX_LEVELS)
        )
        return seeds + grown
    }

    private fun column(tablets: Int, bottom: SlotDisplay, result: ItemStackTemplate): RecipeDisplay =
        ShapedCraftingRecipeDisplay(
            1,
            tablets + 1,
            List(tablets) { TABLET_INGREDIENT.display() } + bottom,
            SlotDisplay.ItemStackSlotDisplay(result),
            SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
        )

    /**
     * Built per call rather than cached: tag contents only bind once datapacks load, well after
     * this class registers its serializer.
     */
    private fun saplingIngredient(): Ingredient =
        Ingredient.of(BuiltInRegistries.ITEM.getOrThrow(ItemTags.SAPLINGS))

    private fun treeTemplate(levels: Int, cost: Int): ItemStackTemplate =
        ItemStackTemplate.fromNonEmptyStack(
            ItemStack(PuzzlePanelItem.ITEM).apply {
                panel = Panel.Tree.ofSize(levels)
                this.cost = cost
            }
        )

    /**
     * What the layout crafts, or null when it isn't a tree craft. Any stray item (neither a
     * sapling, a tablet, nor a tree panel with a cost) rejects the whole layout; the column shape
     * itself is [PanelTreeLayouts.levels]'s business.
     */
    internal fun plan(input: CraftingInput): Plan? {
        var source: ItemStack? = null
        val occupied: List<PanelTreeLayouts.Slot> = buildList {
            for (y in 0 until input.height()) {
                for (x in 0 until input.width()) {
                    val stack: ItemStack = input.getItem(x, y).takeUnless(ItemStack::isEmpty) ?: continue
                    val slot: PanelTreeLayouts.Slot = when {
                        stack.`is`(ItemTags.SAPLINGS) -> PanelTreeLayouts.Slot(x, y, PanelTreeLayouts.Kind.SAPLING)
                        stack.item is AncientPuzzleTablet -> PanelTreeLayouts.Slot(x, y, PanelTreeLayouts.Kind.TABLET)
                        stack.item is PuzzlePanelItem -> {
                            val tree: Panel.Tree = stack.panel as? Panel.Tree ?: return null
                            if (source != null || stack.cost == null) return null
                            source = stack
                            PanelTreeLayouts.Slot(x, y, PanelTreeLayouts.Kind.TREE, tree.levels)
                        }
                        else -> return null
                    }
                    add(slot)
                }
            }
        }
        val levels: Int = PanelTreeLayouts.levels(occupied) ?: return null
        val tablets: Int = occupied.count { it.kind == PanelTreeLayouts.Kind.TABLET }
        val previousCost: Int = source?.cost ?: 0
        return Plan(levels, cost = previousCost + tablets, source = source)
    }
}

/** Pure layout maths, kept separate so it can be regression-tested without Minecraft bootstrap. */
internal object PanelTreeLayouts {

    enum class Kind { SAPLING, TABLET, TREE }

    /** [levels] is only meaningful for a [Kind.TREE] slot: the levels the tree in it already has. */
    data class Slot(val x: Int, val y: Int, val kind: Kind, val levels: Int = 0)

    /** Every height a 3×3 crafting grid can seed: the column is at most 3 slots, one of them the sapling. */
    val seedColumns: List<Int> = listOf(1, 2)

    /**
     * The levels the occupied slots describe, or null when they are not a tree craft: a single
     * contiguous column, a sapling or a tree panel on the bottom row (crafting-grid y runs down,
     * so the bottom is the highest y), one tablet per level above it, within the height cap. A
     * sapling seeds from nothing; a tree grows from the levels it has.
     */
    fun levels(occupied: List<Slot>): Int? {
        if (occupied.size < 2) return null

        val column: Int = occupied.first().x
        if (occupied.any { slot -> slot.x != column }) return null

        val rows: List<Int> = occupied.map(Slot::y).sorted()
        if (rows.last() - rows.first() != rows.size - 1) return null

        val bottom: Slot = occupied.maxBy(Slot::y)
        if (bottom.kind == Kind.TABLET) return null

        // Exactly one seed: anything else in the column has to be a tablet.
        val tablets: Int = occupied.count { slot -> slot.kind == Kind.TABLET }
        if (tablets != occupied.size - 1) return null

        val levels: Int = bottom.levels + tablets
        if (levels > Panel.Tree.MAX_LEVELS) return null
        return levels
    }
}
