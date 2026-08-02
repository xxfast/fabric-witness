package com.xfastgames.witness.entities

import com.xfastgames.witness.Witness
import com.xfastgames.witness.blocks.redstone.IronPuzzleFrameBlock
import com.xfastgames.witness.entities.renderer.PuzzleFrameBlockRenderer
import com.xfastgames.witness.utils.BlockInventory
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.Syncable
import com.xfastgames.witness.utils.registerBlockEntity
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.resources.Identifier
import net.minecraft.world.ContainerHelper
import net.minecraft.world.WorldlyContainer
import net.minecraft.world.WorldlyContainerHolder
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput

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

        override fun onClient() {
            PuzzleFrameBlockRenderer.register()
        }
    }

    val inventory = BlockInventory(INVENTORY_SIZE, this)

    override fun getContainer(state: BlockState, world: LevelAccessor, pos: BlockPos): WorldlyContainer = inventory

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
