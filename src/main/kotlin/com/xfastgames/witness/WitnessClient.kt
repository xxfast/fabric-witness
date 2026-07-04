package com.xfastgames.witness

import com.xfastgames.witness.screens.composer.PuzzleComposerScreen
import com.xfastgames.witness.utils.Clientside
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment

/**
 * Client entrypoint, split from [Witness] so that server environments never classload
 * client-only classes (renderers, screens) — the modern Fabric convention.
 */
@Environment(EnvType.CLIENT)
class WitnessClient : ClientModInitializer {

    override fun onInitializeClient() {
        val screens: List<Clientside> = listOf(PuzzleComposerScreen.Companion)

        (Witness.BLOCKS + Witness.ITEMS + Witness.ENTITIES + screens)
            .filterIsInstance<Clientside>()
            .forEach { it.onClient() }
    }
}
