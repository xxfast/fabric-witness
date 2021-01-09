package com.xfastgames.witness.utils

import com.google.common.truth.Truth.assertThat
import com.xfastgames.witness.items.data.Panel
import com.xfastgames.witness.items.data.getPanel
import com.xfastgames.witness.items.data.putPanel
import net.minecraft.nbt.CompoundTag
import org.junit.jupiter.api.Test

class PanelTests {
    @Test
    fun `Test grid panel serialisation and deserialization`() {
        val gridPanel: Panel.Grid = Panel.Grid.ofSize(2)
        val tag: CompoundTag = CompoundTag().apply { putPanel(gridPanel) }
        println(tag)
        val actual: Panel = tag.getPanel()
        assertThat(actual).isEqualTo(gridPanel)
    }

    @Test
    fun `Test tree panel serialisation and deserialization`() {
        val treePanel: Panel.Tree = Panel.Tree.ofSize(2)
        val tag: CompoundTag = CompoundTag().apply { putPanel(treePanel) }
        println(tag)
        val actual: Panel = tag.getPanel()
        assertThat(actual).isEqualTo(treePanel)
    }
}