package com.xfastgames.witness.screens.composer

import com.google.common.graph.Graphs
import com.google.common.graph.MutableValueGraph
import com.xfastgames.witness.Witness
import com.xfastgames.witness.blocks.redstone.PuzzleComposerBlock
import com.xfastgames.witness.entities.PuzzleComposerBlockEntity
import com.xfastgames.witness.items.PuzzlePanelItem
import com.xfastgames.witness.items.data.*
import com.xfastgames.witness.screens.composer.PuzzleComposerScreen.Companion.PUZZLE_INPUT_SLOT_INDEX
import com.xfastgames.witness.screens.composer.PuzzleComposerScreen.Companion.PUZZLE_OUTPUT_SLOT_INDEX
import com.xfastgames.witness.screens.widgets.WPuzzleEditor
import com.xfastgames.witness.screens.widgets.WRadioGroup
import com.xfastgames.witness.screens.widgets.WRadioImageButton
import com.xfastgames.witness.screens.widgets.WSideTab
import com.xfastgames.witness.screens.widgets.WSideTabGroup
import com.xfastgames.witness.screens.widgets.WSideTabPosition
import com.xfastgames.witness.screens.widgets.icons.BreakIcon
import com.xfastgames.witness.screens.widgets.icons.EndIcon
import com.xfastgames.witness.screens.widgets.icons.EraserIcon
import com.xfastgames.witness.screens.widgets.icons.GridTabIcon
import com.xfastgames.witness.screens.widgets.icons.HexagonDotIcon
import com.xfastgames.witness.screens.widgets.icons.ModifiersTabIcon
import com.xfastgames.witness.screens.widgets.icons.PencilIcon
import com.xfastgames.witness.screens.widgets.icons.SquareIcon
import com.xfastgames.witness.screens.widgets.icons.StartIcon
import com.xfastgames.witness.utils.*
import com.xfastgames.witness.utils.guava.edgeValueOf
import com.xfastgames.witness.utils.guava.putEdgeValue
import io.github.cottonmc.cotton.gui.SyncedGuiDescription
import io.github.cottonmc.cotton.gui.client.BackgroundPainter
import io.github.cottonmc.cotton.gui.client.CottonInventoryScreen
import io.github.cottonmc.cotton.gui.client.ScreenDrawing
import io.github.cottonmc.cotton.gui.widget.WCardPanel
import io.github.cottonmc.cotton.gui.widget.WItemSlot
import io.github.cottonmc.cotton.gui.widget.WPlainPanel
import io.github.cottonmc.cotton.gui.widget.WPlayerInvPanel
import io.github.cottonmc.cotton.gui.widget.WToggleButton
import io.github.cottonmc.cotton.gui.widget.WWidget
import io.github.cottonmc.cotton.gui.widget.data.Vec2i
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.MenuScreens
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import net.minecraft.world.Container
import net.minecraft.world.level.Level
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.MenuType
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.core.BlockPos

val PUZZLE_COMPOSER_SCREEN_HANDLER: MenuType<PuzzleComposerScreenDescription> = Registry.register(
    BuiltInRegistries.MENU,
    PuzzleComposerBlock.IDENTIFIER,
    ExtendedMenuType(
        { syncId: Int, playerInventory: Inventory, pos: BlockPos ->
            PuzzleComposerScreenDescription(
                syncId,
                playerInventory,
                ContainerLevelAccess.create(playerInventory.player.level(), pos)
            )
        },
        BlockPos.STREAM_CODEC
    )
)

class PuzzleComposerScreen(gui: PuzzleComposerScreenDescription, player: Player, title: Component) :
    CottonInventoryScreen<PuzzleComposerScreenDescription>(gui, player, title) {

    companion object : Clientside {

        const val PUZZLE_INPUT_SLOT_INDEX = 0
        const val PUZZLE_OUTPUT_SLOT_INDEX = 1

        override fun onClient() {
            MenuScreens.register(PUZZLE_COMPOSER_SCREEN_HANDLER) { gui, inventory, title ->
                PuzzleComposerScreen(gui, inventory.player, title)
            }
        }
    }
}

class InputSlotBackgroundPainter(private val itemSlot: WItemSlot, private val texture: Identifier) : BackgroundPainter {
    override fun paintBackground(context: GuiGraphicsExtractor, left: Int, top: Int, panel: WWidget) {
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
    playerInventory: Inventory,
    context: ContainerLevelAccess,
    private val composerInventory: Container =
        getBlockInventory(context, PuzzleComposerBlockEntity.INVENTORY_SIZE),
) : SyncedGuiDescription(
    PUZZLE_COMPOSER_SCREEN_HANDLER,
    syncId,
    playerInventory,
    composerInventory,
    null
) {
    // Root is the whole window, tabs included: 32px wider than the panel body so the vanilla-style
    // side tabs (rules/minecraft/04-puzzle-composer.md) have somewhere to live outside it. It has
    // no background of its own; only [body] paints the vanilla panel.
    private val root: WPlainPanel = WPlainPanel().apply { setSize(170 + 32, 220) }
    private val body: WPlainPanel = WPlainPanel().apply { setSize(170, 220) }
    private val inputSlot = WItemSlot(composerInventory, PUZZLE_INPUT_SLOT_INDEX, 1, 1, true)
    private val outputSlot: WItemSlot = WItemSlot(composerInventory, PUZZLE_OUTPUT_SLOT_INDEX, 1, 1, true)

    private val toggleGroup = WRadioGroup()
    private val startButton = WRadioImageButton(icon = StartIcon, group = toggleGroup)
    private val endButton = WRadioImageButton(icon = EndIcon, group = toggleGroup)
    private val breakButton = WRadioImageButton(icon = BreakIcon, group = toggleGroup)
    private val hexagonDotButton = WRadioImageButton(icon = HexagonDotIcon, group = toggleGroup)
    private val squareButton = WRadioImageButton(icon = SquareIcon, group = toggleGroup)
    private val removeButton = WRadioImageButton(group = toggleGroup)

    private val editor = WPuzzleEditor(composerInventory, PUZZLE_OUTPUT_SLOT_INDEX)

    // Two tabs (rules/minecraft/04-puzzle-composer.md): Modifiers, what a click already does above,
    // and Grid (rules/minecraft/04-2-puzzle-composer-grid.md), what a click means once this second
    // rail is showing. Selection is pure client state: never synced, never persisted.
    private val tabGroup = WSideTabGroup()
    private val modifiersTab = WSideTab(icon = ModifiersTabIcon, position = WSideTabPosition.TOP, group = tabGroup, isSelected = true)
    private val gridTab = WSideTab(icon = GridTabIcon, position = WSideTabPosition.BOTTOM, group = tabGroup)

    private val modifiersCard: WPlainPanel = WPlainPanel().apply {
        add(startButton, 0, 0)
        add(endButton, startButton.width + 2, 0)
        add(breakButton, 0, 16)
        add(hexagonDotButton, startButton.width + 2, 16)
        add(squareButton, 0, 32)
        add(removeButton, startButton.width + 2, 32)
    }
    // The Grid rail is two tools rather than two actions: which one is armed decides whether a
    // gesture draws or erases, the same shape of rail as the Modifiers tab above
    // (rules/minecraft/04-2-puzzle-composer-grid.md#pencil-and-eraser).
    private val gridToolGroup = WRadioGroup()
    private val pencilButton = WRadioImageButton(icon = PencilIcon, group = gridToolGroup, isSelected = true)
    private val eraserButton = WRadioImageButton(icon = EraserIcon, group = gridToolGroup)
    private val gridCard: WPlainPanel = WPlainPanel().apply {
        add(pencilButton, 0, 0)
        add(eraserButton, pencilButton.width + 2, 0)
    }
    private val toolCards: WCardPanel = WCardPanel().apply {
        add(modifiersCard, modifiersCard.width, modifiersCard.height)
        add(gridCard, modifiersCard.width, modifiersCard.height)
    }

    private val tutorialToggle = WToggleButton()
    private val playerInventoryPanel: WPlayerInvPanel = this.createPlayerInventoryPanel()

    private val placeholderPuzzleTexture = Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, "textures/gui/placeholder_puzzle.png")

    private fun updateInventory(slotIndex: Int, itemStack: ItemStack) {
        val inventory: Container = composerInventory
        require(inventory is BlockInventory)
        require(inventory.owner is PuzzleComposerBlockEntity)
        if (inventory.owner.level?.isClientSide == true) {
            // Editor widget clicks only happen client-side, so send those changes to the server.
            inventory.owner.syncInventorySlotTag(slotIndex, itemStack)
        } else {
            // WItemSlot change listeners run on the authoritative server AbstractContainerMenu.
            // Update its backing inventory directly; vanilla slot sync sends the result to the client.
            inventory.setItem(slotIndex, itemStack)
        }
    }

    private fun updateOutputFrom(inputStack: ItemStack) {
        if (inputStack.isEmpty) {
            updateInventory(PUZZLE_OUTPUT_SLOT_INDEX, ItemStack.EMPTY)
            syncTutorialToggle()
            return
        }

        val outputStack: ItemStack = composerInventory.getItem(PUZZLE_OUTPUT_SLOT_INDEX)
        if (outputStack.isNotEmpty) return

        // Plain stacks (notably old creative-menu stacks) predate the item's default panel
        // component. Treat them exactly like the renderer does instead of showing a blank editor.
        val inputPanel: Panel = inputStack.panel ?: Panel.DEFAULT
        updateInventory(
            PUZZLE_OUTPUT_SLOT_INDEX,
            inputStack.copy().apply { panel = inputPanel }
        )
        syncTutorialToggle()
    }

    /** Writes an edited panel to the output slot, keeping the input stack's other components. */
    private fun commit(updatedPuzzle: Panel) {
        val outputPuzzle: Panel = composerInventory.getItem(PUZZLE_OUTPUT_SLOT_INDEX).panel ?: return
        if (updatedPuzzle == outputPuzzle) return
        val inputStack: ItemStack = composerInventory.getItem(PUZZLE_INPUT_SLOT_INDEX)
        if (inputStack.isEmpty) return
        updateInventory(
            PUZZLE_OUTPUT_SLOT_INDEX,
            inputStack.copy().apply { panel = updatedPuzzle }
        )
        syncTutorialToggle()
    }

    /** Mirror the output panel's tutorial flag onto the switch without re-firing its listener. */
    private fun syncTutorialToggle() {
        val panel: Panel? = composerInventory.getItem(PUZZLE_OUTPUT_SLOT_INDEX).panel
        tutorialToggle.toggle = panel?.tutorial == true
    }

    init {
        setRootPanel(root)
        // The root origin is now the gutter to the left of the body (the side tabs live there), so
        // the title needs to shift right by the same 32px; its vertical position is untouched.
        setTitlePos(Vec2i(32 + 8, 6))
        inputSlot.setInputFilter { itemStack -> itemStack.item is PuzzlePanelItem }
        outputSlot.setInputFilter { itemStack -> itemStack.item is PuzzlePanelItem }
        inputSlot.setInsertingAllowed(true)
        outputSlot.setModifiable(false)
        outputSlot.setTakingAllowed(true)

        inputSlot.addChangeListener { _, _, index, changedItemStack ->
            if (index != PUZZLE_INPUT_SLOT_INDEX) return@addChangeListener
            updateOutputFrom(changedItemStack)
        }

        outputSlot.addChangeListener { _, _, index, changedItemStack ->
            if (index != PUZZLE_OUTPUT_SLOT_INDEX) return@addChangeListener
            if (changedItemStack.isNotEmpty) {
                syncTutorialToggle()
                return@addChangeListener
            }
            // Taking the working copy consumes the input: one in, one out, no free copy.
            updateInventory(PUZZLE_INPUT_SLOT_INDEX, ItemStack.EMPTY)
            syncTutorialToggle()
        }

        // Panel-level flag, not a tool mode: toggles `Panel.tutorial` on the working copy.
        tutorialToggle.setOnToggle { on ->
            val outputPuzzle: Panel =
                composerInventory.getItem(PUZZLE_OUTPUT_SLOT_INDEX).panel ?: run {
                    // No panel in the editor; snap the switch back off.
                    tutorialToggle.toggle = false
                    return@setOnToggle
                }
            commit(outputPuzzle.withTutorial(on))
        }

        editor.setClickListener { node, edge, edgeNodePair, x, y ->
            val outputPuzzle: Panel =
                composerInventory.getItem(PUZZLE_OUTPUT_SLOT_INDEX).panel ?: return@setClickListener

            val selectedToggle: WRadioImageButton? = toggleGroup.selected

            // A square lives in a cell, which no node or edge hit can name, so it resolves from
            // the click position alone (rules/witness/06-colored-squares.md).
            if (selectedToggle == squareButton) {
                commit(outputPuzzle.withSquareCycled(x, y) ?: return@setClickListener)
                return@setClickListener
            }

            // Every other tool wants a node or an edge under the cursor.
            if (edge == null && node == null && edgeNodePair == null) return@setClickListener

            // An end point is a nub hanging off a border node, not a flag on the node itself,
            // so it edits the graph rather than a modifier (rules/witness/02-end-points.md).
            if (selectedToggle == endButton) {
                if (node == null) return@setClickListener
                commit(outputPuzzle.withEndPointToggled(node) ?: return@setClickListener)
                return@setClickListener
            }

            // A hexagon is a symbol, held beside the node's role and the edge's traversal state,
            // so it edits neither (rules/witness/04-hexagon-dots.md).
            if (selectedToggle == hexagonDotButton) {
                commit(outputPuzzle.withSymbolToggled(node, edgeNodePair) ?: return@setClickListener)
                return@setClickListener
            }

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

            // The start tool only marks nodes. A start point is a node role, so clicking a segment
            // with it selected does nothing (rules/witness/01-start-points.md).
            val updatedEdge: Edge? = when {
                selectedToggle == breakButton && edge != null ->
                    edge.copy(modifier = edge.modifier.nextIn(Modifier.BREAK, Modifier.NORMAL))

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

            commit(updatedPuzzle)
        }

        // Grid tab (rules/minecraft/04-2-puzzle-composer-grid.md): the editor reports where a
        // gesture landed on the lattice, and the armed tool decides whether that draws or erases.
        editor.setAnchorClickListener { x, y ->
            val outputPuzzle: Panel =
                composerInventory.getItem(PUZZLE_OUTPUT_SLOT_INDEX).panel ?: return@setAnchorClickListener
            val updated: Panel = when {
                // The pencil places a node joined to every neighbour already present, so restoring
                // a chunk of carved grid is one click per node rather than one per segment.
                gridToolGroup.selected != eraserButton -> outputPuzzle.withNodeAdded(x, y)
                else -> outputPuzzle.nodeAt(x, y)
                    ?.let { node -> outputPuzzle.withNodeRemoved(node) }
                    ?: return@setAnchorClickListener
            }
            commit(updated)
        }

        // Fires for a click that landed on a segment and for every step of a stroke, since both mean
        // the same thing: the pencil joins that pair, the eraser lifts it without taking its
        // endpoints with it.
        editor.setSegmentListener { fromX, fromY, toX, toY ->
            val outputPuzzle: Panel =
                composerInventory.getItem(PUZZLE_OUTPUT_SLOT_INDEX).panel ?: return@setSegmentListener
            val updated: Panel = when {
                gridToolGroup.selected != eraserButton -> outputPuzzle.withSegmentAdded(fromX, fromY, toX, toY)
                else -> outputPuzzle.withSegmentRemoved(fromX, fromY, toX, toY)
            } ?: return@setSegmentListener
            commit(updated)
        }

        // Which tab is selected is pure client state: nothing to sync, nothing to persist.
        tabGroup.onSelected = { selected ->
            val mode: WPuzzleEditor.EditorMode =
                if (selected == gridTab) WPuzzleEditor.EditorMode.GRID
                else WPuzzleEditor.EditorMode.MODIFIERS
            editor.mode = mode
            toolCards.selectedIndex = if (mode == WPuzzleEditor.EditorMode.GRID) 1 else 0
        }

        // TODO: Re-enable once a tool exists for it
        removeButton.isEnabled = false

        val blockInv = composerInventory as? BlockInventory
        if (blockInv?.owner?.level?.isClientSide == false) {
            updateOutputFrom(blockInv.getItem(PUZZLE_INPUT_SLOT_INDEX))
        }
        syncTutorialToggle()

        layout()
        context.execute { world: Level, pos: BlockPos -> if (world.isClientSide) addPainters() }
    }

    private fun layout() {
        var y = 12
        val marginStart = 4
        // Top-right inside the panel border (LibGui paints the bevel inward, so 0-edge clips).
        val toggleSize = 18
        val panelInset = 7
        body.add(
            tutorialToggle,
            body.width - toggleSize - panelInset,
            panelInset,
            toggleSize,
            toggleSize
        )

        y += 8
        body.add(editor, 46, y, editor.width, editor.height)
        y += 3
        body.add(inputSlot, 12, y)
        y += 24
        body.add(toolCards, marginStart, y, toolCards.width, toolCards.height)
        y += toolCards.height + 6
        body.add(outputSlot, 12, y)
        y += outputSlot.height + 10
        body.add(playerInventoryPanel, marginStart, y)

        // Body sits to the right of the gutter the side tabs live in, at the root's origin's
        // former spot, so every coordinate above is unchanged from the single-panel layout.
        val bodyX = 32
        root.add(body, bodyX, 0, body.width, body.height)

        // Two vanilla-style tabs (rules/minecraft/04-puzzle-composer.md) protruding from the
        // outside left edge, like the advancements screen. tabX overlaps the body's left edge by
        // 4px (bodyX=32, tabX=4, tab width=32: 4+32=36) so the sprite's seam reads as joined to the
        // panel rather than floating next to it; tabs are added after body so that overlap paints
        // on top of the panel border, matching how vanilla layers its own tab strip.
        val tabX = 4
        val tabTopY = 8
        root.add(modifiersTab, tabX, tabTopY)
        root.add(gridTab, tabX, tabTopY + modifiersTab.height)

        root.validate(this)
    }

    @Environment(EnvType.CLIENT)
    override fun addPainters() {
        // Do not re-derive this from `root.backgroundPainter == null`: LibGui's own addPainters
        // sets the *root* to VANILLA unless this flag is off, which would paint the window across
        // the full root width and swallow the gutter the side tabs protrude from.
        setUseDefaultRootBackground(false)
        super.addPainters()
        // Only the body looks like a vanilla panel; the root is the window plus its tab gutter.
        body.backgroundPainter = BackgroundPainter.VANILLA
        inputSlot.backgroundPainter = InputSlotBackgroundPainter(inputSlot, placeholderPuzzleTexture)
    }
}
