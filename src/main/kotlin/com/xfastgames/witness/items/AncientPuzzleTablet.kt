package com.xfastgames.witness.items

import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.itemSettings
import com.xfastgames.witness.utils.registerItem
import net.minecraft.world.item.Item
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Rarity

class AncientPuzzleTablet(settings: Properties) : Item(settings) {

    companion object {
        val IDENTIFIER = Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, "ancient_puzzle_tablet")
        val ITEM: Item = registerItem(
            IDENTIFIER,
            AncientPuzzleTablet(
                itemSettings(IDENTIFIER)
                    .rarity(Rarity.RARE)
                    .fireResistant()
            )
        )
    }
}
