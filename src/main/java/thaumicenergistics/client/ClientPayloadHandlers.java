package thaumicenergistics.client;

import thaumicenergistics.client.platform.TeClientNetwork;
import thaumicenergistics.provider.ProviderRunesPayload;
import thaumicenergistics.terminal.EssentiaListPayload;
import thaumicenergistics.workbench.WorkbenchPartitionPayload;

public final class ClientPayloadHandlers {
    private ClientPayloadHandlers() {
    }

    public static void register() {
        TeClientNetwork.handle(WorkbenchPartitionPayload.TYPE, WorkbenchPartitionPayload::handle);
        TeClientNetwork.handle(EssentiaListPayload.TYPE, EssentiaListPayload::handle);
        TeClientNetwork.handle(ProviderRunesPayload.TYPE, ProviderRunesPayload::handle);
    }
}
