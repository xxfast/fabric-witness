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
 * The one recipe that builds and grows every puzzle panel.
 *
 * The crafting grid is a schematic: one slot is the source, every other slot adds a row or column of
 * cells.  A tablet *is* a 1×1-cell panel that cost 1 tablet, so a grid of nothing but tablets builds
 * a panel from scratch through the same arithmetic that grows one, and the nine hardcoded
 * `puzzle_panel_grid_*.json` base recipes it replaced are just its single-craft cases.
 *
 * It lives in code rather than JSON because the result depends on the source panel's own data (size,
 * colour, cost), which a static shaped recipe can't read.
 */
class PanelGridRecipe(category: CraftingRecipeCategory) : SpecialCraftingRecipe(category) {

    companion object {
        private val TABLET_INGREDIENT: Ingredient = Ingredient.ofItem(AncientPuzzleTablet.ITEM)

        val IDENTIFIER: Identifier = Identifier.of(Witness.IDENTIFIER, "panel_grid")
        val SERIALIZER: RecipeSerializer<PanelGridRecipe> = Registry.register(
            Registries.RECIPE_SERIALIZER,
            IDENTIFIER,
            SpecialRecipeSerializer(::PanelGridRecipe)
        )

        /** Referenced from common init to register this serializer before datapacks load. */
        fun init() {}

        /** What a tablet is worth as a source: one tablet, spent. */
        private const val SEED_COST: Int = 1

        /**
         * A tablet standing in as a source panel: blank, 1×1 cell, 2×2 nodes.
         *
         * Built fresh per craft rather than cached, because [Panel.Grid.expandTo] hands the instance
         * straight back for a 1×1 footprint and a shared graph would then be live on the crafted
         * itemstack.
         */
        private fun seedPanel(): Panel.Grid = Panel.Grid.ofSize(2, 2)
    }

    override fun matches(input: CraftingRecipeInput, world: World): Boolean = plan(input) != null

    override fun craft(input: CraftingRecipeInput, registries: RegistryWrapper.WrapperLookup): ItemStack {
        val planned: Plan = plan(input) ?: return ItemStack.EMPTY
        // Copying the source stack carries its other components forward; a seed has none to carry.
        val result: ItemStack = planned.source?.copyWithCount(1) ?: ItemStack(PuzzlePanelItem.ITEM)
        return result.apply {
            panel = planned.panel.expandTo(planned.width, planned.height, planned.offsetX, planned.offsetY)
            cost = planned.cost
        }
    }

    override fun getSerializer(): RecipeSerializer<out SpecialCraftingRecipe> = SERIALIZER

    override fun isIgnoredInRecipeBook(): Boolean = false

    /**
     * Representative layouts for the recipe book and JEI. The seed layouts come first because they
     * are the entry point, and they stand in for the nine deleted `puzzle_panel_grid_*.json` recipes
     * so nothing vanishes from the book.
     */
    override fun getDisplays(): List<RecipeDisplay> = seedDisplays() + growthDisplays()

    /** A footprint of nothing but tablets, and the panel it builds from scratch. */
    private fun seedDisplays(): List<RecipeDisplay> = PanelGridLayouts.seedFootprints.map { (cellsWide, cellsHigh) ->
        ShapedCraftingRecipeDisplay(
            cellsWide,
            cellsHigh,
            List(cellsWide * cellsHigh) { TABLET_INGREDIENT.toDisplay() },
            SlotDisplay.StackSlotDisplay(panelStack(cellsWide + 1, cellsHigh + 1, cellsWide * cellsHigh)),
            SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
        )
    }

    /** One display per growth layout; input stacks show the required source dimensions. */
    private fun growthDisplays(): List<RecipeDisplay> = PanelGridLayouts.displays.map { layout ->
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
     * Returns the panel [input] describes, or null when its shape, source panel, or component data
     * doesn't satisfy the rule.
     *
     * One slot is the source and every other slot pays for a row or column.  When no panel is
     * present a tablet takes the source's place as a blank 1×1-cell panel costing 1, which is what
     * makes building from scratch the same arithmetic as growing.  All the tablets in that case are
     * interchangeable, so which one is picked cannot affect the result.
     */
    internal fun plan(input: CraftingRecipeInput): Plan? {
        val occupied: List<OccupiedSlot> = buildList {
            for (y in 0 until input.height) {
                for (x in 0 until input.width) {
                    input.getStackInSlot(x, y).takeUnless(ItemStack::isEmpty)?.let { stack ->
                        add(OccupiedSlot(x, y, stack))
                    }
                }
            }
        }

        val panels: List<OccupiedSlot> = occupied.filter { it.stack.item is PuzzlePanelItem }
        if (panels.size > 1) return null
        val tablets: List<OccupiedSlot> = occupied.filter { it.stack.item is AncientPuzzleTablet }
        // Anything that is neither a panel nor a tablet is a stray item.
        if (occupied.size != tablets.size + panels.size) return null

        val sourceSlot: OccupiedSlot = panels.singleOrNull() ?: tablets.firstOrNull() ?: return null
        val sourceIsPanel: Boolean = panels.isNotEmpty()

        val panel: Panel.Grid =
            if (!sourceIsPanel) seedPanel() else sourceSlot.stack.panel as? Panel.Grid ?: return null
        val previousCost: Int =
            if (!sourceIsPanel) SEED_COST else sourceSlot.stack.cost ?: return null

        val bounds: Bounds = Bounds.of(occupied) ?: return null
        val sourceX: Int = sourceSlot.x - bounds.minX
        val sourceY: Int = sourceSlot.y - bounds.minY
        // The source occupies one slot whether it is a panel or the seed tablet, so everything else
        // in the footprint is what was paid.
        val paid: Int = occupied.size - 1
        val target: Pair<Int, Int> = PanelGridLayouts.target(
            panel.width,
            panel.height,
            paid,
            bounds.width,
            bounds.height,
            sourceX,
            sourceY,
            bounds.isFilledBy(occupied),
            sourceIsPanel,
        ) ?: return null

        return Plan(
            source = sourceSlot.stack.takeIf { sourceIsPanel },
            panel = panel,
            width = target.first,
            height = target.second,
            cost = previousCost + paid,
            offsetX = PanelGridLayouts.anchorOffset(bounds.width, sourceX),
            offsetY = PanelGridLayouts.anchorOffset(bounds.height, sourceY),
        )
    }

    internal data class Plan(
        /** The panel being grown, or null when this craft seeds a new one from tablets alone. */
        val source: ItemStack?,
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
internal object PanelGridLayouts {

    /**
     * Every footprint of pure tablets a 3×3 crafting grid can hold, in cells. These are the nine
     * base-grid recipes that used to be hardcoded JSON, and they are the complete set of panels
     * reachable in a single craft from nothing.
     */
    val seedFootprints: List<Pair<Int, Int>> = listOf(
        1 to 1,
        1 to 2, 2 to 1,
        1 to 3, 3 to 1,
        2 to 2,
        2 to 3, 3 to 2,
        3 to 3,
    )

    /**
     * Representative growth layouts for the recipe book and JEI, not an exhaustive list: [target]
     * accepts any filled rectangle, which is infinite. The first fourteen are the pre-formula
     * whitelist, kept so nothing vanishes from the recipe book; the rest show that larger sources
     * work the same way.
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
     * result.  This function doesn't read them; [PanelGridRecipe.plan] turns them into the
     * anchor offsets via [anchorOffset].
     *
     * [tabletCount] is what was *paid*, i.e. the occupied slots minus the source's own.  When
     * [sourceIsPanel] is false the source is a seed tablet, so one fewer tablet is charged than the
     * footprint holds.
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
        sourceIsPanel: Boolean = true,
    ): Pair<Int, Int>? {
        if (!isFilledRectangle) return null

        // A panel with nothing added is a lone panel, which belongs to PanelRecycleRecipe. A seed has
        // no equivalent case: a 1×1 footprint is the single-tablet craft that makes a 1×1-cell panel.
        if (sourceIsPanel && tabletCount < 1) return null

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
