# GTNH Journey 0.1.0-pre6 live-test matrix

Target: GTNH `2.9.0-beta-2`, NEI `2.8.111-GTNH`.

Use the production `gtnhjourney-0.1.0-pre6.jar`. Keep the world backup from before the test. Do not delete the existing Journey research data unless a test explicitly needs a fresh state.

## 1. Startup / baseline

1. Replace the previous Journey jar with pre6 and launch the same test world.
2. Confirm the main menu/mod list reports `GTNH Journey 0.1.0-pre6`.
3. Enter the world and open ordinary NEI without enabling Journey.

Expected:
- no startup/client crash;
- ordinary NEI ordering is unchanged while Journey mode is off;
- no Journey-only presentation variants leak into the normal item list.

## 2. J chronology

Prepare at least five researched states with a known acquisition order. Prefer visibly distinct items and include one exact-NBT state.

1. Press `J`.
2. Compare the panel from top-left onward with the order in which those states were first researched.
3. Toggle `J` off and on again.

Expected:
- researched states are ordered oldest-first;
- the order is stable after toggling;
- NEI search still filters the Journey view without scrambling chronology.

## 3. N chronology

1. With the same catalog, press `N`.
2. Inspect the first several entries.

Expected:
- newest researched state is first;
- following entries go backwards through acquisition history;
- only the configured newest tail is present.

## 4. IC2 Drill / missing BASE state

Test both an IC2 Mining Drill and Diamond Drill if available.

1. Obtain/use a partially charged drill so Journey researches its BASE endpoint.
2. Enable `J` and search for the drill.
3. Retrieve the BASE state from Journey.
4. If possible, also research a fully charged endpoint and verify both states are addressable.

Expected:
- the BASE drill appears even when NEI has no identical native item+meta state;
- no duplicate BASE entry is added when NEI already provides the exact state;
- retrieval produces the researched endpoint rather than a neighboring IC2 variant.

## 5. Tinkers Construct tool preservation

Use a TCon tool with a visible level/progression state and at least one modifier.

1. Note its `ToolLevel`, XP/progress and modifier behavior before retrieval.
2. Research it, damage/use it, then retrieve it from Journey.
3. For a world previously tested with pre5, also retrieve an old pre5-researched TCon tool whose saved template may have lost XP counters.

Expected:
- durability/render-broken wear is reset;
- `ToolLevel`, materials and modifiers survive;
- valid existing `ToolEXP`, `HeadEXP` and modifier payload are not removed;
- a legacy pre5 template missing required XP counters loads as zero progress instead of remaining structurally broken.

## 6. Volumetric Flask renderer safety

Use the exact flask/fluid combination that previously crashed or corrupted NEI rendering, if available.

1. Research the filled volumetric flask.
2. Enable `J`/`N` and make the flask visible in the item panel.
3. Hover it, page/search around NEI, then retrieve it.

Expected:
- NEI can render/hover the entry without a client crash;
- the Journey panel entry remains distinct instead of collapsing into the ordinary empty flask;
- the retrieved server item contains the original researched fluid/NBT, not the client-only sanitized presentation data.

## 7. Furnace acquisition

1. Smelt an item that is not already researched.
2. Shift-click or otherwise merge the output into inventory immediately.
3. Open Journey without manually selecting/moving that resulting stack first.

Expected:
- the smelted result is researched immediately;
- shift-click/merge does not require a later cached inventory scan or manual slot interaction.

## 8. Retrieval sanity

For representative BASE, exact-NBT and FULL endpoint items:

- Journey view left click: request full stack;
- Journey view right click: request one;
- ordinary NEI Ctrl+left / Ctrl+right: same behavior only for researched entries.

Expected:
- server remains authoritative;
- presentation-only NBT never appears on retrieved items;
- unrelated/unresearched NEI entries cannot be spawned through Journey.

## Diagnostics to attach on failure

If any case fails, attach:

- latest client log;
- dedicated/integrated server log if separate;
- `/journey dump` output;
- `/journey inspect` while holding the affected stack;
- exact test case number above and what happened versus expected.

Do not clear research before collecting the dump/inspect data, because the persisted key/template is often the most useful evidence.
