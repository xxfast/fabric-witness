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
import net.minecraft.server.level.ServerLevel
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

        /**
         * Whether this block's horizontal arms lie on the floor (pad + floor strips) rather than
         * standing as mid-height ribbons. Lying needs ground under this block and under every
         * cable it joins sideways; anything suspended, and the top of a column, stands. See [isFloor].
         */
        val FLOOR: BooleanProperty = BooleanProperty.create("floor")

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

        /** A lit run glows like a lit panel: the panel floors its lightmap at 12 (`PuzzlePanelRenderer.PANEL_GLOW`), and a block lights its own faces at its light level, so 12 here reads the same. Asked for 2026-08-30 18:05; was 7. */
        private const val LIT_LIGHT = 12
        private const val SIGNAL = 15

        /** A dark run: near black, as the game's unpowered cables. */
        private const val UNLIT_COLOR: Int = 0x1E1E1E

        /** What a run fed by plain redstone, with no panel behind it, lights up as. */
        private val SOURCELESS_COLOR: DyeColor = DyeColor.WHITE

        // Before BLOCK: the block constructor computes its shape, which reads these.
        // Hitboxes: 4 wide and 1.5 thick, kept from the ribbon era on purpose (the drawn rod is a
        // 2 x 2 tube from tools/gen_cable_models.sh); a slightly generous box is easier to aim at.
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
         * The axis the ribbon is wide across in this block, see [ribbonWidths]: a vertical piece is
         * wide across x or z; a mid-height piece stands on edge (y) or lies flat (either horizontal
         * axis, the models do not distinguish); floor pieces always lie flat. Written for the whole
         * run in one pass by [refresh], so the floor, the frames and the bends all agree.
         */
        val WIDE: EnumProperty<Direction.Axis> = EnumProperty.create("wide", Direction.Axis::class.java)

        /**
         * Whether the cable above is a floor lip (a floor piece whose run drops over an edge).
         * The lip's bend hangs down into this block, so this block draws no rod above its middle.
         */
        val UNDER_LIP: BooleanProperty = BooleanProperty.create("under_lip")

        /** Vertical strips, wide across x; [wideOn] turns them for [WIDE] = z. A foot stands on a full pad. */
        private val RISER_FOOT: VoxelShape = Shapes.or(
            Shapes.box(6.pc.d, 1.pc.d, 7.25f.pc.d, 10.pc.d, 16.pc.d, 8.75f.pc.d),
            Shapes.box(6.pc.d, 1.pc.d, 6.pc.d, 10.pc.d, 2.5f.pc.d, 10.pc.d),
        )
        private val RISER: VoxelShape = Shapes.box(6.pc.d, 6.pc.d, 7.25f.pc.d, 10.pc.d, 16.pc.d, 8.75f.pc.d)
        private val DROP: VoxelShape = Shapes.box(6.pc.d, 0.pc.d, 7.25f.pc.d, 10.pc.d, 10.pc.d, 8.75f.pc.d)
        /** Under a floor lip the column only reaches the pad; a full drop stuck up past it (2026-08-30 02:01). */
        private val DROP_FLOOR: VoxelShape = Shapes.box(6.pc.d, 0.pc.d, 7.25f.pc.d, 10.pc.d, 1.pc.d, 8.75f.pc.d)

        private fun VoxelShape.wideOn(axis: Direction.Axis): VoxelShape =
            if (axis == Direction.Axis.X) this else Shapes.box(
                bounds().minZ, bounds().minY, bounds().minX, bounds().maxZ, bounds().maxY, bounds().maxX
            )
        /** Standing bands out of a column to a side: the ribbon leaving a panel face-on. */
        private val BANDS: Map<Direction, VoxelShape> = mapOf(
            // Bands stop short of the centre and a POST fills it: overlapping them there z-fought
            // (2026-08-30 01:50), meeting at the centre line left a notch (01:47).
            Direction.NORTH to Shapes.box(7.25f.pc.d, 6.pc.d, 0.pc.d, 8.75f.pc.d, 10.pc.d, 7.25f.pc.d),
            Direction.SOUTH to Shapes.box(7.25f.pc.d, 6.pc.d, 8.75f.pc.d, 8.75f.pc.d, 10.pc.d, 16.pc.d),
            Direction.WEST to Shapes.box(0.pc.d, 6.pc.d, 7.25f.pc.d, 7.25f.pc.d, 10.pc.d, 8.75f.pc.d),
            Direction.EAST to Shapes.box(8.75f.pc.d, 6.pc.d, 7.25f.pc.d, 16.pc.d, 10.pc.d, 8.75f.pc.d),
        )

        /** The centre of a standing block with side arms; the bands run out from it. */
        private val POST: VoxelShape = Shapes.box(7.25f.pc.d, 6.pc.d, 7.25f.pc.d, 8.75f.pc.d, 10.pc.d, 8.75f.pc.d)

        val IDENTIFIER = Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, "cable")
        val BLOCK: Block = registerBlock(
            CableBlock(
                blockSettings(IDENTIFIER)
                    .strength(0.5f)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    // Not "solid" for placement rules: a dirt path under a cable stayed a path
                    // only with this off (it turned to dirt, seen 2026-08-30 01:37). Collision is unaffected.
                    .forceSolidOff()
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
                stateDefinition.any().setValue(LIT, false).setValue(COLOR, SOURCELESS_COLOR).setValue(WIDE, Direction.Axis.X).setValue(FLOOR, true).setValue(UNDER_LIP, false)
            ) { state, side -> state.setValue(side, false) }
        )
    }

    override fun createBlockStateDefinition(stateDefinition: StateDefinition.Builder<Block, BlockState>) {
        stateDefinition.add(LIT)
        stateDefinition.add(COLOR)
        stateDefinition.add(WIDE)
        stateDefinition.add(UNDER_LIP)
        stateDefinition.add(FLOOR)
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

    /**
     * What decides the ribbon's width here before the floor does (see [ribbonWidths]): a band
     * out of a frame's side stands in the panel's plane (the frame decides, 2026-08-30 17:10),
     * and a foot under a stand is wide across the stand's facing, so the ribbon runs up the post
     * face-on as in the game. Null when nothing here decides.
     */
    private fun ribbonSeed(world: BlockGetter, pos: BlockPos, state: BlockState, floor: Boolean): Axis? {
        if (state.getValue(BlockStateProperties.UP)) {
            val above: BlockState = world.getBlockState(pos.above())
            if (above.block is IronStandBlock) {
                return if (above.getValue(BlockStateProperties.HORIZONTAL_FACING).axis == Direction.Axis.X) Axis.Z else Axis.X
            }
        }
        if (floor) return null
        val besideFrame: Boolean = Direction.Plane.HORIZONTAL.any { direction ->
            state.getValue(CONNECTIONS[direction.ordinal]) && world.getBlockState(pos.relative(direction)).block is IronPuzzleFrameBlock
        }
        return if (besideFrame) Axis.Y else null
    }

    /** See [UNDER_LIP]: a floor piece with exactly one horizontal arm and a drop, and no climb. */
    private fun isLip(joined: Map<BlockPos, BlockState>, floors: Map<BlockPos, Boolean>, at: BlockPos): Boolean {
        val state: BlockState = joined[at] ?: return false
        val ways: Set<Way> = arms(state)
        return floors.getValue(at) && Way.DOWN in ways && Way.UP !in ways && ways.count { it.horizontal } == 1
    }

    private fun arms(state: BlockState): Set<Way> =
        Way.entries.filter { way -> state.getValue(CONNECTIONS[way.ordinal]) }.toSet()

    private fun Axis.toDirectionAxis(): Direction.Axis = when (this) {
        Axis.X -> Direction.Axis.X
        Axis.Y -> Direction.Axis.Y
        Axis.Z -> Direction.Axis.Z
    }

    /**
     * Something to lie on: any block with collision under [pos] that is not a cable. Not "a sturdy
     * top": dirt paths and farmland are a pixel short and made a ground run stand up (2026-08-30 01:35).
     */
    private fun grounded(world: BlockGetter, pos: BlockPos): Boolean {
        val below: BlockState = world.getBlockState(pos.below())
        return !isRun(below) && !below.getCollisionShape(world, pos.below()).isEmpty
    }

    /**
     * See [FLOOR]. A horizontal cable lies on the floor only when it is on the ground, and so are
     * the cables beside it; otherwise it stands as a mid-height ribbon, which is also what the top
     * of a column runs into. Checking the neighbours too keeps a ground run and a suspended run
     * from meeting at different heights (the gap seen 2026-08-30 01:21; the suspended run drawn
     * lying flat in mid-air, 01:31).
     */
    private fun isFloor(world: BlockGetter, pos: BlockPos, state: BlockState): Boolean {
        if (!lying(world, pos, state)) return false
        return Direction.Plane.HORIZONTAL.all { direction ->
            val next: BlockPos = pos.relative(direction)
            val nextState: BlockState = world.getBlockState(next)
            !(state.getValue(CONNECTIONS[direction.ordinal]) && isRun(nextState)) || lying(world, next, withConnections(nextState, world, next))
        }
    }

    /**
     * A cable lies if it is on the ground, or if it is the lip of a ground run: it continues
     * downward and a cable beside it is on the ground, so the ribbon lies over the edge and then
     * drops (a run over an overhang stood up on top and hung a band at the edge, 2026-08-30 01:54).
     */
    private fun lying(world: BlockGetter, pos: BlockPos, state: BlockState): Boolean {
        if (grounded(world, pos)) return true
        if (!state.getValue(BlockStateProperties.DOWN)) return false
        return Direction.Plane.HORIZONTAL.any { direction ->
            val next: BlockPos = pos.relative(direction)
            state.getValue(CONNECTIONS[direction.ordinal]) && isRun(world.getBlockState(next)) && grounded(world, next)
        }
    }

    /**
     * Re-joins this cable's arms and relights its whole run (see [walkCables]) in the colour of
     * the panel feeding it. Server only, and redstone-cheap: **one walk per change**. The run is
     * written with [Block.UPDATE_CLIENTS] and each changed cable then tells its neighbours once;
     * cables ignore updates that come from cables ([neighborChanged]), so a run never re-walks
     * itself, and every run-wide value breaks ties by position, never by who asked. Both mattered:
     * on 2026-08-30 a run re-walked once per neighbour update it had fired, and two "first found
     * wins" ties (`wide`, `color`) flipped with the walk's start, which hit vanilla's chained-update
     * cap ("Too many chained neighbor updates", millions of writes a tick).
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
        // One colour per run: a frame's over redstone's white, ties to the lowest source position.
        val sources: List<DyeColor> = colours.entries.sortedWith(compareBy({ it.key.y }, { it.key.x }, { it.key.z })).map { it.value }
        val colour: DyeColor = sources.firstOrNull { it != SOURCELESS_COLOR } ?: sources.firstOrNull() ?: SOURCELESS_COLOR
        val joined: Map<BlockPos, BlockState> = walk.component.associateWith { at -> withConnections(world.getBlockState(at), world, at) }
        val floors: Map<BlockPos, Boolean> = joined.mapValues { (at, state) -> isFloor(world, at, state) }
        // In a fixed order: where two deciders disagree the first seeded wins, and that must not
        // depend on which cable started the walk (the x / z ping-pong at -134 7 216, 19:58).
        val cells: List<BlockPos> = joined.keys.sortedWith(compareBy({ it.y }, { it.x }, { it.z }))
        val widths: Map<BlockPos, Axis> = ribbonWidths(
            cells = cells,
            arms = { at -> arms(joined.getValue(at)) },
            floor = { at -> floors.getValue(at) },
            neighbour = { at, way -> at.relative(Direction.entries[way.ordinal]) },
            seeds = cells.mapNotNull { at -> ribbonSeed(world, at, joined.getValue(at), floors.getValue(at))?.let { at to it } }.toMap(),
        )
        walk.component.forEach { at ->
            val current: BlockState = world.getBlockState(at)
            val lit: Boolean = at in walk.lit
            val next: BlockState = joined.getValue(at)
                .setValue(LIT, lit)
                .setValue(COLOR, if (lit) colour else current.getValue(COLOR))
                .setValue(WIDE, widths.getValue(at).toDirectionAxis())
                .setValue(UNDER_LIP, isLip(joined, floors, at.above()))
                .setValue(FLOOR, floors.getValue(at))
            if (next != current) {
                world.setBlock(at, next, Block.UPDATE_CLIENTS)
                // Doors, dust and frames beside this cable hear about it once; cables do not listen.
                world.updateNeighborsAt(at, this, null)
            }
        }
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState =
        withConnections(defaultBlockState(), ctx.level, ctx.clickedPos)

    override fun onPlace(state: BlockState, world: Level, pos: BlockPos, oldState: BlockState, movedByPiston: Boolean) {
        if (oldState.block !== this) refresh(world, pos)
    }

    /** A broken cable splits its run: each cable that was beside it walks what is left. */
    override fun affectNeighborsAfterRemoval(state: BlockState, world: ServerLevel, pos: BlockPos, movedByPiston: Boolean) {
        super.affectNeighborsAfterRemoval(state, world, pos, movedByPiston)
        runNeighbours(world, pos).forEach { next -> refresh(world, next) }
    }

    override fun neighborChanged(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        sourceBlock: Block,
        wireOrientation: Orientation?,
        notify: Boolean
    ) {
        // A cable's run is already settled by whichever refresh wrote it; only the world around it can change it.
        if (sourceBlock === this) return
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
        // Hitboxes only know two planes; a standing mid-height piece (y) takes the x shapes.
        val wide: Direction.Axis = state.getValue(WIDE).takeIf { it != Direction.Axis.Y } ?: Direction.Axis.X
        if (state.getValue(FLOOR)) {
            return joined.fold(CORE) { shape, direction ->
                Shapes.or(shape, when (direction) {
                    Direction.UP -> RISER_FOOT.wideOn(wide)
                    Direction.DOWN -> DROP_FLOOR.wideOn(wide)
                    else -> ARMS.getValue(direction)
                })
            }
        }
        // Standing: nothing is drawn by default, so a suspended run's box does not hang to the
        // floor (the outline disagreed with the render, seen 2026-08-30 01:40).
        return joined.fold(Shapes.empty()) { shape, direction ->
            when (direction) {
                Direction.DOWN -> Shapes.or(shape, DROP.wideOn(wide))
                Direction.UP -> Shapes.or(shape, RISER.wideOn(wide))
                else -> Shapes.or(Shapes.or(shape, POST), BANDS.getValue(direction))
            }
        }
    }
}
