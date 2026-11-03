package thaumicenergistics.client;

import thaumicenergistics.client.platform.TeClientNetwork;
import thaumicenergistics.workbench.WorkbenchPartitionPayload;

public final class ClientPayloadHandlers {
    private ClientPayloadHandlers() {
    }

    public static void register() {
        TeClientNetwork.handle(WorkbenchPartitionPayload.TYPE, WorkbenchPartitionPayload::handle);
    }
}
