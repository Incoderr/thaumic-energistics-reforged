package thaumicenergistics.infusion;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import thaumcraft.platform.Menus;
import thaumicenergistics.block.AeWrenchableBlock;

public final class InfusionInscriberBlock extends AeWrenchableBlock implements EntityBlock {
    public InfusionInscriberBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InfusionInscriberBlockEntity(pos, state);
    }

    @Override
    protected boolean canPlayerInteract(Player player) {
        return !(player instanceof FakePlayer);
    }

    @Override
    protected InteractionResult activate(Level level, BlockPos pos, Player player) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof InfusionInscriberBlockEntity) {
            Menus.open(serverPlayer, new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new InfusionInscriberMenu(containerId, inventory, pos),
                Component.translatable("block.thaumicenergistics.infusion_inscriber")
            ), pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof InfusionInscriberBlockEntity inscriber) {
            Containers.dropContents(level, pos, inscriber);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
