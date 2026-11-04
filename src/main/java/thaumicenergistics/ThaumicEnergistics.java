package thaumicenergistics;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import thaumcraft.aspect.AspectSync;
import thaumicenergistics.network.ModPayloads;
import thaumicenergistics.platform.TeCapabilities;
import thaumicenergistics.platform.TeStackData;
import thaumicenergistics.registry.ModBlockEntities;
import thaumicenergistics.registry.ModBlocks;
import thaumicenergistics.registry.ModCreativeTabs;
import thaumicenergistics.registry.ModEffects;
import thaumicenergistics.registry.ModEntities;
import thaumicenergistics.registry.ModItems;
import thaumicenergistics.registry.ModKeyTypes;
import thaumicenergistics.registry.ModMenus;
import thaumicenergistics.registry.ModSounds;
import thaumicenergistics.test.SmokeGameTests;

@Mod(ThaumicEnergistics.MODID)
public class ThaumicEnergistics {
    public static final String MODID = "thaumicenergistics";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ThaumicEnergistics(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        Features.load();
        TeStackData.register(modEventBus);
        ModKeyTypes.register(modEventBus);
        ModEffects.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModSounds.register(modEventBus);
        ModMenus.register(modEventBus);
        TeCapabilities.register(modEventBus);
        ModPayloads.register();
        Features.recipes();
        Features.research();
        AspectSync.addRegistrar(Features::aspects);
        SmokeGameTests.register(modEventBus);
        Features.tests(modEventBus);
        modEventBus.addListener(FMLCommonSetupEvent.class, event -> event.enqueueWork(Features::setup));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
