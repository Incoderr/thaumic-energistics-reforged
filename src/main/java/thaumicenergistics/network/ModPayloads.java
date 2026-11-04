package thaumicenergistics.network;

import thaumicenergistics.provider.ProviderFeature;
import thaumicenergistics.terminal.TerminalFeature;
import thaumicenergistics.workbench.WorkbenchFeature;

public final class ModPayloads {
    private ModPayloads() {
    }

    public static void register() {
        WorkbenchFeature.payloads();
        TerminalFeature.payloads();
        ProviderFeature.payloads();
    }
}
