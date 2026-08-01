package com.xfastgames.witness.utils

import com.google.common.truth.Truth.assertThat
import com.xfastgames.witness.items.data.Panel
import com.xfastgames.witness.items.data.getPanel
import com.xfastgames.witness.items.data.putPanel
import com.xfastgames.witness.items.data.toNbt
import com.xfastgames.witness.items.data.toPanel
import com.xfastgames.witness.items.data.withTutorial
import net.minecraft.nbt.NbtCompound
import org.junit.jupiter.api.Test

private const val TEST_KEY_GRAPH = "panel"
class PanelTests {
    @Test
    fun `Test grid panel serialisation and deserialization`() {
        val gridPanel: Panel.Grid = Panel.Grid.ofSize(2, 4)
        val tag: NbtCompound = NbtCompound().apply { putPanel(TEST_KEY_GRAPH, gridPanel) }
        println(tag)
        val actual: Panel? = tag.getPanel(TEST_KEY_GRAPH)
        assertThat(actual).isEqualTo(gridPanel)
    }

    @Test
    fun `Test tree panel serialisation and deserialization`() {
        val treePanel: Panel.Tree = Panel.Tree.ofSize(2)
        val tag: NbtCompound = NbtCompound().apply { putPanel(TEST_KEY_GRAPH, treePanel) }
        println(tag)
        val actual: Panel? = tag.getPanel(TEST_KEY_GRAPH)
        assertThat(actual).isEqualTo(treePanel)
    }

    @Test
    fun `Tutorial flag round-trips and defaults off on legacy tags`() {
        val tutorial: Panel = Panel.Grid.ofSize(2).withTutorial(true)
        val tag: NbtCompound = NbtCompound().apply { putPanel(TEST_KEY_GRAPH, tutorial) }
        assertThat(tag.getPanel(TEST_KEY_GRAPH)).isEqualTo(tutorial)

        // Pre-flag panels have no "tutorial" key; they must read as non-tutorial.
        val legacyTag: NbtCompound = Panel.Grid.ofSize(2).toNbt().also { it.remove("tutorial") }
        assertThat(legacyTag.toPanel().tutorial).isFalse()
    }

    @Test
    fun `withTutorial toggles without touching the grid`() {
        val base: Panel.Grid = Panel.Grid.ofSize(3)
        val marked: Panel = base.withTutorial(true)
        assertThat(marked.tutorial).isTrue()
        assertThat(marked.graph).isEqualTo(base.graph)
        assertThat(marked.withTutorial(false).tutorial).isFalse()
    }
}