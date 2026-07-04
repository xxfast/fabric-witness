package com.xfastgames.witness.items.data

import com.mojang.serialization.Codec
import com.xfastgames.witness.Witness
import net.minecraft.component.ComponentType
import net.minecraft.item.ItemStack
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

/**
 * Data components (1.20.5+) replacing the old raw-ItemStack NBT storage of puzzle state.
 * The [Panel] component reuses [Panel.CODEC], which delegates to the original NBT
 * (de)serialization, so the stored shape is behaviourally equivalent to the 1.17 version.
 */
object PanelComponents {

    val PANEL: ComponentType<Panel> = register(
        "panel",
        ComponentType.builder<Panel>()
            .codec(Panel.CODEC)
            .packetCodec(Panel.PACKET_CODEC)
            .build()
    )

    val COST: ComponentType<Int> = register(
        "cost",
        ComponentType.builder<Int>()
            .codec(Codec.INT)
            .packetCodec(PacketCodecs.INTEGER)
            .build()
    )

    private fun <T> register(name: String, type: ComponentType<T>): ComponentType<T> =
        Registry.register(Registries.DATA_COMPONENT_TYPE, Identifier.of(Witness.IDENTIFIER, name), type)

    /** Referenced from mod init to force registration of the component types. */
    fun init() {}
}

var ItemStack.panel: Panel?
    get() = get(PanelComponents.PANEL)
    set(value) {
        if (value != null) set(PanelComponents.PANEL, value) else remove(PanelComponents.PANEL)
    }

var ItemStack.cost: Int?
    get() = get(PanelComponents.COST)
    set(value) {
        if (value != null) set(PanelComponents.COST, value) else remove(PanelComponents.COST)
    }
