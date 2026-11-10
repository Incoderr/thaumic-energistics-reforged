package thaumicenergistics;

import net.neoforged.bus.api.IEventBus;
import thaumcraft.aspect.AspectRegistry;
import thaumicenergistics.act.ActFeature;
import thaumicenergistics.alchemy.AlchemyFeature;
import thaumicenergistics.assembler.AssemblerFeature;
import thaumicenergistics.test.AlchemyGameTests;
import thaumicenergistics.buses.BusesFeature;
import thaumicenergistics.cells.CellsFeature;
import thaumicenergistics.core.CoreFeature;
import thaumicenergistics.distillation.DistillationFeature;
import thaumicenergistics.vibration.VibrationFeature;
import thaumicenergistics.gearbox.GearBoxFeature;
import thaumicenergistics.golem.GolemBackpackFeature;
import thaumicenergistics.wrench.WrenchFocusFeature;
import thaumicenergistics.aspects.AeAspectsFeature;
import thaumicenergistics.dupe.QuartzDupeFeature;
import thaumicenergistics.emitter.EmitterFeature;
import thaumicenergistics.essentia.EssentiaFeature;
import thaumicenergistics.monitor.MonitorFeature;
import thaumicenergistics.provider.ProviderFeature;
import thaumicenergistics.terminal.TerminalFeature;
import thaumicenergistics.test.ActGameTests;
import thaumicenergistics.test.AssemblerGameTests;
import thaumicenergistics.test.BusesGameTests;
import thaumicenergistics.test.CellsGameTests;
import thaumicenergistics.test.CoreGameTests;
import thaumicenergistics.test.DistillationGameTests;
import thaumicenergistics.test.VibrationGameTests;
import thaumicenergistics.test.GearBoxGameTests;
import thaumicenergistics.test.GolemGameTests;
import thaumicenergistics.test.SmallFeaturesGameTests;
import thaumicenergistics.test.EmitterGameTests;
import thaumicenergistics.test.KnowledgeGameTests;
import thaumicenergistics.test.MonitorGameTests;
import thaumicenergistics.test.ProviderGameTests;
import thaumicenergistics.test.TerminalGameTests;
import thaumicenergistics.test.VisRelayGameTests;
import thaumicenergistics.test.WorkbenchGameTests;
import thaumicenergistics.knowledge.KnowledgeFeature;
import thaumicenergistics.visrelay.VisRelayFeature;
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
        MonitorFeature.load();
        ProviderFeature.load();
        ActFeature.load();
        VisRelayFeature.load();
        KnowledgeFeature.load();
        AssemblerFeature.load();
        DistillationFeature.load();
        VibrationFeature.load();
        GearBoxFeature.load();
        QuartzDupeFeature.load();
        AeAspectsFeature.load();
        WrenchFocusFeature.load();
        GolemBackpackFeature.load();
        AlchemyFeature.load();
    }

    public static void setup() {
        TerminalFeature.setup();
        BusesFeature.setup();
        ProviderFeature.setup();
        AssemblerFeature.setup();
        VibrationFeature.setup();
        GolemBackpackFeature.setup();
        AlchemyFeature.setup();
    }

    public static void research() {
        CoreFeature.research();
        CellsFeature.research();
        TerminalFeature.research();
        BusesFeature.research();
        ProviderFeature.research();
        ActFeature.research();
        VisRelayFeature.research();
        KnowledgeFeature.research();
        DistillationFeature.research();
        VibrationFeature.research();
        GearBoxFeature.research();
        QuartzDupeFeature.research();
        WrenchFocusFeature.research();
        GolemBackpackFeature.research();
        AlchemyFeature.research();
    }

    public static void recipes() {
        CoreFeature.recipes();
        CellsFeature.recipes();
        WorkbenchFeature.recipes();
        TerminalFeature.recipes();
        BusesFeature.recipes();
        EmitterFeature.recipes();
        MonitorFeature.recipes();
        ProviderFeature.recipes();
        ActFeature.recipes();
        VisRelayFeature.recipes();
        KnowledgeFeature.recipes();
        AssemblerFeature.recipes();
        DistillationFeature.recipes();
        VibrationFeature.recipes();
        GearBoxFeature.recipes();
        QuartzDupeFeature.recipes();
        WrenchFocusFeature.recipes();
        GolemBackpackFeature.recipes();
        AlchemyFeature.recipes();
    }

    public static void aspects(AspectRegistry registry) {
        AeAspectsFeature.aspects(registry);
    }

    public static void tests(IEventBus modEventBus) {
        CoreGameTests.register(modEventBus);
        CellsGameTests.register(modEventBus);
        WorkbenchGameTests.register(modEventBus);
        TerminalGameTests.register(modEventBus);
        BusesGameTests.register(modEventBus);
        EmitterGameTests.register(modEventBus);
        MonitorGameTests.register(modEventBus);
        ProviderGameTests.register(modEventBus);
        ActGameTests.register(modEventBus);
        KnowledgeGameTests.register(modEventBus);
        VisRelayGameTests.register(modEventBus);
        AssemblerGameTests.register(modEventBus);
        DistillationGameTests.register(modEventBus);
        VibrationGameTests.register(modEventBus);
        GearBoxGameTests.register(modEventBus);
        SmallFeaturesGameTests.register(modEventBus);
        GolemGameTests.register(modEventBus);
        AlchemyGameTests.register(modEventBus);
    }
}
