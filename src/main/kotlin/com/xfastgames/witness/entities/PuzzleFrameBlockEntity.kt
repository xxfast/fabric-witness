package com.xfastgames.witness.entities

import com.xfastgames.witness.Witness
import com.xfastgames.witness.blocks.redstone.IronPuzzleFrameBlock
import com.xfastgames.witness.entities.renderer.PuzzleFrameBlockRenderer
import com.xfastgames.witness.utils.BlockInventory
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.registerBlockEntity
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry
import net.minecraft.block.BlockState
import net.minecraft.block.InventoryProvider
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.SidedInventory
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.WorldAccess

class PuzzleFrameBlockEntity(pos: BlockPos?, state: BlockState?) : BlockEntity(ENTITY_TYPE, pos, state),
    BlockEntityClientSerializable,
    InventoryProvider {

    companion object : Clientside {
        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "puzzle_frame_entity")

        const val INVENTORY_SIZE = 1

        val ENTITY_TYPE: BlockEntityType<PuzzleFrameBlockEntity> = registerBlockEntity(IDENTIFIER) {
            BlockEntityType.Builder
                .create({ pos, state -> PuzzleFrameBlockEntity(pos, state) }, IronPuzzleFrameBlock.BLOCK)
                .build(null)
        }

        override fun onClient() {
            BlockEntityRendererRegistry.INSTANCE.register(ENTITY_TYPE) { PuzzleFrameBlockRenderer() }
        }
    }

    val inventory = BlockInventory(INVENTORY_SIZE, this)

    override fun getInventory(state: BlockState?, world: WorldAccess?, pos: BlockPos?): SidedInventory = inventory

    override fun readNbt(nbt: NbtCompound?) {
        super.readNbt(nbt)
        inventory.items.clear()
        Inventories.readNbt(nbt, inventory.items)
    }

    override fun writeNbt(nbt: NbtCompound): NbtCompound {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory.items)
        return nbt
    }

    override fun toClientTag(nbt: NbtCompound): NbtCompound = writeNbt(nbt)
    override fun fromClientTag(tag: NbtCompound) = readNbt(tag)

    @Environment(EnvType.SERVER)
    override fun sync() {
        super.sync()
        markDirty()
    }
}
