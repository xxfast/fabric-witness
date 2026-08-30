package com.xfastgames.witness.blocks.redstone

import com.mojang.serialization.MapCodec
import com.xfastgames.witness.Witness
import com.xfastgames.witness.entities.PuzzleFrameBlockEntity
import com.xfastgames.witness.items.PuzzlePanelItem
import com.xfastgames.witness.items.data.Panel
import com.xfastgames.witness.items.data.endSides
import com.xfastgames.witness.items.data.panel
import com.xfastgames.witness.items.data.withLine
import com.xfastgames.witness.utils.guava.emptyGraph
import com.xfastgames.witness.screens.solver.PuzzleSolverScreen
import com.xfastgames.witness.sounds.WitnessSounds
import com.xfastgames.witness.sounds.play
import com.xfastgames.witness.utils.BlockInventory
import com.xfastgames.witness.utils.blockSettings
import com.xfastgames.witness.utils.d
import com.xfastgames.witness.utils.pc
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import com.xfastgames.witness.utils.rotateShape
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.util.StringRepresentable
import com.xfastgames.witness.items.data.Side
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

class IronPuzzleFrameBlock(settings: BlockBehaviour.Properties) : BaseEntityBlock(settings) {
    /** Block-state form of a set of [Side]s: one side, or the two of a diagonal corner nub. */
    enum class Exit(val sides: Set<Side>) : StringRepresentable {
        NONE(emptySet()),
        TOP(setOf(Side.TOP)),
        BOTTOM(setOf(Side.BOTTOM)),
        LEFT(setOf(Side.LEFT)),
        RIGHT(setOf(Side.RIGHT)),
        TOP_LEFT(setOf(Side.TOP, Side.LEFT)),
        TOP_RIGHT(setOf(Side.TOP, Side.RIGHT)),
        BOTTOM_LEFT(setOf(Side.BOTTOM, Side.LEFT)),
        BOTTOM_RIGHT(setOf(Side.BOTTOM, Side.RIGHT));

        override fun getSerializedName(): String = name.lowercase()

        companion object {
            fun of(sides: Set<Side>): Exit = entries.first { it.sides == sides }
        }
    }

    companion object {
        /**
         * The frame holds a panel and has a redstone signal into it (rules/minecraft/05-puzzle-frame.md).
         * Replaces the old `enabled`, which only meant "holds a panel"; worlds saved with it load
         * with a dropped-property warning and nothing else, the models never read it.
         */
        val POWERED: BooleanProperty = BooleanProperty.create("powered")

        /**
         * A line was accepted here while powered. Sticky: it outlives the line and clears only with
         * [POWERED]. Set by [PuzzleFrameBlockEntity.submitSolution], server side only.
         */
        val SOLVED: BooleanProperty = BooleanProperty.create("solved")

        /** The side(s) the last accepted line left by; where a solved frame sends its power. */
        val EXIT: EnumProperty<Exit> = EnumProperty.create("exit", Exit::class.java)
        val TOP_CONNECTED: BooleanProperty = BooleanProperty.create("top_connected")
        val LEFT_CONNECTED: BooleanProperty = BooleanProperty.create("left_connected")
        val RIGHT_CONNECTED: BooleanProperty = BooleanProperty.create("right_connected")
        val BOTTOM_CONNECTED: BooleanProperty = BooleanProperty.create("bottom_connected")

        /** Redstone strength a solved tail frame puts out of its back face. */
        private const val SOLVED_SIGNAL = 15

        /** Block light of a powered frame, and of a solved one. */
        private const val POWERED_LIGHT = 10
        private const val SOLVED_LIGHT = 11

        val IDENTIFIER = Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, "iron_puzzle_frame")
        val CODEC: MapCodec<IronPuzzleFrameBlock> = simpleCodec(::IronPuzzleFrameBlock)
        val BLOCK: Block = registerBlock(
            IronPuzzleFrameBlock(
                blockSettings(IDENTIFIER)
                    .strength(2.5f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .lightLevel { state: BlockState ->
                        when {
                            state.getValue(SOLVED) -> SOLVED_LIGHT
                            state.getValue(POWERED) -> POWERED_LIGHT
                            else -> 0
                        }
                    }
            ),
            IDENTIFIER
        )
        val BLOCK_ITEM: BlockItem = registerBlockItem(BLOCK, IDENTIFIER)

        /**
         * Which of the four bracket sides has something to join to. The single source for both
         * placement and neighbour updates: the two used to disagree (placement counted a stand
         * *above* as joined), which was harmless while the flags only picked model parts and is
         * a power bug once they carry a chain.
         */
        fun connections(world: BlockGetter, pos: BlockPos, facing: Direction): Map<BooleanProperty, Boolean> {
            fun frameAt(direction: Direction): Boolean = world.getBlockState(pos.relative(direction)).block is IronPuzzleFrameBlock
            val below: Block = world.getBlockState(pos.below()).block
            // Panel-relative right is clockwise of facing (north-facing frame: right = east), as the
            // model and the old per-facing tables had it.
            val right: Direction = facing.clockWise
            return mapOf(
                TOP_CONNECTED to frameAt(Direction.UP),
                BOTTOM_CONNECTED to (below is IronPuzzleFrameBlock || below is IronStandBlock),
                LEFT_CONNECTED to frameAt(right.opposite),
                RIGHT_CONNECTED to frameAt(right),
            )
        }

        /**
         * Recomputes the derived state (joins, power) and writes it only if something changed, so
         * chains of frames updating each other settle instead of ping-ponging.
         */
        fun refresh(world: Level, pos: BlockPos, state: BlockState = world.getBlockState(pos)) {
            if (state.block !is IronPuzzleFrameBlock) return
            // Server-authoritative, as RedstoneLampBlock does it; UPDATE_ALL carries the result down.
            if (world.isClientSide) return
            val entity: BlockEntity? = world.getBlockEntity(pos)
            val held: Panel? = (entity as? PuzzleFrameBlockEntity)?.inventory?.items?.get(0)?.panel
            val hasPanel: Boolean = held != null
            val facing: Direction = state.getValue(HORIZONTAL_FACING)
            val powered: Boolean = hasPanel && (hasRedstoneInput(world, pos, state, held) || isFedByChain(world, pos, facing))
            // Solved is sticky while powered, and only while powered: a cut resets the chain.
            val solved: Boolean = state.getValue(SOLVED) && powered
            var next: BlockState = state
                .setValue(POWERED, powered)
                .setValue(SOLVED, solved)
                .setValue(EXIT, if (solved) state.getValue(EXIT) else Exit.NONE)
            connections(world, pos, facing).forEach { (property, joined) ->
                next = next.setValue(property, joined)
            }
            if (next != state) world.setBlock(pos, next, Block.UPDATE_ALL)
            // Losing Solved also loses the line: the panel goes back to a puzzle, not a display of
            // an answer it no longer gets credit for (rules/minecraft/05-puzzle-frame.md).
            if (state.getValue(SOLVED) && !solved && entity is PuzzleFrameBlockEntity) {
                val stack: ItemStack = entity.inventory.getItem(0)
                stack.panel?.let { drawn -> entity.inventory.setItem(0, stack.copy().apply { panel = drawn.withLine(emptyGraph()) }) }
                entity.sync()
            }
        }

        /** The world direction out of [side] of a frame that faces [facing]; see [connections]. */
        fun sideDirection(facing: Direction, side: Side): Direction = when (side) {
            Side.TOP -> Direction.UP
            Side.BOTTOM -> Direction.DOWN
            Side.RIGHT -> facing.clockWise
            Side.LEFT -> facing.counterClockWise
        }

        /**
         * Whether a joined, solved frame sends its power here: one whose exit side faces this
         * block (rules/minecraft/05-puzzle-frame.md, "where the power goes"). Each frame resolves
         * its own sides with its own facing, so two frames facing different ways still join.
         */
        fun isFedByChain(world: BlockGetter, pos: BlockPos, facing: Direction): Boolean =
            Side.entries.any { side ->
                val neighbourPos: BlockPos = pos.relative(sideDirection(facing, side))
                val neighbour: BlockState = world.getBlockState(neighbourPos)
                neighbour.block is IronPuzzleFrameBlock &&
                    neighbour.getValue(SOLVED) &&
                    neighbour.getValue(EXIT).sides.any { exit ->
                        neighbourPos.relative(sideDirection(neighbour.getValue(HORIZONTAL_FACING), exit)) == pos
                    }
            }

        /** World directions a solved frame puts power out of: one per exit side. */
        private fun outputDirections(state: BlockState): Set<Direction> {
            if (!state.getValue(SOLVED)) return emptySet()
            val facing: Direction = state.getValue(HORIZONTAL_FACING)
            return state.getValue(EXIT).sides.map { side -> sideDirection(facing, side) }.toSet()
        }

        /**
         * Redstone into the frame from any side, like a lamp, except the sides the panel has an
         * end nub on: those are exits, and an exit never takes power in, whether or not the frame
         * is solved yet. Static, from the panel, so a shared cable run can never feed a frame back
         * through its own output (rules/minecraft/05-puzzle-frame.md, "where the power goes").
         */
        private fun hasRedstoneInput(world: Level, pos: BlockPos, state: BlockState, panel: Panel?): Boolean {
            val facing: Direction = state.getValue(HORIZONTAL_FACING)
            val exits: Set<Direction> = panel?.endSides().orEmpty().map { side -> sideDirection(facing, side) }.toSet()
            return Direction.entries.any { direction ->
                direction !in exits && world.getSignal(pos.relative(direction), direction) > 0
            }
        }

        /**
         * Left-click retrieval, mirroring vanilla item frames: attacking a loaded frame pops the
         * panel out and leaves the frame standing, so the frame only starts breaking once it is
         * empty. Wired to `AttackBlockCallback` in [com.xfastgames.witness.Witness.onInitialize].
         */
        fun retrieveOnAttack(player: Player, world: Level, pos: BlockPos): InteractionResult {
            // Cheap state check first, this runs for every block the player left-clicks.
            val state: BlockState = world.getBlockState(pos)
            if (state.block !is IronPuzzleFrameBlock) return InteractionResult.PASS

            val entity: BlockEntity = world.getBlockEntity(pos) ?: return InteractionResult.PASS
            if (entity !is PuzzleFrameBlockEntity) return InteractionResult.PASS
            // An empty frame breaks normally, same as an empty item frame.
            if (entity.inventory.items[0].isEmpty) return InteractionResult.PASS

            // removeItemNoUpdate hands back the stack itself. removeItem(slot, amount) would
            // split it, which empties the stack still sitting in the slot list.
            val frameStack: ItemStack = entity.inventory.removeItemNoUpdate(0)

            // ItemFrame.dropItem swallows the drop for creative players. popResource is already
            // server-side only and respects the blockDrops gamerule.
            if (!player.hasInfiniteMaterials())
                Block.popResourceFromFace(world, pos, state.getValue(HORIZONTAL_FACING).opposite, frameStack)

            world.playSound(player, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1f, 1f)
            refresh(world, pos, state)
            return InteractionResult.SUCCESS
        }
    }

    init {
        registerDefaultState(stateDefinition.any()
            .setValue(HORIZONTAL_FACING, Direction.NORTH)
            .setValue(POWERED, false)
            .setValue(SOLVED, false)
            .setValue(EXIT, Exit.NONE)
            .setValue(TOP_CONNECTED, false)
            .setValue(LEFT_CONNECTED, false)
            .setValue(RIGHT_CONNECTED, false)
            .setValue(BOTTOM_CONNECTED, false))
    }

    override fun codec(): MapCodec<out BaseEntityBlock> = CODEC

    /**
     * Redstone out (rules/minecraft/05-puzzle-frame.md): a solved frame puts a signal out of its
     * exit side(s), the way the nub points at the cable in the game, so a chain can end in dust or
     * a door as easily as in another frame. Weak power only: no [getDirectSignal], so a solid block
     * on the exit side does not relay the signal round to the frame it came from.
     *
     * Trap, do not re-derive: [direction] is the way from the asking block towards this one, so a
     * block on the exit side asks with the opposite of that side. Verified in game 2026-08-29 (repeater on the exit side).
     */
    override fun isSignalSource(state: BlockState): Boolean = true

    override fun getSignal(state: BlockState, world: BlockGetter, pos: BlockPos, direction: Direction): Int =
        if (direction.opposite in outputDirections(state)) SOLVED_SIGNAL else 0

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        PuzzleFrameBlockEntity(pos, state)

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState? {
        val facing: Direction = ctx.horizontalDirection
        var state: BlockState = super.getStateForPlacement(ctx)?.setValue(HORIZONTAL_FACING, facing) ?: return null
        // Freshly placed, so no panel yet: never powered, whatever the redstone around it says.
        connections(ctx.level, ctx.clickedPos, facing).forEach { (property, joined) ->
            state = state.setValue(property, joined)
        }
        return state
    }

    override fun neighborChanged(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        sourceBlock: Block,
        wireOrientation: net.minecraft.world.level.redstone.Orientation?,
        notify: Boolean
    ) {
        refresh(world, pos, state)
    }

    override fun createBlockStateDefinition(stateDefinition: StateDefinition.Builder<Block, BlockState>) {
        stateDefinition.add(HORIZONTAL_FACING)
        stateDefinition.add(POWERED)
        stateDefinition.add(SOLVED)
        stateDefinition.add(EXIT)
        stateDefinition.add(TOP_CONNECTED)
        stateDefinition.add(LEFT_CONNECTED)
        stateDefinition.add(RIGHT_CONNECTED)
        stateDefinition.add(BOTTOM_CONNECTED)
    }

    override fun getShape(
        state: BlockState,
        world: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape {
        val shape: VoxelShape = Shapes.or(
            Shapes.box(1.pc.d, 1.pc.d, 6.pc.d, 15.pc.d, 15.pc.d, 9.pc.d),
            Shapes.box(0.pc.d, 1.pc.d, 8.pc.d, 1.pc.d, 16.pc.d, 10.pc.d),
            Shapes.box(0.pc.d, 0.pc.d, 8.pc.d, 16.pc.d, 1.pc.d, 10.pc.d),
            Shapes.box(0.pc.d, 15.pc.d, 8.pc.d, 16.pc.d, 16.pc.d, 10.pc.d),
            Shapes.box(15.pc.d, 1.pc.d, 8.pc.d, 16.pc.d, 16.pc.d, 10.pc.d),
        )

        val direction: Direction = requireNotNull(state.getValue(HORIZONTAL_FACING))
        return shape.rotateShape(to = direction)
    }

    /**
     * The frame body sits mid-block, so no face of the block is sturdy and nothing can attach to it.
     * Report the back face (the `facing` side, the front is `facing.opposite`) as a full face so a
     * lever, torch or button can be stuck to the back and power the frame from behind.
     */
    override fun getBlockSupportShape(state: BlockState, world: BlockGetter, pos: BlockPos): VoxelShape =
        Shapes.box(0.pc.d, 0.pc.d, 0.pc.d, 16.pc.d, 16.pc.d, 1.pc.d)
            .rotateShape(to = state.getValue(HORIZONTAL_FACING))

    override fun playerWillDestroy(world: Level, pos: BlockPos, state: BlockState, player: Player): BlockState {
        val entity: BlockEntity? = world.getBlockEntity(pos)
        require(entity is PuzzleFrameBlockEntity)
        entity.inventory.items.forEach { stack -> Block.popResource(world, pos, stack) }
        return super.playerWillDestroy(world, pos, state, player)
    }

    override fun setPlacedBy(
        world: Level,
        pos: BlockPos,
        state: BlockState,
        placer: LivingEntity?,
        itemStack: ItemStack
    ) {
        if (world.isClientSide) return
        val entity: BlockEntity = requireNotNull(world.getBlockEntity(pos))
        require(entity is PuzzleFrameBlockEntity)
        entity.sync()
        world.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL)
    }

    override fun useWithoutItem(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
        hit: BlockHitResult
    ): InteractionResult = interact(state, world, pos, player, hit)

    override fun useItemOn(
        stack: ItemStack,
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult = interact(state, world, pos, player, hit)

    private fun interact(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
        hit: BlockHitResult?
    ): InteractionResult {
        val entity: BlockEntity = requireNotNull(world.getBlockEntity(pos))
        require(entity is PuzzleFrameBlockEntity)
        val inventory: BlockInventory = entity.inventory
        when {
            // when there's no item in the frame, and player is holding a panel
            inventory.items[0].isEmpty &&
                    (player.mainHandItem.item is PuzzlePanelItem ||
                            player.offhandItem.item is PuzzlePanelItem) -> {
                val holdingStack: ItemStack =
                    if (player.mainHandItem.item is PuzzlePanelItem) player.mainHandItem
                    else player.offhandItem

                val frameStack: ItemStack = holdingStack.split(1)
                if (player.mainHandItem.item is PuzzlePanelItem)
                    player.setItemInHand(InteractionHand.MAIN_HAND, holdingStack)
                else player.setItemInHand(InteractionHand.OFF_HAND, holdingStack)

                // A solution only counts where it was drawn: a panel arrives with no line, so a
                // solved panel can never be carried to another frame as a key.
                frameStack.panel?.let { carried -> frameStack.panel = carried.withLine(emptyGraph()) }
                inventory.items[0] = frameStack
                player.playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1f, 1f)
            }

            // when there is an item in the frame and player is sneaking
            !inventory.items[0].isEmpty && player.isShiftKeyDown -> {
                val freeHand: InteractionHand = when {
                    player.mainHandItem.isEmpty -> InteractionHand.MAIN_HAND
                    player.offhandItem.isEmpty -> InteractionHand.OFF_HAND
                    else -> return InteractionResult.FAIL
                }

                // removeItemNoUpdate hands back the stack itself. removeItem(slot, amount) would
                // split it, which empties the stack still sitting in the slot list.
                val frameStack: ItemStack = inventory.removeItemNoUpdate(0)
                player.setItemInHand(freeHand, frameStack)
            }

            // when there's a panel and player is not sneaking
            !inventory.items[0].isEmpty -> {
                if (hit?.direction == state.getValue(HORIZONTAL_FACING).opposite) {
                    // An unpowered frame is inert: visible, dark, and it ignores the click
                    // (rules/minecraft/05-puzzle-frame.md). The dull click is a client cue.
                    if (!state.getValue(POWERED)) {
                        if (world.isClientSide) player.play(WitnessSounds.POINTLESS_CLICK)
                        return InteractionResult.CONSUME
                    }
                    // Pass the frame pos so focus-mode cues (attract / error flash) stick to
                    // this panel instead of whatever screen-centre raycast happens to hit.
                    if (world.isClientSide) {
                        Minecraft.getInstance().gui.setScreen(PuzzleSolverScreen(pos.immutable()))
                    }
                    return InteractionResult.CONSUME
                }
                return InteractionResult.FAIL
            }

            else -> return InteractionResult.FAIL
        }

        if (world.isClientSide) return InteractionResult.SUCCESS
        // The panel came or went, so power may have too. Server only: UPDATE_ALL syncs it.
        refresh(world, pos, state)
        entity.sync()
        world.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL)
        return InteractionResult.SUCCESS
    }
}
