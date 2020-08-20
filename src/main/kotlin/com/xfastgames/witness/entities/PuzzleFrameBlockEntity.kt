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
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.SidedInventory
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.WorldAccess
import java.util.function.Supplier

class PuzzleFrameBlockEntity : BlockEntity(ENTITY_TYPE),
    BlockEntityClientSerializable,
    InventoryProvider {

    companion object : Clientside {
        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "puzzle_frame_entity")

        const val INVENTORY_SIZE = 1

        val ENTITY_TYPE: BlockEntityType<PuzzleFrameBlockEntity> = registerBlockEntity(IDENTIFIER) {
            BlockEntityType.Builder
                .create(Supplier { PuzzleFrameBlockEntity() }, IronPuzzleFrameBlock.BLOCK)
                .build(null)
        }

        override fun onClient() {
            BlockEntityRendererRegistry.INSTANCE.register(ENTITY_TYPE) { dispatcher: BlockEntityRenderDispatcher ->
                PuzzleFrameBlockRenderer(dispatcher)
            }
        }
    }

    val inventory = BlockInventory(INVENTORY_SIZE, this)

    override fun getInventory(state: BlockState?, world: WorldAccess?, pos: BlockPos?): SidedInventory = inventory

    override fun fromTag(state: BlockState?, tag: CompoundTag) {
        super.fromTag(state, tag)
        inventory.items.clear()
        Inventories.fromTag(tag, inventory.items)
    }

    override fun toTag(tag: CompoundTag): CompoundTag {
        super.toTag(tag)
        Inventories.toTag(tag, inventory.items)
        return tag
    }

    override fun toClientTag(tag: CompoundTag): CompoundTag = toTag(tag)
    override fun fromClientTag(tag: CompoundTag) = fromTag(cachedState, tag)

    @Environment(EnvType.SERVER)
    override fun sync() {
        super.sync()
        markDirty()
    }
}
