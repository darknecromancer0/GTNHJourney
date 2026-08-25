# GTNH Journey pre7 Panel and Performance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `J` and `N` own a lightweight Journey list directly, newest-first, with every new unlock at the upper-left slot of page 1 and no full NEI item reload on ordinary research changes.

**Architecture:** Replace pre6's global `ItemInfo.itemVariants` injection and temporary `ItemSorter` comparator with a `JourneyPanelController`. While `J`/`N` is active, the controller builds a small client-only list from `ClientStackMirror`, creates safe presentation stacks mapped to authoritative `ResearchKey`s, applies active NEI filters, and writes the final ordered list through `ItemPanel.updateItemList(...)`. When Journey mode returns to `ALL`, ordinary NEI owns the panel again.

**Tech Stack:** Java 8-compatible source/bytecode via GTNH convention plugin, Forge/FML 1.7.10, NEI 2.8.111-GTNH, JUnit 5, Gradle 9.3.1/JDK 25 build environment.

**Spec:** `docs/superpowers/specs/2026-08-25-pre7-architecture-design.md`

## Global Constraints

- `J`: all researched states, newest -> oldest.
- `N`: newest `JourneyConfig.newestLimit()` states, newest -> oldest.
- A newly unlocked state is item index `0`, therefore upper-left on page 1.
- Incremental unlocks and J/N toggles must never call `ItemList.loadItems.restart()`.
- Server-synchronized research stacks remain authoritative; presentation-only NBT never becomes retrieval NBT.
- `ALL` mode must return cleanly to normal NEI behavior.
- Do not add a mixin/coremod solely for Journey ordering.

---

### Task 1: Make the chronology contract newest-first for both J and N

**Files:**
- Modify: `src/main/java/dev/gtnhjourney/nei/JourneyPanelOrder.java`
- Modify: `src/test/java/dev/gtnhjourney/Pre6RegressionContractTest.java`
- Create: `src/test/java/dev/gtnhjourney/nei/JourneyPanelOrderTest.java`

- [ ] **Step 1: Write the failing tests**

Add direct tests with `oldest, middle, newest` input asserting:

```java
assertEquals(
    Arrays.asList(newest, middle, oldest),
    JourneyPanelOrder.keysForMode(unlockOrder, JourneyViewState.Mode.RESEARCHED, 2));
assertEquals(
    Arrays.asList(newest, middle),
    JourneyPanelOrder.keysForMode(unlockOrder, JourneyViewState.Mode.NEWEST, 2));
```

Also update the pre6 contract test so it no longer encodes the rejected oldest-first J behavior.

- [ ] **Step 2: Run the focused tests and prove RED**

Run:

```bash
gradle test --tests dev.gtnhjourney.nei.JourneyPanelOrderTest --tests dev.gtnhjourney.Pre6RegressionContractTest
```

Expected: RESEARCHED-order assertion fails because pre6 currently returns oldest-first.

- [ ] **Step 3: Implement the minimal chronology change**

Change `JourneyPanelOrder.keysForMode(...)` so both Journey modes iterate the source from the newest end. `RESEARCHED` uses the whole reversed list; `NEWEST` stops at the configured limit. Preserve `ALL -> empty` because ALL is not Journey-owned.

- [ ] **Step 4: Re-run focused tests and prove GREEN**

Run the same command. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/dev/gtnhjourney/nei/JourneyPanelOrder.java src/test/java/dev/gtnhjourney/Pre6RegressionContractTest.java src/test/java/dev/gtnhjourney/nei/JourneyPanelOrderTest.java
git commit -m "fix: make Journey chronology newest first"
```

---

### Task 2: Turn presentation-key mapping into a complete Journey-panel ownership marker

**Files:**
- Modify: `src/main/java/dev/gtnhjourney/nei/JourneyPresentationKeyResolver.java`
- Modify: `src/main/java/dev/gtnhjourney/nei/JourneyPresentationSafety.java`
- Create: `src/test/java/dev/gtnhjourney/nei/JourneyPresentationKeyResolverTest.java`

- [ ] **Step 1: Write RED tests for presentation ownership**

Required public/package-private behavior:

```java
JourneyPresentationKeyResolver.register(display, authoritativeKey);
assertTrue(JourneyPresentationKeyResolver.isPresentation(display));
assertEquals(authoritativeKey, JourneyPresentationKeyResolver.keyOf(display));
JourneyPresentationKeyResolver.clear();
assertFalse(JourneyPresentationKeyResolver.isPresentation(display));
```

Also retain the existing flask regression: sanitizing a display copy must not mutate the authoritative source tag.

- [ ] **Step 2: Run focused tests and prove RED**

```bash
gradle test --tests dev.gtnhjourney.nei.JourneyPresentationKeyResolverTest --tests dev.gtnhjourney.Pre6RegressionContractTest
```

Expected: compilation/test failure because `isPresentation` does not exist yet.

- [ ] **Step 3: Implement the minimal marker API**

Add:

```java
static synchronized boolean isPresentation(ItemStack stack)
```

It returns true only when the identity map or `GTNHJourneyPresentation` marker resolves to a currently registered key. Keep markers client-only. Do not alter `JourneyPresentationSafety.forNei(...)` semantics except to ensure it always returns a defensive copy or `null`, never the authoritative object itself.

- [ ] **Step 4: Re-run focused tests and prove GREEN**

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/dev/gtnhjourney/nei/JourneyPresentationKeyResolver.java src/main/java/dev/gtnhjourney/nei/JourneyPresentationSafety.java src/test/java/dev/gtnhjourney/nei/JourneyPresentationKeyResolverTest.java
git commit -m "refactor: mark Journey panel presentation stacks"
```

---

### Task 3: Build the direct Journey panel controller

**Files:**
- Create: `src/main/java/dev/gtnhjourney/nei/JourneyPanelController.java`
- Create: `src/main/java/dev/gtnhjourney/nei/JourneyPanelSnapshot.java`
- Create: `src/test/java/dev/gtnhjourney/nei/JourneyPanelSnapshotTest.java`

**Interfaces:**

`JourneyPanelSnapshot` is a pure planner over authoritative keys:

```java
static List<ResearchKey> keys(
    List<ResearchKey> oldestFirst,
    JourneyViewState.Mode mode,
    int newestLimit)
```

`JourneyPanelController` owns runtime NEI integration:

```java
public static void refresh(boolean resetPage);
public static void ensureOwned();
public static void releaseToNei();
public static void clear();
```

- [ ] **Step 1: Write RED tests for the pure panel snapshot**

Cover:
- J returns every key newest-first.
- N returns the configured tail newest-first.
- newest key is index 0.
- ALL returns no Journey-owned list.
- duplicate/null input does not create duplicate/null output.

- [ ] **Step 2: Run tests and prove RED**

```bash
gradle test --tests dev.gtnhjourney.nei.JourneyPanelSnapshotTest
```

Expected: class missing.

- [ ] **Step 3: Implement `JourneyPanelSnapshot` minimally**

Delegate chronology to the corrected `JourneyPanelOrder` and return immutable deterministic output.

- [ ] **Step 4: Implement `JourneyPanelController`**

Runtime algorithm for `refresh(resetPage)`:

1. If mode is `ALL`, call `releaseToNei()` and return.
2. Read authoritative stacks in oldest-first order from `ClientStackMirror.snapshot()`.
3. Convert to newest-first according to mode/limit without using `ItemSorter`.
4. Before rebuilding, call `JourneyPresentationKeyResolver.clear()`.
5. For each authoritative stack:
   - resolve authoritative `ResearchKey` with `ItemStackKeyFactory.from(original)`;
   - call `JourneyPresentationSafety.forNei(original)`;
   - skip `null`/broken display only, never delete research;
   - register **every** Journey display stack with `JourneyPresentationKeyResolver.register(display, key)` so panel ownership is detectable even when the display NBT did not require sanitization;
   - apply `ItemList.getItemListFilter().matches(display)` inside a defensive try/catch;
   - append matching displays in the already-correct Journey order.
6. Call `ItemPanel.updateItemList(finalList)` directly.
7. When `resetPage` is true, call `ItemPanels.itemPanel.getGrid().setPage(0)` after list replacement.

`ensureOwned()` is deliberately cheap: while J/N is active, inspect `ItemPanels.itemPanel.realItems`; if the list is empty while Journey has visible research, or any current entry is not a registered Journey presentation, call `refresh(false)`. This lets Journey reclaim the panel one tick after NEI search/filter updates without rebuilding the global item universe.

`releaseToNei()` clears presentation mappings and invokes only:

```java
ItemList.updateFilter.restart();
```

Never call `loadItems.restart()` here.

- [ ] **Step 5: Add source-level regression contract for forbidden full reload**

Create a test that reads `JourneyPanelController.java` and `JourneyNEIRefreshTracker.java` as classpath/source resources or use a small pure `JourneyRefreshDecision` enum if source access is awkward. The tested contract must prove ordinary `RESEARCH_CHANGED`/`VIEW_CHANGED` paths choose `PANEL_REFRESH`/`NEI_FILTER_REFRESH`, never `FULL_NEI_RELOAD`.

Prefer the pure decision object:

```java
enum Action { PANEL_REFRESH, NEI_FILTER_REFRESH }
```

with no `FULL_NEI_RELOAD` action at all.

- [ ] **Step 6: Run focused tests**

```bash
gradle test --tests dev.gtnhjourney.nei.JourneyPanelSnapshotTest --tests dev.gtnhjourney.nei.JourneyRefreshDecisionTest
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/dev/gtnhjourney/nei/JourneyPanelController.java src/main/java/dev/gtnhjourney/nei/JourneyPanelSnapshot.java src/main/java/dev/gtnhjourney/nei/JourneyRefreshDecision.java src/test/java/dev/gtnhjourney/nei/JourneyPanelSnapshotTest.java src/test/java/dev/gtnhjourney/nei/JourneyRefreshDecisionTest.java
git commit -m "feat: give Journey direct ownership of J and N panels"
```

---

### Task 4: Replace pre6 NEI variant injection and sorter refresh flow

**Files:**
- Modify: `src/main/java/dev/gtnhjourney/nei/JourneyNEIRefreshTracker.java`
- Modify: `src/main/java/dev/gtnhjourney/nei/JourneyNEIToggleWidget.java`
- Modify: `src/main/java/dev/gtnhjourney/nei/JourneyItemFilterProvider.java`
- Modify: `src/main/java/dev/gtnhjourney/client/ClientConnectionTracker.java`
- Delete: `src/main/java/dev/gtnhjourney/nei/JourneyNEIOrderBridge.java`
- Delete: `src/main/java/dev/gtnhjourney/nei/JourneyNEIVariantBridge.java`
- Delete: `src/main/java/dev/gtnhjourney/nei/JourneyVariantScope.java`
- Modify: `src/test/java/dev/gtnhjourney/Pre6RegressionContractTest.java`
- Create: `src/test/java/dev/gtnhjourney/nei/JourneyRefreshFlowTest.java`

- [ ] **Step 1: Write RED tests for refresh decisions**

Assert:
- research revision change in J/N -> direct panel refresh with page reset;
- view change ALL -> normal NEI filter refresh only;
- view change into J/N -> direct panel refresh with page reset;
- stable J/N -> ownership check, not global rebuild;
- no action path contains full item loading.

Remove the obsolete pre6 assertion about `JourneyVariantScope.shouldInjectVariant(...)`; pre7 no longer injects research states into the global NEI universe.

- [ ] **Step 2: Run focused tests and prove RED**

```bash
gradle test --tests dev.gtnhjourney.nei.JourneyRefreshFlowTest --tests dev.gtnhjourney.Pre6RegressionContractTest
```

- [ ] **Step 3: Rewrite `JourneyNEIRefreshTracker`**

On each client END tick:
- if full research sync is active, do nothing;
- if J/N active and research/view revision changed, `JourneyPanelController.refresh(true)`;
- if J/N active and revisions stable, `JourneyPanelController.ensureOwned()`;
- if mode becomes ALL, `JourneyPanelController.releaseToNei()` once;
- do **not** call `SubsetWidget.updateHiddenItems()` merely because one research state was added;
- do **not** mutate `ItemInfo.itemVariants`;
- do **not** touch `ItemSorter.list`;
- do **not** call `ItemList.loadItems.restart()` or `ItemList.refreshItems.restart()` for Journey chronology.

Replace `clearInjectedVariants()` with a clearer lifecycle method such as:

```java
public static void resetJourneyPanel()
```

that clears controller state and returns panel ownership to normal NEI without a full item reload.

- [ ] **Step 4: Update toggle widget and Journey mode filter**

Buttons only change `JourneyViewState`; the tracker/controller performs the actual panel update on the next client tick. Remove the direct `ItemList.updateFilter.restart()` calls from both buttons.

While J/N owns the panel, `JourneyItemFilterProvider.getFilter()` must return an allow-all filter so the controller does not recursively filter its already-selected research list. In `ALL`, preserve allow-all behavior as before. Journey subset filters remain available as independent NEI subsets.

- [ ] **Step 5: Update disconnect cleanup**

`ClientConnectionTracker.resetClientSessionState()` calls `JourneyPanelController.clear()` / new tracker reset method instead of the deleted variant bridge.

- [ ] **Step 6: Delete obsolete bridges and compile**

Run:

```bash
gradle compileJava compileTestJava
```

Expected: PASS with no references to `JourneyNEIOrderBridge`, `JourneyNEIVariantBridge`, or `JourneyVariantScope`.

- [ ] **Step 7: Run focused and full tests**

```bash
gradle test
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add -A src/main/java/dev/gtnhjourney/nei src/main/java/dev/gtnhjourney/client/ClientConnectionTracker.java src/test/java/dev/gtnhjourney
git commit -m "perf: stop rebuilding NEI for Journey unlocks"
```

---

### Task 5: Add panel/performance diagnostics

**Files:**
- Create: `src/main/java/dev/gtnhjourney/diagnostics/JourneyRuntimeCounters.java`
- Modify: `src/main/java/dev/gtnhjourney/nei/JourneyPanelController.java`
- Modify: `src/main/java/dev/gtnhjourney/diagnostics/JourneyDiagnosticDump.java`
- Modify: `src/main/java/dev/gtnhjourney/client/ClientConnectionTracker.java`
- Create: `src/test/java/dev/gtnhjourney/diagnostics/JourneyRuntimeCountersTest.java`

- [ ] **Step 1: Write RED counter tests**

Required API:

```java
JourneyRuntimeCounters.reset();
JourneyRuntimeCounters.panelIncrementalUpdate();
JourneyRuntimeCounters.fullNeiReloadRequest();
assertEquals(1L, JourneyRuntimeCounters.snapshot().getPanelIncrementalUpdates());
assertEquals(1L, JourneyRuntimeCounters.snapshot().getFullNeiReloadRequests());
```

Counters are process/session diagnostics only, never persisted research data.

- [ ] **Step 2: Implement thread-safe counters**

Use synchronized longs or `AtomicLong`. Include fields already required by later plans:
- `panelIncrementalUpdates`
- `fullNeiReloadRequests`
- `unlockNotifications`
- `furnaceOutputObservations`
- `furnaceOutputUnlocks`

The panel controller increments `panelIncrementalUpdates` only when it actually replaces the Journey panel. `fullNeiReloadRequests` must remain zero for ordinary pre7 Journey activity; reserve the method only for any explicitly documented global-reload call site.

- [ ] **Step 3: Emit counters in `/journey dump`**

Add:

```text
== Runtime counters ==
panelIncrementalUpdates=...
fullNeiReloadRequests=...
unlockNotifications=...
furnaceOutputObservations=...
furnaceOutputUnlocks=...
```

Reset counters on client connection/session reset and server-stop lifecycle where applicable. In the integrated single-player target, client/server share the process so the dump exposes the intended live-test evidence.

- [ ] **Step 4: Run tests**

```bash
gradle test --tests dev.gtnhjourney.diagnostics.JourneyRuntimeCountersTest
gradle test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/dev/gtnhjourney/diagnostics/JourneyRuntimeCounters.java src/main/java/dev/gtnhjourney/diagnostics/JourneyDiagnosticDump.java src/main/java/dev/gtnhjourney/nei/JourneyPanelController.java src/main/java/dev/gtnhjourney/client/ClientConnectionTracker.java src/test/java/dev/gtnhjourney/diagnostics/JourneyRuntimeCountersTest.java
git commit -m "diagnostics: measure Journey fast panel path"
```

---

### Task 6: Verify panel phase before moving to acquisition work

**Files:** no production changes expected.

- [ ] **Step 1: Apply formatting**

```bash
gradle spotlessApply
```

- [ ] **Step 2: Run complete verification**

```bash
gradle test build --stacktrace
```

Expected: all JUnit tests, checkstyle/format checks, Forge compilation/reobf and jar build pass.

- [ ] **Step 3: Static forbidden-call audit**

```bash
grep -R "ItemList\.loadItems\.restart" -n src/main/java/dev/gtnhjourney || true
grep -R "JourneyNEIOrderBridge\|JourneyNEIVariantBridge\|JourneyVariantScope" -n src/main/java src/test/java || true
```

Expected:
- no ordinary Journey unlock/panel code calls `ItemList.loadItems.restart()`;
- no obsolete bridge references remain.

- [ ] **Step 4: Record the verified phase commit if formatting changed files**

```bash
git add -A
git commit -m "test: verify pre7 Journey panel fast path"
```

If `git status --short` is empty, do not create an empty commit.
