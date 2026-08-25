# GTNH Journey 0.1.0-pre7 Architecture Design

## Goal

Make Journey reliable and responsive enough for broader live testing by separating Journey's research view from NEI's global item-list rebuild pipeline, moving acquisition toward event-first detection, and preserving exact server research states while using safe client display states.

## Confirmed user-visible behavior

- `J` shows all researched states, newest first.
- `N` shows only the newest configured window, newest first.
- A newly researched item appears at index 0, the upper-left slot of page 1.
- New unlocks do not trigger a full NEI item reload.
- Journey/NEI integration remains usable in Creative mode.
- Filled fluid containers and electric-item endpoint states remain researchable and visible.
- Wearing armor must not create duplicate research merely because equip/runtime NBT changed.
- A furnace output may unlock while the GUI is closed; ownership follows the last player who used/opened that furnace. In the current target use case there is one player.
- Incremental unlocks show a short notification such as `Unlocked: Universal Fluid Cell`; login/full sync does not replay notifications.

## 1. Journey panel architecture

### Problem

pre6 injects Journey-owned variants into `ItemInfo.itemVariants` and calls `ItemList.loadItems.restart()` when the variant universe changes. NEI's full item-loading pipeline then regathers all mod items, rebuilds caches, reindexes collapsible groups, sorts, and refilters. This is the likely source of the visible freeze on each unlock.

The pre6 chronology bridge also relies on temporarily inserting a Journey comparator into `ItemSorter.list`. NEI may later reconstruct that list from config, and `updateFilter` ultimately orders filtered entries by NEI's internal ordering map. Therefore Journey cannot treat NEI's global sorter as authoritative for J/N chronology.

### Design

Add a `JourneyPanelController` as the owner of J/N panel contents.

When mode is `RESEARCHED` or `NEWEST`, the controller:

1. Reads authoritative synchronized stacks from `ClientStackMirror`.
2. Orders them newest-first.
3. Limits the list for `NEWEST` after ordering.
4. Converts each research stack to a safe presentation stack while retaining a mapping back to the original `ResearchKey`.
5. Applies the active NEI item/search filters to this small Journey list, while Journey's own mode filter becomes a no-op when the controller owns the panel so filtering is not recursive.
6. Calls `ItemPanel.updateItemList(...)` directly with the final ordered list.

When a new unlock arrives while J/N is active:

- rebuild or incrementally update only the small Journey list;
- set the panel to page 0;
- do not call `ItemList.loadItems.restart()`.

When mode returns to `ALL`, Journey relinquishes panel ownership and requests the normal NEI filter/update path to restore the ordinary item list.

### Ordering contract

- `J`: all research, newest -> oldest.
- `N`: newest `newestLimit` research states, newest -> oldest.
- newest item is always item index 0 and therefore the upper-left slot on page 1.

## 2. Research acquisition architecture

### Event-first model

Acquisition is split into two layers:

- Event-first observers for known actions and containers.
- A slower inventory reconciliation scan as a safety net.

Existing crafting, smelting-pickup, login, and inventory scan paths continue to feed one shared research unlock service. Additional event hooks reuse that service rather than create alternate persistence paths.

### Inventory fallback

The inventory scanner remains authoritative for player-owned inventory surfaces but becomes reconciliation for missed events and unusual mod interactions rather than the primary trigger for every unlock.

The pre7 default incremental scan interval is 20 ticks (1 second). The existing slower forced full-rescan concept remains available as a separate safety net. Both remain configurable.

### FurnaceOwnershipTracker

Add a tracker for vanilla/Forge furnace tile entities the player has actually interacted with.

- On opening/using a furnace, record the player's UUID as `lastUser` for that furnace position/dimension.
- Track only furnaces that have a known last user; do not scan every loaded furnace in the world.
- Poll only the tracked furnaces for a meaningful output-slot transition and send the resulting output stack through the normal research unlock service for that `lastUser`, even if the GUI is closed.
- Remove tracking when the relevant world/chunk/session is unloaded or the tile is invalidated.
- Avoid repeated unlock attempts for an unchanged output stack by caching an output signature.

For multiplayer ambiguity, last-user ownership is the defined rule. For the current single-player use case this is effectively deterministic.

## 3. Research state versus display state

### Research state

The server-side research key/template remains the source of truth.

- Persistence and retrieval use the authoritative normalized research state.
- Filled containers keep their meaningful fluid NBT.
- IC2/GT/CoFH/etc. electric items keep the existing endpoint semantics: transient intermediate charge does not create unbounded states, while meaningful BASE/FULL endpoints remain available according to each policy.
- Client presentation never rewrites the saved server template.

### Display state

Introduce/extend a presentation layer that creates a client-only display stack from an authoritative research state.

- A display stack may remove or replace renderer-hostile transient data.
- It retains an internal mapping back to the original `ResearchKey` so clicking/retrieval requests the real state.
- Display-only marker data never reaches the server-issued item.
- If no safe display can be produced, keep the research state persisted and retrievable but omit that state from the Journey panel rather than crash the client.

### Filled fluid containers

Filled `IC2:itemFluidCell`, GregTech cells/flasks, and analogous containers do not collapse to the empty container merely because the underlying item/meta is shared. Fluid identity/content is meaningful research data unless a specific semantic policy proves otherwise.

### IC2 drills

Existing pre6 saved Drill/Diamond Drill/Iridium Drill states remain valid. Their absence from the panel is treated as a presentation problem, not a persistence reset. pre7 panel construction uses the Journey research mirror directly, so visibility no longer depends on native NEI permutations.

## 4. Armor and wearable normalization

Wearing an item must not create a duplicate research state solely because a mod writes transient equip/runtime fields.

Add narrowly-scoped normalization policies only for fields proven to be transient for a particular supported item/API family. Examples seen in live data include equip flags and runtime shield/protection state.

Rules:

- Do not globally strip unknown NBT from all armor.
- Preserve material, upgrades, enchantments, configuration and other persistent identity-bearing data.
- Normalize only fields with verified transient semantics.
- Existing duplicated pre6 entries are migrated/deduplicated only when the new canonical key proves they represent the same logical state.

## 5. Creative mode

Journey panel mode and client research mirror are gamemode-independent.

- Switching Survival <-> Creative does not disable J/N or clear Journey client state.
- Normal NEI cheat/retrieval permissions remain governed by their existing logic.
- Journey does not depend on a Survival-only GUI/container class to remain active.

The reported GregTech `ItemVolumetricFlask` crash in `GuiContainerCreative` is not treated as a Journey-owned renderer bug. Journey protects its own presentation stacks, but pre7 does not patch the global GregTech Creative renderer.

## 6. Unlock notifications

Use incremental research-unlock messages as the notification boundary.

- On a truly new incremental unlock, show one concise notification using the item's display name.
- Do not emit notifications for login/full snapshot synchronization or migration.
- If one observation expands into multiple technical semantic endpoints, coalesce notifications into one message for that logical acquisition so BASE/FULL does not spam.
- Initial pre7 presentation is chat text: `Unlocked: <display name>`.
- Notification production is isolated behind a small client notification service so a HUD/toast can replace chat later without changing acquisition/network code.

## 7. Performance rules

The following are hard performance contracts for pre7:

- Incremental unlock does not call `ItemList.loadItems.restart()`.
- J/N mode changes and incremental research changes operate on the small Journey list, not the global mod item universe.
- Full NEI reload remains allowed for genuine connection/global-resource/item-universe changes, not normal research events.
- Expensive subset/global-cache allocation is not restarted merely because one research state was added.

Add diagnostics counters for at least:

- `panelIncrementalUpdates`
- `fullNeiReloadRequests`
- `unlockNotifications`
- `furnaceOutputObservations`
- `furnaceOutputUnlocks`

This lets the next live dump verify that the new fast path is actually being used.

## 8. Persistence and migration

pre7 preserves existing pre6 research wherever a valid item/template can still be resolved.

Migration requirements:

- Do not delete existing IC2 drill states.
- Do not delete filled fluid-cell states.
- Recanonicalize only where a newly verified semantic normalization requires it.
- When multiple old keys collapse to one new canonical state, preserve the earliest timeline position among the collapsed keys. This represents the first time the logical item was researched and gives deterministic chronology.
- Keep one normalized valid template for the surviving key; prefer the template from that earliest entry unless it is invalid, then use the first later valid normalized template.
- Migration marks data dirty once and does not create unlock notifications.

## 9. Failure handling

- Optional-mod/API integration failures remain fail-closed to exact state where safe.
- A broken display state never deletes the underlying research state.
- A presentation failure is diagnostic and non-fatal.
- A furnace tracker losing ownership/state falls back to normal acquisition when the item eventually enters the player's inventory.
- Research observation failures remain logged through the existing diagnostics path without crashing gameplay.

## 10. Test plan

### Unit/regression tests

Add tests for:

- `J` newest-first order.
- `N` newest-first order with configured limit.
- newest unlock becomes index 0.
- panel incremental update does not request full NEI reload.
- active NEI search/filter still constrains J/N without changing Journey chronology among matching entries.
- IC2 drill BASE/FULL states survive and appear in Journey panel input.
- filled fluid containers retain fluid NBT as distinct research states.
- display-state mapping resolves back to the authoritative `ResearchKey`.
- unsafe display fallback omits/neutralizes presentation without modifying retrieval template.
- armor transient fields normalize without erasing persistent upgrades/enchantments.
- furnace last-user ownership and output-signature deduplication.
- Creative-mode transitions do not reset Journey mode/mirror.
- incremental unlock notifications are emitted once; full sync emits none.
- pre6 -> pre7 migration preserves drills and filled cells and deterministically deduplicates newly normalized states while keeping the earliest logical chronology.

### Live test checklist

In GTNH 2.9.0-beta-2:

1. Unlock several items and verify each newest item appears in the upper-left slot of J and N.
2. Confirm J shows all research and N shows only the configured recent window.
3. Observe no visible full-NEI freeze on ordinary unlocks.
4. Verify IC2 Drill, Diamond Drill and Iridium Drill visibility/retrieval.
5. Verify filled Universal Fluid Cells remain distinct and retrievable with contents.
6. Smelt an unresearched output, close the furnace GUI, and verify automatic unlock when the furnace completes.
7. Equip/unequip representative armor and confirm no duplicate Journey state appears from transient equip NBT.
8. Switch Survival/Creative repeatedly and verify Journey remains functional.
9. Verify one concise `Unlocked: <name>` notification for new acquisitions and no login spam.
10. Generate `/journey dump` and inspect the new performance/acquisition counters.

## Non-goals for pre7

- Patching GregTech's global Creative Inventory renderer for broken fluid icons.
- Broad unknown-NBT stripping.
- Replacing NEI itself or introducing a coremod/mixin dependency solely for Journey sorting.
- Multiplayer team-shared research semantics beyond the defined furnace last-user rule.
