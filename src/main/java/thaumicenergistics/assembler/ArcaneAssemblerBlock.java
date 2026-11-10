package thaumicenergistics.assembler;

import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardMessages;
import appeng.api.ids.AEComponents;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import javax.annotation.Nullable;
import net.neoforged.neoforge.common.util.FakePlayer;
import thaumcraft.aspect.AspectList;
import thaumcraft.platform.Menus;
import thaumicenergistics.block.AeWrenchableBlock;
import thaumicenergistics.visrelay.VisRelayFeature;

public final class ArcaneAssemblerBlock extends AeWrenchableBlock implements EntityBlock {
    public ArcaneAssemblerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArcaneAssemblerBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != AssemblerFeature.ENTITY.get()) {
            return null;
        }
        return level.isClientSide()
            ? (BlockEntityTicker<T>) (BlockEntityTicker<ArcaneAssemblerBlockEntity>) ArcaneAssemblerBlockEntity::clientTick
            : (BlockEntityTicker<T>) (BlockEntityTicker<ArcaneAssemblerBlockEntity>) ArcaneAssemblerBlockEntity::serverTick;
    }

    @Override
    protected boolean canPlayerInteract(Player player) {
        return !(player instanceof FakePlayer);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof IMemoryCard card && canPlayerInteract(player)) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ArcaneAssemblerBlockEntity assembler) {
                CompoundTag data = VisRelayFeature.LINK.get(stack);
                if (data != null) {
                    assembler.visSourceInfo().readFromTag(data);
                    if (assembler.visSourceInfo().hasSourceData()) {
                        card.notifyUser(player, MemoryCardMessages.SETTINGS_LOADED);
                    }
                    assembler.linkChanged();
                } else if (stack.get(AEComponents.EXPORTED_SETTINGS_SOURCE) == null) {
                    assembler.visSourceInfo().clearData();
                    card.notifyUser(player, MemoryCardMessages.SETTINGS_CLEARED);
                    assembler.linkChanged();
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult activate(Level level, BlockPos pos, Player player) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof ArcaneAssemblerBlockEntity) {
            Menus.open(serverPlayer, new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new ArcaneAssemblerMenu(containerId, inventory, pos),
                Component.translatable("thaumicenergistics.gui.arcane.assembler.title")
            ), pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected ItemStack dismantledStack(Level level, BlockPos pos) {
        ItemStack stack = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof ArcaneAssemblerBlockEntity assembler && assembler.storedVis().size() > 0) {
            AssemblerFeature.STORED_VIS.set(stack, assembler.storedVis().copy());
        }
        return stack;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        AspectList vis = AssemblerFeature.STORED_VIS.get(stack);
        if (vis != null && level.getBlockEntity(pos) instanceof ArcaneAssemblerBlockEntity assembler) {
            assembler.setStoredVis(vis);
        }
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof ArcaneAssemblerBlockEntity assembler) {
            assembler.getMainNode().setOwningPlayer(player);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (AssemblerFeature.STORED_VIS.has(stack)) {
            tooltip.add(Component.translatable("thaumicenergistics.tooltip.arcane.assembler.hasVis"));
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ArcaneAssemblerBlockEntity assembler) {
            for (ItemStack drop : assembler.drops()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), drop);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
