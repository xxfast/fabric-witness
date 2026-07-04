package com.xfastgames.witness.blocks.redstone

import com.mojang.serialization.MapCodec
import com.xfastgames.witness.Witness
import com.xfastgames.witness.entities.PuzzleComposerBlockEntity
import com.xfastgames.witness.screens.composer.PuzzleComposerScreen.Companion.PUZZLE_OUTPUT_SLOT_INDEX
import com.xfastgames.witness.utils.blockSettings
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.minecraft.block.*
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.*
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.state.StateManager
import net.minecraft.state.property.Properties.HORIZONTAL_FACING
import net.minecraft.util.ActionResult
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.World

class PuzzleComposerBlock(settings: AbstractBlock.Settings) : BlockWithEntity(settings) {

    init {
        defaultState = stateManager.defaultState.with(HORIZONTAL_FACING, Direction.NORTH)
    }

    companion object {
        val IDENTIFIER = Identifier.of(Witness.IDENTIFIER, "puzzle_composer")
        val CODEC: MapCodec<PuzzleComposerBlock> = createCodec(::PuzzleComposerBlock)
        val BLOCK: Block = registerBlock(
            PuzzleComposerBlock(blockSettings(IDENTIFIER).strength(2.5F).sounds(BlockSoundGroup.METAL)),
            IDENTIFIER
        )
        val BLOCK_ITEM: BlockItem = registerBlockItem(BLOCK, IDENTIFIER)
    }

    override fun getCodec(): MapCodec<out BlockWithEntity> = CODEC

    override fun getRenderType(state: BlockState?): BlockRenderType = BlockRenderType.MODEL

    override fun createBlockEntity(pos: BlockPos?, state: BlockState?): BlockEntity? =
        PuzzleComposerBlockEntity(pos, state)

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? {
        return super.getPlacementState(ctx)?.with(HORIZONTAL_FACING, ctx.horizontalPlayerFacing)
    }

    override fun appendProperties(stateManager: StateManager.Builder<Block, BlockState>) {
        stateManager.add(HORIZONTAL_FACING)
    }

    override fun getOutlineShape(
        state: BlockState?,
        world: BlockView?,
        pos: BlockPos?,
        context: ShapeContext?
    ): VoxelShape = VoxelShapes.fullCube()

    override fun onPlaced(
        world: World,
        pos: BlockPos,
        state: BlockState,
        placer: LivingEntity?,
        itemStack: ItemStack?
    ) {
        if (world.isClient) return
        val entity: BlockEntity = requireNotNull(world.getBlockEntity(pos))
        require(entity is PuzzleComposerBlockEntity)
        entity.sync()
        world.updateListeners(pos, state, state, 3)
    }

    override fun onBreak(world: World, pos: BlockPos, state: BlockState, player: PlayerEntity): BlockState {
        val entity: BlockEntity? = world.getBlockEntity(pos)
        require(entity is PuzzleComposerBlockEntity)
        // Output slot should not be dropped
        entity.inventory.removeStack(PUZZLE_OUTPUT_SLOT_INDEX)
        val updatedList: DefaultedList<ItemStack> = entity.inventory.items
        updatedList.forEach { stack -> dropStack(world, pos, stack) }
        return super.onBreak(world, pos, state, player)
    }

    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hit: BlockHitResult
    ): ActionResult {
        val factory: NamedScreenHandlerFactory = world.getBlockEntity(pos) as? NamedScreenHandlerFactory
            ?: return ActionResult.PASS
        player.openHandledScreen(factory)
        return ActionResult.SUCCESS
    }
}
