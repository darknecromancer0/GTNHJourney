# GTNH Journey

Current release: `1.1.5`.

GT New Horizons 1.7.10 addon that automatically researches item states the player genuinely obtains and allows server-authoritative infinite retrieval through the existing NEI frontend.

## Compatibility target

- Minecraft 1.7.10 / Forge 10.13.4.1614 / MCP stable_12
- GTNH `2.9.0-beta-2` primary baseline
- NotEnoughItems `2.8.111-GTNH` integration baseline
- Development JDK 25 with Java 8-compatible output bytecode
- Newer GTNH 2.9 builds are best-effort until separately verified
- 1.1.5 adds a client-only WR-CBE render-boundary recovery for the `Already tesselating!` crash reported when a shared 1.7.10 Tessellator batch reaches WR-CBE still open. Clean render boundaries are left untouched.

## Core behavior

Research belongs to the player UUID and is persisted once per save. Journey observes real server-side inventory state, crafting, smelting and supported recovery scans. Recipe previews, arbitrary open-container GUI slots and other client-only previews are not research sources.

Stack size is not part of identity. Registry id, meaningful metadata and canonical semantic NBT are. Unknown mod NBT remains exact by default. Verified runtime-only fields are normalized only for narrowly supported item families.

Representative compatibility rules include:

- classic durability wear is not a separate research identity for ordinary damageable non-subtyped items;
- verified GregTech electric items use BASE/FULL charge endpoints;
- IC2, CoFH and supported OpenComputers electric items use equivalent endpoint semantics through their APIs;
- verified GregTech tool wear/mode counters, supported Tinkers wear, Botania magnet cooldown, selected Draconic runtime shield/profile noise and known wearable runtime fields are normalized without generically stripping unrelated NBT;
- obsolete GregTech generated-tool metas that the live runtime positively identifies as unregistered remain preserved in old Journey history, but are not newly researched, shown or issued;
- real filled fluid containers remain exact, including their fluid contents;
- GT++ Hand Pump transient internal fluid/init fields are normalized while its electric state remains meaningful.

## Inventory research and recovery

The normal background inventory fallback is intentionally cheap:

- `research.inventoryScanIntervalTicks` defaults to `5` ticks, or 4 fallback checks per second;
- saved values below `5` are raised to `5` when the config is loaded;
- stable slots use cheap signatures between forced safety rescans;
- the normal fallback does not recursively walk nested container contents.

For existing worlds and manual recovery, use the NEI `S` button or `/journey rescan`. Both use the same server-authoritative deep scanner and always finish with one complete research sync, even when no new state is found.

The manual scanner covers:

- main player inventory, armor, cursor stack and supported extra player slots;
- recognized embedded serialized ItemStack lists;
- nested recognized containers, bounded to depth 8 and 4096 embedded stacks;
- Backpack Edited external inventories through its UUID-backed save API when that optional integration is available.

Malformed optional-mod data fails closed and is skipped instead of crashing the scan. Manual scanning never treats arbitrary open machine/container GUI slots as owned inventory.

### Containers are researched empty

Journey never stores retrievable copies that contain another real inventory.

- `SpiceOfLife:lunchbag` strips `Inventory`, `Open` and `UUID` from research identity/template. Its contents are researched separately during manual deep scan.
- `IC2:itemToolbox` strips its `Items` payload and instance `uid`; other meaningful/custom data remains intact.
- Backpack Edited backpack items strip `backpack-UID`, so a retrieved Journey copy cannot point at the original backpack's external save. During manual scan, the original backpack contents are read separately and researched as their own states.
- Unknown `Items` tags are stripped only when the structure is conservatively proven to be a serialized ItemStack list. Arbitrary unknown NBT remains exact.

## NEI controls

Journey reuses GTNH's normal NEI item panel rather than adding a second browser.

- `J` toggles Journey/Researched. It shows the complete researched set newest-first by genuine research chronology.
- `N` shows the same complete researched set ordered by meaningful Journey activity. Successful Journey retrieval moves an existing state to the front of N without changing J chronology.
- `D` enables exact researched-state deletion. Recovery remains available through undo/redo and snapshots.
- `S` runs the deep manual inventory recovery scan described above.
- `T` requests the Debug Researcher Tool. The authoritative server applies the same integrated-owner/operator permission policy as `/journey debugtool`.

NEI's native `G` expand/collapse control is not replaced or owned by Journey. It remains NEI's normal Collapsible Items display control and must not change Journey's authoritative research membership.

While Journey owns the item panel, it preserves J/N chronology and then applies NEI's currently active native filters as visibility predicates. This includes Item Subsets and the normal NEI search parser, so configured prefixes such as `@`, `#`, `%` and other registered search providers work on Journey entries too. Journey's own filter is excluded from this second pass to avoid recursion. A hidden search field is still ignored so stale text from another GUI cannot silently filter Journey.

Journey/Newest clicks:

- left click requests one item;
- right click requests one natural full stack.

Shift does not change the requested amount. In ordinary NEI, Ctrl+left/Ctrl+right uses the Journey retrieval path only when that exact state is researched. The server resolves a fixed research fingerprint against authoritative persisted research before recreating the stored template.

## Persistence, sync and recovery

Exact server retrieval templates are stored alongside research keys. Save migration rebuilds old entries through the current semantic identity/template policy, so newly verified normalization rules can collapse old duplicates while preserving the earliest valid chronology survivor.

Full research sync is epoch-based, byte-budgeted, double-buffered and expected-count validated. The client tracks received wire entries separately from unique semantic entries, so legitimate semantic deduplication does not make a complete sync look truncated.

Incremental unlock/remove/activity packets received during a full sync are deferred and replayed after the matching End packet. Oversized or unserializable states remain server-side rather than risking a disconnect.

Recovery features include:

- `/journey forget`, clear and prune operations as undoable transactions;
- `/journey undo` and `/journey redo` with fail-closed conflict checks;
- deletion history and `/journey restore-deleted`;
- manual, rotating automatic and pre-operation SAFETY snapshots;
- snapshot restore as one undoable transaction;
- automatic reconciliation of active/inactive deletion records against restored snapshots.

## World safety

GTNH Journey keeps the world-protection features independent of Journey research state and hardens world backups for large saves.

### World backups

Automatic world backups are enabled by default. Their cadence starts after the server world has actually loaded, uses wall-clock time, runs every `300` seconds and retains the newest `3` successful ZIP archives. Backups are stored outside the active save directory under:

`gtnhjourney-backups/<world-name>/`

For a normal Prism/Minecraft instance this directory is rooted at the instance level rather than inside `saves/<world>`.

Backup preparation still happens on the authoritative server thread so the snapshot is consistent: Journey saves player data and loaded worlds, waits for threaded chunk I/O to finish, then temporarily suppresses further world-file saving. The expensive ZIP walk and Deflate compression then run on the lower-priority `GTNHJourney-WorldBackup` worker instead of blocking the server tick loop. Compression uses Deflate `BEST_SPEED`, prioritizing a shorter snapshot window over maximum archive compactness. Gameplay can keep ticking while the worker reads the frozen on-disk snapshot. On the next END server tick after completion, Journey restores every world's original save-state flag.

Each ZIP contains the real save-directory name as its top-level folder, for example `<world-name>/level.dat`, rather than placing `level.dat` and region folders loose at archive root. A new archive is written to a temporary file and promoted only after successful completion. Rotation happens afterwards, so a failed new backup does not delete previous successful backups.

If the server begins shutting down while a backup worker is still active, Journey stops accepting new backup work, waits for that worker to finish, restores world saving, and only then lets shutdown continue to its final save. This avoids leaving the world in the temporary snapshot state.

- `/journey backup status` reports enabled/disabled, interval, retention, `running`/`idle`, the last backup duration and the last result.
- `/journey backup now` starts a manual background backup immediately and still works when automatic backups are disabled.
- `/journey backup on` and `/journey backup off` persist the automatic-backup toggle.
- `backup now`, `backup on` and `backup off` require the integrated-server owner or a level-2 operator.

### Session speed

GTNH Journey 1.1.4 introduced a session-only server-speed control that accelerates the normal `MinecraftServer` cadence rather than directly ticking TileEntities or individual machines.

- `/journey speed status` is read-only and reports the active multiplier, target TPS and whether the runtime pacing hook is available.
- `/journey speed 1|2|4|8` requires the integrated-server owner or a level-2 operator.
- targets are 20, 40, 80 and 160 TPS respectively; 4x and 8x use fractional millisecond pacing cycles so the requested average rate is not distorted by integer rounding.
- unsupported or failed runtime hooks fail closed and restore/remain at 1x.
- speed is intentionally not persisted and resets to 1x when the server session restarts.
- world-backup scheduling remains based on wall-clock time rather than accelerated game ticks.

### Explosion guard

Explosions are enabled by default. `/journey explosions off` globally cancels Forge explosion start events until explosions are enabled again. Cancellation itself is never throttled. Operator diagnostics are throttled to one message per five seconds and include best-effort source information and coordinates, with suppressed explosion counts aggregated into the next message.

`/journey explosions status` is read-only. `/journey explosions on|off` requires the integrated-server owner or a level-2 operator and persists the setting.

### Cleanse

`/journey cleanse` removes the caller's currently active potion effects whose registered `Potion` reports `isBadEffect()`. Positive effects are preserved. IC2 radiation is covered because it is registered as a harmful potion effect. The command does not rewrite player NBT or clear persistent non-potion systems such as Warp.

## Debug Researcher Tool

Use the NEI `T` button or `/journey debugtool` as the integrated singleplayer owner or a dedicated-server operator.

Modes cycle with Shift+right-click:

`BLOCK -> CONTENTS -> AREA_16 -> BLOCK`

- `BLOCK` researches only the targeted placed block's item representation.
- `CONTENTS` copies real ItemStacks from a targeted `IInventory` for observation without moving or mutating them.
- `AREA_16` scans the 33 x 33 x 33 cube centered on the player, exactly 35,937 planned positions before validity/loading filters.
- AREA_16 never force-loads or generates chunks.
- one physical debug action becomes one bulk recovery transaction and one compact sync/summary.
- the Debug Researcher Tool itself is never a research candidate.

## Commands

`/journey help` prints the same list in-game with one command per line. Minecraft 1.7.10 native Tab completion is supported for Journey subcommands and fixed arguments such as `trace on/off`, world-safety actions, session-speed values and destructive `confirm` arguments. Up/down arrows remain vanilla command history.

- `/journey help` - show command help
- `/journey count` - show researched state count
- `/journey stats` - show research/recovery statistics
- `/journey inspect` - inspect the held item's Journey identity
- `/journey research` - research or refresh the held item and force a sync
- `/journey rescan` - run the same deep manual inventory scan as `S`
- `/journey list [page]` - list researched states
- `/journey newest [n]` - list newest researched states
- `/journey get <index> [amount]` - retrieve a researched state
- `/journey forget <index>` - forget one researched state
- `/journey undo [n]` - undo Journey transactions
- `/journey redo [n]` - redo Journey transactions
- `/journey restore-deleted [n]` - restore deleted states
- `/journey snapshot [name]` - create a manual snapshot
- `/journey snapshots` - list snapshots
- `/journey restore <id|name>` - restore a snapshot
- `/journey backup status|now|on|off` - inspect, run or toggle world backups
- `/journey explosions status|on|off` - inspect or toggle the global explosion guard
- `/journey cleanse` - remove the caller's active negative potion effects
- `/journey speed <1|2|4|8|status>` - inspect or change session server speed
- `/journey debug` - show runtime compatibility diagnostics
- `/journey trace [on|off]` - toggle live research tracing
- `/journey dump` - write an attachable diagnostic dump into `logs/`
- `/journey hotspots [n]` - show item families with many stored states
- `/journey debugtool` - give the Debug Researcher Tool when permitted
- `/journey prune-missing confirm` - remove unavailable researched states
- `/journey clear confirm` - clear all Journey research

## Configuration

`config/gtnhjourney.cfg`:

- `research.inventoryScanIntervalTicks`: default/minimum effective value `5`, maximum `40`
- `research.inventoryFullRescanIntervalTicks`: default `200`
- `client.newestLimit`: deprecated compatibility key, ignored; N contains the full researched set
- `compatibility.normalizeGtTransientIdentity`: default `true`
- `compatibility.resetGtToolTemplateState`: default `true`
- `compatibility.normalizeGtChargeEndpoints`: default `true`
- `compatibility.normalizeIc2ChargeEndpoints`: default `true`
- `compatibility.normalizeTconToolWear`: default `true`
- `compatibility.normalizeCofhChargeEndpoints`: default `true`
- `backup.worldBackupsEnabled`: default `true`
- `backup.worldBackupIntervalSeconds`: default `300`, bounded to `60..86400`
- `backup.worldBackupRetention`: default `3`, bounded to `1..32`
- `safety.explosionsEnabled`: default `true`

Network/security hard limits and AREA_16 radius remain compile-time constants.

## Diagnostics

`/journey dump` records acquisition, migration, presentation, recovery and sync counters. For interface visibility debugging, the important panel chain is:

- authoritative/client-mirror stack count;
- semantic stack count after Journey keying;
- visible panel stack count after active NEI visibility filters.

The 1.1.4 dump also records server authoritative/syncable/oversized counts, Journey mode, active NEI search/filter state, command-hint resolver state and backup completion state. This makes a server-research versus client-sync versus panel-visibility loss distinguishable in one dump without mutating research.

## Development verification

The release gate is the real GTNH/Forge Gradle build on the exact triggering Git SHA:

```bash
gradle spotlessApply --stacktrace
gradle build --stacktrace
```

CI verifies checkout provenance, runs the complete regression suite, checks that `GTNHJourney.VERSION`, the production JAR filename and packaged `mcmod.info` version agree, then uploads production/dev/sources JARs.

See `docs/first-live-test.md` for the core live-world regression matrix, `docs/v1.1.1-backup-live-test.md` for the 1.1.1 backup regression pass, `docs/v1.1.4-live-test.md` for the 1.1.4 integrity, hints, diagnostics and session-speed pass, and `docs/v1.1.5-live-test.md` for the WR-CBE render-boundary crash regression.
