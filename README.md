# GTNH Journey

Current release: `1.0.0`.

GT New Horizons 1.7.10 addon that automatically researches item states the player genuinely obtains and allows server-authoritative infinite retrieval through the existing NEI frontend.

## Compatibility target

- Minecraft 1.7.10 / Forge 10.13.4.1614 / MCP stable_12
- GTNH `2.9.0-beta-2` primary baseline
- NotEnoughItems `2.8.111-GTNH` integration baseline
- Development JDK 25 with Java 8-compatible output bytecode
- Newer GTNH 2.9 builds are best-effort until separately verified

## Core behavior

Research belongs to the player UUID and is persisted once per save. Journey observes real server-side inventory state, crafting, smelting and supported recovery scans. Recipe previews, arbitrary open-container GUI slots and other client-only previews are not research sources.

Stack size is not part of identity. Registry id, meaningful metadata and canonical semantic NBT are. Unknown mod NBT remains exact by default. Verified runtime-only fields are normalized only for narrowly supported item families.

Representative compatibility rules include:

- classic durability wear is not a separate research identity for ordinary damageable non-subtyped items;
- verified GregTech electric items use BASE/FULL charge endpoints;
- IC2, CoFH and supported OpenComputers electric items use equivalent endpoint semantics through their APIs;
- verified GregTech tool wear/mode counters, supported Tinkers wear, Botania magnet cooldown, selected Draconic runtime shield/profile noise and known wearable runtime fields are normalized without generically stripping unrelated NBT;
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

Journey search handling uses the current NEI search expression while the direct Journey panel is active. Global NEI item-universe filters are not allowed to discard authoritative Journey templates merely because those exact NBT variants are absent from ordinary NEI permutations.

Journey/Newest clicks:

- left click requests a full stack;
- right click requests one item.

In ordinary NEI, Ctrl+left/Ctrl+right uses the Journey retrieval path only when that exact state is researched. The server resolves a fixed research fingerprint against authoritative persisted research before recreating the stored template.

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

`/journey help` prints the same list in-game with one command per line. Minecraft 1.7.10 native Tab completion is supported for Journey subcommands and fixed arguments such as `trace on/off` and destructive `confirm` arguments. Up/down arrows remain vanilla command history.

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

Network/security hard limits and AREA_16 radius remain compile-time constants.

## Diagnostics

`/journey dump` records acquisition, migration, presentation, recovery and sync counters. For interface visibility debugging, the important panel chain is:

- authoritative/client-mirror stack count;
- semantic stack count after Journey keying;
- visible panel stack count after the active search expression.

This makes a server-research versus client-sync versus panel-visibility loss distinguishable in one dump.

## Development verification

The release gate is the real GTNH/Forge Gradle build on the exact triggering Git SHA:

```bash
gradle spotlessApply --stacktrace
gradle build --stacktrace
```

CI verifies checkout provenance, runs the complete regression suite, checks that `GTNHJourney.VERSION`, the production JAR filename and packaged `mcmod.info` version agree, then uploads production/dev/sources JARs.

See `docs/first-live-test.md` for the 1.0.0 live-world regression matrix.
