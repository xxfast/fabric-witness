package com.xfastgames.witness.entities

import com.xfastgames.witness.Witness
import com.xfastgames.witness.blocks.redstone.IronPuzzleFrameBlock
import com.xfastgames.witness.entities.renderer.PuzzleFrameBlockRenderer
import com.xfastgames.witness.utils.BlockInventory
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.Syncable
import com.xfastgames.witness.utils.registerBlockEntity
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.InventoryProvider
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.SidedInventory
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.registry.RegistryWrapper
import net.minecraft.storage.ReadView
import net.minecraft.storage.WriteView
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.WorldAccess

class PuzzleFrameBlockEntity(pos: BlockPos?, state: BlockState?) : BlockEntity(ENTITY_TYPE, pos, state),
    Syncable,
    InventoryProvider {

    companion object : Clientside {
        val IDENTIFIER = Identifier.of(Witness.IDENTIFIER, "puzzle_frame_entity")

        const val INVENTORY_SIZE = 1

        val ENTITY_TYPE: BlockEntityType<PuzzleFrameBlockEntity> = registerBlockEntity(IDENTIFIER) {
            FabricBlockEntityTypeBuilder
                .create({ pos, state -> PuzzleFrameBlockEntity(pos, state) }, IronPuzzleFrameBlock.BLOCK)
                .build()
        }

        override fun onClient() {
            PuzzleFrameBlockRenderer.register()
        }
    }

    val inventory = BlockInventory(INVENTORY_SIZE, this)

    override fun getInventory(state: BlockState?, world: WorldAccess?, pos: BlockPos?): SidedInventory = inventory

    override fun readData(view: ReadView) {
        super.readData(view)
        inventory.items.clear()
        Inventories.readData(view, inventory.items)
    }

    override fun writeData(view: WriteView) {
        super.writeData(view)
        Inventories.writeData(view, inventory.items)
    }

    override fun toUpdatePacket(): Packet<ClientPlayPacketListener>? = BlockEntityUpdateS2CPacket.create(this)

    override fun toInitialChunkDataNbt(registries: RegistryWrapper.WrapperLookup): NbtCompound = createNbt(registries)

    /** Server-side: push the block entity state to watching clients. */
    override fun sync() {
        val world = world ?: return
        if (world.isClient) return
        markDirty()
        world.updateListeners(pos, cachedState, cachedState, Block.NOTIFY_ALL)
    }
}
