package dev.gtnhjourney.debug;

import java.util.Map;

/** Shared policy for preserving vanilla/NEI mob-spawner entity identity. */
final class MobSpawnerResearchIdentity {

    private static final String VANILLA_SPAWNER_ID = "minecraft:mob_spawner";

    private MobSpawnerResearchIdentity() {}

    static int resolveEntityMeta(String entityName, Map<String, Integer> entityIds) {
        if (entityName == null || entityName.length() == 0 || entityIds == null) return -1;
        Integer id = entityIds.get(entityName);
        return id == null || id.intValue() <= 0 ? -1 : id.intValue();
    }

    static boolean isLegacyUntypedSpawner(String itemId, int meta, String canonicalNbt) {
        return VANILLA_SPAWNER_ID.equals(itemId)
            && meta == 0
            && (canonicalNbt == null || canonicalNbt.length() == 0);
    }
}
