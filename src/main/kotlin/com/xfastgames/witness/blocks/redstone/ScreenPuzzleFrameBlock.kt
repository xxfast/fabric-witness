package com.xfastgames.witness.blocks.redstone

import com.mojang.serialization.MapCodec
import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.d
import com.xfastgames.witness.utils.pc
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.minecraft.resources.Identifier
import net.minecraft.world.item.BlockItem
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

/**
 * The puzzle frame in the opening shed's CRT cabinet (rules/minecraft/05-1-screen-frame.md): a
 * slate box the full width of the block, so a row of them shares one cabinet with no join models,
 * and a black glass face with the panel drawn as a glowing tube inside it.
 *
 * Behaviour is the iron frame's, untouched: this class only changes the housing. The face is drawn
 * by [com.xfastgames.witness.entities.renderer.PuzzleFrameBlockRenderer], which picks the screen
 * look off the block class.
 */
class ScreenPuzzleFrameBlock(settings: BlockBehaviour.Properties) : IronPuzzleFrameBlock(settings) {

    companion object {
        val IDENTIFIER = Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, "screen_puzzle_frame")
        val CODEC: MapCodec<ScreenPuzzleFrameBlock> = simpleCodec(::ScreenPuzzleFrameBlock)
        val BLOCK: Block = registerBlock(ScreenPuzzleFrameBlock(frameSettings(IDENTIFIER)), IDENTIFIER)
        val BLOCK_ITEM: BlockItem = registerBlockItem(BLOCK, IDENTIFIER)
    }

    override fun codec(): MapCodec<out BaseEntityBlock> = CODEC

    /** The cabinet: the whole block from the wall behind to the bezel's front, 10 px deep. */
    override val housingShape: VoxelShape = Shapes.box(0.pc.d, 0.pc.d, 0.pc.d, 16.pc.d, 16.pc.d, 10.pc.d)
}
