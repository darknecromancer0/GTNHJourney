# GTNH Journey 1.0.0 live-test matrix

Target: GTNH `2.9.0-beta-2`, NEI `2.8.111-GTNH`.

Use only the production `gtnhjourney-1.0.0.jar` from the final exact-SHA green CI artifact. Keep a world backup from before the test. Do not clear existing Journey research unless a step explicitly asks for it.

The broad pre7 migration/recovery matrix has already been exercised during development. This 1.0 matrix focuses on the final inventory-recovery, container-safety, command and panel regressions that changed after that baseline.

## 1. Startup, migration and version

1. Replace the previous Journey jar with `gtnhjourney-1.0.0.jar`.
2. Launch the same existing test world.
3. Confirm the mod list reports `GTNH Journey 1.0.0`.
4. Run `/journey count`, `/journey stats` and `/journey dump` before clearing or manually rebuilding anything.

Expected:

- no startup/client crash;
- existing research survives migration;
- old Lunch Bag, Toolbox and Backpack instance/content-bearing variants recanonicalize through the current semantic policy rather than multiplying states;
- no login/migration `Unlocked:` spam;
- ordinary NEI remains usable before J/N is enabled.

## 2. J membership and server-to-panel chain

Use an existing world with enough research to make missing entries obvious.

1. Open `J` with an empty search box.
2. Compare visible membership with `/journey count` and `/journey stats`.
3. Search for several known researched items, then completely clear the search box.
4. Repeat with `N`.
5. Generate `/journey dump`.

Expected:

- J and N contain the same complete syncable researched membership, differing only in ordering;
- clearing search returns the complete active Journey view, not ordinary NEI and not a small leftover subset;
- exact-NBT Journey templates do not disappear merely because ordinary NEI does not expose the same permutation;
- the diagnostic panel chain does not show an unexplained large loss between authoritative/client mirror, semantic and visible panel counts when search is empty;
- server-only oversized states, if any, are reported separately instead of silently vanishing.

## 3. Native NEI G expand/collapse regression

1. With J active and empty search, note several visible researched items.
2. Use NEI's native `G` Collapsible Items expand/collapse control.
3. Toggle it several times.
4. Switch J -> N -> ordinary NEI -> J.

Expected:

- G continues to work as native NEI display grouping;
- Journey does not replace or own G;
- expanding/collapsing may change visual grouping where NEI normally does so, but does not delete or mutate authoritative Journey research;
- J/N remain usable after the toggle cycle.

## 4. S deep inventory recovery

Prepare a player inventory containing several ordinary items plus supported containers.

1. Press `S` once.
2. Note the compact `Inventory scan:` summary.
3. Press `S` again without changing anything.
4. Reopen J and search for ordinary top-level inventory items and known embedded contents.

Expected:

- the first scan reads real player-owned top-level stacks and supported embedded/external inventories;
- one scan produces one compact summary and one full research sync rather than per-item unlock spam;
- the second unchanged scan reports `+0` new states;
- top-level ordinary inventory items become available in Journey;
- recognized nested contents become separate researched states;
- malformed optional-mod data is isolated instead of crashing the scan.

Manual recursion is bounded to depth 8 and 4096 embedded stacks. Normal 5-tick fallback remains shallow.

## 5. Container safety

### Lunch Bag

Use at least two `SpiceOfLife:lunchbag` items with different contents and, if practical, different open/instance state.

Expected:

- Journey stores/retrieves an empty Lunch Bag representation;
- `Inventory`, `Open` and `UUID` do not create separate research identities;
- contents discovered by S appear as their own Journey states;
- retrieving the Lunch Bag does not duplicate its old contents.

### IC2 Toolbox

Use a Toolbox with real stored items.

Expected:

- the Toolbox itself remains researchable;
- embedded `Items` and instance `uid` are not preserved in the retrievable Journey template;
- contained items are researched separately by S;
- unrelated meaningful/custom NBT is not generically stripped.

### Backpack Edited

Use a Backpack whose contents are stored through the mod's external UUID-backed save.

Expected:

- S reads the original backpack's external inventory read-only when the optional integration is available;
- those contents become separate Journey states;
- the Journey backpack template does not preserve `backpack-UID`;
- a retrieved Backpack therefore cannot point at or clone the original external inventory.

## 6. `/journey rescan` parity with S

1. Add one or more new top-level or supported nested inventory items.
2. Run `/journey rescan`.
3. Run it again unchanged.

Expected:

- command uses the same deep manual scanner as S;
- it reports the same style of summary;
- it always finishes with a full research sync, including the `+0` case;
- no shallow-only discrepancy remains between S and `/journey rescan`.

## 7. 5-tick background fallback

1. Inspect `config/gtnhjourney.cfg` and `/journey dump`.
2. If testing migration, temporarily set `inventoryScanIntervalTicks` below 5, restart, and inspect the effective value.
3. Acquire ordinary inventory items without pressing S.

Expected:

- new/default interval is 5 ticks;
- saved values below 5 are normalized upward to 5;
- ordinary event-driven pickup/crafting/smelting paths remain responsive;
- fallback inventory validation runs at 4 checks per second rather than the old 2-tick rate;
- deep nested recursion is not performed by this periodic fallback.

## 8. T Debug Researcher Tool shortcut

1. As integrated singleplayer owner, press `T`.
2. On dedicated multiplayer, verify a non-op is denied and an operator is allowed.
3. Compare with `/journey debugtool`.

Expected:

- T grants exactly the same Debug Researcher Tool through a server-authoritative request;
- the same owner/operator permission policy applies to T and `/journey debugtool`;
- full inventory drops the granted tool rather than deleting it;
- the tool itself is not added to Journey research.

Mode cycle remains:

`BLOCK -> CONTENTS -> AREA_16 -> BLOCK`

## 9. Help and Minecraft 1.7.10 command completion

1. Run `/journey help`.
2. Type `/journey ` and press Tab.
3. Try `/journey tr` + Tab.
4. Try `/journey trace ` + Tab.
5. Try `/journey clear ` + Tab and `/journey prune-missing ` + Tab.
6. Use up/down arrows after executing a few Journey commands.

Expected:

- help is short and literal, with one command on each line;
- Tab completes Journey subcommands using the native 1.7.10 command API;
- trace offers `on`/`off`;
- destructive commands offer `confirm`;
- up/down arrows remain normal vanilla command-history navigation;
- no modern Brigadier-style dropdown is expected on 1.7.10.

## 10. `/journey research` held-item refresh

1. Hold a valid ordinary item and run `/journey research`.
2. Run it a second time on the already researched state.
3. Try the command with an empty hand.

Expected:

- held stack is copied for research and the physical held item is not mutated;
- a missing state is added;
- an already existing semantic state still forces a full refresh/sync;
- empty hand produces a concise instruction instead of an error.

## 11. Retrieval and empty-container sanity

For representative ordinary, exact-NBT, electric endpoint, filled-fluid and empty-container states:

- J/N left click requests a full stack;
- J/N right click requests one;
- ordinary NEI Ctrl+left/Ctrl+right uses Journey only for researched entries.

Expected:

- server remains authoritative;
- display-only sanitization never contaminates stored retrieval templates;
- real filled fluid containers preserve their researched contents;
- inventory containers retrieve empty, without nested inventory duplication;
- successful Journey retrieval changes N activity ordering without changing J chronology.

## 12. Recovery smoke test

1. Create `/journey snapshot v1-smoke`.
2. Forget one known state.
3. Run undo, redo, then undo.
4. Restore the snapshot if useful.

Expected:

- exact key/template and J chronology survive recovery;
- conflicting undo/redo fails closed instead of overwriting a differently re-researched state;
- clear/prune destructive operations create SAFETY snapshots when there is state to protect;
- restoring a snapshot reconciles active/inactive deletion history consistently.

## 13. Final diagnostics

Generate `/journey dump` after the test.

Check at least:

- runtime version `1.0.0`;
- effective `scan=5t`;
- research count and server-only/unavailable counts;
- panel authoritative/semantic/visible counters;
- `panelIncrementalUpdates` and `fullNeiReloadRequests`;
- `presentationFailures`;
- furnace output counters if furnace tracking was exercised;
- recovery undo/redo/deletion/snapshot depths;
- deep/manual scan summary evidence.

Attach the final dump and `latest.log` to any bug report. They should make acquisition, server sync and final panel visibility distinguishable without guessing.
