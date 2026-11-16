package thaumicenergistics.client;

import appeng.api.util.AEColor;
import appeng.client.InitScreens;
import appeng.client.api.model.parts.RegisterPartModelsEvent;
import appeng.client.api.renderer.parts.RegisterPartRendererEvent;
import java.util.List;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.act.ActFeature;
import thaumicenergistics.alchemy.AlchemyFeature;
import thaumicenergistics.assembler.AssemblerFeature;
import thaumicenergistics.buses.BusesFeature;
import thaumicenergistics.client.act.ArcaneCraftingTermScreen;
import thaumicenergistics.client.alchemy.AlchemyInscriberScreen;
import thaumicenergistics.client.alchemy.CrucibleAssemblerRenderer;
import thaumicenergistics.client.alchemy.CrucibleAssemblerScreen;
import thaumicenergistics.client.assembler.ArcaneAssemblerModel;
import thaumicenergistics.client.assembler.ArcaneAssemblerRenderer;
import thaumicenergistics.client.assembler.ArcaneAssemblerScreen;
import thaumicenergistics.client.buses.EssentiaBusScreen;
import thaumicenergistics.client.distillation.DistillationEncoderScreen;
import thaumicenergistics.client.emitter.EssentiaLevelEmitterScreen;
import thaumicenergistics.client.essentia.ClientEssentia;
import thaumicenergistics.client.gearbox.GearBoxRenderer;
import thaumicenergistics.client.golem.GolemBackpackRenderer;
import thaumicenergistics.client.infusion.InfusionAssemblerRenderer;
import thaumicenergistics.client.infusion.InfusionAssemblerScreen;
import thaumicenergistics.client.infusion.InfusionInscriberScreen;
import thaumicenergistics.client.knowledge.KnowledgeInscriberScreen;
import thaumicenergistics.client.monitor.EssentiaMonitorRenderer;
import thaumicenergistics.client.monitor.MonitorEmptyPartModel;
import thaumicenergistics.client.platform.TeItemRenderer;
import thaumicenergistics.client.terminal.EssentiaTerminalScreen;
import thaumicenergistics.client.vibration.VibrationChamberScreen;
import thaumicenergistics.client.visrelay.VisRelayInterfaceRenderer;
import thaumicenergistics.client.workbench.EssentiaCellWorkbenchScreen;
import thaumicenergistics.distillation.DistillationFeature;
import thaumicenergistics.emitter.EmitterFeature;
import thaumicenergistics.gearbox.GearBoxFeature;
import thaumicenergistics.infusion.InfusionFeature;
import thaumicenergistics.knowledge.KnowledgeFeature;
import thaumicenergistics.monitor.EssentiaStorageMonitorPart;
import thaumicenergistics.provider.ProviderBlockEntity;
import thaumicenergistics.provider.ProviderFeature;
import thaumicenergistics.terminal.TerminalFeature;
import thaumicenergistics.vibration.VibrationFeature;
import thaumicenergistics.visrelay.VisRelayInterfacePart;
import thaumicenergistics.workbench.WorkbenchFeature;

public final class ClientFeatures {
    private ClientFeatures() {
    }

    public static void setup(FMLClientSetupEvent event) {
        GolemBackpackRenderer.register();
        ClientEssentia.setup(event);
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(WorkbenchFeature.MENU.get(), EssentiaCellWorkbenchScreen::new);
        event.register(TerminalFeature.MENU.get(), EssentiaTerminalScreen::new);
        event.register(BusesFeature.MENU.get(), EssentiaBusScreen::new);
        event.register(EmitterFeature.MENU.get(), EssentiaLevelEmitterScreen::new);
        InitScreens.register(event, ActFeature.MENU.get(), ArcaneCraftingTermScreen::new, ArcaneCraftingTermScreen.STYLE);
        event.register(KnowledgeFeature.MENU.get(), KnowledgeInscriberScreen::new);
        event.register(AssemblerFeature.MENU.get(), ArcaneAssemblerScreen::new);
        event.register(DistillationFeature.MENU.get(), DistillationEncoderScreen::new);
        event.register(VibrationFeature.MENU.get(), VibrationChamberScreen::new);
        event.register(AlchemyFeature.INSCRIBER_MENU.get(), AlchemyInscriberScreen::new);
        event.register(AlchemyFeature.ASSEMBLER_MENU.get(), CrucibleAssemblerScreen::new);
        event.register(InfusionFeature.INSCRIBER_MENU.get(), InfusionInscriberScreen::new);
        event.register(InfusionFeature.ASSEMBLER_MENU.get(), InfusionAssemblerScreen::new);
    }

    public static void registerPartModels(RegisterPartModelsEvent event) {
        event.registerModelType(MonitorEmptyPartModel.ID, MonitorEmptyPartModel.Unbaked.MAP_CODEC);
    }

    public static void registerPartRenderers(RegisterPartRendererEvent event) {
        event.register(EssentiaStorageMonitorPart.class, new EssentiaMonitorRenderer());
        event.register(VisRelayInterfacePart.class, new VisRelayInterfaceRenderer());
    }

    public static void registerBlockTints(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(List.of(new ProviderTint()), ProviderFeature.ESSENTIA_PROVIDER.get(), ProviderFeature.INFUSION_PROVIDER.get());
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(AssemblerFeature.ENTITY.get(), ArcaneAssemblerRenderer::new);
        event.registerBlockEntityRenderer(GearBoxFeature.ENTITY.get(), GearBoxRenderer::new);
        event.registerBlockEntityRenderer(AlchemyFeature.ASSEMBLER_ENTITY.get(), CrucibleAssemblerRenderer::new);
        event.registerBlockEntityRenderer(InfusionFeature.ASSEMBLER_ENTITY.get(), InfusionAssemblerRenderer::new);
    }

    public static void registerSpecialRenderers(RegisterSpecialModelRendererEvent event) {
        event.register(ThaumicEnergistics.id("arcane_assembler"), TeItemRenderer.codec(ArcaneAssemblerModel::render));
        event.register(ThaumicEnergistics.id("crucible_assembler"), TeItemRenderer.codec((collector, pose, light) -> CrucibleAssemblerRenderer.renderAll(collector, pose, light, Direction.NORTH)));
        event.register(ThaumicEnergistics.id("infusion_assembler"), TeItemRenderer.codec((collector, pose, light) -> InfusionAssemblerRenderer.renderAll(collector, pose, light, false, 0.0F)));
        event.register(ThaumicEnergistics.id("iron_gear_box"), TeItemRenderer.codec((collector, pose, light) -> GearBoxRenderer.render(collector, false, new boolean[6], 0.0F, pose, light)));
        event.register(ThaumicEnergistics.id("thaumium_gear_box"), TeItemRenderer.codec((collector, pose, light) -> GearBoxRenderer.render(collector, true, new boolean[6], 0.0F, pose, light)));
    }

    public static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        GolemBackpackRenderer.registerModifiers(event);
    }

    private static final class ProviderTint implements BlockTintSource {
        @Override
        public int color(BlockState state) {
            return 0xFF000000 | AEColor.TRANSPARENT.mediumVariant;
        }

        @Override
        public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
            if (level.getBlockEntity(pos) instanceof ProviderBlockEntity provider) {
                return 0xFF000000 | (provider.isActive() ? provider.getColor().mediumVariant : 0);
            }
            return color(state);
        }
    }
}
