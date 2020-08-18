package com.xfastgames.witness.blocks.redstone

import com.xfastgames.witness.Witness
import com.xfastgames.witness.entities.PuzzleFrameBlockEntity
import com.xfastgames.witness.items.PuzzlePanelItem
import com.xfastgames.witness.utils.BlockInventory
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import com.xfastgames.witness.utils.rotateShape
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.*
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.*
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.sound.SoundEvents
import net.minecraft.state.StateManager
import net.minecraft.state.property.Properties.HORIZONTAL_FACING
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.World

class PuzzleFrameBlock : BlockWithEntity(
    FabricBlockSettings.of(Material.METAL)
        .strength(2.5f)
        .sounds(BlockSoundGroup.METAL)
        .lightLevel { state -> 15 }
) {

    init {
        defaultState = stateManager.defaultState.with(HORIZONTAL_FACING, Direction.NORTH)
    }

    companion object {
        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "puzzle_frame")
        val BLOCK: Block = registerBlock(PuzzleFrameBlock(), IDENTIFIER)
        val BLOCK_ITEM: BlockItem = registerBlockItem(BLOCK, IDENTIFIER, Item.Settings().group(ItemGroup.REDSTONE))
    }

    override fun getRenderType(state: BlockState?): BlockRenderType = BlockRenderType.MODEL

    override fun createBlockEntity(world: BlockView?): BlockEntity? = PuzzleFrameBlockEntity()

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? {
        return super.getPlacementState(ctx)?.with(HORIZONTAL_FACING, ctx.playerFacing)
    }

    override fun appendProperties(stateManager: StateManager.Builder<Block, BlockState>) {
        stateManager.add(HORIZONTAL_FACING)
    }

    override fun getOutlineShape(
        state: BlockState?,
        world: BlockView?,
        pos: BlockPos?,
        context: ShapeContext?
    ): VoxelShape {
        val baseShape: VoxelShape =
            VoxelShapes.cuboid(.0, .0, .375, 1.0, 1.0, .625)
        val direction: Direction = requireNotNull(state?.get(HORIZONTAL_FACING))
        return baseShape.rotateShape(to = direction)
    }

    override fun onPlaced(
        world: World,
        pos: BlockPos?,
        state: BlockState?,
        placer: LivingEntity?,
        itemStack: ItemStack?
    ) {
        if (world.isClient) return
        val entity: BlockEntity = requireNotNull(world.getBlockEntity(pos))
        require(entity is PuzzleFrameBlockEntity)
        entity.sync()
        world.updateListeners(pos, state, state, 3)
    }

    override fun onUse(
        state: BlockState?,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand?,
        hit: BlockHitResult?
    ): ActionResult {
        val entity: BlockEntity = requireNotNull(world.getBlockEntity(pos))
        require(entity is PuzzleFrameBlockEntity)
        val inventory: BlockInventory = entity.inventory
        when {
            // if there's no item in the frame, and player is holding a panel
            inventory.items[0].isEmpty && player.mainHandStack.item is PuzzlePanelItem -> {
                inventory.items[0] = player.inventory.removeStack(player.inventory.selectedSlot)
                player.playSound(SoundEvents.ITEM_ARMOR_EQUIP_IRON, 1f, 1f)
            }

            // if there is an item in the frame
            !inventory.items[0].isEmpty && player.isInSneakingPose -> when {
                player.mainHandStack.isEmpty -> {
                    val item: ItemStack = inventory.items[0]
                    inventory.removeStack(0)
                    player.setStackInHand(Hand.MAIN_HAND, item)
                }
                player.offHandStack.isEmpty -> {
                    val item: ItemStack = inventory.items[0]
                    inventory.removeStack(0)
                    player.setStackInHand(Hand.OFF_HAND, item)
                }
                else -> ActionResult.FAIL
            }

            else -> return ActionResult.FAIL

        }

        if (world.isClient) return ActionResult.SUCCESS
        entity.sync()
        world.updateListeners(pos, state, state, 3)
        return ActionResult.SUCCESS
    }
}