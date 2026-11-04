package thaumicenergistics.monitor;

import appeng.api.parts.IPartHost;
import appeng.api.parts.PartModels;
import appeng.api.parts.SelectedPart;
import appeng.items.parts.PartItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import thaumcraft.crafting.TcStack;
import thaumicenergistics.crafting.TeRecipes;
import thaumicenergistics.essentia.EssentiaContainers;
import thaumicenergistics.registry.ModItems;
import thaumicenergistics.research.TeResearch;
import thaumicenergistics.terminal.TerminalFeature;

public final class MonitorFeature {
    public static final DeferredItem<PartItem<EssentiaStorageMonitorPart>> STORAGE_MONITOR = ModItems.ITEMS.registerItem(
        "essentia_storage_monitor", properties -> new PartItem<>(properties, EssentiaStorageMonitorPart.class, EssentiaStorageMonitorPart::new), properties -> properties
    );
    public static final DeferredItem<PartItem<EssentiaConversionMonitorPart>> CONVERSION_MONITOR = ModItems.ITEMS.registerItem(
        "essentia_conversion_monitor", properties -> new PartItem<>(properties, EssentiaConversionMonitorPart.class, EssentiaConversionMonitorPart::new), properties -> properties
    );

    private MonitorFeature() {
    }

    public static void load() {
        PartModels.registerModels(
            EssentiaStorageMonitorPart.MODEL_BASE, EssentiaStorageMonitorPart.MODEL_EMPTY_OFF, EssentiaStorageMonitorPart.MODEL_EMPTY_ON,
            EssentiaStorageMonitorPart.MODEL_OFF, EssentiaStorageMonitorPart.MODEL_ON,
            EssentiaConversionMonitorPart.MODEL_OFF, EssentiaConversionMonitorPart.MODEL_ON
        );
        NeoForge.EVENT_BUS.addListener(MonitorFeature::onRightClickBlock);
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getEntity().isSecondaryUseActive() || !EssentiaContainers.isContainer(event.getItemStack())) {
            return;
        }
        BlockEntity blockEntity = event.getLevel().getBlockEntity(event.getPos());
        if (blockEntity instanceof IPartHost host) {
            SelectedPart selected = host.selectPartWorld(event.getHitVec().getLocation());
            if (selected != null && selected.part instanceof EssentiaConversionMonitorPart) {
                event.setUseBlock(TriState.TRUE);
            }
        }
    }

    public static void recipes() {
        TeRecipes.vanilla(TerminalFeature.STORAGE_MONITOR_RECIPE, TeResearch.ESSENTIA_TERMINAL, "essentia_storage_monitor", TcStack.of(STORAGE_MONITOR.getId().toString()));
        TeRecipes.vanilla(TerminalFeature.CONVERSION_MONITOR_RECIPE, TeResearch.ESSENTIA_TERMINAL, "essentia_conversion_monitor", TcStack.of(CONVERSION_MONITOR.getId().toString()));
    }
}
