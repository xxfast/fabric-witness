package com.xfastgames.witness.recipes

import com.xfastgames.witness.Witness
import com.xfastgames.witness.items.AncientPuzzleTablet
import com.xfastgames.witness.items.PuzzlePanelItem
import com.xfastgames.witness.items.data.Panel
import com.xfastgames.witness.items.data.cost
import com.xfastgames.witness.items.data.panel
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.recipe.SpecialCraftingRecipe
import net.minecraft.recipe.book.CraftingRecipeCategory
import net.minecraft.recipe.display.RecipeDisplay
import net.minecraft.recipe.display.ShapedCraftingRecipeDisplay
import net.minecraft.recipe.display.SlotDisplay
import net.minecraft.recipe.input.CraftingRecipeInput
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryWrapper
import net.minecraft.util.Identifier
import net.minecraft.world.World

/**
 * Replaces the nbtcrafting-backed grid-expansion recipes.
 *
 * Vanilla recipe JSON can assign a literal data component, but cannot express the old
 * `input.cost + tablets` calculation or copy the input panel's background colour.  The legacy
 * recipes accepted the layouts handled by [upgrade] below; keeping that table here makes the
 * dynamic part explicit while the nine base-grid recipes remain ordinary shaped JSON recipes.
 */
class PanelGridUpgradeRecipe(category: CraftingRecipeCategory) : SpecialCraftingRecipe(category) {

    companion object {
        private val TABLET_INGREDIENT: Ingredient = Ingredient.ofItem(AncientPuzzleTablet.ITEM)

        val IDENTIFIER: Identifier = Identifier.of(Witness.IDENTIFIER, "panel_grid_upgrade")
        val SERIALIZER: RecipeSerializer<PanelGridUpgradeRecipe> = Registry.register(
            Registries.RECIPE_SERIALIZER,
            IDENTIFIER,
            SpecialRecipeSerializer(::PanelGridUpgradeRecipe)
        )

        /** Referenced from common init to register this serializer before datapacks load. */
        fun init() {}
    }

    override fun matches(input: CraftingRecipeInput, world: World): Boolean = upgrade(input) != null

    override fun craft(input: CraftingRecipeInput, registries: RegistryWrapper.WrapperLookup): ItemStack {
        val upgrade: Upgrade = upgrade(input) ?: return ItemStack.EMPTY
        return upgrade.source.copyWithCount(1).apply {
            panel = upgrade.panel.expandTo(upgrade.width, upgrade.height, upgrade.offsetX, upgrade.offsetY)
            cost = upgrade.cost
        }
    }

    override fun getSerializer(): RecipeSerializer<out SpecialCraftingRecipe> = SERIALIZER

    override fun isIgnoredInRecipeBook(): Boolean = false

    /** One display for each distinct old layout; input stacks show the required source dimensions. */
    override fun getDisplays(): List<RecipeDisplay> = PanelGridUpgradeLayouts.displays.map { layout ->
        ShapedCraftingRecipeDisplay(
            layout.layoutWidth,
            layout.layoutHeight,
            List(layout.layoutWidth * layout.layoutHeight) { slot ->
                val x: Int = slot % layout.layoutWidth
                val y: Int = slot / layout.layoutWidth
                if (x == layout.sourceX && y == layout.sourceY) {
                    SlotDisplay.StackSlotDisplay(panelStack(layout.sourceWidth, layout.sourceHeight, layout.inputCost))
                } else {
                    TABLET_INGREDIENT.toDisplay()
                }
            },
            SlotDisplay.StackSlotDisplay(panelStack(layout.targetWidth, layout.targetHeight, layout.inputCost + layout.tabletCount)),
            SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
        )
    }

    private fun panelStack(width: Int, height: Int, cost: Int): ItemStack = ItemStack(PuzzlePanelItem.ITEM).apply {
        panel = Panel.Grid.ofSize(width, height)
        this.cost = cost
    }

    /**
     * Returns the legacy expansion selected by [input], or null when its shape, source panel, or
     * component data does not match one of the old NbtCrafting recipes.
     */
    internal fun upgrade(input: CraftingRecipeInput): Upgrade? {
        val occupied: List<OccupiedSlot> = buildList {
            for (y in 0 until input.height) {
                for (x in 0 until input.width) {
                    input.getStackInSlot(x, y).takeUnless(ItemStack::isEmpty)?.let { stack ->
                        add(OccupiedSlot(x, y, stack))
                    }
                }
            }
        }

        val source: OccupiedSlot = occupied.singleOrNull { it.stack.item is PuzzlePanelItem } ?: return null
        val tablets: List<OccupiedSlot> = occupied.filter { it.stack.item is AncientPuzzleTablet }
        if (occupied.size != tablets.size + 1) return null

        val panel: Panel.Grid = source.stack.panel as? Panel.Grid ?: return null
        val previousCost: Int = source.stack.cost ?: return null
        val bounds: Bounds = Bounds.of(occupied) ?: return null
        val sourceX: Int = source.x - bounds.minX
        val sourceY: Int = source.y - bounds.minY
        val target: Pair<Int, Int> = PanelGridUpgradeLayouts.target(
            panel.width,
            panel.height,
            tablets.size,
            bounds.width,
            bounds.height,
            sourceX,
            sourceY,
            bounds.isFilledBy(occupied),
        ) ?: return null

        return Upgrade(
            source = source.stack,
            panel = panel,
            width = target.first,
            height = target.second,
            cost = previousCost + tablets.size,
            offsetX = PanelGridUpgradeLayouts.anchorOffset(bounds.width, sourceX),
            offsetY = PanelGridUpgradeLayouts.anchorOffset(bounds.height, sourceY),
        )
    }

    internal data class Upgrade(
        val source: ItemStack,
        val panel: Panel.Grid,
        val width: Int,
        val height: Int,
        val cost: Int,
        val offsetX: Int,
        val offsetY: Int,
    )

    private data class OccupiedSlot(val x: Int, val y: Int, val stack: ItemStack)

    private data class Bounds(val minX: Int, val minY: Int, val maxX: Int, val maxY: Int) {
        val width: Int get() = maxX - minX + 1
        val height: Int get() = maxY - minY + 1

        fun isFilledBy(occupied: List<OccupiedSlot>): Boolean = occupied.size == width * height

        companion object {
            fun of(occupied: List<OccupiedSlot>): Bounds? = occupied.takeIf(List<OccupiedSlot>::isNotEmpty)?.let {
                Bounds(
                    minX = it.minOf(OccupiedSlot::x),
                    minY = it.minOf(OccupiedSlot::y),
                    maxX = it.maxOf(OccupiedSlot::x),
                    maxY = it.maxOf(OccupiedSlot::y),
                )
            }
        }
    }
}

/** Pure layout maths, kept separate so it can be regression-tested without Minecraft bootstrap. */
internal object PanelGridUpgradeLayouts {

    /**
     * Representative layouts for the recipe book and JEI, not an exhaustive list: [target] accepts
     * any filled rectangle, which is infinite. The first fourteen are the pre-formula whitelist, kept
     * so nothing vanishes from the recipe book; the rest show that larger sources work the same way.
     */
    val displays: List<DisplayLayout> = listOf(
        DisplayLayout(2, 2, 1, 2, 2, 3),
        DisplayLayout(2, 2, 2, 1, 3, 2),
        DisplayLayout(2, 3, 1, 2, 2, 4),
        DisplayLayout(2, 3, 2, 1, 3, 3),
        DisplayLayout(3, 2, 1, 2, 3, 3),
        DisplayLayout(3, 2, 2, 1, 4, 2),
        DisplayLayout(2, 4, 2, 1, 3, 4),
        DisplayLayout(4, 2, 1, 2, 4, 3),
        DisplayLayout(3, 3, 1, 2, 3, 4),
        DisplayLayout(3, 3, 2, 1, 4, 3),
        DisplayLayout(3, 3, 2, 2, 4, 4),
        DisplayLayout(3, 4, 2, 1, 4, 4),
        DisplayLayout(4, 3, 1, 2, 4, 4),
        DisplayLayout(2, 2, 3, 3, 4, 4, sourceX = 1, sourceY = 1),
        DisplayLayout(4, 4, 2, 1, 5, 4),
        DisplayLayout(5, 4, 1, 2, 5, 5),
        DisplayLayout(6, 6, 3, 3, 8, 8, sourceX = 1, sourceY = 1),
    )

    /**
     * Where the source lands inside the result, on one axis: the number of new rows or columns that
     * go *before* it in node-index order.
     *
     * Both axes are mirrored between the two coordinate systems, so both get the same flip. Verified
     * in game rather than derived: tablets placed to the right of the panel have to grow the panel to
     * the right, and tablets above it have to grow it upwards, in the frame and in the item icon
     * alike.
     */
    fun anchorOffset(footprint: Int, sourcePosition: Int): Int = footprint - 1 - sourcePosition

    /**
     * The crafting grid is a schematic: the panel slot is the panel you have, every tablet slot next
     * to it is one more row or column of cells.  So each axis grows by one node per extra slot along
     * it, whatever the source size, which is why this is arithmetic rather than the old whitelist.
     *
     * [sourceX] and [sourceY] say which sides grow, and so where the source's content lands in the
     * result.  Nothing reads them yet: [PanelGridUpgradeRecipe.craft] still rebuilds a blank grid.
     */
    @Suppress("UNUSED_PARAMETER")
    fun target(
        sourceWidth: Int,
        sourceHeight: Int,
        tabletCount: Int,
        layoutWidth: Int,
        layoutHeight: Int,
        sourceX: Int,
        sourceY: Int,
        isFilledRectangle: Boolean,
    ): Pair<Int, Int>? {
        if (!isFilledRectangle) return null

        // No tablets is a lone panel, which belongs to PanelRecycleRecipe.
        if (tabletCount < 1) return null

        // The footprint is the source's slot plus one slot per tablet, so anything else is a gap.
        if (tabletCount != layoutWidth * layoutHeight - 1) return null

        val width: Int = sourceWidth + layoutWidth - 1
        val height: Int = sourceHeight + layoutHeight - 1
        if (width > Panel.Grid.MAX_NODES || height > Panel.Grid.MAX_NODES) return null

        return width to height
    }

    internal data class DisplayLayout(
        val sourceWidth: Int,
        val sourceHeight: Int,
        val layoutWidth: Int,
        val layoutHeight: Int,
        val targetWidth: Int,
        val targetHeight: Int,
        val sourceX: Int = 0,
        val sourceY: Int = 0,
    ) {
        val tabletCount: Int = layoutWidth * layoutHeight - 1
        val inputCost: Int = (sourceWidth - 1) * (sourceHeight - 1)
    }
}
