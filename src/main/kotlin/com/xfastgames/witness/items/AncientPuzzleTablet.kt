package com.xfastgames.witness.items

import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.registerItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.util.Identifier
import net.minecraft.util.Rarity

class AncientPuzzleTablet : Item(
    Settings()
        .group(ItemGroup.MATERIALS)
        .rarity(Rarity.RARE)
        .fireproof()
) {

    companion object {
        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "ancient_puzzle_tablet")
        val ITEM: Item = registerItem(IDENTIFIER, AncientPuzzleTablet())
    }
}