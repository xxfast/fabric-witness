package com.xfastgames.witness.entities.renderers

import com.xfastgames.witness.Witness
import com.xfastgames.witness.entities.PuzzleFrameEntity
import com.xfastgames.witness.entities.models.PuzzleFrameModel
import net.minecraft.client.render.entity.EntityRenderDispatcher
import net.minecraft.client.render.entity.MobEntityRenderer
import net.minecraft.util.Identifier

/*
* A renderer is used to provide an entity model, shadow size, and texture.
*/
class PuzzleFrameRenderer(entityRenderDispatcher: EntityRenderDispatcher?) :
    MobEntityRenderer<PuzzleFrameEntity, PuzzleFrameModel>(entityRenderDispatcher, PuzzleFrameModel(), 0.5f) {

    override fun getTexture(entity: PuzzleFrameEntity): Identifier {
        return Identifier(Witness.IDENTIFIER, "textures/entity/cube.png")
    }
}
