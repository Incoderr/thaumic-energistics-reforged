package thaumicenergistics.vibration;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import javax.annotation.Nullable;
import net.neoforged.neoforge.common.util.FakePlayer;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectKeys;
import thaumcraft.aspect.AspectList;
import thaumcraft.platform.Menus;
import thaumicenergistics.block.AeWrenchableBlock;

public final class VibrationChamberBlock extends AeWrenchableBlock implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final EnumProperty<Processing> PROCESSING = EnumProperty.create("processing", Processing.class);

    public enum Processing implements StringRepresentable {
        OFF, IGNIS, POTENTIA;

        public static Processing of(@Nullable Aspect aspect) {
            return aspect == Aspect.FIRE ? IGNIS : aspect == Aspect.ENERGY ? POTENTIA : OFF;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public VibrationChamberBlock(Properties properties) {
        super(properties.lightLevel(state -> state.getValue(PROCESSING) != Processing.OFF ? 12 : 0));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PROCESSING, Processing.OFF));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PROCESSING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Player player = context.getPlayer();
        Direction face = Direction.NORTH;
        if (player != null) {
            if (player.getXRot() > 50) {
                face = Direction.UP;
            } else if (player.getXRot() < -50) {
                face = Direction.DOWN;
            } else {
                face = switch (net.minecraft.util.Mth.floor(player.getYRot() * 4.0F / 360.0F + 0.5D) & 3) {
                    case 1 -> Direction.EAST;
                    case 2 -> Direction.SOUTH;
                    case 3 -> Direction.WEST;
                    default -> Direction.NORTH;
                };
            }
        }
        return defaultBlockState().setValue(FACING, face);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VibrationChamberBlockEntity(pos, state);
    }

    @Override
    protected boolean canPlayerInteract(Player player) {
        return !(player instanceof FakePlayer);
    }

    @Override
    protected InteractionResult activate(Level level, BlockPos pos, Player player) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof VibrationChamberBlockEntity) {
            Menus.open(serverPlayer, new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new VibrationChamberMenu(containerId, inventory, pos),
                Component.translatable("block.thaumicenergistics.essentia_vibration_chamber")
            ), pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected ItemStack dismantledStack(Level level, BlockPos pos) {
        ItemStack stack = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof VibrationChamberBlockEntity chamber && chamber.hasStoredEssentia()) {
            AspectKeys.ASPECTS.set(stack, new AspectList().add(chamber.storedAspect(), chamber.storedAmount()));
        }
        return stack;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof VibrationChamberBlockEntity chamber)) {
            return;
        }
        AspectList stored = AspectKeys.ASPECTS.get(stack);
        if (stored != null && !stored.isEmpty()) {
            Aspect aspect = stored.getAspects().get(0);
            chamber.setStored(aspect, stored.getAmount(aspect));
        }
        if (placer instanceof Player player) {
            chamber.getMainNode().setOwningPlayer(player);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        AspectList stored = AspectKeys.ASPECTS.get(stack);
        if (stored != null && !stored.isEmpty()) {
            Aspect aspect = stored.getAspects().get(0);
            tooltip.add(Component.literal(aspect.getName() + " x " + stored.getAmount(aspect)));
        }
    }
}
