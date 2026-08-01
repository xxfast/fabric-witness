package com.xfastgames.witness.sounds

import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.registerSound
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.sound.SoundEvent
import net.minecraft.util.Identifier
import kotlin.math.pow
import kotlin.random.Random

private const val SEMITONES_PER_OCTAVE = 12f

/**
 * A registered sound together with the mix it plays at: a volume on a 0-1 scale, and how far its
 * pitch is jittered per play, in semitones. Both come from the event model in
 * `assets/witness/sounds/USAGE.md`, so a cue is played the same way everywhere it fires.
 */
class WitnessSound(name: String, val volume: Float, private val pitchJitter: Float = 0f) {

    val event: SoundEvent = registerSound(Identifier.of(Witness.IDENTIFIER, name))

    /** Pitch multiplier for one playback, jittered by up to ±[pitchJitter] semitones. */
    fun pitch(): Float {
        if (pitchJitter == 0f) return 1f
        val semitones: Float = Random.nextFloat() * 2 * pitchJitter - pitchJitter
        return 2f.pow(semitones / SEMITONES_PER_OCTAVE)
    }
}

/** Plays [sound] at its documented mix. Called from the client, where this plays at the listener. */
fun PlayerEntity.play(sound: WitnessSound, volumeScale: Float = 1f) =
    playSound(sound.event, sound.volume * volumeScale, sound.pitch())

/**
 * Sound events must be registered during common init: registries freeze afterwards, and this
 * object must not live in a client-only class so dedicated servers can classload it.
 *
 * Panel cues are 2D interface sounds played while focused on a panel, not positional ones: a solve
 * chime shouldn't attenuate with distance. The per-surface reverb variants shipped alongside these
 * (`crt_`, `defaultverb_`, `glassverb_`) need an acoustic zone concept and aren't wired up yet.
 */
object WitnessSounds {
    val PANEL_START_TRACING = WitnessSound("panel_start_tracing", volume = .4f)
    val PANEL_FINISH_TRACING = WitnessSound("panel_finish_tracing", volume = .2f)
    val PANEL_ABORT_TRACING = WitnessSound("panel_abort_tracing", volume = .4f)
    val PANEL_ABORT_FINISH_TRACING = WitnessSound("panel_abort_finish_tracing", volume = .3f)

    /** Pitch jittered per play so hovering a lattice of nodes doesn't get grating. */
    val PANEL_SCINT_STARTPOINT = WitnessSound("panel_scint_startpoint", volume = .12f, pitchJitter = .9f)
    val PANEL_SCINT_ENDPOINT = WitnessSound("panel_scint_endpoint", volume = .15f, pitchJitter = .9f)

    val PANEL_PATH_COMPLETE = WitnessSound("panel_path_complete", volume = .15f)

    /**
     * Registered but not played yet: this is the interim warning for a rule that fails visibly
     * mid-trace, which is eliminators (rules/witness/11-eliminators.md), not a guess at whether
     * the finished path would validate.
     */
    val PANEL_POTENTIAL_FAILURE = WitnessSound("panel_potential_failure", volume = .4f)

    /** Four alternates behind the one event, picked at random by `sounds.json`. */
    val PANEL_SUCCESS = WitnessSound("panel_success", volume = .3f)
    val PANEL_FAILURE = WitnessSound("panel_failure", volume = .3f)

    val POINTLESS_CLICK = WitnessSound("pointless_click", volume = .3f)

    /** `enter` and `exit` bracket the solver screen; the rest are ambient layers inside it. */
    val FOCUS_MODE_ENTER = WitnessSound("focus_mode_enter", volume = .2f)
    val FOCUS_MODE_EXIT = WitnessSound("focus_mode_exit", volume = .2f)

    /**
     * Well under the .13 the rest of focus mode sits at, and faded in rather than dropped in: this
     * is the bed that runs the whole time a panel is open, so it has to sit behind the cues instead
     * of competing with them.
     */
    val FOCUS_MODE_BEING = WitnessSound("focus_mode_being", volume = .05f)

    val FOCUS_MODE_DOING = WitnessSound("focus_mode_doing", volume = .13f)
    val FOCUS_MODE_WONDERING = WitnessSound("focus_mode_wondering", volume = .17f)
    val FOCUS_MODE_CONSIDERING_EXIT = WitnessSound("focus_mode_considering_exit", volume = .17f)

    fun init() {}
}
