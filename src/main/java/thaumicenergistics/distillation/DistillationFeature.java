package thaumicenergistics.distillation;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcStack;
import thaumcraft.platform.Menus;
import thaumcraft.research.ResearchPage;
import thaumicenergistics.block.AeWrenchableBlock;
import thaumicenergistics.crafting.TeRecipes;
import thaumicenergistics.registry.ModBlockEntities;
import thaumicenergistics.registry.ModBlocks;
import thaumicenergistics.registry.ModMenus;
import thaumicenergistics.research.TeResearch;

public final class DistillationFeature {
    public static final String RECIPE = TeRecipes.key("BLOCK_DISTILLATION_PATTERN_ENCODER");

    public static final DeferredBlock<EncoderBlock> ENCODER = ModBlocks.withItem(ModBlocks.BLOCKS.registerBlock(
        "distillation_pattern_encoder", EncoderBlock::new, properties -> properties.strength(1.0F).sound(SoundType.METAL)
    ));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DistillationEncoderBlockEntity>> ENTITY = ModBlockEntities.BLOCK_ENTITIES.register(
        "distillation_pattern_encoder", () -> BlockEntityType.Builder.of(DistillationEncoderBlockEntity::new, ENCODER.get()).build(null)
    );
    public static final DeferredHolder<MenuType<?>, MenuType<DistillationEncoderMenu>> MENU = ModMenus.MENUS.register(
        "distillation_pattern_encoder", () -> Menus.withPos(DistillationEncoderMenu::new)
    );

    private DistillationFeature() {
    }

    public static void load() {
    }

    public static void recipes() {
        TeRecipes.arcane(
            RECIPE, TeResearch.DISTILLATION_ENCODER, TcStack.of(ENCODER.getId().toString()),
            new AspectList().add(Aspect.ORDER, 5).add(Aspect.ENTROPY, 5).add(Aspect.FIRE, 3),
            new String[] {"IPI", " T ", "IEI"},
            'I', TcIngredient.tag("c:ingots/iron"),
            'P', TcIngredient.anyOf(TcIngredient.item("ae2:monitor"), TcIngredient.item("ae2:semi_dark_monitor"), TcIngredient.item("ae2:dark_monitor")),
            'T', TcIngredient.item("thaumcraft:thaumometer"), 'E', TcIngredient.item("ae2:engineering_processor")
        );
    }

    public static void research() {
        TeResearch.item(
                TeResearch.DISTILLATION_ENCODER,
                new AspectList().add(Aspect.CRAFT, 5).add(Aspect.EXCHANGE, 5).add(Aspect.ENTROPY, 5).add(Aspect.MECHANISM, 3).add(Aspect.GREED, 3).add(Aspect.MIND, 1),
                -4, -4, TeResearch.COMPLEXITY_LARGE, TcStack.of(ENCODER.getId().toString())
            )
            .setPages(TeResearch.text(TeResearch.DISTILLATION_ENCODER, 1), ResearchPage.arcane(RECIPE))
            .setParents(TeResearch.IO)
            .setParentsHidden(TeResearch.ESSENTIA_TERMINAL)
            .setConcealed()
            .registerResearchItem();
    }

    public static final class EncoderBlock extends AeWrenchableBlock implements EntityBlock {
        public EncoderBlock(Properties properties) {
            super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new DistillationEncoderBlockEntity(pos, state);
        }

        @Override
        protected boolean canPlayerInteract(Player player) {
            return !(player instanceof FakePlayer);
        }

        @Override
        protected InteractionResult activate(Level level, BlockPos pos, Player player) {
            if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof DistillationEncoderBlockEntity) {
                Menus.open(serverPlayer, new SimpleMenuProvider(
                    (containerId, inventory, ignored) -> new DistillationEncoderMenu(containerId, inventory, pos),
                    Component.translatable("block.thaumicenergistics.distillation_pattern_encoder")
                ), pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
            if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof DistillationEncoderBlockEntity encoder) {
                for (int slot : new int[] {DistillationEncoderBlockEntity.SLOT_BLANK_PATTERNS, DistillationEncoderBlockEntity.SLOT_ENCODED_PATTERN}) {
                    ItemStack stack = encoder.getItem(slot);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }
}
