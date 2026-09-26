package com.xfastgames.witness.sounds

import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.registerSound
import net.minecraft.world.entity.player.Player
import net.minecraft.sounds.SoundEvent
import com.xfastgames.witness.blocks.redstone.ScreenPuzzleFrameBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.resources.Identifier
import kotlin.math.pow
import kotlin.random.Random

private const val SEMITONES_PER_OCTAVE = 12f

/**
 * A registered sound together with the mix it plays at: a volume on a 0-1 scale, and how far its
 * pitch is jittered per play, in semitones. Both come from the event model in
 * `assets/witness/sounds/USAGE.md`, so a cue is played the same way everywhere it fires.
 */
class WitnessSound(name: String, val volume: Float, private val pitchJitter: Float = 0f) {

    val event: SoundEvent = registerSound(Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, name))

    /** PitchDirection multiplier for one playback, jittered by up to ±[pitchJitter] semitones. */
    fun pitch(): Float {
        if (pitchJitter == 0f) return 1f
        val semitones: Float = Random.nextFloat() * 2 * pitchJitter - pitchJitter
        return 2f.pow(semitones / SEMITONES_PER_OCTAVE)
    }
}

/** Plays [sound] at its documented mix. Called from the client, where this plays at the listener. */
fun Player.play(sound: WitnessSound, volumeScale: Float = 1f) =
    playSound(sound.event, sound.volume * volumeScale, sound.pitch())

/**
 * Sound events must be registered during common init: registries freeze afterwards, and this
 * object must not live in a client-only class so dedicated servers can classload it.
 *
 * Panel cues are 2D interface sounds played while focused on a panel, not positional ones: a solve
 * chime shouldn't attenuate with distance. The per-surface reverb variants shipped alongside these
 * (`crt_`, `defaultverb_`, `glassverb_`) need an acoustic zone concept and aren't wired up yet.
 */
/**
 * The nine panel cues as one surface plays them (`assets/witness/sounds/USAGE.md`, "Variants by
 * surface"): [zone] null is the base set every panel uses, `"crt"` the tube treatment a screen frame
 * plays (rules/minecraft/05-1-screen-frame.md). The name is composed as `<zone>_panel_<event>`, the
 * way the original composes it, so a new surface is one more instance and its files.
 */
class PanelCues(private val zone: String?) {
    private fun name(event: String): String = if (zone == null) "panel_$event" else "${zone}_panel_$event"

    val START_TRACING = WitnessSound(name("start_tracing"), volume = .4f)
    val FINISH_TRACING = WitnessSound(name("finish_tracing"), volume = .2f)
    val ABORT_TRACING = WitnessSound(name("abort_tracing"), volume = .4f)
    val ABORT_FINISH_TRACING = WitnessSound(name("abort_finish_tracing"), volume = .3f)

    /** Pitch jittered per play so hovering a lattice of nodes doesn't get grating. */
    val SCINT_STARTPOINT = WitnessSound(name("scint_startpoint"), volume = .12f, pitchJitter = .9f)
    val SCINT_ENDPOINT = WitnessSound(name("scint_endpoint"), volume = .15f, pitchJitter = .9f)

    /**
     * Registered but not played yet: this is the interim warning for a rule that fails visibly
     * mid-trace, which is eliminators (rules/witness/11-eliminators.md), not a guess at whether
     * the finished path would validate.
     */
    val POTENTIAL_FAILURE = WitnessSound(name("potential_failure"), volume = .4f)

    /** The base set has four alternates behind the one event, picked at random by `sounds.json`. */
    val SUCCESS = WitnessSound(name("success"), volume = .3f)
    val FAILURE = WitnessSound(name("failure"), volume = .3f)
}

object WitnessSounds {
    /** The cues every panel plays. */
    val PANEL = PanelCues(zone = null)

    /** The cues a screen frame plays: the same nine events through a tube (rules/minecraft/05-1-screen-frame.md). */
    val CRT_PANEL = PanelCues(zone = "crt")

    /** Which set the panel in a frame of [state]'s kind plays. */
    fun panelCues(state: BlockState): PanelCues =
        if (state.block is ScreenPuzzleFrameBlock) CRT_PANEL else PANEL

    /** Outside the per-surface scheme: the same chirp on every frame (`USAGE.md`, "Variants by surface"). */
    val PANEL_PATH_COMPLETE = WitnessSound("panel_path_complete", volume = .15f)

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
