# GTNH Journey 0.1.0-pre7 live-test matrix

Target: GTNH `2.9.0-beta-2`, NEI `2.8.111-GTNH`.

Use the production `gtnhjourney-0.1.0-pre7.jar`. Keep a world backup from before the test. Do not delete existing Journey research unless a test explicitly needs a fresh state.

## 1. Startup / baseline

1. Replace the previous Journey jar with pre7 and launch the same test world.
2. Confirm the main menu/mod list reports `GTNH Journey 0.1.0-pre7`.
3. Enter the world and open ordinary NEI without enabling Journey.

Expected:
- no startup/client crash;
- ordinary NEI ordering is unchanged while Journey mode is off;
- no Journey-only presentation variants leak into the normal item list.

## 2. J / N chronology

Prepare at least five researched states with a known acquisition order. Prefer visibly distinct items and include one exact-NBT state.

1. Press `J` and compare the panel from top-left onward with first-acquisition order.
2. Toggle `J` off/on and confirm the order is stable.
3. Press `N` and inspect the first several entries.

Expected:
- `J` is oldest-first;
- `N` is newest-first and contains only the configured newest tail;
- NEI search still filters Journey without scrambling chronology.

## 3. Semantic-state regressions

Test representative items where Journey has special semantic handling:

- partially/full charged IC2 drill;
- GT electric item/tool if available;
- Tinkers Construct tool with level/progression/modifiers;
- the previously renderer-hostile volumetric flask/fluid combination if available.

Expected:
- BASE/FULL electric endpoints remain independently addressable;
- TCon wear resets without deleting progression/material/modifier state;
- legacy repaired templates remain structurally valid;
- volumetric flask can render/hover in J/N without client crash and retrieves the authoritative server NBT.

## 4. Acquisition regressions

1. Smelt a previously unresearched output and immediately shift-click/merge it.
2. Pick up a previously unresearched item with normal inventory space.
3. Attempt a pickup that cannot actually enter the inventory.

Expected:
- furnace result is researched without waiting for a manual slot interaction;
- successful pickup is validated from the real server inventory;
- cancelled/full-inventory pickup does not create ghost research.

## 5. Retrieval sanity

For representative BASE, exact-NBT and FULL endpoint items:

- Journey view left click: request full stack;
- Journey view right click: request one;
- ordinary NEI Ctrl+left / Ctrl+right: same behavior only for researched entries.

Expected:
- server remains authoritative;
- presentation-only NBT never appears on retrieved items;
- unrelated/unresearched NEI entries cannot be spawned through Journey.

## 6. Recovery baseline

Before testing the migration tool, verify recovery independently.

1. Create a manual snapshot with `/journey snapshot pre7-baseline`.
2. Forget one known state.
3. `/journey undo`, then `/journey redo`, then `/journey undo` again.
4. List snapshots and restore the manual snapshot if useful.

Expected:
- exact key/template and unlock chronology survive undo/redo;
- redo is invalidated by a later unrelated research mutation;
- snapshots are bounded/persisted as designed and restore through one transaction.

## 7. Debug Researcher Tool command and modes

1. As the integrated singleplayer owner, run `/journey debugtool`.
2. On dedicated multiplayer, verify a non-op is denied and an operator is allowed.
3. Confirm exactly one `Debug Researcher Tool` is granted. If inventory is full, it should be dropped at the player instead of disappearing.
4. Confirm the item looks like a stick, has permanent enchanted glint, and does not stack above one.
5. Shift+right-click repeatedly.

Expected mode sequence:
`BLOCK -> CONTENTS -> AREA_16 -> BLOCK`.

Expected:
- mode follows the physical item through its NBT;
- harmless `/journey count`, `/journey stats`, `/journey dump`, etc. remain usable without globally raising `/journey` permission level;
- one short chat line reports each mode switch.

## 8. BLOCK migration import

Choose an existing placed block/machine whose item state is not already researched.

1. Put the tool in `BLOCK` mode.
2. Right-click the block, preferably one that normally opens a GUI.
3. Inspect Journey/newest and the target block after the click.
4. Run `/journey undo`, then `/journey redo`.

Expected:
- the tool action consumes the interaction before the machine/chest GUI opens where Forge's item-first hook permits;
- only the placed block item representation is observed;
- target inventory contents are not implicitly scanned;
- the block, metadata and TileEntity remain unchanged;
- one chat summary is emitted instead of per-candidate `Unlocked:` spam;
- the entire click is one recovery transaction;
- undo removes only states newly added by that click and redo reapplies them.

## 9. CONTENTS migration import

Choose an existing chest/machine implementing `IInventory`, with at least one item not already researched.

1. Record its slot contents/counts/NBT before the test.
2. Put the tool in `CONTENTS` mode and right-click it.
3. Re-open/inspect the inventory normally after the scan.
4. Undo and redo the import.

Expected:
- actual inventory stacks are copied for observation;
- the target block itself is not implicitly researched;
- no stack is moved, consumed, inserted, extracted, charged, filled or drained;
- slot counts and NBT are byte-for-byte/semantically unchanged from the player's perspective;
- one physical click is one transaction and one summary/sync.

Fluid tanks that are not exposed as real inventory ItemStacks are intentionally not synthesized into fake containers in pre7.

## 10. AREA_16 migrated-base workflow

Use this on a backed-up migrated world/base containing many already-existing blocks and machines.

1. Put the tool in `AREA_16` mode.
2. Stand at a known integer block position `(px, py, pz)` near the old base.
3. Right-click air once.
4. Repeat once while pointing at a machine/block.
5. Check the summary and `/journey dump`.
6. List snapshots, then `/journey undo` and `/journey redo`.

Expected:
- each action plans exactly 4096 positions: x/z `-8..+7`, y `-8..+7` around the player's integer position;
- clicking a block in AREA_16 still centers the cube on the player, not the clicked block;
- only already-loaded/valid positions are read;
- repeated stone/cables/etc. are semantically deduplicated before bulk research;
- placed blocks plus actual `IInventory` contents are observed;
- one safety snapshot is created before the AREA_16 mutation when there is a valid current research state;
- one action produces one bulk recovery transaction, one full Journey sync and one compact summary;
- zero-new-state scans create no undo transaction;
- blocks and inventories remain physically untouched.

`/journey dump` should include cumulative:
- `debugResearchScans`;
- `debugResearchPositionsVisited`;
- `debugResearchInventoriesVisited`;
- `debugResearchUniqueCandidates`;
- `debugResearchNewStates`.

## 11. No forced chunk loading / no global NEI rebuild

For a base near a loaded-chunk boundary:

1. Note which chunks are loaded before AREA_16 if you have a diagnostic method available.
2. Run AREA_16 near the boundary.
3. Watch server/client logs for chunk generation/loading caused by Journey.
4. Toggle ordinary NEI/J/N after the import.

Expected:
- Journey never force-loads or generates an unloaded chunk for migration scanning;
- no world-wide/region-file scan occurs;
- no full global NEI item-list rebuild is triggered by a migration action;
- Journey's direct panel/full research sync remains functional after the bulk import.

## Recommended migrated-world workflow

For an old world installed into Journey after years of progression:

1. Back up the save.
2. Run `/journey debugtool`.
3. Walk through the important parts of the old base and use `AREA_16` in overlapping positions to seed already-existing blocks and machine inventories.
4. Use `BLOCK` for individual placed-state misses.
5. Use `CONTENTS` for a specific chest/machine inventory miss.
6. Use `/journey undo`, `/journey redo`, snapshots and `/journey dump` if a scan imports something unexpected.

The debug tool is intentionally explicit migration/recovery assistance. It does not reconstruct lifetime item history, scan region files, synthesize tank fluids or grant arbitrary NEI items.

## Diagnostics to attach on failure

If any case fails, attach:

- latest client log;
- dedicated/integrated server log if separate;
- `/journey dump` output;
- `/journey inspect` while holding the affected stack where applicable;
- exact test section/step and what happened versus expected.

For migration-tool failures also include:
- current Debug Researcher Tool mode;
- player coordinates;
- whether the clicked target normally opens a GUI;
- whether the relevant chunk was already loaded;
- inventory contents before/after if CONTENTS was involved.

Do not clear research before collecting dump/inspect data, because persisted keys/templates and recovery history are often the most useful evidence.
