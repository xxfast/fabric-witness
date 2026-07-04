package com.xfastgames.witness.sounds

import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.registerSound
import net.minecraft.sound.SoundEvent
import net.minecraft.util.Identifier

/**
 * Sound events must be registered during common init: registries freeze afterwards, and this
 * object must not live in a client-only class so dedicated servers can classload it.
 */
object WitnessSounds {
    val PANEL_START_TRACING: SoundEvent = registerSound(Identifier.of(Witness.IDENTIFIER, "panel_start_tracing"))
    val POINTLESS_CLICK: SoundEvent = registerSound(Identifier.of(Witness.IDENTIFIER, "pointless_click"))
    val FOCUS_MODE_ENTER: SoundEvent = registerSound(Identifier.of(Witness.IDENTIFIER, "focus_mode_enter"))
    val FOCUS_MODE_EXIT: SoundEvent = registerSound(Identifier.of(Witness.IDENTIFIER, "focus_mode_exit"))
    val FOCUS_MODE_DOING: SoundEvent = registerSound(Identifier.of(Witness.IDENTIFIER, "focus_mode_doing"))
    val FOCUS_MODE_BEING: SoundEvent = registerSound(Identifier.of(Witness.IDENTIFIER, "focus_mode_being"))
    val FOCUS_MODE_CONSIDERING_EXIT: SoundEvent =
        registerSound(Identifier.of(Witness.IDENTIFIER, "focus_mode_considering_exit"))
    val FOCUS_MODE_WONDERING: SoundEvent =
        registerSound(Identifier.of(Witness.IDENTIFIER, "focus_mode_wondering"))

    fun init() {}
}
