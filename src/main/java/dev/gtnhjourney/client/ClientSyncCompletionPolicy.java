package dev.gtnhjourney.client;

/** Separates reliable transport completeness from client-side semantic representability. */
public final class ClientSyncCompletionPolicy {

    private ClientSyncCompletionPolicy() {}

    public static boolean mayPublish(int advertisedSyncable, int receivedEntries, int uniqueSemanticStacks) {
        if (advertisedSyncable < 0 || receivedEntries < 0 || uniqueSemanticStacks < 0) return false;
        if (receivedEntries != advertisedSyncable) return false;
        return uniqueSemanticStacks <= receivedEntries;
    }
}
