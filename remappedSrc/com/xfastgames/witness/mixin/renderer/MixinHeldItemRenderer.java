package com.xfastgames.witness.mixin.renderer;

import com.xfastgames.witness.registries.HeldItemRendererRegistryImpl;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class MixinHeldItemRenderer {

    private ItemStack getActiveItemStack(LivingEntity entity) {
        ItemStack itemStack;
        if (entity.isUsingItem()) itemStack = entity.getActiveItem();
        else itemStack = entity.getMainHandStack();
        return itemStack;
    }

    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
            at = @At("HEAD"),
            cancellable = true)
    public void fabric_renderItem(float tickDelta, MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers, ClientPlayerEntity player, int light, CallbackInfo ci) {
        ItemStack itemStack = getActiveItemStack(player);
        HeldItemRenderer renderer = HeldItemRendererRegistryImpl.Companion.getRenderer(itemStack.getItem());

        if (renderer != null) {
            renderer.renderItem(tickDelta, matrices, vertexConsumers, player, light);
            ci.cancel();
        }
    }
}
