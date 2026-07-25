package com.xfastgames.witness.sounds

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.sound.AbstractSoundInstance
import net.minecraft.client.sound.SoundInstance
import net.minecraft.client.sound.TickableSoundInstance
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.util.math.random.Random

/**
 * A sound that loops until [stop] is called. Plays at the listener, not in the world: the default
 * [AbstractSoundInstance] position is (0, 0, 0) with LINEAR attenuation, which is inaudible
 * anywhere but world origin.
 *
 * One instance is one playback. Build a new one to start the loop again.
 */
@Environment(EnvType.CLIENT)
class LoopingSoundInstance(
    soundEvent: SoundEvent,
    soundCategory: SoundCategory,
    volume: Float = 1f
) : AbstractSoundInstance(soundEvent, soundCategory, Random.create()), TickableSoundInstance {

    private var stopped: Boolean = false

    init {
        this.volume = volume
        repeat = true
        repeatDelay = 0
        relative = true
        attenuationType = SoundInstance.AttenuationType.NONE
    }

    fun play() {
        MinecraftClient.getInstance().soundManager.play(this)
    }

    /**
     * Flags the loop as finished. `SoundManager.stop` alone isn't enough: the sound system keeps
     * ticking the instance until [isDone] reports true, so a stopped-but-not-done loop restarts.
     */
    fun stop() {
        stopped = true
        MinecraftClient.getInstance().soundManager.stop(this)
    }

    override fun tick() = Unit

    override fun isDone(): Boolean = stopped
}
