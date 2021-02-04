package com.xfastgames.witness.screens.composer

import com.google.common.graph.Graphs
import com.google.common.graph.MutableValueGraph
import com.xfastgames.witness.Witness
import com.xfastgames.witness.blocks.redstone.PuzzleComposerBlock
import com.xfastgames.witness.entities.PuzzleComposerBlockEntity
import com.xfastgames.witness.items.KEY_PANEL
import com.xfastgames.witness.items.PuzzlePanelItem
import com.xfastgames.witness.items.data.*
import com.xfastgames.witness.screens.composer.PuzzleComposerScreen.Companion.PUZZLE_BACKGROUND_DYE_SLOT_INDEX
import com.xfastgames.witness.screens.composer.PuzzleComposerScreen.Companion.PUZZLE_INPUT_SLOT_INDEX
import com.xfastgames.witness.screens.composer.PuzzleComposerScreen.Companion.PUZZLE_INVENTORY_SLOT_INDEX
import com.xfastgames.witness.screens.composer.PuzzleComposerScreen.Companion.PUZZLE_OUTPUT_SLOT_INDEX
import com.xfastgames.witness.screens.widgets.WPuzzleEditor
import com.xfastgames.witness.screens.widgets.WRadioGroup
import com.xfastgames.witness.screens.widgets.WRadioImageButton
import com.xfastgames.witness.screens.widgets.icons.BreakIcon
import com.xfastgames.witness.screens.widgets.icons.EndIcon
import com.xfastgames.witness.screens.widgets.icons.StartIcon
import com.xfastgames.witness.utils.*
import io.github.cottonmc.cotton.gui.SyncedGuiDescription
import io.github.cottonmc.cotton.gui.client.BackgroundPainter
import io.github.cottonmc.cotton.gui.client.CottonInventoryScreen
import io.github.cottonmc.cotton.gui.client.ScreenDrawing
import io.github.cottonmc.cotton.gui.widget.*
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

@Suppress("UnstableApiUsage")
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
    private val inputSlot = WItemSlot(blockInventory, PUZZLE_INPUT_SLOT_INDEX, 1, 1, true)
    private val inventorySlots: WItemSlot = WItemSlot.of(blockInventory, PUZZLE_INVENTORY_SLOT_INDEX, 2, 3)
    private val outputSlot: WItemSlot = WItemSlot(blockInventory, PUZZLE_OUTPUT_SLOT_INDEX, 1, 1, true)

    private val toggleGroup = WRadioGroup()
    private val startButton = WRadioImageButton(icon = StartIcon, group = toggleGroup)
    private val endButton = WRadioImageButton(icon = EndIcon, group = toggleGroup)
    private val breakButton = WRadioImageButton(icon = BreakIcon, group = toggleGroup)
    private val hexagonDotButton = WRadioImageButton(group = toggleGroup)
    private val freeSlotButton1 = WRadioImageButton(group = toggleGroup)
    private val freeSlotButton2 = WRadioImageButton(group = toggleGroup)

    private val editor = WPuzzleEditor(blockInventory, PUZZLE_OUTPUT_SLOT_INDEX)
    private val playerInventoryPanel: WPlayerInvPanel = this.createPlayerInventoryPanel()

    private val placeholderPuzzleTexture = Identifier(Witness.IDENTIFIER, "textures/gui/placeholder_puzzle.png")

    private fun updateInventory(slotIndex: Int, itemStack: ItemStack) {
        val inventory: Inventory = blockInventory
        require(inventory is BlockInventory)
        require(inventory.owner is PuzzleComposerBlockEntity)
        if (inventory.owner.world?.isClient == true) inventory.owner.syncInventorySlotTag(slotIndex, itemStack)
    }

    init {
        setRootPanel(root)
        inputSlot.setFilter { itemStack -> itemStack.item is PuzzlePanelItem }
        outputSlot.setFilter { itemStack -> itemStack.item is PuzzlePanelItem }
        inputSlot.isInsertingAllowed = true
        outputSlot.isModifiable = false
        outputSlot.isTakingAllowed = true

        inputSlot.addChangeListener { _, inventory, index, changedItemStack ->
            // if empty remove the output
            if (changedItemStack.isEmpty) {
                updateInventory(PUZZLE_OUTPUT_SLOT_INDEX, ItemStack.EMPTY)
                return@addChangeListener
            }

            if (index != PUZZLE_INPUT_SLOT_INDEX) return@addChangeListener

            // Stack is used to distinguish changes from the screen load
            val outputStack: ItemStack = inventory.getStack(PUZZLE_OUTPUT_SLOT_INDEX)
            if (!outputStack.isEmpty && !changedItemStack.isEmpty) return@addChangeListener

            val dyeItemStack: ItemStack = inventory.getStack(PUZZLE_BACKGROUND_DYE_SLOT_INDEX)
            val dyeStackItem: Item = dyeItemStack.item
            val updatedColor: DyeColor =
                if (dyeItemStack.isEmpty || dyeStackItem !is DyeItem)
                    changedItemStack.tag?.getPanel(KEY_PANEL)?.backgroundColor ?: return@addChangeListener
                else dyeStackItem.color

            val updatedPanel: Panel = changedItemStack.tag?.getPanel(KEY_PANEL) ?: return@addChangeListener
            val tintedPanel: Panel = when (updatedPanel) {
                is Panel.Grid -> updatedPanel.copy(backgroundColor = updatedColor)
                is Panel.Tree -> updatedPanel.copy(backgroundColor = updatedColor)
                is Panel.Freeform -> updatedPanel.copy(backgroundColor = updatedColor)
            }

            val updatedStack: ItemStack = changedItemStack.copy().apply { tag?.putPanel(KEY_PANEL, tintedPanel) }
            updateInventory(PUZZLE_OUTPUT_SLOT_INDEX, updatedStack)
        }

        outputSlot.addChangeListener { slot, inventory, index, changedItemStack ->
            val inputStack: ItemStack = inventory.getStack(PUZZLE_INPUT_SLOT_INDEX)
            val dyeStack: ItemStack = inventory.getStack(PUZZLE_BACKGROUND_DYE_SLOT_INDEX)

            if (index != PUZZLE_OUTPUT_SLOT_INDEX) return@addChangeListener
            if (changedItemStack.isNotEmpty) return@addChangeListener
            updateInventory(PUZZLE_INPUT_SLOT_INDEX, ItemStack.EMPTY)
            // Consume dye if the puzzle color has changed
            val inputBackgroundColor: DyeColor? = inputStack.tag?.getPanel(KEY_PANEL)?.backgroundColor
            val outputBackgroundColor: DyeColor? = changedItemStack.tag?.getPanel(KEY_PANEL)?.backgroundColor
            // TODO: This is currently broken
            if (inputBackgroundColor != outputBackgroundColor) {
                val updatedDyeStack: ItemStack = dyeStack.copy().apply { decrement(changedItemStack.count) }
                updateInventory(PUZZLE_BACKGROUND_DYE_SLOT_INDEX, updatedDyeStack)
            }
        }

        editor.setClickListener { node, edge, edgeNodePair ->
            // if no edge or node is clicked, ignore
            if (edge == null && node == null && edgeNodePair == null) return@setClickListener

            val outputPuzzle: Panel =
                blockInventory.getStack(PUZZLE_OUTPUT_SLOT_INDEX)
                    .tag?.getPanel(KEY_PANEL) ?: return@setClickListener

            val selectedToggle: WRadioImageButton? = toggleGroup.selected

            val updatedNodeModifier: Modifier = when {
                selectedToggle == startButton && node != null ->
                    node.modifier.nextIn(Modifier.START, Modifier.NORMAL)

                node?.modifier != null -> node.modifier
                else -> Modifier.NONE
            }

            val updatedNode: Node? = node?.copy(modifier = updatedNodeModifier)

            val updatedGraph: MutableValueGraph<Node, Edge> = Graphs.copyOf(outputPuzzle.graph)

            updatedNode?.let {
                val neighbours: List<Node> = outputPuzzle.graph.adjacentNodes(node).toList()
                val neighbourhood: MutableMap<Node, Edge> = mutableMapOf()
                neighbours.forEach { neighbour ->
                    neighbourhood[neighbour] = outputPuzzle.graph.edgeValue(neighbour, node).get()
                }
                updatedGraph.removeNode(node)
                updatedGraph.addNode(updatedNode)
                neighbourhood.forEach { (neighbour, edge) ->
                    updatedGraph.putEdgeValue(neighbour, updatedNode, edge)
                }
            }

            val updatedEdge: Modifier? = when {
                selectedToggle == startButton && edge != null ->
                    edge.nextIn(Modifier.START, Modifier.NORMAL)

                selectedToggle == breakButton && edge != null ->
                    edge.nextIn(Modifier.BREAK, Modifier.NORMAL)

                edge != null -> edge
                else -> null
            }

            if (updatedNode == null && updatedEdge != null && edgeNodePair != null) {
                updatedGraph.removeEdge(edgeNodePair.nodeU(), edgeNodePair.nodeV())
                updatedGraph.putEdgeValue(edgeNodePair, updatedEdge)
            }

            // TODO: Do this nicely 😅💩
            val updatedPuzzle: Panel = when (outputPuzzle) {
                is Panel.Grid -> outputPuzzle.copy(graph = updatedGraph)
                is Panel.Tree -> outputPuzzle.copy(graph = updatedGraph)
                is Panel.Freeform -> outputPuzzle.copy(graph = updatedGraph)
            }

            // If nothing is changed, ignore
            if (updatedPuzzle == outputPuzzle) return@setClickListener

            val inputStack: ItemStack = blockInventory.getStack(PUZZLE_INPUT_SLOT_INDEX)
            val inputTag: CompoundTag = inputStack.tag ?: return@setClickListener
            val inputPanel: Panel? = inputTag.getPanel(KEY_PANEL)
            if (updatedPuzzle == inputPanel) return@setClickListener

            val outputStack: ItemStack = inputStack.copy().apply { tag?.putPanel(KEY_PANEL, updatedPuzzle) }
            updateInventory(PUZZLE_OUTPUT_SLOT_INDEX, outputStack)
        }

        layout()
        context?.run { world, pos -> if (world.isClient) addPainters() }
    }

    private fun layout() {
        var y = 12
        y += 8
        root.add(editor, 46, y, editor.width, editor.height)
        y += 3
        root.add(inputSlot, 8, y)
        y += 24
        root.add(startButton, 0, y)
        root.add(endButton, 17, y)
        y += 16
        root.add(breakButton, 0, y)
        root.add(hexagonDotButton, 17, y)
        y += 16
        root.add(freeSlotButton1, 0, y)
        root.add(freeSlotButton2, 17, y)
        y += 22
        root.add(outputSlot, 8, y)
        y += outputSlot.height + 16
        root.add(playerInventoryPanel, 0, y)
        root.validate(this)
    }

    @Environment(EnvType.CLIENT)
    override fun addPainters() {
        super.addPainters()
        inputSlot.backgroundPainter = InputSlotBackgroundPainter(inputSlot, placeholderPuzzleTexture)
    }
}