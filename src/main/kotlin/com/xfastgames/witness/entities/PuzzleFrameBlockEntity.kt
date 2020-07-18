package com.xfastgames.witness.entities

import com.xfastgames.witness.Witness
import com.xfastgames.witness.blocks.redstone.PuzzleFrameBlock
import com.xfastgames.witness.entities.renderer.PuzzleFrameBlockRenderer
import com.xfastgames.witness.items.Panel
import com.xfastgames.witness.items.PuzzlePanel
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.registerBlockEntity
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.Identifier
import java.util.function.Supplier

private const val KEY_DATA = "frameData"

class PuzzleFrameBlockEntity : BlockEntity(ENTITY_TYPE), BlockEntityClientSerializable {

    var puzzleStack: ItemStack = Panel.DEFAULT.asItemStack()

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

    override fun fromTag(state: BlockState?, tag: CompoundTag) {
        super.fromTag(state, tag)
        if (!tag.contains(KEY_DATA)) return
        puzzleStack = ItemStack(PuzzlePanel.ITEM, 1).apply {
            this.tag = tag.getCompound(KEY_DATA)
        }
    }

    override fun toTag(tag: CompoundTag): CompoundTag {
        super.toTag(tag)
        tag.put(KEY_DATA, puzzleStack.tag)
        return tag
    }

    override fun toClientTag(tag: CompoundTag): CompoundTag = toTag(tag)

    override fun fromClientTag(tag: CompoundTag) = fromTag(null, tag)

    override fun sync() {
        super.sync()
        markDirty()
    }
}