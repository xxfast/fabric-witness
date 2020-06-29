package com.xfastgames.witness.entities

import com.xfastgames.witness.Witness
import com.xfastgames.witness.entities.renderers.PuzzleFrameRenderer
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.registerEntity
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricEntityTypeBuilder
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry
import net.minecraft.client.render.entity.EntityRenderDispatcher
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnGroup
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.mob.MobEntityWithAi
import net.minecraft.util.Identifier
import net.minecraft.world.World

class PuzzleFrameEntity(entityType: EntityType<out MobEntityWithAi?>?, world: World?) :
    MobEntityWithAi(entityType, world) {

    companion object : Clientside {
        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "puzzle_frame")
        val ENTITY_TYPE: EntityType<PuzzleFrameEntity> =
            registerEntity(IDENTIFIER) {
                FabricEntityTypeBuilder
                    .create(SpawnGroup.CREATURE) { entityType: EntityType<PuzzleFrameEntity>, world: World ->
                        PuzzleFrameEntity(entityType, world)
                    }
                    .dimensions(EntityDimensions.fixed(0.75f, 0.75f))
                    .build()
            }

        val ENTITY: Companion = this

        init {
            FabricDefaultAttributeRegistry.register(
                ENTITY_TYPE, MobEntity.createMobAttributes()
            )
        }

        override fun onClient() {
            EntityRendererRegistry.INSTANCE.register(ENTITY_TYPE) { dispatcher: EntityRenderDispatcher?, context: EntityRendererRegistry.Context? ->
                PuzzleFrameRenderer(dispatcher)
            }
        }
    }
}
