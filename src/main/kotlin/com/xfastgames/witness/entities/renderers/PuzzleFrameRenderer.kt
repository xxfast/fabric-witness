package com.xfastgames.witness.entities.renderers

import com.xfastgames.witness.Witness
import com.xfastgames.witness.entities.PuzzleFrameEntity
import net.minecraft.block.BlockState
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.TexturedRenderLayers
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.entity.EntityRenderDispatcher
import net.minecraft.client.render.entity.EntityRenderer
import net.minecraft.client.util.ModelIdentifier
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.util.math.Vector3f
import net.minecraft.util.Identifier

/*
* A renderer is used to provide an entity model, shadow size, and texture.
*/
class PuzzleFrameRenderer(entityRenderDispatcher: EntityRenderDispatcher?) :
    EntityRenderer<PuzzleFrameEntity>(entityRenderDispatcher) {

    private val FRAME = ModelIdentifier("grass")
    private val client: MinecraftClient = MinecraftClient.getInstance()

    override fun getTexture(entity: PuzzleFrameEntity): Identifier {
        return Identifier(Witness.IDENTIFIER, "textures/entity/cube.png")
    }

    override fun render(
        entity: PuzzleFrameEntity,
        yaw: Float,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int
    ) {
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light)
        matrices.push()
        val direction = entity.horizontalFacing
        val vec3d = getPositionOffset(entity, tickDelta)
        matrices.translate(-vec3d.getX(), -vec3d.getY(), -vec3d.getZ())
        val directionalOffset = 0.46875
        matrices.translate(
            direction.offsetX.toDouble() * directionalOffset,
            direction.offsetY.toDouble() * directionalOffset,
            direction.offsetZ.toDouble() * directionalOffset
        )
        matrices.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(entity.pitch))
        matrices.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(180.0f - entity.yaw))
        val bl = entity.isInvisible
        if (!bl) {
            val blockRenderManager = client.blockRenderManager
            val bakedModelManager = blockRenderManager.models.modelManager
            val modelIdentifier = FRAME
            matrices.push()
            matrices.translate(-0.5, -0.5, -0.5)
            blockRenderManager.modelRenderer.render(
                matrices.peek(),
                vertexConsumers.getBuffer(TexturedRenderLayers.getEntitySolid()),
                null as BlockState?,
                bakedModelManager.getModel(modelIdentifier),
                1.0f,
                1.0f,
                1.0f,
                light,
                OverlayTexture.DEFAULT_UV
            )
            matrices.pop()
        }
        matrices.pop()
    }
}
