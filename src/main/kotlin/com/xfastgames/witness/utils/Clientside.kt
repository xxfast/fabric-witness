package com.xfastgames.witness.utils

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment

interface Clientside {
    @Environment(EnvType.CLIENT)
    fun onClient() {
    }
}