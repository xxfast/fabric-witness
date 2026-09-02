package com.xfastgames.witness.blocks.redstone

import com.xfastgames.witness.entities.PuzzleFrameBlockEntity
import com.xfastgames.witness.items.data.Panel
import com.xfastgames.witness.items.data.panel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.DyeColor
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING

/**
 * The cables, frames and stands of a build as one redstone network, powered by one [walkNetwork]
 * per change (rules/minecraft/06-cable.md, "a walk, not a neighbour update"; 05-puzzle-frame.md,
 * "power comes from the source"). Any member block calls [refresh] when the world beside it
 * changes; members ignore neighbour updates that come from members ([isMember]), so a network
 * never re-walks itself.
 *
 * Direction lives in [feeds]: a cable feeds everything it joins, a stand feeds only the frame on
 * it, a solved frame feeds the frames and cables on its output sides. Sources are vanilla signals
 * from outside the network only, so no member can ever be lit by its own output.
 */
object RedstoneNetwork {
    /** Whether [block] takes part in the walk, and so whose neighbour updates a member ignores. */
    fun isMember(block: Block): Boolean =
        block is CableBlock || block is IronPuzzleFrameBlock || block is IronStandBlock

    /** Every member block beside [pos], whichever way power would flow. */
    fun membersBeside(world: Level, pos: BlockPos): List<BlockPos> =
        Direction.entries.map(pos::relative).filter { at -> isMember(world.getBlockState(at).block) }

    /**
     * Redstone into [pos] from a block outside the network, along [directions]. `getSignal`'s
     * direction runs from the asker towards the asked block, so it is the direction out of [pos].
     */
    fun vanillaSignal(world: Level, pos: BlockPos, directions: Iterable<Direction>): Boolean =
        directions.any { direction ->
            val at: BlockPos = pos.relative(direction)
            !isMember(world.getBlockState(at).block) && world.getSignal(at, direction) > 0
        }

    /**
     * Re-walks the network the member at [pos] belongs to and writes every member whose state
     * changed. Server only, and redstone-cheap: one walk, writes with [Block.UPDATE_CLIENTS], and
     * only then does each written member tell its neighbours, so nothing re-walks a half-written
     * network (a frame's first write once let a cable beside it settle the rest of the chain,
     * which the stale walk then overwrote).
     */
    fun refresh(world: Level, pos: BlockPos) {
        if (world.isClientSide) return
        if (!isMember(world.getBlockState(pos).block)) return

        val panels: MutableMap<BlockPos, Panel?> = mutableMapOf()
        fun panelAt(at: BlockPos): Panel? = panels.getOrPut(at) {
            (world.getBlockEntity(at) as? PuzzleFrameBlockEntity)?.inventory?.items?.get(0)?.panel
        }
        fun stateAt(at: BlockPos): BlockState = world.getBlockState(at)
        fun isCable(at: BlockPos): Boolean = stateAt(at).block is CableBlock
        fun isFrame(at: BlockPos): Boolean = stateAt(at).block is IronPuzzleFrameBlock
        fun isStand(at: BlockPos): Boolean = stateAt(at).block is IronStandBlock

        fun links(at: BlockPos): List<BlockPos> = when {
            isCable(at) -> Direction.entries.map(at::relative).filter { next ->
                isCable(next) || isFrame(next) || (isStand(next) && next == at.above())
            }
            isFrame(at) -> IronPuzzleFrameBlock.joinedFrames(world, at, stateAt(at).getValue(HORIZONTAL_FACING)) +
                Direction.entries.map(at::relative).filter { next -> isCable(next) || (isStand(next) && next == at.below()) }
            isStand(at) -> listOf(at.below(), at.above()).filter { next -> (isCable(next) && next == at.below()) || (isFrame(next) && next == at.above()) }
            else -> emptyList()
        }

        fun feeds(from: BlockPos, to: BlockPos): Boolean = when {
            isCable(from) -> !isFrame(to) || IronPuzzleFrameBlock.takesInputFrom(stateAt(to), panelAt(to), from, to)
            isStand(from) -> isFrame(to)
            isFrame(from) -> when {
                !stateAt(from).getValue(IronPuzzleFrameBlock.SOLVED) -> false
                isFrame(to) -> IronPuzzleFrameBlock.feedsFrame(stateAt(from), panelAt(from), from, to)
                isCable(to) -> to in IronPuzzleFrameBlock.outputDirections(stateAt(from), panelAt(from)).map(from::relative)
                else -> false
            }
            else -> false
        }

        fun isSource(at: BlockPos): Boolean = when {
            isCable(at) -> vanillaSignal(world, at, Direction.entries)
            isFrame(at) -> panelAt(at)?.let { panel -> IronPuzzleFrameBlock.hasVanillaInput(world, at, stateAt(at), panel) } == true
            isStand(at) -> IronStandBlock.hasVanillaInput(world, at)
            else -> false
        }

        val walk: NetworkWalk<BlockPos> = walkNetwork(
            start = pos.immutable(),
            links = ::links,
            feeds = ::feeds,
            isSource = ::isSource,
            canHold = { at -> !isFrame(at) || panelAt(at) != null },
            decays = { from, to -> isCable(from) && isCable(to) },
            order = compareBy({ it.y }, { it.x }, { it.z }),
        )

        // One colour per cable run (the cables joined to each other between frames): the panel of
        // the lowest-positioned frame lighting it, else white. Ties by position, never by walk order.
        val cables: Set<BlockPos> = walk.component.filter(::isCable).toSet()
        val colours: Map<BlockPos, DyeColor> = cableRuns(cables).flatMap { run ->
            val litFrames: List<BlockPos> = run
                .mapNotNull { at -> walk.powered[at] }
                .filter(::isFrame)
                .distinct()
                .sortedWith(compareBy({ it.y }, { it.x }, { it.z }))
            val colour: DyeColor = litFrames.firstNotNullOfOrNull { at -> panelAt(at)?.backgroundColor } ?: DyeColor.WHITE
            run.map { at -> at to colour }
        }.toMap()

        val written: MutableList<BlockPos> = mutableListOf()
        if (cables.isNotEmpty()) {
            val cable: CableBlock = stateAt(cables.first()).block as CableBlock
            written += cable.writeRun(world, cables, lit = { at -> at in walk.powered }, colour = colours::getValue)
        }
        walk.component.forEach { at ->
            val powered: Boolean = at in walk.powered
            when {
                isFrame(at) -> if (IronPuzzleFrameBlock.write(world, at, powered)) written += at
                isStand(at) -> if (IronStandBlock.write(world, at, powered)) written += at
            }
        }
        // The whole network is settled, so whatever re-walks from here sees the finished state.
        written.forEach { at -> world.updateNeighborsAt(at, stateAt(at).block, null) }
    }

    /** [cables] split into the groups that touch each other face to face. */
    private fun cableRuns(cables: Set<BlockPos>): List<Set<BlockPos>> {
        val left: MutableSet<BlockPos> = cables.toMutableSet()
        val runs: MutableList<Set<BlockPos>> = mutableListOf()
        while (left.isNotEmpty()) {
            val run: MutableSet<BlockPos> = linkedSetOf(left.first())
            val frontier: ArrayDeque<BlockPos> = ArrayDeque(run)
            while (frontier.isNotEmpty()) {
                val at: BlockPos = frontier.removeFirst()
                Direction.entries.map(at::relative).filter { next -> next in left && run.add(next) }.forEach(frontier::addLast)
            }
            left -= run
            runs += run
        }
        return runs
    }
}
