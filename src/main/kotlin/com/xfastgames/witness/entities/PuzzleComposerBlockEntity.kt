package com.xfastgames.witness.entities

import com.xfastgames.witness.Witness
import com.xfastgames.witness.blocks.redstone.PuzzleComposerBlock
import com.xfastgames.witness.entities.renderer.PuzzleComposerBlockRenderer
import com.xfastgames.witness.items.Panel
import com.xfastgames.witness.items.PuzzlePanel
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.registerBlockEntity
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier
import java.util.function.Supplier

class PuzzleComposerBlockEntity : BlockEntity(ENTITY_TYPE) {

    var puzzle: ItemStack? = ItemStack(PuzzlePanel.ITEM, 1).apply { tag = Panel(6).toTag() }

    companion object : Clientside {
        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "puzzle_composer_entity")
        val ENTITY_TYPE: BlockEntityType<PuzzleComposerBlockEntity> = registerBlockEntity(IDENTIFIER) {
            BlockEntityType.Builder
                .create(Supplier { PuzzleComposerBlockEntity() }, PuzzleComposerBlock.BLOCK)
                .build(null)
        }

        override fun onClient() {
            BlockEntityRendererRegistry.INSTANCE.register(ENTITY_TYPE) { dispatcher: BlockEntityRenderDispatcher ->
                PuzzleComposerBlockRenderer(dispatcher)
            }
        }
    }

}