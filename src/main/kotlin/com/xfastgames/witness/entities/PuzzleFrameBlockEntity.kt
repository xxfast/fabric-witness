package com.xfastgames.witness.entities

import com.xfastgames.witness.Witness
import com.xfastgames.witness.blocks.redstone.PuzzleFrameBlock
import com.xfastgames.witness.entities.renderer.PuzzleFrameBlockRenderer
import com.xfastgames.witness.utils.BlockInventory
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.registerBlockEntity
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher
import net.minecraft.inventory.Inventories
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import java.util.function.Supplier

class PuzzleFrameBlockEntity : BlockEntity(ENTITY_TYPE), BlockEntityClientSerializable, BlockInventory {

    companion object : Clientside {
        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "puzzle_frame_entity")
        val ENTITY_TYPE: BlockEntityType<PuzzleFrameBlockEntity> = registerBlockEntity(IDENTIFIER) {
            BlockEntityType.Builder
                .create(Supplier { PuzzleFrameBlockEntity() }, PuzzleFrameBlock.BLOCK)
                .build(null)
        }

        override fun onClient() {
            BlockEntityRendererRegistry.INSTANCE.register(ENTITY_TYPE) { dispatcher: BlockEntityRenderDispatcher ->
                PuzzleFrameBlockRenderer(dispatcher)
            }
        }
    }

    override val items: DefaultedList<ItemStack> = DefaultedList.ofSize(1, ItemStack.EMPTY)

    override fun fromTag(state: BlockState?, tag: CompoundTag) {
        super.fromTag(state, tag)
        this.items.clear()
        Inventories.fromTag(tag, items)
    }

    override fun toTag(tag: CompoundTag): CompoundTag {
        super.toTag(tag)
        Inventories.toTag(tag, items)
        return tag
    }

    override fun toClientTag(tag: CompoundTag): CompoundTag = toTag(tag)

    override fun fromClientTag(tag: CompoundTag) = fromTag(null, tag)

    @Environment(EnvType.SERVER)
    override fun sync() {
        super.sync()
        markDirty()
    }
}
