package thaumicenergistics.network;

import thaumicenergistics.workbench.WorkbenchFeature;

public final class ModPayloads {
    private ModPayloads() {
    }

    public static void register() {
        WorkbenchFeature.payloads();
    }
}
