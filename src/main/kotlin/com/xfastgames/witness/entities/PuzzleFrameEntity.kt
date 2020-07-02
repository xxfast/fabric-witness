package com.xfastgames.witness.entities

import com.xfastgames.witness.Witness
import com.xfastgames.witness.entities.renderers.PuzzleFrameRenderer
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.registerEntity
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricEntityTypeBuilder
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry
import net.minecraft.client.render.entity.EntityRenderDispatcher
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnGroup
import net.minecraft.entity.decoration.AbstractDecorationEntity
import net.minecraft.network.Packet
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Identifier
import net.minecraft.world.World

class PuzzleFrameEntity(world: World) : AbstractDecorationEntity(ENTITY_TYPE, world) {

    companion object : Clientside {
        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "puzzle_frame")
        val ENTITY_TYPE: EntityType<PuzzleFrameEntity> =
            registerEntity(IDENTIFIER) {
                FabricEntityTypeBuilder
                    .create(SpawnGroup.CREATURE) { entityType: EntityType<PuzzleFrameEntity>, world: World ->
                        PuzzleFrameEntity(world)
                    }
                    .dimensions(EntityDimensions.fixed(0.75f, 0.75f))
                    .build()
            }

        val ENTITY: Companion = this

        override fun onClient() {
            EntityRendererRegistry.INSTANCE.register(ENTITY_TYPE) { dispatcher: EntityRenderDispatcher?, context: EntityRendererRegistry.Context? ->
                PuzzleFrameRenderer(dispatcher)
            }
        }
    }

    override fun createSpawnPacket(): Packet<*> = EntitySpawnS2CPacket(this, type, facing.id, this.decorationBlockPos)

    override fun onPlace() {
        playSound(SoundEvents.ENTITY_DOLPHIN_PLAY, 1.0f, 1.0f)
    }

    override fun getHeightPixels(): Int = 12
    override fun getWidthPixels(): Int = 12

    override fun onBreak(entity: Entity?) {
    }
}
