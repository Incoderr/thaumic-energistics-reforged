package thaumicenergistics.gearbox;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.CrucibleRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchPage;
import thaumicenergistics.block.AeWrenchableBlock;
import thaumicenergistics.core.CoreFeature;
import thaumicenergistics.crafting.TeRecipes;
import thaumicenergistics.registry.ModBlockEntities;
import thaumicenergistics.registry.ModBlocks;
import thaumicenergistics.research.TeResearch;

public final class GearBoxFeature {
    public static final String IRON_GEAR_RECIPE = TeRecipes.key("MATERIAL_IRON_GEAR");
    public static final String IRON_GEAR_BOX_RECIPE = TeRecipes.key("BLOCK_IRONGEARBOX");
    public static final String THAUMIUM_GEAR_BOX_RECIPE = TeRecipes.key("BLOCK_THAUMIUMGEARBOX");
    private static final String GOLEM_NAME = "FakeThaumcraftGolem";

    public static final DeferredBlock<GearBoxBlock> IRON_GEAR_BOX = ModBlocks.withItem(ModBlocks.BLOCKS.registerBlock(
        "iron_gear_box", properties -> new GearBoxBlock(properties, false), GearBoxFeature::properties
    ));
    public static final DeferredBlock<GearBoxBlock> THAUMIUM_GEAR_BOX = ModBlocks.withItem(ModBlocks.BLOCKS.registerBlock(
        "thaumium_gear_box", properties -> new GearBoxBlock(properties, true), GearBoxFeature::properties
    ));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GearBoxBlockEntity>> ENTITY = ModBlockEntities.BLOCK_ENTITIES.register(
        "gear_box", () -> BlockEntityType.Builder.of(GearBoxBlockEntity::new, IRON_GEAR_BOX.get(), THAUMIUM_GEAR_BOX.get()).build(null)
    );

    private GearBoxFeature() {
    }

    private static Block.Properties properties(Block.Properties properties) {
        return properties.strength(0.6F).sound(SoundType.STONE).noOcclusion();
    }

    public static void load() {
    }

    public static void recipes() {
        TeRecipes.arcane(
            IRON_GEAR_RECIPE, TeResearch.IRON_GEARBOX, TcStack.of(CoreFeature.IRON_GEAR.getId().toString()),
            new AspectList().add(Aspect.EARTH, 1).add(Aspect.FIRE, 1),
            new String[] {" I ", " W ", "I I"},
            'I', TcIngredient.tag("c:ingots/iron"),
            'W', TcIngredient.anyOf(TcIngredient.tag("c:gears/wood"), TcIngredient.item("minecraft:stick"))
        );
        TeRecipes.arcane(
            IRON_GEAR_BOX_RECIPE, TeResearch.IRON_GEARBOX, TcStack.of(IRON_GEAR_BOX.getId().toString()),
            new AspectList().add(Aspect.AIR, 2).add(Aspect.ORDER, 2),
            new String[] {"SGS", "GGG", "SGS"},
            'S', TcIngredient.item("minecraft:cobblestone"),
            'G', TcIngredient.anyOf(TcIngredient.item(CoreFeature.IRON_GEAR.getId().toString()), TcIngredient.tag("c:gears/iron"))
        );
        TeRecipes.register(new CrucibleRecipe(
            THAUMIUM_GEAR_BOX_RECIPE, TeResearch.THAUMIUM_GEARBOX, TcStack.of(THAUMIUM_GEAR_BOX.getId().toString()),
            TcIngredient.item(IRON_GEAR_BOX.getId().toString()), new AspectList().add(Aspect.METAL, 16).add(Aspect.MAGIC, 16)
        ));
    }

    public static void research() {
        TeResearch.item(
                TeResearch.IRON_GEARBOX,
                new AspectList().add(Aspect.MECHANISM, 6).add(Aspect.METAL, 4).add(Aspect.EXCHANGE, 4),
                4, -5, TeResearch.COMPLEXITY_SMALL, TcStack.of(CoreFeature.IRON_GEAR.getId().toString())
            )
            .setPages(
                TeResearch.text(TeResearch.IRON_GEARBOX, 1),
                TeResearch.text(TeResearch.IRON_GEARBOX, 2),
                ResearchPage.arcane(IRON_GEAR_RECIPE),
                ResearchPage.arcane(IRON_GEAR_BOX_RECIPE)
            )
            .setSecondary()
            .registerResearchItem();
        TeResearch.item(
                TeResearch.THAUMIUM_GEARBOX,
                new AspectList().add(Aspect.MECHANISM, 10).add(Aspect.MAGIC, 8).add(Aspect.METAL, 5),
                4, -6, TeResearch.COMPLEXITY_SMALL, TcStack.of(THAUMIUM_GEAR_BOX.getId().toString())
            )
            .setPages(TeResearch.text(TeResearch.THAUMIUM_GEARBOX, 1), ResearchPage.crucible(THAUMIUM_GEAR_BOX_RECIPE))
            .setParents(TeResearch.IRON_GEARBOX, TeResearch.pseudo(TeResearch.COREUSE))
            .setParentsHidden(TeResearch.COREUSE)
            .setSecondary()
            .registerResearchItem();
    }

    public static final class GearBoxBlock extends AeWrenchableBlock implements EntityBlock {
        private static final VoxelShape SHAPE = Block.box(3.68D, 3.68D, 3.68D, 12.32D, 12.32D, 12.32D);
        private final boolean allowGolemInteraction;

        public GearBoxBlock(Properties properties, boolean allowGolemInteraction) {
            super(properties);
            this.allowGolemInteraction = allowGolemInteraction;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new GearBoxBlockEntity(pos, state);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            if (type != ENTITY.get()) {
                return null;
            }
            return level.isClientSide()
                ? (BlockEntityTicker<T>) (BlockEntityTicker<GearBoxBlockEntity>) GearBoxBlockEntity::clientTick
                : (BlockEntityTicker<T>) (BlockEntityTicker<GearBoxBlockEntity>) GearBoxBlockEntity::serverTick;
        }

        @Override
        protected boolean canPlayerInteract(Player player) {
            if (player instanceof FakePlayer) {
                return allowGolemInteraction && GOLEM_NAME.equalsIgnoreCase(player.getGameProfile().getName());
            }
            return true;
        }

        @Override
        protected InteractionResult activate(Level level, BlockPos pos, Player player) {
            if (level.getBlockEntity(pos) instanceof GearBoxBlockEntity gearbox && gearbox.crankGearbox()) {
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
            return InteractionResult.PASS;
        }

        @Override
        protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean movedByPiston) {
            super.neighborChanged(state, level, pos, neighbor, neighborPos, movedByPiston);
            if (level.getBlockEntity(pos) instanceof GearBoxBlockEntity gearbox) {
                gearbox.updateCrankables();
                gearbox.sync();
            }
        }
    }

}
