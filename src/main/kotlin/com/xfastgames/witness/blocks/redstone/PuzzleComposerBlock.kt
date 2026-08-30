package com.xfastgames.witness.blocks.redstone

import com.mojang.serialization.MapCodec
import com.xfastgames.witness.Witness
import com.xfastgames.witness.entities.PuzzleComposerBlockEntity
import com.xfastgames.witness.screens.composer.PuzzleComposerScreen.Companion.PUZZLE_OUTPUT_SLOT_INDEX
import com.xfastgames.witness.utils.blockSettings
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.*
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.MenuProvider
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING
import net.minecraft.world.InteractionResult
import net.minecraft.resources.Identifier
import net.minecraft.core.NonNullList
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level

class PuzzleComposerBlock(settings: BlockBehaviour.Properties) : BaseEntityBlock(settings) {

    init {
        registerDefaultState(stateDefinition.any().setValue(HORIZONTAL_FACING, Direction.NORTH))
    }

    companion object {
        val IDENTIFIER = Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, "puzzle_composer")
        val CODEC: MapCodec<PuzzleComposerBlock> = simpleCodec(::PuzzleComposerBlock)
        val BLOCK: Block = registerBlock(
            PuzzleComposerBlock(blockSettings(IDENTIFIER).strength(2.5F).requiresCorrectToolForDrops().sound(SoundType.METAL)),
            IDENTIFIER
        )
        val BLOCK_ITEM: BlockItem = registerBlockItem(BLOCK, IDENTIFIER)
    }

    override fun codec(): MapCodec<out BaseEntityBlock> = CODEC

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        PuzzleComposerBlockEntity(pos, state)

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState {
        return defaultBlockState().setValue(HORIZONTAL_FACING, ctx.horizontalDirection)
    }

    override fun createBlockStateDefinition(stateDefinition: StateDefinition.Builder<Block, BlockState>) {
        stateDefinition.add(HORIZONTAL_FACING)
    }

    override fun getShape(
        state: BlockState,
        world: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape = Shapes.block()

    override fun setPlacedBy(
        world: Level,
        pos: BlockPos,
        state: BlockState,
        placer: LivingEntity?,
        itemStack: ItemStack
    ) {
        if (world.isClientSide) return
        val entity: BlockEntity = requireNotNull(world.getBlockEntity(pos))
        require(entity is PuzzleComposerBlockEntity)
        entity.sync()
        world.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL)
    }

    override fun playerWillDestroy(world: Level, pos: BlockPos, state: BlockState, player: Player): BlockState {
        val entity: BlockEntity? = world.getBlockEntity(pos)
        require(entity is PuzzleComposerBlockEntity)
        // Output slot should not be dropped
        entity.inventory.removeItem(PUZZLE_OUTPUT_SLOT_INDEX, entity.inventory.getItem(PUZZLE_OUTPUT_SLOT_INDEX).count)
        val updatedList: NonNullList<ItemStack> = entity.inventory.items
        updatedList.forEach { stack -> Block.popResource(world, pos, stack) }
        return super.playerWillDestroy(world, pos, state, player)
    }

    override fun useWithoutItem(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
        hit: BlockHitResult
    ): InteractionResult {
        val factory: MenuProvider = world.getBlockEntity(pos) as? MenuProvider
            ?: return InteractionResult.PASS
        player.openMenu(factory)
        return InteractionResult.SUCCESS
    }
}
