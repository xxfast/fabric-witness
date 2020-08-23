package com.xfastgames.witness.screens

import com.xfastgames.witness.blocks.redstone.PuzzleComposerBlock
import com.xfastgames.witness.entities.PuzzleComposerBlockEntity
import com.xfastgames.witness.items.PuzzlePanelItem
import com.xfastgames.witness.items.data.Panel
import com.xfastgames.witness.items.data.getPanel
import com.xfastgames.witness.items.data.putPanel
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

val PUZZLE_SCREEN_HANDLER: ScreenHandlerType<PuzzleScreenDescription> =
    ScreenHandlerRegistry.registerExtended(PuzzleComposerBlock.IDENTIFIER) { syncId, playerInventory, buf ->
        val pos: BlockPos = buf.readBlockPos()
        PuzzleScreenDescription(
            syncId,
            playerInventory,
            ScreenHandlerContext.create(playerInventory.player.world, pos)
        )
    }

class PuzzleScreen(gui: PuzzleScreenDescription?, player: PlayerEntity?, title: Text?) :
    CottonInventoryScreen<PuzzleScreenDescription?>(gui, player, title) {

    companion object : Clientside {
        override fun onClient() {
            ScreenRegistry.register<PuzzleScreenDescription, PuzzleScreen>(PUZZLE_SCREEN_HANDLER) { gui, inventory, title ->
                PuzzleScreen(gui, inventory.player, title)
            }
        }
    }
}

class PuzzleScreenDescription(
    syncId: Int,
    playerInventory: PlayerInventory,
    context: ScreenHandlerContext?
) : SyncedGuiDescription(
    PUZZLE_SCREEN_HANDLER,
    syncId,
    playerInventory,
    getBlockInventory(context, PuzzleComposerBlockEntity.INVENTORY_SIZE),
    null
) {
    init {
        val root: WPlainPanel = WPlainPanel().apply { setSize(150, 150) }
        setRootPanel(root)

        val puzzleInputSlot: WItemSlot = WItemSlot(blockInventory, 0, 1, 1, true)
            .apply { setFilter { itemStack -> itemStack.item is PuzzlePanelItem } }

        val composerInventorySlots = WItemSlot.of(blockInventory, 1, 2, 3)
        val puzzleSlot = WPuzzleEditor(blockInventory, 0)
        val resizeSlider = WSlider(2, 10, Axis.VERTICAL)
        resizeSlider.setValueChangeListener { value ->
            val itemStack: ItemStack = blockInventory.getStack(0)
            val tag: CompoundTag = itemStack.tag ?: return@setValueChangeListener
            val puzzle: Panel = tag.getPanel()
            val updatedPuzzle: Panel = puzzle.resize(value)
            tag.putPanel(updatedPuzzle)
            puzzleSlot.updateInventory(tag)
        }

        val hotBarLabel = WLabel(playerInventory.displayName)
        val hotBar: WItemSlot = WItemSlot.of(playerInventory, 0, 9, 1)

        var y = 16
        root.add(puzzleSlot, 38, y)
        root.add(resizeSlider, 150, y - 2, 5, 110)
        y += 16
        root.add(puzzleInputSlot, 8, y)
        y += 24
        root.add(composerInventorySlots, 0, y)
        y += 16 * 4 + 10
        root.add(hotBarLabel, 0, y)
        y += 14
        root.add(hotBar, 0, y)
        root.validate(this)
    }
}