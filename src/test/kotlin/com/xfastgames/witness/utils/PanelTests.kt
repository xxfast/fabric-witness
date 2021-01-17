package com.xfastgames.witness.utils

import com.google.common.truth.Truth.assertThat
import com.xfastgames.witness.items.data.Panel
import com.xfastgames.witness.items.data.getPanel
import com.xfastgames.witness.items.data.putPanel
import net.minecraft.nbt.CompoundTag
import org.junit.jupiter.api.Test

private const val TEST_KEY_GRAPH = "graph"
class PanelTests {
    @Test
    fun `Test grid panel serialisation and deserialization`() {
        val gridPanel: Panel.Grid = Panel.Grid.ofSize(2)
        val tag: CompoundTag = CompoundTag().apply { putPanel(TEST_KEY_GRAPH, gridPanel) }
        println(tag)
        val actual: Panel? = tag.getPanel(TEST_KEY_GRAPH)
        assertThat(actual).isEqualTo(gridPanel)
    }

    @Test
    fun `Test tree panel serialisation and deserialization`() {
        val treePanel: Panel.Tree = Panel.Tree.ofSize(2)
        val tag: CompoundTag = CompoundTag().apply { putPanel(TEST_KEY_GRAPH, treePanel) }
        println(tag)
        val actual: Panel? = tag.getPanel(TEST_KEY_GRAPH)
        assertThat(actual).isEqualTo(treePanel)
    }
}