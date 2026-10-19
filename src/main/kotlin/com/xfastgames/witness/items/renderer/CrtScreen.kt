package com.xfastgames.witness.items.renderer

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology
import com.mojang.renderpearl.api.pipeline.BlendFunction
import com.mojang.renderpearl.api.pipeline.ColorTargetState
import com.mojang.renderpearl.api.pipeline.DepthStencilState
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import com.xfastgames.witness.Witness
import com.xfastgames.witness.mixin.render.RenderTypeInvokerMixin
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.network.chat.Component
import net.minecraft.client.renderer.BindGroupLayouts
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.resources.Identifier

/**
 * The layer a lit screen frame's picture is drawn on: vanilla's world `text` layer, with the
 * scanlines and static of `shaders/core/crt_screen` multiplied in
 * (rules/minecraft/05-1-screen-frame.md#the-picture).
 */
object CrtScreen {

    private val shader: Identifier = Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, "core/crt_screen")

    // Vanilla's TEXT pipeline, rebuilt from the public parts of its private TEXT_SNIPPET and
    // WORLD_TEXT_SNIPPET in the same order, with the shader swapped. Compiled on first use.
    private val pipeline: RenderPipeline by lazy {
        RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, "pipeline/crt_screen"))
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .withBindGroupLayout(BindGroupLayouts.FOG)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER2)
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withVertexShader(shader)
            .withFragmentShader(shader)
            .build()
    }

    /**
     * Debug switches for comparing frame rates, flipped with `/crt picture` and `/crt curve` in a
     * development run only; not saved, and always on for players.
     */
    var pictureEnabled: Boolean = true
    var curveEnabled: Boolean = true

    fun registerCommand() {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment) return

        fun toggle(name: String, flip: () -> Boolean): LiteralArgumentBuilder<FabricClientCommandSource> =
            ClientCommands.literal(name).executes { context ->
                val on: Boolean = flip()
                context.source.sendFeedback(Component.literal("CRT $name: ${if (on) "on" else "off"}"))
                return@executes 1
            }

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommands.literal("crt")
                    .then(toggle("picture") { pictureEnabled = !pictureEnabled; pictureEnabled })
                    .then(toggle("curve") { curveEnabled = !curveEnabled; curveEnabled })
            )
        }
    }

    private val layers: MutableMap<Identifier, RenderType> = HashMap()

    fun layer(texture: Identifier): RenderType = layers.getOrPut(texture) {
        val setup: RenderSetup = RenderSetup.builder(pipeline)
            .withTexture("Sampler0", texture)
            .useLightmap()
            .createRenderSetup()
        RenderTypeInvokerMixin.invokeCreate("${Witness.IDENTIFIER}_crt_screen", setup)
    }

    /**
     * Whether a shader pack is repainting the world, in which case the picture is left off
     * (rules/minecraft/05-1-screen-frame.md#the-picture). Iris having loaded is not enough: with
     * no pack selected it draws vanilla's pipelines, ours included. Read through reflection so the
     * mod neither needs Iris to compile nor to run.
     */
    val shaderPackInUse: () -> Boolean by lazy {
        if (!FabricLoader.getInstance().isModLoaded("iris")) return@lazy { false }
        try {
            val api: Class<*> = Class.forName("net.irisshaders.iris.api.v0.IrisApi")
            val instance: Any = api.getMethod("getInstance").invoke(null)
            val inUse = api.getMethod("isShaderPackInUse")
            return@lazy { inUse.invoke(instance) as Boolean }
        } catch (_: ReflectiveOperationException) {
            return@lazy { false }
        }
    }
}
