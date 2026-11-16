package thaumicenergistics.test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumicenergistics.ThaumicEnergistics;

public final class TeGameTestSuite {
    private final String name;
    private final DeferredRegister<Consumer<GameTestHelper>> functions;
    private final List<String> tests = new ArrayList<>();
    private boolean isolated;

    public TeGameTestSuite(String name) {
        this.name = name;
        this.functions = DeferredRegister.create(Registries.TEST_FUNCTION, ThaumicEnergistics.MODID);
    }

    public TeGameTestSuite add(String test, Consumer<GameTestHelper> body) {
        String testName = name + "_" + test;
        tests.add(testName);
        functions.register(testName, () -> helper -> body.accept(new TeGameTestHelper(helper)));
        return this;
    }

    public TeGameTestSuite isolated() {
        isolated = true;
        return this;
    }

    public void register(IEventBus modEventBus) {
        functions.register(modEventBus);
        modEventBus.addListener(this::registerTests);
    }

    private void registerTests(RegisterGameTestsEvent event) {
        var shared = isolated ? null : event.registerEnvironment(ThaumicEnergistics.id(name + "_default"), new TestEnvironmentDefinition.AllOf());
        for (String testName : tests) {
            var environment = isolated ? event.registerEnvironment(ThaumicEnergistics.id(testName + "_env"), new TestEnvironmentDefinition.AllOf()) : shared;
            ResourceKey<Consumer<GameTestHelper>> function = ResourceKey.create(Registries.TEST_FUNCTION, ThaumicEnergistics.id(testName));
            event.registerTest(
                ThaumicEnergistics.id(testName),
                new FunctionGameTestInstance(function, new TestData<>(environment, ThaumicEnergistics.id("test/platform"), 100, 0, true))
            );
        }
    }
}
