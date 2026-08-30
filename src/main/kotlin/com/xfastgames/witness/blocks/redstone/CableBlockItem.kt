package com.xfastgames.witness.blocks.redstone

import com.xfastgames.witness.utils.Clientside
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.GlobalPos
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import java.util.UUID

/**
 * The cable in hand (rules/minecraft/06-cable.md, "Laying a run"). A plain click places one
 * cable as any block item; a sneak-click pins the spot that cable would have gone, and a second
 * sneak-click lays a whole run from the pin to that spot along [findCablePath].
 *
 * The pin is per player and lives in memory on both sides: every click runs [useOn] on the client
 * and on the server, so each keeps its own copy in step without a packet. The client's copy
 * drives the preview; the server's is the one that lays. The copies are keyed by side as well as
 * player, because in singleplayer both sides share one JVM and one map: with one entry the
 * client's lay would remove the pin before the server saw the click, and the server would re-pin.
 */
class CableBlockItem(block: Block, settings: Item.Properties) : BlockItem(block, settings) {

    override fun useOn(ctx: UseOnContext): InteractionResult {
        val player: Player = ctx.player ?: return super.useOn(ctx)
        if (!ctx.isSecondaryUseActive) return super.useOn(ctx)
        val world: Level = ctx.level
        val spot: BlockPos = BlockPlaceContext(ctx).clickedPos.immutable()
        val key = Pin(player.uuid, world.isClientSide)
        val pin: GlobalPos? = pins[key]?.takeIf { it.dimension() == world.dimension() }
        if (pin == null) {
            pins[key] = GlobalPos(world.dimension(), spot)
            if (world.isClientSide) player.sendOverlayMessage(Component.translatable("item.witness.cable.pinned"))
            return InteractionResult.SUCCESS
        }
        val route: List<BlockPos>? = trace(world, pin.pos(), spot)
        if (route == null) {
            if (world.isClientSide) player.sendOverlayMessage(Component.translatable("item.witness.cable.no_route", CABLE_MAX_DISTANCE))
            return InteractionResult.FAIL
        }
        val toLay: List<BlockPos> = route.filter { at -> !isCable(world.getBlockState(at)) }
        val stack: ItemStack = ctx.itemInHand
        if (!player.isCreative && stack.count < toLay.size) {
            if (world.isClientSide) player.sendOverlayMessage(Component.translatable("item.witness.cable.short", toLay.size))
            return InteractionResult.FAIL
        }
        if (toLay.any { at -> !world.mayInteract(player, at) }) return InteractionResult.FAIL
        pins.remove(key)
        if (world.isClientSide) {
            player.sendOverlayMessage(Component.translatable("item.witness.cable.laid", toLay.size))
            return InteractionResult.SUCCESS
        }
        toLay.forEach { at -> world.setBlock(at, block.defaultBlockState(), Block.UPDATE_ALL) }
        if (!player.isCreative) stack.shrink(toLay.size)
        return InteractionResult.SUCCESS
    }

    /** Sneak-click on nothing forgets the pin. */
    override fun use(world: Level, player: Player, hand: InteractionHand): InteractionResult {
        if (!player.isSecondaryUseActive) return super.use(world, player, hand)
        if (pins.remove(Pin(player.uuid, world.isClientSide)) == null) return super.use(world, player, hand)
        if (world.isClientSide) player.sendOverlayMessage(Component.translatable("item.witness.cable.cleared"))
        return InteractionResult.SUCCESS
    }

    companion object : Clientside {
        /** One pin per player per logical side; see the class doc for why the side is in the key. */
        private data class Pin(val player: UUID, val clientSide: Boolean)

        private val pins: MutableMap<Pin, GlobalPos> = hashMapOf()

        /** How often the preview redraws its dots, in ticks. */
        private const val PREVIEW_PERIOD = 5
        private const val PREVIEW_OK: Int = 0x55FF55
        private const val PREVIEW_BAD: Int = 0xFF5555
        private const val PREVIEW_DOT: Float = 0.35f
        private const val PREVIEW_DOTS_PER_STEP: Int = 4

        private fun isCable(state: BlockState): Boolean = state.block === CableBlock.BLOCK

        /** Something to rest against: a block with collision that is not itself a cable. */
        private fun solid(world: Level, pos: BlockPos): Boolean {
            val state: BlockState = world.getBlockState(pos)
            return !isCable(state) && !state.getCollisionShape(world, pos).isEmpty
        }

        /** [findCablePath] over the world: a cell is placeable air (or a cable) touching a solid block. */
        fun trace(world: Level, from: BlockPos, to: BlockPos): List<BlockPos>? {
            fun Cell.pos(): BlockPos = BlockPos(x, y, z)
            fun BlockPos.cell(): Cell = Cell(x, y, z)
            return findCablePath(
                start = from.cell(),
                end = to.cell(),
                passable = { cell -> world.getBlockState(cell.pos()).let { it.canBeReplaced() || isCable(it) } },
                supported = { cell -> Cell.TOUCHING.any { touch -> solid(world, (cell + touch).pos()) } },
                existing = { cell -> isCable(world.getBlockState(cell.pos())) },
            )?.map(Cell::pos)
        }

        /** Each [PREVIEW_PERIOD] ticks, dot the route from the pin to whatever the crosshair is on. */
        override fun onClient() {
            var tick = 0
            ClientTickEvents.END_CLIENT_TICK.register { client: Minecraft ->
                if (++tick % PREVIEW_PERIOD != 0) return@register
                val player = client.player ?: return@register
                val world = client.level ?: return@register
                val pin: GlobalPos = pins[Pin(player.uuid, true)]?.takeIf { it.dimension() == world.dimension() } ?: return@register
                val hit: BlockHitResult = client.hitResult as? BlockHitResult ?: return@register
                val hand: InteractionHand = listOf(InteractionHand.MAIN_HAND, InteractionHand.OFF_HAND)
                    .firstOrNull { hand -> player.getItemInHand(hand).item is CableBlockItem } ?: return@register
                val stack: ItemStack = player.getItemInHand(hand)
                val spot: BlockPos = BlockPlaceContext(player, hand, stack, hit).clickedPos
                val route: List<BlockPos>? = trace(world, pin.pos(), spot)
                val enough: Boolean = route != null && (player.isCreative || stack.count >= route.count { !isCable(world.getBlockState(it)) })
                val colour = DustParticleOptions(if (route != null && enough) PREVIEW_OK else PREVIEW_BAD, PREVIEW_DOT)
                val cells: List<BlockPos> = route ?: listOf(pin.pos(), spot)
                // Dots along each step, not one per block, so the route reads as a line (feedback 2026-08-30).
                cells.zipWithNext { a, b ->
                    for (i in 0 until PREVIEW_DOTS_PER_STEP) {
                        val t: Double = i.toDouble() / PREVIEW_DOTS_PER_STEP
                        world.addParticle(colour, true, true, a.x + 0.5 + (b.x - a.x) * t, a.y + 0.5 + (b.y - a.y) * t, a.z + 0.5 + (b.z - a.z) * t, 0.0, 0.0, 0.0)
                    }
                }
                cells.last().let { at -> world.addParticle(colour, true, true, at.x + 0.5, at.y + 0.5, at.z + 0.5, 0.0, 0.0, 0.0) }
            }
        }
    }
}
