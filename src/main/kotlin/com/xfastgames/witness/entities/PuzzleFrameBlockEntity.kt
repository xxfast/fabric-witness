package com.xfastgames.witness.entities

import com.google.common.graph.Graph
import com.xfastgames.witness.Witness
import com.xfastgames.witness.blocks.redstone.IronPuzzleFrameBlock
import com.xfastgames.witness.blocks.redstone.RedstoneNetwork
import com.xfastgames.witness.entities.renderer.PuzzleFrameBlockRenderer
import com.xfastgames.witness.items.data.Node
import com.xfastgames.witness.items.data.Panel
import com.xfastgames.witness.items.data.Verdict
import com.xfastgames.witness.items.data.exitSides
import com.xfastgames.witness.items.data.panel
import com.xfastgames.witness.items.data.toLine
import com.xfastgames.witness.items.data.toModifier
import com.xfastgames.witness.items.data.toSymbol
import com.xfastgames.witness.items.data.verdict
import com.xfastgames.witness.items.data.withLine
import com.xfastgames.witness.utils.BlockInventory
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.Syncable
import com.xfastgames.witness.utils.guava.emptyGraph
import com.xfastgames.witness.utils.registerBlockEntity
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.resources.Identifier
import net.minecraft.world.ContainerHelper
import net.minecraft.world.WorldlyContainer
import net.minecraft.world.WorldlyContainerHolder
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput

/**
 * C2S: the path a player just released on an end point, for the server to judge itself
 * (rules/minecraft/05-puzzle-frame.md). Carries the path, never the verdict: the client's
 * accept / reject is feedback only, and the frame's state is the server's to change.
 */
data class SubmitSolutionPayload(val pos: BlockPos, val path: List<Node>) : CustomPacketPayload {
    companion object {
        val ID: CustomPacketPayload.Type<SubmitSolutionPayload> =
            CustomPacketPayload.Type(Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, "submit_solution"))

        private val NODE_CODEC: StreamCodec<RegistryFriendlyByteBuf, Node> = StreamCodec.composite(
            ByteBufCodecs.FLOAT, Node::x,
            ByteBufCodecs.FLOAT, Node::y,
            ByteBufCodecs.VAR_INT, { node -> node.modifier.ordinal },
            ByteBufCodecs.VAR_INT, { node -> node.symbol.ordinal },
        ) { x, y, modifier, symbol -> Node(x, y, modifier.toModifier(), symbol.toSymbol()) }

        val CODEC: StreamCodec<RegistryFriendlyByteBuf, SubmitSolutionPayload> = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SubmitSolutionPayload::pos,
            NODE_CODEC.apply(ByteBufCodecs.list()), SubmitSolutionPayload::path,
            ::SubmitSolutionPayload
        )
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = ID
}

class PuzzleFrameBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ENTITY_TYPE, pos, state),
    Syncable,
    WorldlyContainerHolder {

    companion object : Clientside {
        val IDENTIFIER = Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, "puzzle_frame_entity")

        const val INVENTORY_SIZE = 1

        val ENTITY_TYPE: BlockEntityType<PuzzleFrameBlockEntity> = registerBlockEntity(IDENTIFIER) {
            FabricBlockEntityTypeBuilder
                .create(::PuzzleFrameBlockEntity, IronPuzzleFrameBlock.BLOCK)
                .build()
        }

        // Reached from Witness.onInitialize through ENTITIES, so the payload registers before the
        // registries freeze (AGENT.md, registration gotcha).
        init {
            PayloadTypeRegistry.serverboundPlay().register(SubmitSolutionPayload.ID, SubmitSolutionPayload.CODEC)
            ServerPlayNetworking.registerGlobalReceiver(SubmitSolutionPayload.ID) { payload, context ->
                val entity: BlockEntity? = context.player().level().getBlockEntity(payload.pos)
                if (entity is PuzzleFrameBlockEntity) entity.submitSolution(payload.path)
            }
        }

        override fun onClient() {
            PuzzleFrameBlockRenderer.register()
        }
    }

    val inventory = BlockInventory(INVENTORY_SIZE, this)

    override fun getContainer(state: BlockState, world: LevelAccessor, pos: BlockPos): WorldlyContainer = inventory

    /**
     * Server side: judges [path] against the panel in this frame, writes the authoritative line
     * (the path when accepted, nothing otherwise) and, on an accept, marks the frame solved.
     * Solved is sticky, so a later reject leaves it alone; an unpowered frame ignores the whole
     * thing, since its solver could not have been opened legitimately.
     */
    fun submitSolution(path: List<Node>) {
        val level: Level = level ?: return
        if (level.isClientSide) return
        val state: BlockState = blockState
        if (!state.getValue(IronPuzzleFrameBlock.POWERED)) return
        val stack: ItemStack = inventory.getItem(0)
        val panel: Panel = stack.panel ?: return

        val accepted: Boolean = panel.verdict(path) is Verdict.Accepted
        val line: Graph<Node> = if (accepted) path.toLine() else emptyGraph()
        inventory.setItem(0, stack.copy().apply { this.panel = panel.withLine(line) })

        if (accepted) {
            // The exit follows the last accepted line, so a re-solve to another end re-routes.
            val next: BlockState = state
                .setValue(IronPuzzleFrameBlock.SOLVED, true)
                .setValue(IronPuzzleFrameBlock.EXIT, IronPuzzleFrameBlock.Exit.of(path.exitSides()))
            if (next != state) {
                level.setBlock(blockPos, next, Block.UPDATE_ALL)
                // Frames ignore updates from frames, so the chain past this one is walked here.
                RedstoneNetwork.refresh(level, blockPos)
            }
        }
        sync()
    }

    override fun loadAdditional(view: ValueInput) {
        super.loadAdditional(view)
        inventory.items.clear()
        ContainerHelper.loadAllItems(view, inventory.items)
    }

    override fun saveAdditional(view: ValueOutput) {
        super.saveAdditional(view)
        ContainerHelper.saveAllItems(view, inventory.items)
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener>? =
        ClientboundBlockEntityDataPacket.create(this)

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag =
        saveWithoutMetadata(registries)

    /** Server-side: push the block entity state to watching clients. */
    override fun sync() {
        val level = level ?: return
        if (level.isClientSide) return
        setChanged()
        level.sendBlockUpdated(blockPos, blockState, blockState, Block.UPDATE_ALL)
    }
}
