package com.xfastgames.witness.screens

import com.xfastgames.witness.Witness
import com.xfastgames.witness.blocks.redstone.PuzzleComposerBlock
import com.xfastgames.witness.entities.PuzzleComposerBlockEntity
import com.xfastgames.witness.items.PuzzlePanelItem
import com.xfastgames.witness.items.data.Panel
import com.xfastgames.witness.items.data.getPanel
import com.xfastgames.witness.items.data.putPanel
import com.xfastgames.witness.screens.PuzzleComposerScreen.Companion.PUZZLE_BACKGROUND_DYE_SLOT_INDEX
import com.xfastgames.witness.screens.PuzzleComposerScreen.Companion.PUZZLE_INPUT_SLOT_INDEX
import com.xfastgames.witness.screens.PuzzleComposerScreen.Companion.PUZZLE_INVENTORY_SLOT_INDEX
import com.xfastgames.witness.screens.PuzzleComposerScreen.Companion.PUZZLE_LINE_DYE_SLOT_INDEX
import com.xfastgames.witness.screens.PuzzleComposerScreen.Companion.PUZZLE_OUTPUT_SLOT_INDEX
import com.xfastgames.witness.screens.widgets.WPuzzleEditor
import com.xfastgames.witness.utils.BlockInventory
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.Colors
import com.xfastgames.witness.utils.isNotEmpty
import io.github.cottonmc.cotton.gui.SyncedGuiDescription
import io.github.cottonmc.cotton.gui.client.BackgroundPainter
import io.github.cottonmc.cotton.gui.client.CottonInventoryScreen
import io.github.cottonmc.cotton.gui.client.ScreenDrawing
import io.github.cottonmc.cotton.gui.widget.*
import io.github.cottonmc.cotton.gui.widget.data.Axis
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.DyeItem
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.text.Text
import net.minecraft.util.DyeColor
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos

val PUZZLE_COMPOSER_SCREEN_HANDLER: ScreenHandlerType<PuzzleComposerScreenDescription> =
    ScreenHandlerRegistry.registerExtended(PuzzleComposerBlock.IDENTIFIER) { syncId, playerInventory, buf ->
        val pos: BlockPos = buf.readBlockPos()
        PuzzleComposerScreenDescription(
            syncId,
            playerInventory,
            ScreenHandlerContext.create(playerInventory.player.world, pos)
        )
    }

class PuzzleComposerScreen(gui: PuzzleComposerScreenDescription?, player: PlayerEntity?, title: Text?) :
    CottonInventoryScreen<PuzzleComposerScreenDescription?>(gui, player, title) {

    companion object : Clientside {

        const val PUZZLE_INPUT_SLOT_INDEX = 0
        const val PUZZLE_BACKGROUND_DYE_SLOT_INDEX = 1
        const val PUZZLE_LINE_DYE_SLOT_INDEX = 2
        const val PUZZLE_INVENTORY_SLOT_INDEX = 3
        const val PUZZLE_OUTPUT_SLOT_INDEX = 7

        override fun onClient() {
            ScreenRegistry.register(PUZZLE_COMPOSER_SCREEN_HANDLER) { gui, inventory, title ->
                PuzzleComposerScreen(gui, inventory.player, title)
            }
        }
    }
}

class InputSlotBackgroundPainter(private val itemSlot: WItemSlot, private val texture: Identifier) : BackgroundPainter {
    override fun paintBackground(left: Int, top: Int, panel: WWidget?) {
        BackgroundPainter.SLOT.paintBackground(left, top, panel)
        ScreenDrawing.texturedRect(left, top, itemSlot.width, itemSlot.height, texture, Colors.TRANSPARENT.toRgb())
    }
}

class PuzzleComposerScreenDescription(
    syncId: Int,
    playerInventory: PlayerInventory,
    context: ScreenHandlerContext?
) : SyncedGuiDescription(
    PUZZLE_COMPOSER_SCREEN_HANDLER,
    syncId,
    playerInventory,
    getBlockInventory(context, PuzzleComposerBlockEntity.INVENTORY_SIZE),
    null
) {
    private val root: WPlainPanel = WPlainPanel().apply { setSize(150, 150) }
    private val inputSlot = WItemSlot(blockInventory, PUZZLE_INPUT_SLOT_INDEX, 1, 1, false)
    private val backgroundDyeSlot: WItemSlot = WItemSlot.of(blockInventory, PUZZLE_BACKGROUND_DYE_SLOT_INDEX, 1, 1)
    private val lineDyeSlot: WItemSlot = WItemSlot.of(blockInventory, PUZZLE_LINE_DYE_SLOT_INDEX, 1, 1)
    private val inventorySlots: WItemSlot = WItemSlot.of(blockInventory, PUZZLE_INVENTORY_SLOT_INDEX, 2, 2)
    private val outputSlot: WItemSlot = WItemSlot(blockInventory, PUZZLE_OUTPUT_SLOT_INDEX, 1, 1, true)
    private val editor = WPuzzleEditor(blockInventory, PUZZLE_OUTPUT_SLOT_INDEX)
    private val resizeSlider = WSlider(2, 10, Axis.VERTICAL)
    private val hotBarLabel = WLabel(playerInventory.displayName)
    private val hotBar: WItemSlot = WItemSlot.of(playerInventory, 0, 9, 1)

    private val placeholderDyeTexture = Identifier(Witness.IDENTIFIER, "textures/gui/placeholder_dye.png")
    private val placeholderPuzzleTexture = Identifier(Witness.IDENTIFIER, "textures/gui/placeholder_puzzle.png")

    private fun updateInventory(slotIndex: Int, itemStack: ItemStack) {
        val inventory: Inventory = blockInventory
        require(inventory is BlockInventory)
        require(inventory.owner is PuzzleComposerBlockEntity)
        if (inventory.owner.world?.isClient == true) inventory.owner.syncInventorySlotTag(slotIndex, itemStack)
    }

    init {
        setRootPanel(root)
        backgroundDyeSlot.setFilter { itemStack -> itemStack.item is DyeItem }
        lineDyeSlot.setFilter { itemStack -> itemStack.item is DyeItem }
        inputSlot.setFilter { itemStack -> itemStack.item is PuzzlePanelItem }
        outputSlot.setFilter { itemStack -> itemStack.item is PuzzlePanelItem }
        inputSlot.isInsertingAllowed = true
        outputSlot.isModifiable = false
        outputSlot.isTakingAllowed = true

        resizeSlider.setValueChangeListener { value ->
            val itemStack: ItemStack = blockInventory.getStack(PUZZLE_OUTPUT_SLOT_INDEX)
            val outputTag: CompoundTag = itemStack.tag ?: return@setValueChangeListener
            if (outputTag.isEmpty) return@setValueChangeListener
            val puzzle: Panel = outputTag.getPanel()
            val updatedPuzzle: Panel = puzzle.resize(value)
            val updatedStack: ItemStack = itemStack.copy().apply { tag?.putPanel(updatedPuzzle) }
            updateInventory(PUZZLE_OUTPUT_SLOT_INDEX, updatedStack)
        }

        inputSlot.addChangeListener { _, inventory, index, changedItemStack ->
            // if empty remove the output
            if (changedItemStack.isEmpty) {
                updateInventory(PUZZLE_OUTPUT_SLOT_INDEX, ItemStack.EMPTY)
                return@addChangeListener
            }

            if (index != PUZZLE_INPUT_SLOT_INDEX) return@addChangeListener
            val width: Int = changedItemStack.tag?.getPanel()?.width ?: 0
            resizeSlider.setValue(width, false)

            // Stack is used to distinguish changes from the screen load
            val outputStack: ItemStack = inventory.getStack(PUZZLE_OUTPUT_SLOT_INDEX)
            if (!outputStack.isEmpty && !changedItemStack.isEmpty) return@addChangeListener

            val dyeItemStack: ItemStack = inventory.getStack(PUZZLE_BACKGROUND_DYE_SLOT_INDEX)
            val dyeStackItem: Item = dyeItemStack.item
            val updatedColor: DyeColor =
                if (dyeItemStack.isEmpty || dyeStackItem !is DyeItem)
                    changedItemStack.tag?.getPanel()?.backgroundColor ?: return@addChangeListener
                else dyeStackItem.color

            val updatedPanel: Panel =
                changedItemStack.tag?.getPanel()?.copy(backgroundColor = updatedColor) ?: return@addChangeListener

            val updatedStack: ItemStack = changedItemStack.copy().apply { tag?.putPanel(updatedPanel) }
            updateInventory(PUZZLE_OUTPUT_SLOT_INDEX, updatedStack)
        }

        backgroundDyeSlot.addChangeListener { slot, inventory, index, changedItemStack ->
            val inputStack: ItemStack = inventory.getStack(PUZZLE_INPUT_SLOT_INDEX)
            if (inputStack.isEmpty) return@addChangeListener
            val outputStack: ItemStack = inventory.getStack(PUZZLE_OUTPUT_SLOT_INDEX)
            val stackItem: Item = changedItemStack.item
            val panel: Panel = outputStack.tag?.getPanel() ?: return@addChangeListener

            val color: DyeColor =
                if (!changedItemStack.isEmpty && stackItem is DyeItem) stackItem.color
                else inputStack.tag?.getPanel()?.backgroundColor ?: DyeColor.WHITE

            val updatedStack: ItemStack =
                outputStack.copy().apply { tag?.putPanel(panel.copy(backgroundColor = color)) }

            updateInventory(PUZZLE_OUTPUT_SLOT_INDEX, updatedStack)
        }

        outputSlot.addChangeListener { slot, inventory, index, changedItemStack ->
            val inputStack: ItemStack = inventory.getStack(PUZZLE_INPUT_SLOT_INDEX)
            val dyeStack: ItemStack = inventory.getStack(PUZZLE_BACKGROUND_DYE_SLOT_INDEX)

            if (index != PUZZLE_OUTPUT_SLOT_INDEX) return@addChangeListener
            if (changedItemStack.isNotEmpty) return@addChangeListener
            resizeSlider.setValue(0, false)
            updateInventory(PUZZLE_INPUT_SLOT_INDEX, ItemStack.EMPTY)
            // Consume dye if the puzzle color has changed
            val inputBackgroundColor: DyeColor? = inputStack.tag?.getPanel()?.backgroundColor
            val outputBackgroundColor: DyeColor? = changedItemStack.tag?.getPanel()?.backgroundColor
            // TODO: This is currently broken
            if (inputBackgroundColor != outputBackgroundColor) {
                val updatedDyeStack: ItemStack = dyeStack.copy().apply { decrement(changedItemStack.count) }
                updateInventory(PUZZLE_BACKGROUND_DYE_SLOT_INDEX, updatedDyeStack)
            }
        }

        editor.setClickListener { panel ->
            val inputStack: ItemStack = blockInventory.getStack(PUZZLE_OUTPUT_SLOT_INDEX)
            val inputTag: CompoundTag = inputStack.tag ?: return@setClickListener
            val inputPanel: Panel? = inputTag.getPanel()
            if (panel == inputPanel) return@setClickListener
            val outputStack: ItemStack = inputStack.copy().apply { tag?.putPanel(panel) }
            updateInventory(PUZZLE_OUTPUT_SLOT_INDEX, outputStack)
        }

        layout()
        context?.run { world, pos -> if (world.isClient) addPainters() }
    }

    private fun layout() {
        var y = 16
        root.add(editor, 38, y)
        root.add(resizeSlider, 150, y - 2, 5, 110)
        y += 3
        root.add(inputSlot, 8, y)
        y += 21
        root.add(backgroundDyeSlot, 0, y)
        root.add(lineDyeSlot, 18, y)
        y += 16 + 4
        root.add(inventorySlots, 0, y)
        y += 16 * 2 + 11
        root.add(outputSlot, 8, y)
        y += 16 * 1 + 10
        root.add(hotBarLabel, 0, y)
        y += 14
        root.add(hotBar, 0, y)
        root.validate(this)
    }

    @Environment(EnvType.CLIENT)
    override fun addPainters() {
        super.addPainters()
        inputSlot.backgroundPainter = InputSlotBackgroundPainter(inputSlot, placeholderPuzzleTexture)
        backgroundDyeSlot.backgroundPainter = InputSlotBackgroundPainter(inputSlot, placeholderDyeTexture)
        lineDyeSlot.backgroundPainter = InputSlotBackgroundPainter(inputSlot, placeholderDyeTexture)
    }
}