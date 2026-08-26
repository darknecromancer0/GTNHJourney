# GTNH Journey 0.1.0-pre7 live-test matrix

Target: GTNH `2.9.0-beta-2`, NEI `2.8.111-GTNH`.

Use only the production `gtnhjourney-0.1.0-pre7.jar` from the final green CI artifact. Keep a world backup from before the test. Do not clear existing Journey research unless a step explicitly requires a fresh acquisition.

## 1. Startup / version / old-save migration

1. Replace the previous Journey jar with pre7 and launch the same test world.
2. Confirm the mod list reports `GTNH Journey 0.1.0-pre7`.
3. Enter the world and open ordinary NEI before enabling Journey.
4. Run `/journey count` and `/journey dump` before doing new research.

Expected:
- no startup/client crash;
- ordinary NEI remains usable and Journey does not leak private panel variants into normal view;
- existing valid research survives migration;
- old entries newly collapsed by verified pre7 normalization keep the earliest valid chronology position/template;
- migration itself produces no `Unlocked:` spam.

## 2. J chronology and newest placement

Prepare/acquire at least five visibly distinct states with a known order.

1. Press `J`.
2. Observe page 1 from the upper-left slot onward.
3. Acquire one additional new state while J remains active.

Expected:
- `J` contains all researched states **newest-first**;
- the newest state is index 0, upper-left on page 1;
- the new unlock moves/keeps the panel at page 1 without a full NEI rebuild;
- toggling J off/on preserves the same Journey chronology.

## 3. N chronology and configured limit

1. Press `N` after enough states exist to exceed or meaningfully exercise `client.newestLimit`.
2. Compare the first entries with the same known acquisition order.
3. Search for a subset using normal NEI search.

Expected:
- `N` is **newest-first**;
- only the configured newest tail is eligible;
- newest is upper-left page 1;
- NEI search constrains the Journey list without scrambling chronology among matching states.

## 4. Unlock performance / no ordinary full-NEI reload

1. Leave J or N active.
2. Acquire several unrelated new items one by one.
3. Watch for freezes and later inspect `/journey dump`.

Expected:
- no visible global item-regather/reindex freeze on ordinary unlocks;
- only the small Journey panel refreshes;
- `panelIncrementalUpdates` increases;
- `fullNeiReloadRequests` stays at zero for ordinary research events.

## 5. IC2 Drill / Diamond Drill / Iridium Drill states

Test available Drill, Diamond Drill and Iridium Drill states from the migrated save or by acquiring them normally. Include charged state/endpoints where obtainable.

Expected:
- existing valid drill research is not lost in migration;
- BASE and meaningful FULL endpoints remain independently represented according to IC2 semantics;
- partial charge does not create an unbounded sequence of research states;
- drill states appear in J/N even if native NEI does not expose the same permutation;
- retrieval recreates the authoritative saved state.

## 6. Filled Universal Fluid Cells / meaningful container contents

Use at least two filled cells containing different fluids, preferably one from existing pre6 research.

Expected:
- empty and filled states remain distinct;
- two different fluid contents remain distinct;
- existing filled-cell research survives migration;
- J/N visibility comes from Journey research, not native NEI permutations;
- retrieval preserves the researched fluid identity/content rather than returning an empty generic cell.

## 7. Closed-GUI furnace automatic research

1. Put an unresearched recipe into a furnace you have opened/used.
2. Close the GUI before smelting finishes.
3. Let the furnace complete without reopening it first.

Expected:
- the output is researched for the tracked last user even with the GUI closed;
- unchanged furnace output is not repeatedly treated as a new observation;
- `furnaceOutputObservations` advances and a real new unlock advances `furnaceOutputUnlocks`;
- ordinary inventory fallback would still recover the state later if furnace tracking misses it.

## 8. Unlock notifications / no login spam

1. Acquire one genuinely new logical item/state.
2. Re-observe or move the same already researched state.
3. Reconnect/reload the world so a full research snapshot sync occurs.

Expected:
- one concise `Unlocked: <name>` message for a new logical acquisition;
- endpoint expansion such as BASE/FULL does not spam multiple acquisition messages;
- re-observation of existing state produces no new unlock message;
- login/full sync and migration do not replay old unlock notifications.

## 9. Wearable equip/runtime duplicates

Test the live-observed families if present:
- `EMT:itemArmorQuantumChestplate`;
- `DraconicEvolution:wyvernChest`.

1. Record their Journey states/count before equip.
2. Equip/unequip repeatedly and allow the mod to update runtime NBT.
3. Run `/journey inspect` while holding the affected stack and then `/journey dump`.

Expected:
- EMT zero `unequip`/`wing` runtime initialization does not create duplicate research;
- non-zero/persistent state is not generically stripped;
- Wyvern `ProtectionPoints`/`ShieldEntropy` runtime values do not create duplicate research;
- energy, upgrades, enchantments, configurations and unrelated payload remain meaningful;
- `/journey inspect` reports `Wearable-transient=true` for supported items;
- dump counts them under `wearable-transient`, not `unknownExactNbt` solely because of the normalized runtime fields.

## 10. Survival <-> Creative continuity

1. Open J and N in Survival.
2. Switch to Creative and repeat J/N/retrieval checks.
3. Switch back to Survival.

Expected:
- Journey mode/client research mirror is not cleared by gamemode changes;
- J/N chronology remains the same;
- normal NEI/permission behavior remains governed by its existing rules;
- Journey does not depend on a Survival-only container to stay active.

## 11. Journey-safe flask presentation

Test the previously renderer-hostile GregTech volumetric flask/fluid state if available.

Expected:
- Journey J/N does not crash while constructing/hovering that state;
- if a renderer-safe display copy is necessary, the authoritative server research/template remains unchanged;
- clicks/retrieval still resolve to the original authoritative research key;
- a third-party presentation failure may omit only that display entry for the current refresh and increments `presentationFailures`;
- pre7 does **not** claim to fix a GregTech crash in the global Creative inventory renderer outside Journey.

## 12. Retrieval sanity

For representative BASE, exact-NBT, filled-container and FULL endpoint items:

- Journey view left click: request full stack;
- Journey view right click: request one;
- ordinary NEI Ctrl+left / Ctrl+right: same behavior only for researched entries.

Expected:
- server remains authoritative;
- display-only sanitization never contaminates retrieved items;
- unrelated/unresearched NEI entries cannot be spawned through Journey.

## 13. Recovery baseline

1. Create `/journey snapshot pre7-baseline`.
2. Forget one known state.
3. `/journey undo`, `/journey redo`, then `/journey undo` again.
4. Restore the manual snapshot if useful.

Expected:
- exact key/template and unlock chronology survive undo/redo;
- redo is invalidated by a later unrelated mutation;
- snapshot restore is one undoable transaction and creates its safety layer as designed.

## 14. Debug Researcher Tool command and modes

1. As integrated singleplayer owner, run `/journey debugtool`.
2. On dedicated multiplayer, verify a non-op is denied and an operator is allowed.
3. Confirm exactly one `Debug Researcher Tool` is granted; with full inventory it should drop instead of disappearing.
4. Confirm stack size 1, stick presentation and permanent enchanted glint.
5. Shift+right-click repeatedly.

Expected mode sequence:
`BLOCK -> CONTENTS -> AREA_16 -> BLOCK`.

Ordinary `/journey count|stats|dump|...` remains permission level 0; only `debugtool` has the owner/op gate.

## 15. BLOCK migration import

Choose an existing placed block/machine whose item representation is not already researched.

1. Use `BLOCK` mode on it.
2. Inspect Journey/newest and the target after the click.
3. `/journey undo`, then `/journey redo`.

Expected:
- only the placed block item representation is observed;
- target inventory contents are not implicitly scanned;
- block, metadata and TileEntity remain unchanged;
- one click is one recovery transaction and one compact summary/sync;
- undo removes only states introduced by that click and redo reapplies them.

## 16. CONTENTS migration import

Choose an `IInventory` chest/machine with at least one unresearched real ItemStack.

1. Record slot contents/counts/NBT.
2. Use `CONTENTS` mode.
3. Inspect the inventory normally afterward.
4. Undo/redo the import.

Expected:
- actual ItemStacks are copied only for observation;
- the target block itself is not implicitly researched;
- nothing is moved, consumed, charged, filled, drained, inserted or extracted;
- one click is one transaction and one summary/sync.

Fluid tanks not exposed as real inventory ItemStacks are intentionally not synthesized into fake containers.

## 17. AREA_16 migrated-base workflow

1. Stand near a backed-up old base and use `AREA_16`.
2. Repeat while pointing at a machine to verify the center still follows the player.
3. Inspect `/journey dump`, snapshots, undo and redo.

Expected:
- exactly 4096 candidate positions are planned: x/y/z `-8..+7` around the player's integer position;
- only valid, already-loaded positions are read;
- no chunk is force-loaded/generated by the scan;
- repeated blocks/cables/stacks are semantically deduplicated before bulk research;
- placed blocks plus actual `IInventory` contents are observed;
- a pre-mutation safety snapshot is created when there is current state to protect;
- zero-new-state scans create no undo transaction;
- blocks and inventories remain physically untouched.

## 18. `/journey dump` final counters and semantic evidence

After the preceding tests, generate a final dump.

Check at least:
- `panelIncrementalUpdates`;
- `fullNeiReloadRequests`;
- `presentationFailures`;
- `unlockNotifications`;
- `furnaceOutputObservations`;
- `furnaceOutputUnlocks`;
- `debugResearchScans`;
- `debugResearchPositionsVisited`;
- `debugResearchInventoriesVisited`;
- `debugResearchUniqueCandidates`;
- `debugResearchNewStates`;
- semantic policy matches including `wearable-transient` when those items were tested;
- `unknownExactNbt` entries for truly unsupported exact-NBT states only.

## Recommended migrated-world workflow

1. Back up the save.
2. Launch the final pre7 jar and first confirm old research/drills/filled cells survived.
3. Run `/journey debugtool`.
4. Walk important parts of the base and use overlapping `AREA_16` scans.
5. Use `BLOCK` for individual placed-state misses and `CONTENTS` for a specific inventory miss.
6. Use undo/redo/snapshots and `/journey dump` if anything unexpected is imported.

The debug tool is explicit migration/recovery assistance. It does not reconstruct lifetime history, scan region files, synthesize tank fluids or grant arbitrary NEI items.

## Diagnostics to attach on failure

Attach:
- latest client log;
- dedicated/integrated server log if separate;
- `/journey dump`;
- `/journey inspect` while holding the affected stack where applicable;
- exact section/step and observed versus expected behavior.

For migration-tool failures also include the tool mode, player coordinates, whether the clicked target normally opens a GUI, whether the chunk was already loaded, and inventory before/after for CONTENTS.

Do not clear research before collecting dump/inspect data; persisted keys/templates, chronology and recovery history are often the best evidence.
