package com.xfastgames.witness.screens

import com.xfastgames.witness.blocks.redstone.PuzzleComposerBlock
import com.xfastgames.witness.screens.widgets.PuzzleWidget
import com.xfastgames.witness.utils.Clientside
import io.github.cottonmc.cotton.gui.SyncedGuiDescription
import io.github.cottonmc.cotton.gui.client.CottonInventoryScreen
import io.github.cottonmc.cotton.gui.widget.WGridPanel
import io.github.cottonmc.cotton.gui.widget.WItemSlot
import io.github.cottonmc.cotton.gui.widget.WLabel
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.text.Text

class PuzzleScreen(gui: PuzzleScreenDescription?, player: PlayerEntity?, title: Text?) :
    CottonInventoryScreen<PuzzleScreen.PuzzleScreenDescription?>(gui, player, title) {

    companion object : Clientside {
        const val INVENTORY_SIZE = 1

        val TYPE: ScreenHandlerType<PuzzleScreenDescription> by lazy {
            ScreenHandlerRegistry.registerSimple(PuzzleComposerBlock.IDENTIFIER) { syncId: Int, inventory: PlayerInventory ->
                PuzzleScreenDescription(syncId, inventory, ScreenHandlerContext.EMPTY)
            }
        }

        override fun onClient() {
            ScreenRegistry.register<PuzzleScreenDescription, PuzzleScreen>(TYPE) { gui, inventory, title ->
                PuzzleScreen(gui, inventory.player, title)
            }
        }
    }

    class PuzzleScreenDescription(
        syncId: Int,
        playerInventory: PlayerInventory,
        context: ScreenHandlerContext?
    ) : SyncedGuiDescription(
        TYPE,
        syncId,
        playerInventory,
        getBlockInventory(context, INVENTORY_SIZE),
        getBlockPropertyDelegate(context)
    ) {

        init {
            val root = WGridPanel()
            setRootPanel(root)
            val itemSlot = WItemSlot.of(blockInventory, 0)

            var y: Int = 1 // Label is always at 0
            root.add(itemSlot, 0, y++)

            // Hotbar
            val hotBarLabel = WLabel(playerInventory.displayName)
            val hotBar: WItemSlot = WItemSlot.of(playerInventory, 0, 9, 1)
            root.add(hotBarLabel, 0, y++)
            root.add(hotBar, 0, y++)

            val puzzleWidget = PuzzleWidget()
            root.validate(this)
        }
    }
}


