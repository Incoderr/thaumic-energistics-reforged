package thaumicenergistics.wrench;

import appeng.api.parts.IPartHost;
import appeng.core.definitions.AEItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.wand.WandItem;
import thaumcraft.wand.focus.FocusItem;
import thaumcraft.wand.focus.FocusUpgradeType;
import thaumicenergistics.ThaumicEnergistics;

public final class AeWrenchFocusItem extends FocusItem {
    private static final AspectList CAST_COST = new AspectList().add(Aspect.FIRE, 10).add(Aspect.AIR, 10);
    private static final int COOLDOWN = 250;

    public AeWrenchFocusItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getFocusColor(ItemStack focus) {
        return Aspect.ENERGY.getColor();
    }

    @Override
    public @Nullable ResourceLocation getFocusTexture(ItemStack focus) {
        return ThaumicEnergistics.id("item/foci.aewrench");
    }

    @Override
    public AspectList getVisCost(ItemStack focus) {
        return CAST_COST;
    }

    @Override
    public int getActivationCooldown(ItemStack focus) {
        return COOLDOWN;
    }

    @Override
    public FocusUpgradeType[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
        return new FocusUpgradeType[0];
    }

    @Override
    public InteractionResult onFocusRightClick(ItemStack wand, Level level, Player player, @Nullable HitResult hit) {
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            return use(level, player, blockHit, wand, false) ? InteractionResult.sidedSuccess(level.isClientSide()) : InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean onEntitySwing(LivingEntity entity, ItemStack wand) {
        if (entity instanceof Player player) {
            BlockHitResult hit = getPlayerPOVHitResult(player.level(), player, ClipContext.Fluid.ANY);
            if (hit.getType() == HitResult.Type.BLOCK) {
                return use(player.level(), player, hit, wand, true);
            }
        }
        return false;
    }

    private static boolean canCast(ItemStack wandStack, Player player) {
        return wandStack.getItem() instanceof WandItem wand && wand.consumeAllVis(wandStack, player, CAST_COST, false, false);
    }

    private static void consumeVis(ItemStack wandStack, Player player) {
        ((WandItem) wandStack.getItem()).consumeAllVis(wandStack, player, CAST_COST, true, false);
    }

    static boolean use(Level level, Player player, BlockHitResult hit, ItemStack wandStack, boolean leftClick) {
        if (!player.isShiftKeyDown() || !canCast(wandStack, player)) {
            return false;
        }
        BlockPos pos = hit.getBlockPos();
        if (!level.mayInteract(player, pos)) {
            return false;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return false;
        }
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return true;
        }
        boolean partHost = blockEntity instanceof IPartHost;
        if (leftClick && !partHost) {
            if (rotate(level, pos)) {
                consumeVis(wandStack, player);
            }
            return true;
        }
        ItemStack previous = player.getItemInHand(InteractionHand.MAIN_HAND);
        boolean handled = false;
        try {
            ItemStack wrench = AEItems.CERTUS_QUARTZ_WRENCH.stack();
            player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
            if (leftClick) {
                player.setShiftKeyDown(false);
            }
            handled = serverPlayer.gameMode.useItemOn(serverPlayer, level, wrench, InteractionHand.MAIN_HAND, hit).consumesAction();
            if (handled || level.getBlockState(pos).isAir()) {
                consumeVis(wandStack, player);
            }
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, previous);
            player.setShiftKeyDown(true);
        }
        return handled;
    }

    private static boolean rotate(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockState rotated = state.rotate(level, pos, Rotation.CLOCKWISE_90);
        if (rotated == state) {
            return false;
        }
        level.setBlock(pos, rotated, Block.UPDATE_ALL);
        rotated.handleNeighborChanged(level, pos, state.getBlock(), pos, false);
        return true;
    }
}
