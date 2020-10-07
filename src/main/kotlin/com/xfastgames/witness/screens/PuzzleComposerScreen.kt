package com.xfastgames.witness.screens

import com.xfastgames.witness.blocks.redstone.PuzzleComposerBlock
import com.xfastgames.witness.entities.PuzzleComposerBlockEntity
import com.xfastgames.witness.items.PuzzlePanelItem
import com.xfastgames.witness.items.data.Panel
import com.xfastgames.witness.items.data.getPanel
import com.xfastgames.witness.items.data.putPanel
import com.xfastgames.witness.screens.PuzzleComposerScreen.Companion.PUZZLE_INPUT_SLOT_INDEX
import com.xfastgames.witness.screens.PuzzleComposerScreen.Companion.PUZZLE_INVENTORY_SLOT_INDEX
import com.xfastgames.witness.screens.PuzzleComposerScreen.Companion.PUZZLE_OUTPUT_SLOT_INDEX
import com.xfastgames.witness.screens.widgets.WPuzzleEditor
import com.xfastgames.witness.utils.Clientside
import io.github.cottonmc.cotton.gui.SyncedGuiDescription
import io.github.cottonmc.cotton.gui.client.CottonInventoryScreen
import io.github.cottonmc.cotton.gui.widget.WItemSlot
import io.github.cottonmc.cotton.gui.widget.WLabel
import io.github.cottonmc.cotton.gui.widget.WPlainPanel
import io.github.cottonmc.cotton.gui.widget.WSlider
import io.github.cottonmc.cotton.gui.widget.data.Axis
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.text.Text
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
        const val PUZZLE_INVENTORY_SLOT_INDEX = 1
        const val PUZZLE_OUTPUT_SLOT_INDEX = 7

        override fun onClient() {
            ScreenRegistry.register<PuzzleComposerScreenDescription, PuzzleComposerScreen>(
                PUZZLE_COMPOSER_SCREEN_HANDLER
            ) { gui, inventory, title ->
                PuzzleComposerScreen(gui, inventory.player, title)
            }
        }
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
    private val puzzleInputSlot = WItemSlot(blockInventory, PUZZLE_INPUT_SLOT_INDEX, 1, 1, false)
    private val composerInventorySlots: WItemSlot = WItemSlot.of(blockInventory, PUZZLE_INVENTORY_SLOT_INDEX, 2, 3)
    private val puzzleOutputSlot: WItemSlot = WItemSlot(blockInventory, PUZZLE_OUTPUT_SLOT_INDEX, 1, 1, true)
    private val puzzleEditor = WPuzzleEditor(blockInventory, PUZZLE_OUTPUT_SLOT_INDEX)
    private val resizeSlider = WSlider(2, 10, Axis.VERTICAL)
    private val hotBarLabel = WLabel(playerInventory.displayName)
    private val hotBar: WItemSlot = WItemSlot.of(playerInventory, 0, 9, 1)

    init {
        setRootPanel(root)
        puzzleInputSlot.setFilter { itemStack -> itemStack.item is PuzzlePanelItem }
        puzzleOutputSlot.setFilter { itemStack -> itemStack.item is PuzzlePanelItem }
        puzzleInputSlot.isInsertingAllowed = true
        puzzleOutputSlot.isInsertingAllowed = false

        resizeSlider.setValueChangeListener { value ->
            val itemStack: ItemStack = blockInventory.getStack(PUZZLE_OUTPUT_SLOT_INDEX)
            val outputTag: CompoundTag = itemStack.tag ?: return@setValueChangeListener
            if (outputTag.isEmpty) return@setValueChangeListener
            val puzzle: Panel = outputTag.getPanel()
            val updatedPuzzle: Panel = puzzle.resize(value)
            val updatedStack: ItemStack = itemStack.copy().apply { tag?.putPanel(updatedPuzzle) }
            puzzleEditor.updateInventory(PUZZLE_OUTPUT_SLOT_INDEX, updatedStack)
        }

        // TODO: This get called when the screen is first loaded
        puzzleInputSlot.addChangeListener { slot, inventory, index, stack ->
            if (index != PUZZLE_INPUT_SLOT_INDEX) return@addChangeListener
            val width: Int = stack.tag?.getPanel()?.width ?: 0
            resizeSlider.setValue(width, false)
            val outputStack: ItemStack = stack.copy()
            puzzleEditor.updateInventory(PUZZLE_OUTPUT_SLOT_INDEX, outputStack)
        }

        puzzleOutputSlot.addChangeListener { slot, inventory, index, itemStack ->
            if (index != PUZZLE_OUTPUT_SLOT_INDEX) return@addChangeListener
            if (!itemStack.isEmpty) return@addChangeListener
            puzzleEditor.updateInventory(PUZZLE_INPUT_SLOT_INDEX, ItemStack.EMPTY)
        }


        puzzleEditor.setClickListener { panel ->
            val inputStack: ItemStack = blockInventory.getStack(PUZZLE_OUTPUT_SLOT_INDEX)
            val inputTag: CompoundTag = inputStack.tag ?: return@setClickListener
            val inputPanel: Panel? = inputTag.getPanel()
            if (panel == inputPanel) return@setClickListener
            val outputStack: ItemStack = inputStack.copy().apply { tag?.putPanel(panel) }
            puzzleEditor.updateInventory(PUZZLE_OUTPUT_SLOT_INDEX, outputStack)
        }

        layout()
    }

    private fun layout() {
        var y = 16
        root.add(puzzleEditor, 38, y)
        root.add(resizeSlider, 150, y - 2, 5, 110)
        y += 3
        root.add(puzzleInputSlot, 8, y)
        y += 21
        root.add(composerInventorySlots, 0, y)
        y += 16 * 3 + 14
        root.add(puzzleOutputSlot, 8, y)
        y += 16 * 1 + 10
        root.add(hotBarLabel, 0, y)
        y += 14
        root.add(hotBar, 0, y)
        root.validate(this)
    }
}