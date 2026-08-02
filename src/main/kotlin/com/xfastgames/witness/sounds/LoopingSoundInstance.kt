package com.xfastgames.witness.sounds

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.AbstractSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.client.resources.sounds.TickableSoundInstance
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource

/**
 * A sound that loops until [stop] is called, optionally easing up to its mix volume over
 * [fadeInTicks] client ticks. Plays at the listener, not in the world: the default
 * [AbstractSoundInstance] position is (0, 0, 0) with LINEAR attenuation, which is inaudible
 * anywhere but world origin.
 *
 * One instance is one playback. Build a new one to start the loop again.
 */
@Environment(EnvType.CLIENT)
class LoopingSoundInstance(
    sound: WitnessSound,
    soundCategory: SoundSource,
    private val fadeInTicks: Int = 0
) : AbstractSoundInstance(sound.event, soundCategory, RandomSource.create()), TickableSoundInstance {

    private val mixVolume: Float = sound.volume
    private var stopped: Boolean = false
    private var fadeTicks: Int = 0

    init {
        // Never start at exactly zero: the sound system skips a sound that is silent on the tick
        // it is played, and the loop would never begin.
        this.volume = fadedVolume(if (fadeInTicks > 0) 1 else fadeInTicks)
        looping = true
        delay = 0
        relative = true
        attenuation = SoundInstance.Attenuation.NONE
    }

    fun play() {
        Minecraft.getInstance().soundManager.play(this)
    }

    /**
     * Flags the loop as finished. `SoundManager.stop` alone isn't enough: the sound system keeps
     * ticking the instance until [isStopped] reports true, so a stopped-but-not-done loop restarts.
     */
    fun stop() {
        stopped = true
        Minecraft.getInstance().soundManager.stop(this)
    }

    /** The sound system re-reads [volume] from a tickable instance every tick, so ramping it here
     * is the fade. */
    override fun tick() {
        if (fadeTicks >= fadeInTicks) return
        fadeTicks++
        volume = fadedVolume(fadeTicks)
    }

    /**
     * Squared rather than linear: amplitude tracks perceived loudness poorly, and a linear ramp
     * arrives most of the way in its first moments, which is the abruptness the fade is here to
     * avoid.
     */
    private fun fadedVolume(ticks: Int): Float {
        if (fadeInTicks <= 0) return mixVolume
        val progress: Float = ticks.toFloat() / fadeInTicks
        return mixVolume * progress * progress
    }

    override fun isStopped(): Boolean = stopped
}
