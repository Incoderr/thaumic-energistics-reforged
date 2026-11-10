package thaumicenergistics.block;

import appeng.util.InteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public abstract class AeWrenchableBlock extends Block {
    protected AeWrenchableBlock(Properties properties) {
        super(properties);
    }

    protected InteractionResult activate(Level level, BlockPos pos, Player player) {
        return InteractionResult.PASS;
    }

    protected ItemStack dismantledStack(Level level, BlockPos pos) {
        return new ItemStack(this);
    }

    protected boolean canPlayerInteract(Player player) {
        return true;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!canPlayerInteract(player)) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (!InteractionUtil.canWrenchDisassemble(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (player.isShiftKeyDown() && !level.isClientSide()) {
            ItemStack dismantled = dismantledStack(level, pos);
            level.removeBlock(pos, false);
            popResource(level, pos, dismantled);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!canPlayerInteract(player)) {
            return InteractionResult.PASS;
        }
        return activate(level, pos, player);
    }
}
