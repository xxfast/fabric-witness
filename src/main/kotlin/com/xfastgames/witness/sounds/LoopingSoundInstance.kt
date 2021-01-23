package com.xfastgames.witness.sounds

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.sound.AbstractSoundInstance
import net.minecraft.client.sound.SoundManager
import net.minecraft.client.sound.TickableSoundInstance
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent

@Environment(EnvType.CLIENT)
class LoopingSoundInstance(soundEvent: SoundEvent, soundCategory: SoundCategory) :
    AbstractSoundInstance(soundEvent, soundCategory), TickableSoundInstance {

    private val soundManager: SoundManager = MinecraftClient.getInstance().soundManager

    override fun tick() {
        if (!soundManager.isPlaying(this)) soundManager.playNextTick(this)
    }

    fun stop() {
        soundManager.stop(this)
    }

    override fun isRepeatable(): Boolean = true
    override fun isLooping(): Boolean = true
    override fun isDone(): Boolean = false
}