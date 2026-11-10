package thaumicenergistics.alchemy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import javax.annotation.Nullable;
import net.neoforged.neoforge.common.util.FakePlayer;
import thaumcraft.platform.Menus;
import thaumicenergistics.block.AeWrenchableBlock;

public final class CrucibleAssemblerBlock extends AeWrenchableBlock implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public CrucibleAssemblerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrucibleAssemblerBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != AlchemyFeature.ASSEMBLER_ENTITY.get()) {
            return null;
        }
        return level.isClientSide()
            ? (BlockEntityTicker<T>) (BlockEntityTicker<CrucibleAssemblerBlockEntity>) CrucibleAssemblerBlockEntity::clientTick
            : (BlockEntityTicker<T>) (BlockEntityTicker<CrucibleAssemblerBlockEntity>) CrucibleAssemblerBlockEntity::serverTick;
    }

    @Override
    protected boolean canPlayerInteract(Player player) {
        return !(player instanceof FakePlayer);
    }

    @Override
    protected InteractionResult activate(Level level, BlockPos pos, Player player) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof CrucibleAssemblerBlockEntity) {
            Menus.open(serverPlayer, new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new CrucibleAssemblerMenu(containerId, inventory, pos),
                Component.translatable("block.thaumicenergistics.crucible_assembler")
            ), pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof CrucibleAssemblerBlockEntity assembler) {
            assembler.getMainNode().setOwningPlayer(player);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CrucibleAssemblerBlockEntity assembler) {
            for (ItemStack drop : assembler.drops()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), drop);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
