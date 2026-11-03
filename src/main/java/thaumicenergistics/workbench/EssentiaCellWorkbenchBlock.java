package thaumicenergistics.workbench;

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

public final class EssentiaCellWorkbenchBlock extends AeWrenchableBlock implements EntityBlock {
    public EssentiaCellWorkbenchBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EssentiaCellWorkbenchBlockEntity(pos, state);
    }

    @Override
    protected boolean canPlayerInteract(Player player) {
        return !(player instanceof FakePlayer);
    }

    @Override
    protected InteractionResult activate(Level level, BlockPos pos, Player player) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof EssentiaCellWorkbenchBlockEntity) {
            Menus.open(serverPlayer, new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new EssentiaCellWorkbenchMenu(containerId, inventory, pos),
                Component.translatable("thaumicenergistics.gui.essentia.cell.workbench.title")
            ), pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof EssentiaCellWorkbenchBlockEntity workbench) {
            Containers.dropContents(level, pos, workbench);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
