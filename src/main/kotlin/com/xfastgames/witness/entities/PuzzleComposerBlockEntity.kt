package com.xfastgames.witness.entities

import com.xfastgames.witness.Witness
import com.xfastgames.witness.blocks.redstone.PuzzleComposerBlock
import com.xfastgames.witness.entities.renderer.PuzzleComposerBlockRenderer
import com.xfastgames.witness.screens.PuzzleScreen
import com.xfastgames.witness.utils.BlockInventory
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.registerBlockEntity
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry
import net.minecraft.block.BlockState
import net.minecraft.block.InventoryProvider
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.WorldAccess
import java.util.function.Supplier

class PuzzleComposerBlockEntity : BlockEntity(ENTITY_TYPE),
    NamedScreenHandlerFactory,
    InventoryProvider,
    BlockEntityClientSerializable,
    BlockInventory {

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

    override var items: DefaultedList<ItemStack> = DefaultedList.ofSize(1, ItemStack.EMPTY)

    override fun createMenu(syncId: Int, inv: PlayerInventory, player: PlayerEntity?): ScreenHandler? =
        PuzzleScreen.PuzzleScreenDescription(syncId, inv, ScreenHandlerContext.create(world, pos))

    override fun getDisplayName(): Text = TranslatableText(cachedState.block.translationKey)

    override fun canPlayerUse(player: PlayerEntity?): Boolean = true
    override fun getInventory(state: BlockState?, world: WorldAccess?, pos: BlockPos?): SidedInventory = this

    override fun fromTag(state: BlockState?, tag: CompoundTag) {
        super.fromTag(state, tag)
        items.clear()
        Inventories.fromTag(tag, items)
    }

    override fun toTag(tag: CompoundTag): CompoundTag {
        super.toTag(tag)
        Inventories.toTag(tag, items)
        return tag
    }

    override fun toClientTag(tag: CompoundTag): CompoundTag = toTag(tag)
    override fun fromClientTag(tag: CompoundTag) = fromTag(null, tag)

    override fun markDirty() {
        sync()
    }
}