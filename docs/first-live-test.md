# GTNH Journey 0.1.0-pre7 live-test matrix

Target: GTNH `2.9.0-beta-2`, NEI `2.8.111-GTNH`.

Use only the production `gtnhjourney-0.1.0-pre7.jar` from the final green CI artifact. Keep a world backup from before the test. Do not clear existing Journey research unless a step explicitly requires a fresh acquisition.

## 1. Startup / version / old-save migration

1. Replace the previous Journey jar with pre7 and launch the same test world.
2. Confirm the mod list reports `GTNH Journey 0.1.0-pre7`.
3. Enter the world and open ordinary NEI before enabling Journey.
4. Run `/journey count` and `/journey dump` before doing new research.
5. Specifically look for previously observed Hand Pump, Vajra and Debug Researcher Tool duplicates.

Expected:
- no startup/client crash;
- ordinary NEI remains usable and Journey does not leak private panel variants into normal view;
- existing valid research survives migration;
- old entries newly collapsed by verified pre7 normalization keep the earliest valid chronology position/template;
- old GT++ Hand Pump fluid-payload variants collapse to the same tool state rather than remaining separate because of `mFluid`/`mFluidAmount`;
- partial-charge Vajra visual-damage variants collapse to the stable IC2 BASE endpoint where the IC2 manager is available;
- accidentally persisted Debug Researcher Tool mode variants are removed and cannot be imported again;
- migration itself produces no `Unlocked:` spam.

## 2. J chronology, incremental refresh and search ownership

Prepare/acquire at least five visibly distinct states with a known order.

1. Press `J`.
2. Observe page 1 from the upper-left slot onward.
3. Acquire one additional genuinely new state while J remains active.
4. Without toggling J, search for that state by name and then completely clear the search box.
5. Repeat with another new state while the search box is empty.

Expected:
- `J` contains all researched states **newest-first by genuine research chronology**;
- the newest state is index 0, upper-left on page 1;
- a new unlock appears in the open J automatically without requiring search text as a refresh trigger;
- search constrains the Journey-owned list but clearing search returns to J, not ordinary unrestricted NEI;
- toggling J off/on preserves the same Journey chronology;
- re-observing an already researched item does not reorder J.

## 3. N full-set activity chronology

Use a research set large enough to span more than one row/page if practical.

1. Compare the total visible research membership of J and N with an empty search box.
2. Note the first few items in N.
3. Obtain an already researched item normally in the world, for example pick up another log that is already researched.
4. Check N again.
5. Retrieve an older researched item by clicking it in J or N.
6. Check N again.
7. Genuinely research a brand-new state and check N again.
8. Search for a subset, clear search, then relog and check N once more.

Expected:
- `N` contains the **same complete researched set as J**; it is never a one-item view and is not truncated by `client.newestLimit`;
- N differs from J only in ordering;
- normally obtaining/re-observing an already researched state does **not** move it in J or N;
- successful Journey retrieval of an already researched state leaves J unchanged and moves that state to the upper-left/front of N;
- a genuinely new research state becomes newest in both J and N;
- repeating Journey retrieval of the current N-newest state creates no duplicate;
- NEI search constrains N without losing items from its underlying researched membership or scrambling the relative activity chronology of matches;
- activity order survives relog/full sync.

## 4. Unlock performance / no ordinary full-NEI reload

1. Leave J or N active.
2. Acquire several unrelated new items one by one.
3. Watch for freezes and later inspect `/journey dump`.

Expected:
- no visible global item-regather/reindex freeze on ordinary unlocks;
- only the small Journey panel refreshes;
- `panelIncrementalUpdates` increases;
- `fullNeiReloadRequests` stays at zero for ordinary research events.

## 5. IC2 Drill / Vajra / charge-state duplicates

Test available Drill, Diamond Drill, Iridium Drill and Vajra states from the migrated save or by acquiring them normally. Include partial and full charge where obtainable.

Expected:
- existing valid electric research is not lost in migration;
- BASE and meaningful FULL endpoints remain independently represented according to IC2 semantics;
- partial charge does not create an unbounded sequence of research states;
- slightly recharging a Vajra does not create another state merely because its visual item-damage step changed;
- BASE Vajra normalizes to its stable empty visual damage rather than keeping charge-animation metas such as 26 versus 27;
- electric states appear in J/N even if native NEI does not expose the same permutation;
- retrieval recreates the authoritative normalized endpoint.

## 6. Fluid containers versus fluid-carrying tools

Use at least two filled Universal Fluid Cells containing different fluids, plus the GT++ Hand Pump if available.

Expected:
- empty and filled real fluid-container states remain distinct;
- two different fluid contents in real containers remain distinct;
- existing filled-cell research survives migration;
- J/N visibility comes from Journey research, not native NEI permutations;
- retrieval preserves researched fluid identity/content for real containers;
- changing/filling a GT++ Hand Pump does **not** create another researched pump solely because of `mFluid`, `mFluidAmount` or initialization payload;
- Hand Pump normalization does not become a generic rule that strips fluid data from real containers or volumetric flasks.

## 7. Furnace output research

### Existing output on first interaction

1. Put an unresearched cooked/result item in the output slot of a vanilla furnace before Journey has observed it.
2. Right-click/open that furnace while the output is still present.
3. Do not take the output item.

Expected:
- the existing real output is immediately observed for research on that interaction;
- taking it into the inventory is not required.

### Closed-GUI completion

1. Put another unresearched recipe into a furnace you have opened/used.
2. Close the GUI before smelting finishes.
3. Let the furnace complete without reopening it first.

Expected:
- the output is researched for the tracked last user even with the GUI closed;
- unchanged furnace output is not repeatedly treated as a new observation;
- `furnaceOutputObservations` advances and a real new unlock advances `furnaceOutputUnlocks`;
- ordinary inventory fallback would still recover the state later if furnace tracking misses it.

## 8. Unlock notifications / no login spam

1. Acquire one genuinely new logical item/state.
2. Keep J or N open while doing so.
3. Re-observe or move the same already researched state.
4. Reconnect/reload the world so a full research snapshot sync occurs.

Expected:
- one concise `Unlocked: <name>` chat message for a new logical acquisition;
- the new state appears in active J/N without needing search/toggle input;
- endpoint expansion such as BASE/FULL does not spam multiple acquisition messages;
- re-observation of existing state produces no new unlock message and no N reorder;
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
- dump counts them under `wearable-transient`, not `unknownExactNbt` solely because of normalized runtime fields.

## 10. Survival <-> Creative continuity and NEI visibility

1. Open ordinary NEI, J and N in Survival.
2. Switch to Creative and open several Creative tabs, not only the inventory tab.
3. Confirm the NEI item section remains visible and usable.
4. With J active in Creative, take a genuinely unresearched item such as a distinct food/block and wait for research reconciliation.
5. Search for it, clear search, then switch between J/N/ordinary NEI.
6. Switch back to Survival.

Expected:
- NEI item panel remains visible in Creative instead of disappearing because of stock Creative visibility rules;
- Journey mode/client research mirror is not cleared by gamemode changes;
- a genuinely new Creative-obtained state is researched and appears automatically in active J/N;
- search/clear-search never silently replaces active J/N with ordinary unrestricted NEI;
- J/N ordering survives the gamemode round trip;
- Journey does not alter normal NEI cheat permission semantics outside its own server-authoritative retrieval path.

## 11. Volumetric flask renderer safety

Test the previously renderer-hostile GregTech volumetric flask/fluid state if available, including opening the global Creative inventory where it previously crashed.

Expected:
- Journey J/N does not crash while constructing/hovering a saved flask state;
- renderer-safe Journey display copies do not mutate authoritative server research/templates;
- clicks/retrieval still resolve to the original authoritative research key;
- a third-party Journey presentation failure may omit only that display entry for the current refresh and increments `presentationFailures`;
- vanilla Creative and normal NEI do not crash on a GT volumetric-flask permutation whose fluid has no icon;
- only proven unsafe volumetric-flask permutations are removed from the display list; normal flask states remain available;
- after exercising Creative, `/journey dump` may show `creativeUnsafeFlaskVariantsRemoved > 0` as direct evidence that the safety path ran.

## 12. Retrieval sanity

For representative BASE, exact-NBT, filled-container and FULL endpoint items:

- Journey view left click: request full stack;
- Journey view right click: request one;
- ordinary NEI Ctrl+left / Ctrl+right: same behavior only for researched entries.

Expected:
- server remains authoritative;
- display-only sanitization never contaminates retrieved items;
- unrelated/unresearched NEI entries cannot be spawned through Journey;
- successful J/N retrieval updates N activity order but does not alter J chronology.

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
3. Confirm exactly one physical `Debug Researcher Tool` is granted; with full inventory it should drop instead of disappearing.
4. Confirm stack size 1, stick presentation and permanent enchanted glint.
5. Shift+right-click repeatedly.
6. Verify J/N never acquire separate debug-tool research entries while cycling modes or while the tool is present during login/rescan/AREA import.

Expected mode sequence:
`BLOCK -> CONTENTS -> AREA_16 -> BLOCK`.

Expected research behavior:
- the Debug Researcher Tool itself is not researchable;
- mode NBT does not create BLOCK/CONTENTS/AREA variants in J/N;
- old accidentally persisted variants disappear after migration/load.

Ordinary `/journey count|stats|dump|...` remains permission level 0; only `debugtool` has the owner/op gate.

## 15. BLOCK migration import

Choose an existing placed block/machine whose item representation is not already researched.

1. Hold the debug tool in `BLOCK` mode and right-click the target block normally.
2. Prefer a target that normally opens a GUI, to verify item-first interaction routing.
3. Inspect Journey/N and the target after the click.
4. `/journey undo`, then `/journey redo`.

Expected:
- the server actually executes the targeted BLOCK action; it is not swallowed client-side;
- only the placed block item representation is observed;
- target inventory contents are not implicitly scanned;
- the block GUI does not steal the debug action before the tool can run;
- block, metadata and TileEntity remain unchanged;
- one click is one recovery transaction and one compact summary/sync;
- undo removes only states introduced by that click and redo reapplies them.

## 16. CONTENTS migration import

Choose an `IInventory` chest/furnace/machine with at least one unresearched real ItemStack. A furnace with cooked food in slot 2 is a useful regression target.

1. Record slot contents/counts/NBT.
2. Hold the tool in `CONTENTS` mode and right-click the inventory block normally.
3. Confirm a migration summary appears without first manually taking the contents.
4. Inspect the inventory normally afterward.
5. Undo/redo the import.

Expected:
- the server actually executes targeted CONTENTS instead of silently opening/consuming the client interaction;
- actual ItemStacks are copied only for observation;
- the target block itself is not implicitly researched;
- nothing is moved, consumed, charged, filled, drained, inserted or extracted;
- one click is one transaction and one summary/sync.

Fluid tanks not exposed as real inventory ItemStacks are intentionally not synthesized into fake containers.

## 17. AREA_16 migrated-base workflow

1. Stand near a backed-up old base and use `AREA_16`.
2. Repeat while pointing at a machine to verify the center still follows the player rather than the clicked block.
3. Inspect `/journey dump`, snapshots, undo and redo.

Expected:
- exactly **35,937** candidate positions are planned: x/y/z `-16..+16` inclusive around the player's integer position;
- this is a `33 x 33 x 33` cube and includes the player's own block coordinate;
- only valid, already-loaded positions are read;
- no chunk is force-loaded/generated by the scan;
- repeated blocks/cables/stacks are semantically deduplicated before bulk research;
- placed blocks plus actual `IInventory` contents are observed;
- the Debug Researcher Tool itself is rejected even if seen by an inventory/bulk migration path;
- a pre-mutation safety snapshot is created when there is current state to protect;
- zero-new-state scans create no undo transaction;
- blocks and inventories remain physically untouched.

## 18. `/journey dump` final counters and semantic evidence

After the preceding tests, generate a final dump.

Check at least:
- `panelIncrementalUpdates`;
- `fullNeiReloadRequests`;
- `presentationFailures`;
- `creativeUnsafeFlaskVariantsRemoved`;
- `unlockNotifications`;
- `furnaceOutputObservations`;
- `furnaceOutputUnlocks`;
- `debugResearchScans`;
- `debugResearchPositionsVisited`;
- `debugResearchInventoriesVisited`;
- `debugResearchUniqueCandidates`;
- `debugResearchNewStates`;
- runtime config line reports `N=full-research/activity-order`, not a numeric newest limit;
- semantic policy matches including `wearable-transient` when those items were tested;
- `unknownExactNbt` entries for truly unsupported exact-NBT states only.

For one AREA_16 action, `debugResearchPositionsVisited` should advance by 35,937. Multiple scans accumulate.

## Recommended migrated-world workflow

1. Back up the save.
2. Launch the final pre7 jar and first confirm old research/drills/filled cells survived and known duplicate Hand Pump/Vajra/debugtool states collapsed as expected.
3. Run `/journey debugtool`.
4. Walk important parts of the base and use overlapping AREA_16 scans.
5. Use BLOCK for individual placed-state misses and CONTENTS for a specific inventory miss.
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
