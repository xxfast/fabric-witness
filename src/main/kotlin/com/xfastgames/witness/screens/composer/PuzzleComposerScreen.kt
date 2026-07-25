package com.xfastgames.witness.screens.composer

import com.google.common.graph.Graphs
import com.google.common.graph.MutableValueGraph
import com.xfastgames.witness.Witness
import com.xfastgames.witness.blocks.redstone.PuzzleComposerBlock
import com.xfastgames.witness.entities.PuzzleComposerBlockEntity
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
import com.xfastgames.witness.utils.guava.edgeValueOf
import com.xfastgames.witness.utils.guava.putEdgeValue
import io.github.cottonmc.cotton.gui.SyncedGuiDescription
import io.github.cottonmc.cotton.gui.client.BackgroundPainter
import io.github.cottonmc.cotton.gui.client.CottonInventoryScreen
import io.github.cottonmc.cotton.gui.client.ScreenDrawing
import io.github.cottonmc.cotton.gui.widget.WItemSlot
import io.github.cottonmc.cotton.gui.widget.WPlainPanel
import io.github.cottonmc.cotton.gui.widget.WPlayerInvPanel
import io.github.cottonmc.cotton.gui.widget.WWidget
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreens
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.DyeItem
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.text.Text
import net.minecraft.util.DyeColor
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos

val PUZZLE_COMPOSER_SCREEN_HANDLER: ScreenHandlerType<PuzzleComposerScreenDescription> = Registry.register(
    Registries.SCREEN_HANDLER,
    PuzzleComposerBlock.IDENTIFIER,
    ExtendedScreenHandlerType(
        { syncId, playerInventory, pos: BlockPos ->
            PuzzleComposerScreenDescription(
                syncId,
                playerInventory,
                ScreenHandlerContext.create(playerInventory.player.entityWorld, pos)
            )
        },
        BlockPos.PACKET_CODEC
    )
)

class PuzzleComposerScreen(gui: PuzzleComposerScreenDescription, player: PlayerEntity, title: Text?) :
    CottonInventoryScreen<PuzzleComposerScreenDescription>(gui, player, title) {

    companion object : Clientside {

        const val PUZZLE_INPUT_SLOT_INDEX = 0
        const val PUZZLE_BACKGROUND_DYE_SLOT_INDEX = 1
        const val PUZZLE_INVENTORY_SLOT_INDEX = 3
        const val PUZZLE_OUTPUT_SLOT_INDEX = 7

        override fun onClient() {
            HandledScreens.register(PUZZLE_COMPOSER_SCREEN_HANDLER) { gui, inventory, title ->
                PuzzleComposerScreen(gui, inventory.player, title)
            }
        }
    }
}

class InputSlotBackgroundPainter(private val itemSlot: WItemSlot, private val texture: Identifier) : BackgroundPainter {
    override fun paintBackground(context: DrawContext, left: Int, top: Int, panel: WWidget?) {
        BackgroundPainter.SLOT.paintBackground(context, left, top, panel)
        ScreenDrawing.texturedRect(
            context,
            left,
            top,
            itemSlot.width,
            itemSlot.height,
            texture,
            Colors.TRANSPARENT.toRgb()
        )
    }
}

@Suppress("UnstableApiUsage")
class PuzzleComposerScreenDescription(
    syncId: Int,
    playerInventory: PlayerInventory,
    context: ScreenHandlerContext
) : SyncedGuiDescription(
    PUZZLE_COMPOSER_SCREEN_HANDLER,
    syncId,
    playerInventory,
    getBlockInventory(context, PuzzleComposerBlockEntity.INVENTORY_SIZE),
    null
) {
    private val root: WPlainPanel = WPlainPanel().apply { setSize(170, 220) }
    private val inputSlot = WItemSlot(blockInventory, PUZZLE_INPUT_SLOT_INDEX, 1, 1, true)
    private val inventorySlots: WItemSlot = WItemSlot.of(blockInventory, PUZZLE_INVENTORY_SLOT_INDEX, 2, 3)
    private val outputSlot: WItemSlot = WItemSlot(blockInventory, PUZZLE_OUTPUT_SLOT_INDEX, 1, 1, true)

    private val toggleGroup = WRadioGroup()
    private val startButton = WRadioImageButton(icon = StartIcon, group = toggleGroup)
    private val endButton = WRadioImageButton(icon = EndIcon, group = toggleGroup)
    private val breakButton = WRadioImageButton(icon = BreakIcon, group = toggleGroup)
    private val hexagonDotButton = WRadioImageButton(group = toggleGroup)
    private val addButton = WRadioImageButton(group = toggleGroup)
    private val removeButton = WRadioImageButton(group = toggleGroup)

    private val editor = WPuzzleEditor(blockInventory, PUZZLE_OUTPUT_SLOT_INDEX)
    private val playerInventoryPanel: WPlayerInvPanel = this.createPlayerInventoryPanel()

    private val placeholderPuzzleTexture = Identifier.of(Witness.IDENTIFIER, "textures/gui/placeholder_puzzle.png")

    private fun updateInventory(slotIndex: Int, itemStack: ItemStack) {
        val inventory: Inventory = blockInventory
        require(inventory is BlockInventory)
        require(inventory.owner is PuzzleComposerBlockEntity)
        if (inventory.owner.world?.isClient == true) {
            // Editor widget clicks only happen client-side, so send those changes to the server.
            inventory.owner.syncInventorySlotTag(slotIndex, itemStack)
        } else {
            // WItemSlot change listeners run on the authoritative server ScreenHandler.
            // Update its backing inventory directly; vanilla slot sync sends the result to the client.
            inventory.setStack(slotIndex, itemStack)
        }
    }

    private fun updateOutputFrom(inputStack: ItemStack) {
        if (inputStack.isEmpty) {
            updateInventory(PUZZLE_OUTPUT_SLOT_INDEX, ItemStack.EMPTY)
            return
        }

        val outputStack: ItemStack = blockInventory.getStack(PUZZLE_OUTPUT_SLOT_INDEX)
        if (outputStack.isNotEmpty) return

        val dyeStack: ItemStack = blockInventory.getStack(PUZZLE_BACKGROUND_DYE_SLOT_INDEX)
        val dyeItem: Item = dyeStack.item
        // Plain stacks (notably old creative-menu stacks) predate the item's default panel
        // component. Treat them exactly like the renderer does instead of showing a blank editor.
        val inputPanel: Panel = inputStack.panel ?: Panel.DEFAULT
        val updatedColor: DyeColor =
            if (dyeStack.isEmpty || dyeItem !is DyeItem) inputPanel.backgroundColor else dyeItem.color

        val tintedPanel: Panel = when (inputPanel) {
            is Panel.Grid -> inputPanel.copy(backgroundColor = updatedColor)
            is Panel.Tree -> inputPanel.copy(backgroundColor = updatedColor)
            is Panel.Freeform -> inputPanel.copy(backgroundColor = updatedColor)
        }

        updateInventory(
            PUZZLE_OUTPUT_SLOT_INDEX,
            inputStack.copy().apply { panel = tintedPanel }
        )
    }

    init {
        setRootPanel(root)
        inputSlot.setInputFilter { itemStack -> itemStack.item is PuzzlePanelItem }
        outputSlot.setInputFilter { itemStack -> itemStack.item is PuzzlePanelItem }
        inputSlot.setInsertingAllowed(true)
        outputSlot.setModifiable(false)
        outputSlot.setTakingAllowed(true)

        inputSlot.addChangeListener { _, _, index, changedItemStack ->
            if (index != PUZZLE_INPUT_SLOT_INDEX) return@addChangeListener
            updateOutputFrom(changedItemStack)
        }

        outputSlot.addChangeListener { slot, inventory, index, changedItemStack ->
            val inputStack: ItemStack = inventory.getStack(PUZZLE_INPUT_SLOT_INDEX)
            val dyeStack: ItemStack = inventory.getStack(PUZZLE_BACKGROUND_DYE_SLOT_INDEX)

            if (index != PUZZLE_OUTPUT_SLOT_INDEX) return@addChangeListener
            if (changedItemStack.isNotEmpty) return@addChangeListener
            updateInventory(PUZZLE_INPUT_SLOT_INDEX, ItemStack.EMPTY)
            // Consume dye if the puzzle color has changed
            val inputBackgroundColor: DyeColor? = inputStack.panel?.backgroundColor
            val outputBackgroundColor: DyeColor? = changedItemStack.panel?.backgroundColor
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
                blockInventory.getStack(PUZZLE_OUTPUT_SLOT_INDEX).panel ?: return@setClickListener

            val selectedToggle: WRadioImageButton? = toggleGroup.selected

            val updatedNodeModifier: Modifier = when {
                selectedToggle == startButton && node != null ->
                    node.modifier.nextIn(Modifier.START, Modifier.NORMAL)

                selectedToggle == endButton && node != null ->
                    node.modifier.nextIn(Modifier.END, Modifier.NORMAL)

                node?.modifier != null -> node.modifier
                else -> Modifier.NONE
            }

            val updatedNode: Node? = node?.copy(modifier = updatedNodeModifier)

            val updatedGraph: MutableValueGraph<Node, Edge> = Graphs.copyOf(outputPuzzle.graph)

            updatedNode?.let {
                val neighbours: List<Node> = outputPuzzle.graph.adjacentNodes(node).toList()
                val neighbourhood: MutableMap<Node, Edge> = mutableMapOf()
                neighbours.forEach { neighbour ->
                    outputPuzzle.graph.edgeValueOf(neighbour, node)?.let { value ->
                        neighbourhood[neighbour] = value
                    }
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
            val inputPanel: Panel = inputStack.panel ?: return@setClickListener
            if (updatedPuzzle == inputPanel) return@setClickListener

            val outputStack: ItemStack = inputStack.copy().apply { panel = updatedPuzzle }
            updateInventory(PUZZLE_OUTPUT_SLOT_INDEX, outputStack)
        }

        // TODO: Re-enable once this is implemented
        endButton.isEnabled = false
        hexagonDotButton.isEnabled = false
        addButton.isEnabled = false
        removeButton.isEnabled = false

        val composerInventory = blockInventory as? BlockInventory
        if (composerInventory?.owner?.world?.isClient == false) {
            updateOutputFrom(composerInventory.getStack(PUZZLE_INPUT_SLOT_INDEX))
        }

        layout()
        context.run { world, pos -> if (world.isClient) addPainters() }
    }

    private fun layout() {
        var y = 12
        val marginStart = 4
        y += 8
        root.add(editor, 46, y, editor.width, editor.height)
        y += 3
        root.add(inputSlot, 12, y)
        y += 24
        root.add(startButton, marginStart, y)
        root.add(endButton, marginStart + startButton.width + 2, y)
        y += 16
        root.add(breakButton, marginStart, y)
        root.add(hexagonDotButton, marginStart + startButton.width + 2, y)
        y += 16
        root.add(addButton, marginStart, y)
        root.add(removeButton, marginStart + startButton.width + 2, y)
        y += 22
        root.add(outputSlot, 12, y)
        y += outputSlot.height + 10
        root.add(playerInventoryPanel, marginStart, y)
        root.validate(this)
    }

    @Environment(EnvType.CLIENT)
    override fun addPainters() {
        super.addPainters()
        inputSlot.backgroundPainter = InputSlotBackgroundPainter(inputSlot, placeholderPuzzleTexture)
    }
}
