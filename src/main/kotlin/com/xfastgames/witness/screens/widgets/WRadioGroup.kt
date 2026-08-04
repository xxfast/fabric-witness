package com.xfastgames.witness.screens.widgets

class WRadioGroup {
    private var members: MutableList<WRadioImageButton> = mutableListOf()

    val selected: WRadioImageButton? get() = members.firstOrNull { it.isSelected }

    fun add(member: WRadioImageButton) {
        members.add(member)
    }

    /**
     * Arms [selectedMember] and disarms the rest. Clicking the armed member re-arms it rather than
     * leaving the group with nothing selected: both rails are documented as having exactly one tool
     * armed (rules/minecraft/04-1-puzzle-composer-modifiers.md), and a Grid gesture has to resolve
     * to either the pencil or the eraser, never to neither.
     */
    fun select(selectedMember: WRadioImageButton) {
        selectedMember.isSelected = true
        members.forEach { member -> if (member != selectedMember) member.isSelected = false }
    }
}