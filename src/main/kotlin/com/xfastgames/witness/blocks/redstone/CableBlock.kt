package com.xfastgames.witness.blocks.redstone

import com.xfastgames.witness.Witness
import com.xfastgames.witness.entities.PuzzleFrameBlockEntity
import com.xfastgames.witness.items.data.panel
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.blockSettings
import com.xfastgames.witness.utils.d
import com.xfastgames.witness.utils.pc
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry
import net.fabricmc.fabric.api.client.rendering.v1.BlockTintsFactory
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.Identifier
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.level.redstone.Orientation
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

/**
 * The wire from a solved frame to a far door (rules/minecraft/06-cable.md). A flat ribbon that
 * joins to the cables around it, to frames, stands and vanilla redstone; a run is lit end to end
 * when any cable in it touches a source, and carries strength 15 to everything it joins.
 *
 * A lit run takes the colour of the panel that powers it, as in the game; dark otherwise. The
 * colour is block state written by the same walk that lights the run, so it needs no block entity.
 */
class CableBlock(settings: BlockBehaviour.Properties) : Block(settings) {
    companion object : Clientside {
        val LIT: BooleanProperty = BlockStateProperties.LIT

        /** The colour of the panel feeding the run; meaningless (and drawn dark) while unlit. */
        val COLOR: EnumProperty<DyeColor> = EnumProperty.create("color", DyeColor::class.java)

        /** Connection flags, one per side, in [Direction] order so [Direction.ordinal] indexes them. */
        val CONNECTIONS: List<BooleanProperty> = listOf(
            BlockStateProperties.DOWN,
            BlockStateProperties.UP,
            BlockStateProperties.NORTH,
            BlockStateProperties.SOUTH,
            BlockStateProperties.WEST,
            BlockStateProperties.EAST,
        )

        private const val LIT_LIGHT = 7
        private const val SIGNAL = 15

        /** A dark run: near black, as the game's unpowered cables. */
        private const val UNLIT_COLOR: Int = 0x1E1E1E

        /** What a run fed by plain redstone, with no panel behind it, lights up as. */
        private val SOURCELESS_COLOR: DyeColor = DyeColor.WHITE

        // Before BLOCK: the block constructor computes its shape, which reads these.
        // A flat ribbon, 4 wide and 1.5 thick (was 5 x 2; thinned 2026-08-30 on request): it lies
        // on the floor of the block for horizontal runs and stands as a strip through the middle
        // for vertical ones. Mirrors the models: change the numbers here and there together.
        // Full 4x4 pad: fills the outer corner of a floor L-bend (a gap seen 2026-08-30). Safe now
        // that vertical pieces use standing bands and never sit on it.
        private val CORE: VoxelShape = Shapes.box(6.pc.d, 1.pc.d, 6.pc.d, 10.pc.d, 2.5f.pc.d, 10.pc.d)
        private val ARMS: Map<Direction, VoxelShape> = mapOf(
            Direction.DOWN to Shapes.box(6.pc.d, 0.pc.d, 7.25f.pc.d, 10.pc.d, 1.pc.d, 8.75f.pc.d),
            Direction.UP to Shapes.box(6.pc.d, 1.pc.d, 7.25f.pc.d, 10.pc.d, 16.pc.d, 8.75f.pc.d),
            // Arms run to the block centre so corners overlap instead of sitting on a pad that
            // would poke out past a riser (the "lips" seen 2026-08-29).
            Direction.NORTH to Shapes.box(6.pc.d, 1.pc.d, 0.pc.d, 10.pc.d, 2.5f.pc.d, 8.pc.d),
            Direction.SOUTH to Shapes.box(6.pc.d, 1.pc.d, 8.pc.d, 10.pc.d, 2.5f.pc.d, 16.pc.d),
            Direction.WEST to Shapes.box(0.pc.d, 1.pc.d, 6.pc.d, 8.pc.d, 2.5f.pc.d, 10.pc.d),
            Direction.EAST to Shapes.box(8.pc.d, 1.pc.d, 6.pc.d, 16.pc.d, 2.5f.pc.d, 10.pc.d),
        )

        /**
         * The axis a vertical strip is wide across: along its own horizontal arm, for every
         * vertical piece; arm-less blocks copy the nearest arm below, then above. An edge-on
         * riser shows its 2px edge, which glows like the faces; it was the dark casing on that
         * edge that made edge-on segments read as broken, not the plane. A crossed-strip riser
         * (a `+`) was tried and rejected: its second strip drew a dark rib down the face.
         */
        val WIDE: EnumProperty<Direction.Axis> =
            EnumProperty.create("wide", Direction.Axis::class.java, Direction.Axis.X, Direction.Axis.Z)

        /** How far along a vertical run an arm-less block looks for one with an arm to copy its plane from. */
        private const val WIDE_SEARCH = 32

        /** Vertical strips, wide across x; [wideOn] turns them for [WIDE] = z. A foot stands on a full pad. */
        private val RISER_FOOT: VoxelShape = Shapes.or(
            Shapes.box(6.pc.d, 1.pc.d, 7.25f.pc.d, 10.pc.d, 16.pc.d, 8.75f.pc.d),
            Shapes.box(6.pc.d, 1.pc.d, 6.pc.d, 10.pc.d, 2.5f.pc.d, 10.pc.d),
        )
        private val RISER: VoxelShape = Shapes.box(6.pc.d, 6.pc.d, 7.25f.pc.d, 10.pc.d, 16.pc.d, 8.75f.pc.d)
        private val DROP: VoxelShape = Shapes.box(6.pc.d, 0.pc.d, 7.25f.pc.d, 10.pc.d, 10.pc.d, 8.75f.pc.d)

        private fun VoxelShape.wideOn(axis: Direction.Axis): VoxelShape =
            if (axis == Direction.Axis.X) this else Shapes.box(
                bounds().minZ, bounds().minY, bounds().minX, bounds().maxZ, bounds().maxY, bounds().maxX
            )
        /** Standing bands out of a column to a side: the ribbon leaving a panel face-on. */
        private val BANDS: Map<Direction, VoxelShape> = mapOf(
            Direction.NORTH to Shapes.box(7.25f.pc.d, 6.pc.d, 0.pc.d, 8.75f.pc.d, 10.pc.d, 8.pc.d),
            Direction.SOUTH to Shapes.box(7.25f.pc.d, 6.pc.d, 8.pc.d, 8.75f.pc.d, 10.pc.d, 16.pc.d),
            Direction.WEST to Shapes.box(0.pc.d, 6.pc.d, 7.25f.pc.d, 8.pc.d, 10.pc.d, 8.75f.pc.d),
            Direction.EAST to Shapes.box(8.pc.d, 6.pc.d, 7.25f.pc.d, 16.pc.d, 10.pc.d, 8.75f.pc.d),
        )

        val IDENTIFIER = Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, "cable")
        val BLOCK: Block = registerBlock(
            CableBlock(
                blockSettings(IDENTIFIER)
                    .strength(0.5f)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel { state -> if (state.getValue(LIT)) LIT_LIGHT else 0 }
            ),
            IDENTIFIER
        )
        val BLOCK_ITEM: BlockItem = registerBlockItem(BLOCK, IDENTIFIER)

        /**
         * One tinted model set, two tints: index 0 is the glowing broad face (the run's colour when
         * lit, near black when not); index 1 is the casing on the edges, always dark, as the game's
         * cable shows a black rim beside its lit strip.
         */
        override fun onClient() {
            BlockColorRegistry.register(
                BlockTintsFactory { state, _, _, tints ->
                    tints.add(if (state.getValue(LIT)) state.getValue(COLOR).textureDiffuseColor else UNLIT_COLOR)
                    tints.add(UNLIT_COLOR)
                },
                BLOCK
            )
        }
    }

    init {
        registerDefaultState(
            CONNECTIONS.fold(
                stateDefinition.any().setValue(LIT, false).setValue(COLOR, SOURCELESS_COLOR).setValue(WIDE, Direction.Axis.X)
            ) { state, side -> state.setValue(side, false) }
        )
    }

    override fun createBlockStateDefinition(stateDefinition: StateDefinition.Builder<Block, BlockState>) {
        stateDefinition.add(LIT)
        stateDefinition.add(COLOR)
        stateDefinition.add(WIDE)
        CONNECTIONS.forEach(stateDefinition::add)
    }

    /** A cable: the only thing power travels *along*. Everything else is an end. */
    private fun isRun(state: BlockState): Boolean = state.block === this

    /**
     * What the arm on [direction] reaches for: another cable, a frame on any side (every side of
     * a frame is an input or its exit), a stand only from underneath (a stand carries power up out
     * of its top and nothing sideways, so a stub into its side would be a lie), or any other
     * redstone component. The arm is drawn whether or not power flows that way.
     */
    private fun joins(world: BlockGetter, pos: BlockPos, direction: Direction): Boolean {
        val neighbour: BlockState = world.getBlockState(pos.relative(direction))
        return when {
            isRun(neighbour) -> true
            neighbour.block is IronPuzzleFrameBlock -> true
            neighbour.block is IronStandBlock -> direction == Direction.UP
            else -> neighbour.isSignalSource
        }
    }

    /**
     * The colour a source next to [pos] would light the run in: the panel's, when the source is a
     * frame sending power out of its exit; white for plain redstone. Null when nothing there is a
     * source. Frames answer through their own [Block.getSignal], so their input sides read 0 and
     * only the exit side counts.
     */
    private fun sourceColor(world: Level, pos: BlockPos): DyeColor? {
        var found: DyeColor? = null
        Direction.entries.forEach { direction ->
            val neighbourPos: BlockPos = pos.relative(direction)
            if (isRun(world.getBlockState(neighbourPos))) return@forEach
            if (world.getSignal(neighbourPos, direction) <= 0) return@forEach
            val entity: BlockEntity? = world.getBlockEntity(neighbourPos)
            val panelColor: DyeColor? = (entity as? PuzzleFrameBlockEntity)?.inventory?.items?.get(0)?.panel?.backgroundColor
            // A frame's colour wins over redstone's white, so a run fed by both still reads as the panel's.
            if (panelColor != null) return panelColor
            found = found ?: SOURCELESS_COLOR
        }
        return found
    }

    private fun runNeighbours(world: Level, pos: BlockPos): List<BlockPos> =
        Direction.entries.map(pos::relative).filter { next -> isRun(world.getBlockState(next)) }

    private fun withConnections(state: BlockState, world: BlockGetter, pos: BlockPos): BlockState =
        Direction.entries.fold(state) { next, direction ->
            next.setValue(CONNECTIONS[direction.ordinal], joins(world, pos, direction))
        }

    /** The axis this block's own horizontal arms run along, if any: z for north / south, x for east / west. */
    private fun armAxis(state: BlockState): Direction.Axis? = when {
        state.getValue(BlockStateProperties.NORTH) || state.getValue(BlockStateProperties.SOUTH) -> Direction.Axis.Z
        state.getValue(BlockStateProperties.EAST) || state.getValue(BlockStateProperties.WEST) -> Direction.Axis.X
        else -> null
    }

    /**
     * What [WIDE] this block wants, or null if nothing here decides it. A riser faces the same way
     * as the frame it serves: a foot under a stand takes the stand's facing (the ribbon runs up the
     * post face-on, as in the game), and a piece with an arm is wide along that arm, which for the
     * band into a frame's side is the same thing. A foot wide *across* its floor arm was edge-on
     * whenever the run came toward the viewer, and "along" was edge-on for feet under stands
     * (2026-08-30 00:20, 00:44, 00:46). Do not decide feet from their floor arm again.
     */
    private fun ownWide(world: BlockGetter, pos: BlockPos, state: BlockState): Direction.Axis? {
        if (state.getValue(BlockStateProperties.UP)) {
            val above: BlockState = world.getBlockState(pos.above())
            if (above.block is IronStandBlock) {
                return if (above.getValue(BlockStateProperties.HORIZONTAL_FACING).axis == Direction.Axis.X) Direction.Axis.Z else Direction.Axis.X
            }
        }
        // Only a piece that continues downward (a top or mid piece with a band) decides from its
        // arm. A foot with nothing above it does not: F3 on 2026-08-30 00:55 showed a foot at
        // wide=z from its own arm under a top at wide=x, which is the twist. Feet follow the top.
        if (!state.getValue(BlockStateProperties.DOWN)) return null
        return armAxis(state)
    }

    private fun wideFor(world: Level, pos: BlockPos, state: BlockState): Direction.Axis {
        ownWide(world, pos, state)?.let { return it }
        // Up first: the top of a climb (band into a frame, stand) is what a foot must match.
        for (direction in listOf(Direction.UP, Direction.DOWN)) {
            var at: BlockPos = pos.relative(direction)
            for (step in 0 until WIDE_SEARCH) {
                val other: BlockState = world.getBlockState(at)
                if (!isRun(other)) break
                ownWide(world, at, withConnections(other, world, at))?.let { return it }
                at = at.relative(direction)
            }
        }
        // A climb with no top at all: stand along the foot's own arm.
        return armAxis(state) ?: state.getValue(WIDE)
    }

    /**
     * Re-joins this cable's arms and relights its whole run (see [walkCables]) in the colour of
     * the panel feeding it. Server only, and every cable in the run is written in one pass, so the
     * neighbour updates this causes find nothing left to change and the cascade stops.
     */
    private fun refresh(world: Level, pos: BlockPos) {
        if (world.isClientSide) return
        val state: BlockState = world.getBlockState(pos)
        if (!isRun(state)) return
        val colours: MutableMap<BlockPos, DyeColor> = mutableMapOf()
        val walk: CableWalk<BlockPos> = walkCables(
            start = pos.immutable(),
            neighbours = { at -> runNeighbours(world, at) },
            isSource = { at -> sourceColor(world, at)?.also { colour -> colours[at] = colour } != null },
        )
        // One colour per run: a frame's over redstone's white, then whichever source came first.
        val colour: DyeColor = colours.values.firstOrNull { it != SOURCELESS_COLOR }
            ?: colours.values.firstOrNull()
            ?: SOURCELESS_COLOR
        walk.component.forEach { at ->
            val current: BlockState = world.getBlockState(at)
            val lit: Boolean = at in walk.lit
            val joined: BlockState = withConnections(current, world, at)
            val next: BlockState = joined
                .setValue(LIT, lit)
                .setValue(COLOR, if (lit) colour else current.getValue(COLOR))
                .setValue(WIDE, wideFor(world, at, joined))
            if (next != current) world.setBlock(at, next, Block.UPDATE_ALL)
        }
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState =
        withConnections(defaultBlockState(), ctx.level, ctx.clickedPos)

    override fun onPlace(state: BlockState, world: Level, pos: BlockPos, oldState: BlockState, movedByPiston: Boolean) {
        if (oldState.block !== this) refresh(world, pos)
    }

    override fun neighborChanged(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        sourceBlock: Block,
        wireOrientation: Orientation?,
        notify: Boolean
    ) {
        refresh(world, pos)
    }

    /**
     * A lit run gives weak power to everything it joins. Cables never read this off each other
     * (they read [LIT] in the walk), so a run cannot feed itself. No `getDirectSignal`, as with
     * the frame: a solid block on the end of a run must not relay the signal back round.
     *
     * A stand is only fed from directly underneath, the same rule as [joins]: a run passing beside
     * a stand's base must not power the frame on it, or a run lit by a frame further along feeds
     * that frame back through its own stand (seen 2026-08-29: lever off, frame stayed On).
     * [direction] runs from the asker to this block, so a stand above asks with DOWN.
     */
    override fun isSignalSource(state: BlockState): Boolean = true

    override fun getSignal(state: BlockState, world: BlockGetter, pos: BlockPos, direction: Direction): Int {
        if (!state.getValue(LIT)) return 0
        val asker: BlockState = world.getBlockState(pos.relative(direction.opposite))
        if (asker.block is IronStandBlock && direction != Direction.DOWN) return 0
        return SIGNAL
    }

    /**
     * Mirrors the multipart. A block that does not continue downward is a floor piece (pad, floor
     * arms, a column up if it climbs); one that does is a vertical piece (column, standing bands).
     */
    override fun getShape(state: BlockState, world: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        val joined: List<Direction> = Direction.entries.filter { direction -> state.getValue(CONNECTIONS[direction.ordinal]) }
        val wide: Direction.Axis = state.getValue(WIDE)
        if (Direction.DOWN !in joined) {
            return joined.fold(CORE) { shape, direction ->
                Shapes.or(shape, if (direction == Direction.UP) RISER_FOOT.wideOn(wide) else ARMS.getValue(direction))
            }
        }
        return joined.fold(DROP.wideOn(wide)) { shape, direction ->
            when (direction) {
                Direction.DOWN -> shape
                Direction.UP -> Shapes.or(shape, RISER.wideOn(wide))
                else -> Shapes.or(shape, BANDS.getValue(direction))
            }
        }
    }
}
