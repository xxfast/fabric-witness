package com.xfastgames.witness.items.data

import com.mojang.serialization.Codec
import com.xfastgames.witness.Witness
import net.minecraft.core.component.DataComponentType
import net.minecraft.world.item.ItemStack
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry
import net.minecraft.resources.Identifier

/**
 * Data components (1.20.5+) replacing the old raw-ItemStack NBT storage of puzzle state.
 * The [Panel] component reuses [Panel.CODEC], which delegates to the original NBT
 * (de)serialization, so the stored shape is behaviourally equivalent to the 1.17 version.
 */
object PanelComponents {

    val PANEL: DataComponentType<Panel> = register(
        "panel",
        DataComponentType.builder<Panel>()
            .persistent(Panel.CODEC)
            .networkSynchronized(Panel.STREAM_CODEC)
            .build()
    )

    val COST: DataComponentType<Int> = register(
        "cost",
        DataComponentType.builder<Int>()
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.INT)
            .build()
    )

    private fun <T : Any> register(name: String, type: DataComponentType<T>): DataComponentType<T> =
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, name), type)

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
