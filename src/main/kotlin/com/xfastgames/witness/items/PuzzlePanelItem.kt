package com.xfastgames.witness.items

import com.xfastgames.witness.Witness
import com.xfastgames.witness.items.data.Panel
import com.xfastgames.witness.items.data.getPanel
import com.xfastgames.witness.items.data.putPanel
import com.xfastgames.witness.items.renderer.PuzzlePanelRenderer
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.registerItem
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry
import net.minecraft.client.item.TooltipContext
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World

class PuzzlePanelItem : Item(Settings().group(ItemGroup.REDSTONE)), Clientside {

    companion object {
        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "puzzle_panel")
        val ITEM = registerItem(
            IDENTIFIER,
            PuzzlePanelItem()
        )
    }

    override fun onCraft(stack: ItemStack?, world: World?, player: PlayerEntity?) {
        stack?.tag = CompoundTag().apply { putPanel(Panel(3)) }
    }

    override fun onClient() {
        BuiltinItemRendererRegistry.INSTANCE.register(ITEM, PuzzlePanelRenderer)
    }

    override fun use(world: World, user: PlayerEntity, hand: Hand?): TypedActionResult<ItemStack> {
        val panel: Panel = requireNotNull(user.mainHandStack.tag).getPanel().copy(line = listOf(1f, 0f, 0f, 0f, 0f, 1f))
        user.mainHandStack.orCreateTag?.putPanel(panel)
        return TypedActionResult(
            ActionResult.SUCCESS,
            if (hand === Hand.MAIN_HAND) user.mainHandStack else user.offHandStack
        )
    }

    override fun appendTooltip(
        stack: ItemStack,
        world: World?,
        tooltip: MutableList<Text>,
        context: TooltipContext?
    ) {
        val puzzle: Panel? = stack.tag?.getPanel()
        puzzle?.let { tooltip.add(Text.of("size ${it.width} x ${it.height}")) }
    }
}