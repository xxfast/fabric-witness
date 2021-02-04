package com.xfastgames.witness.screens.widgets

class WRadioGroup {
    private var members: MutableList<WRadioImageButton> = mutableListOf()

    val selected: WRadioImageButton? get() = members.firstOrNull { it.isSelected }

    fun add(member: WRadioImageButton) {
        members.add(member)
    }

    fun select(selectedMember: WRadioImageButton) {
        members.forEach { member -> if (member != selectedMember) member.isSelected = false }
    }
}