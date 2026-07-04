package com.xfastgames.witness.items

import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.itemSettings
import com.xfastgames.witness.utils.registerItem
import net.minecraft.item.Item
import net.minecraft.util.Identifier
import net.minecraft.util.Rarity

class AncientPuzzleTablet(settings: Settings) : Item(settings) {

    companion object {
        val IDENTIFIER = Identifier.of(Witness.IDENTIFIER, "ancient_puzzle_tablet")
        val ITEM: Item = registerItem(
            IDENTIFIER,
            AncientPuzzleTablet(
                itemSettings(IDENTIFIER)
                    .rarity(Rarity.RARE)
                    .fireproof()
            )
        )
    }
}
