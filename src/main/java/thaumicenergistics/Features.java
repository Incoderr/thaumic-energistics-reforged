package thaumicenergistics;

import net.neoforged.bus.api.IEventBus;
import thaumcraft.aspect.AspectRegistry;
import thaumicenergistics.buses.BusesFeature;
import thaumicenergistics.cells.CellsFeature;
import thaumicenergistics.core.CoreFeature;
import thaumicenergistics.emitter.EmitterFeature;
import thaumicenergistics.essentia.EssentiaFeature;
import thaumicenergistics.terminal.TerminalFeature;
import thaumicenergistics.test.BusesGameTests;
import thaumicenergistics.test.CellsGameTests;
import thaumicenergistics.test.CoreGameTests;
import thaumicenergistics.test.EmitterGameTests;
import thaumicenergistics.test.TerminalGameTests;
import thaumicenergistics.test.WorkbenchGameTests;
import thaumicenergistics.workbench.WorkbenchFeature;

public final class Features {
    private Features() {
    }

    public static void load() {
        EssentiaFeature.load();
        CoreFeature.load();
        CellsFeature.load();
        WorkbenchFeature.load();
        TerminalFeature.load();
        BusesFeature.load();
        EmitterFeature.load();
    }

    public static void setup() {
        TerminalFeature.setup();
        BusesFeature.setup();
    }

    public static void research() {
        CoreFeature.research();
        CellsFeature.research();
        TerminalFeature.research();
        BusesFeature.research();
    }

    public static void recipes() {
        CoreFeature.recipes();
        CellsFeature.recipes();
        WorkbenchFeature.recipes();
        TerminalFeature.recipes();
        BusesFeature.recipes();
        EmitterFeature.recipes();
    }

    public static void aspects(AspectRegistry registry) {
    }

    public static void tests(IEventBus modEventBus) {
        CoreGameTests.register(modEventBus);
        CellsGameTests.register(modEventBus);
        WorkbenchGameTests.register(modEventBus);
        TerminalGameTests.register(modEventBus);
        BusesGameTests.register(modEventBus);
        EmitterGameTests.register(modEventBus);
    }
}
