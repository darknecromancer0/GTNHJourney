# GTNH Journey

Current development version: `0.1.0-pre7`.

GT New Horizons 1.7.10 addon that automatically researches item states the player genuinely obtains and allows server-authoritative infinite retrieval through the existing NEI frontend.

## Compatibility target

- Minecraft 1.7.10 / Forge 10.13.4.1614 / MCP stable_12
- **GTNH 2.9.0-beta-2** primary pack baseline
- **NotEnoughItems 2.8.111-GTNH** compile/runtime integration baseline
- Newer 2.9 nightlies are best-effort until separately verified
- Official GTNH Gradle convention plugin
- Development JDK 25 + Jabel syntax mode; Java 8-compatible output bytecode

## Current pre-alpha behavior

### Research authority

- Research belongs to a player UUID and is stored once per save in overworld `WorldSavedData`.
- Real player inventory stacks are researched automatically.
- Crafting and smelting trigger an immediate research attempt; smelting also forces an immediate real-inventory revalidation so shift-click/merge paths do not wait for a later cached scan.
- Pickups are *not* trusted as proof by themselves; the real server inventory scan validates them, avoiding cancelled/full-inventory ghost unlocks.
- Periodic inventory validation includes main inventory, armor, cursor stack and optional Baubles-Expanded slots.
- Installing Journey into an existing world imports the player's current real inventories before the first full client sync.
- GUI recipe previews, machine output previews and arbitrary open-container slots are never normal research sources.

### State identity

- Stack size is not part of research identity.
- Registry id, meaningful metadata and canonical semantic NBT are.
- Compound NBT key order is canonicalized deterministically; list order remains significant.
- Classic durability on non-subtyped items is treated as wear, not a new researched item.
- Verified `MetaGeneratedTool` runtime fields `GT.ToolStats.Damage` and `GT.ToolStats.Mode` are ignored in identity and reset in retrieval templates; foreign lookalike NBT stays exact.
- Filled fluid/container payload remains meaningful research state. Filled cells with different contents stay distinct; no generic `Fluid`/container stripping is applied.
- GT fluid/container payload (`GT.FluidContent`) remains exact.
- GT++ `ItemGregtechPump` auto-initialized fluid/tool fields (`mInit`, `mFluid`, `mFluidAmount`, `mMeta`, `mCapacity`, `capacityInit`) are treated as transient derived state, so tooltip/fluid initialization or filling a Hand Pump does not create another researched tool. Its real electric `GT.ItemCharge` remains meaningful and follows charge-endpoint semantics; actual fluid containers remain exact.
- GT electric items use Journey endpoints: partial charge => BASE; verified max charge => BASE + FULL.
- IC2 `IElectricItem` stacks use the same BASE/FULL model through the IC2 electric API/manager, not a generic `charge`-NBT heuristic. Legacy visual damage for an empty electric item is normalized to its stable empty value, preventing partial-charge damage steps such as Vajra charge animation from becoming research duplicates.
- CoFH `IEnergyContainerItem` stacks use the same BASE/FULL model through the CoFH energy API; arbitrary `Energy` NBT on foreign items remains exact.
- OpenComputers Hover Boots use their own public charge API and follow the same BASE/FULL endpoint model.
- Tinkers Construct `ToolCore` durability/render wear is normalized while `ToolLevel`, XP counters, modifiers, materials and other meaningful tool payload remain intact. Legacy pre5 templates that already lost XP counters are repaired to zero progress instead of stripping progression state again.
- Botania magnet rings ignore the verified per-tick `cooldown` field in research identity and retrieval templates.
- Draconic Evolution `ToolBase` items ignore only the auto-created five-empty-profile initialization payload; real profile contents remain exact.
- `EMT:itemArmorQuantumChestplate` removes only observed zero-value `unequip`/`wing` initialization noise. Non-zero values and unrelated upgrades/enchantments remain exact.
- `DraconicEvolution:wyvernChest` removes only verified runtime `ProtectionPoints`/`ShieldEntropy`; energy, configuration, upgrades and unrelated payload remain meaningful.
- Wearable rules are exact registry-id scoped. Foreign armor carrying similarly named NBT stays untouched.
- Unknown mod NBT remains exact by default. Compatibility rules fail closed rather than merging unknown states.

### Persistence and recovery

- Exact server retrieval templates are stored alongside research keys.
- Save-format migration rebuilds both identity and template through the same current semantic policy on load, so already-saved duplicates can collapse when a verified normalization rule is added.
- When old entries collapse to one canonical pre7 state, the first valid/earliest persisted occurrence keeps the chronology position and template deterministically.
- Broken NBT-backed entries with no retrieval template are rejected instead of becoming a bare item; a later valid occurrence may then become the migration survivor.
- Existing drill/electric endpoints and filled-cell research are preserved when their semantic states remain distinct.
- Removed mod items remain diagnosable as orphan research until explicitly pruned.
- Explicit destructive/recovery operations are transaction-aware and persist undo/redo state.
- `/journey forget`, `/journey clear confirm` and `/journey prune-missing confirm` are undoable through the recovery journal.
- `/journey undo` and `/journey redo` restore exact keys/templates and unlock chronology.
- Manual, rotating automatic and pre-operation safety snapshots provide a second recovery layer.
- Snapshot restore is itself an undoable transaction and creates a safety snapshot before replacing state.

### pre7 migrated-world recovery tool

pre7 adds an explicit admin migration tool for already-existing bases. It is deliberately separate from normal research progression.

- Obtain exactly one tool with `/journey debugtool`.
- Integrated singleplayer owner is allowed; dedicated multiplayer requires operator-level permission for this subcommand only.
- The item is a stack-size-1 `Debug Researcher Tool`, visually based on a vanilla stick with permanent enchanted glint.
- The debug tool itself is never a research candidate; old accidentally persisted tool-mode states are rejected by migration.
- Shift+right-click cycles `BLOCK -> CONTENTS -> AREA_16 -> BLOCK`; mode is stored on the physical tool's NBT.
- `BLOCK` observes only the item representation of the targeted placed block. Forge pick-block is preferred, with a safe item/meta fallback.
- `CONTENTS` copies actual ItemStacks from a targeted `IInventory` without opening or mutating it. The block itself is not implicitly added.
- `AREA_16` is a true 16-block radius around the player's integer position: `-16..+16` on X, Y and Z. The resulting cube is `33 x 33 x 33`, exactly **35,937** positions before validity/loading filters.
- AREA_16 reads only already-loaded valid positions and never force-loads/generates chunks.
- Repeated observations are deduplicated by Journey semantic identity before research expansion.
- The tool never breaks blocks, changes metadata, moves inventory contents, synthesizes machine-fluid containers or scans region files/the entire world.
- One physical action is one bulk recovery transaction and one compact summary/sync. Zero-new-state imports create no undo transaction.
- AREA_16 creates a pre-mutation safety snapshot when there is current state to preserve.
- `/journey undo` removes only states newly introduced by the last migration transaction; `/journey redo` can reapply it while the redo branch remains valid.
- Runtime diagnostics accumulate migration scan count, positions, inventories, unique candidates and newly unlocked states in `/journey dump`.

Recommended old-world workflow: back up the save, walk through important parts of the old base using overlapping AREA_16 scans, then use BLOCK/CONTENTS for targeted misses.

### Networking / security

- Client is never authoritative for research or spawning.
- Retrieval requests send a fixed SHA-256 research fingerprint, not arbitrary ItemStack/NBT payloads.
- Server resolves the fingerprint against the player's current research again before creating the saved template.
- Retrieval requests are rate/queue limited per player.
- Large research catalogs stream over multiple server ticks with entry-count and actual serialized-ItemStack byte budgets.
- Full sync is epoch-based, expected-count validated and double-buffered client-side; an incomplete/outdated stream never replaces the last complete snapshot or its staged N activity order.
- Client-bound network mutations are FIFO-queued onto the client tick thread instead of mutating the NEI mirror from Netty callbacks.
- Unlocks and N retrieval touches occurring during a full sync are deferred and replayed after its matching End packet.
- Oversized or unserializable single NBT states stay server-side rather than risking a disconnect; the GUI reports the server-only count.
- Server-selected semantic compatibility flags are transmitted at Begin-sync so client fingerprints cannot drift from server identity rules.
- Migration imports use one bulk server-side mutation and one research sync rather than one network refresh per candidate.

## NEI GUI

Journey deliberately reuses GTNH's existing NEI item panel instead of adding a second item browser. While J/N is active, Journey owns the small visible panel list directly from its synchronized research mirror instead of rebuilding or injecting variants into NEI's global item universe.

- `J` toggles **Journey / Researched** and shows all researched states **newest-first by first/fresh research chronology**.
- `N` contains the **same complete researched set as J**, but orders it by recent meaningful Journey activity.
- A genuinely new researched state becomes newest in both J and N.
- Re-observing or normally obtaining an already researched state does not reorder either view.
- Successfully retrieving an already researched state by clicking it in J or N leaves J unchanged and moves that state to the front of N. This activity order is persisted across relogs.
- Stored Drill/Diamond Drill/Iridium Drill and filled-cell states are eligible directly from Journey research; visibility does not depend on NEI having a native permutation for them.
- Normal unlocks rebuild only the small Journey panel list and do **not** call NEI's full item-universe reload.
- Returning to ordinary NEI relinquishes Journey panel ownership and requests the normal NEI filter/update path.
- Renderer-hostile Journey states use client-only presentation copies. If third-party presentation/filter code fails, only that display entry is omitted for the current refresh; authoritative server research and retrieval templates remain intact.
- Journey presentation and vanilla Creative omit only proven renderer-hostile GregTech volumetric-flask permutations whose fluid has no icon; normal flask states remain available. Ordinary NEI is not globally filtered by Journey. `/journey dump` counts removed vanilla Creative permutations as `creativeUnsafeFlaskVariantsRemoved`.
- Normal NEI search and recipe/usage browsing remain available and constrain the Journey list without changing J/N chronology among matching states.
- In Journey/Newest view: left click requests a full stack; right click requests one item.
- In ordinary NEI: Ctrl + left click requests a full researched stack; Ctrl + right click requests one.
- `Journey.Researched` and `Journey.Newest` remain fallback subsets for layouts too narrow to show both buttons; both subsets use the full researched membership.
- Researched tooltips are marked and include Journey retrieval hotkey hints.
- Migration actions update Journey through the research sync path and do not require a global NEI item-list rebuild.

The NEI built-in infinite/cheat item path is intentionally **not** used because it would bypass Journey's server-side research validation.

## Commands

Diagnostics and ordinary player operations keep `/journey` permission level 0. The `debugtool` subcommand performs its own owner/operator permission check.

- `/journey count`
- `/journey stats`
- `/journey debug`
- `/journey debugtool` - grants the admin migration/recovery tool to the integrated owner or an operator
- `/journey trace [on|off]` - opt-in live unlock chat/log trace for this server session
- `/journey dump` - writes an attachable diagnostic file into `logs/`, including semantic-policy, presentation/performance, recovery and pre7 migration counters
- `/journey hotspots [limit]`
- `/journey list [page]`
- `/journey newest [limit]`
- `/journey get <index> [amount]`
- `/journey forget <index>`
- `/journey undo [count]`
- `/journey redo [count]`
- `/journey restore-deleted [count]`
- `/journey snapshot [name]`
- `/journey snapshots`
- `/journey restore <snapshot-id-or-name>`
- `/journey inspect` - reports normalized identity, wire/sync information, GT/IC2/CoFH/OpenComputers charge classification, GT/TCon ownership, Botania magnet state, Draconic tool state, wearable-transient matching and semantic endpoint count
- `/journey rescan`
- `/journey prune-missing confirm`
- `/journey clear confirm`

## Configuration

`config/gtnhjourney.cfg`:

- `research.inventoryScanIntervalTicks` (default `20`, one-second fallback reconciliation)
- `research.inventoryFullRescanIntervalTicks` (default `200`, safety deep-rescan while stable slots use cheap signatures)
- `client.newestLimit` is a deprecated compatibility key and is ignored by pre7; N always contains the full researched set.
- `compatibility.normalizeGtTransientIdentity` (default `true`)
- `compatibility.resetGtToolTemplateState` (default `true`)
- `compatibility.normalizeGtChargeEndpoints` (default `true`)
- `compatibility.normalizeIc2ChargeEndpoints` (default `true`)
- `compatibility.normalizeTconToolWear` (default `true`)
- `compatibility.normalizeCofhChargeEndpoints` (default `true`)

Security/network hard limits and the pre7 AREA_16 radius intentionally remain compile-time constants.

## Development verification

The real GTNH/Forge Gradle build is the release gate. CI runs formatting followed by the full build:

```bash
gradle spotlessApply --stacktrace
gradle build --stacktrace
```

`gradle build` includes the regression suite for chronology, N activity ordering, semantic endpoint classification, filled-container exactness, wearable/tool normalization and migration, renderer-safe presentation, acquisition, recovery transactions/snapshots and the pre7 Debug Researcher Tool. CI also verifies that the Java `VERSION`, production jar filename and packaged `mcmod.info` version agree before uploading production, dev and sources jars.

For pre7 release verification, the debug package must contain no forced chunk-loading path and no global NEI item-universe reload path. AREA_16 is bounded to exactly **35,937** planned positions (`-16..+16` on each axis) and skips unavailable/unloaded cells.

See `docs/first-live-test.md` for the live-world matrix and migrated-base workflow.
