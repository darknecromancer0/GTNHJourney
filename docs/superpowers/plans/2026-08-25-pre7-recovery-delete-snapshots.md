# GTNH Journey pre7 Recovery, Delete Mode, and Snapshots Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add persistent transaction-aware Journey recovery with D delete mode, undo/redo, deleted-state restore, and bounded automatic/manual snapshots.

**Architecture:** Keep `JourneyResearchData` authoritative. Add separate world-saved recovery/snapshot stores and one transaction-aware mutation facade that is the only write path for explicit destructive/recovery actions. Passive gameplay research stays outside undo history but invalidates redo after divergence.

**Tech Stack:** Java 8 source level on Forge 1.7.10 / GTNH 2.9.0-beta-2, JUnit 4, NEI 2.8.111-GTNH, Forge `WorldSavedData`, existing Journey direct panel/network infrastructure.

**Spec:** `docs/superpowers/specs/2026-08-25-pre7-delete-history-snapshots-design.md`

## Global Constraints

- J, N and D are newest-first; newest state is top-left on page 1.
- D deletes one exact displayed research state only.
- Undo and redo stacks are persistent and bounded to 100 transactions each.
- Delete history is persistent and bounded to 1000 records.
- AUTO + SAFETY snapshots share one ring of 20; manual snapshots are bounded to 10.
- Automatic snapshot cadence is 2400 server ticks and only runs when research changed.
- No recovery action may call global NEI item-list reload APIs.
- Server remains authoritative for all mutation requests.

---

### Task 1: Exact research entry snapshots and ordered mutation primitives

**Files:**
- Create: `src/main/java/dev/gtnhjourney/recovery/ResearchEntrySnapshot.java`
- Create: `src/main/java/dev/gtnhjourney/recovery/ResearchStateSnapshot.java`
- Modify: `src/main/java/dev/gtnhjourney/persistence/JourneyResearchData.java`
- Modify: `src/main/java/dev/gtnhjourney/persistence/PlayerResearchService.java`
- Test: `src/test/java/dev/gtnhjourney/recovery/ResearchEntrySnapshotTest.java`
- Test: `src/test/java/dev/gtnhjourney/recovery/OrderedResearchMutationTest.java`

**Interfaces:**
- Produce `ResearchEntrySnapshot(ResearchKey key, NBTTagCompound template, int timelineIndex)` with defensive copies.
- Produce `PlayerResearchService.captureState(player)` returning an ordered `ResearchStateSnapshot`.
- Produce exact low-level restore/remove helpers used only by the recovery facade.

- [ ] Write RED tests proving entry NBT is copied, timeline index survives round-trip, removing/restoring an entry preserves exact order, and duplicate restore is idempotent.
- [ ] Run focused tests and verify RED from missing recovery types/APIs.
- [ ] Implement minimal snapshot classes plus package-bounded ordered insert/remove operations in `JourneyResearchData`.
- [ ] Expose them through `PlayerResearchService` without changing passive `unlockStates` semantics.
- [ ] Run focused tests and full `gradle test build --stacktrace`.
- [ ] Commit as `feat: add exact research recovery snapshots`.

### Task 2: Transaction model and persistent undo/redo store

**Files:**
- Create: `src/main/java/dev/gtnhjourney/recovery/ResearchTransaction.java`
- Create: `src/main/java/dev/gtnhjourney/recovery/ResearchTransactionResult.java`
- Create: `src/main/java/dev/gtnhjourney/persistence/JourneyRecoveryData.java`
- Test: `src/test/java/dev/gtnhjourney/recovery/ResearchTransactionTest.java`
- Test: `src/test/java/dev/gtnhjourney/persistence/JourneyRecoveryDataTest.java`

**Interfaces:**
- `ResearchTransaction` stores id, timestamp, description, ordered added/removed entry snapshots, and delete-record state changes.
- `JourneyRecoveryData.pushUndo(UUID, ResearchTransaction)`, `popUndo`, `pushRedo`, `popRedo`, `clearRedo`, `undoDepth`, `redoDepth`.
- Hard bounds: 100 undo and 100 redo.

- [ ] Write RED tests for forward/reverse delta semantics, stack order, NBT persistence round-trip, 100-item ring trimming, and redo clearing on new mutation.
- [ ] Implement immutable transaction value objects.
- [ ] Implement `WorldSavedData` persistence in `JourneyRecoveryData`, separate from `JourneyResearchData`.
- [ ] Run focused/full tests.
- [ ] Commit as `feat: persist Journey undo redo transactions`.

### Task 3: Transaction-aware explicit mutation facade

**Files:**
- Create: `src/main/java/dev/gtnhjourney/recovery/JourneyMutationService.java`
- Modify: `src/main/java/dev/gtnhjourney/acquisition/ResearchObservationService.java`
- Modify: `src/main/java/dev/gtnhjourney/GTNHJourney.java`
- Test: `src/test/java/dev/gtnhjourney/recovery/JourneyMutationServiceTest.java`

**Interfaces:**
- `deleteExact(player, key, description)` records one removed entry transaction.
- `applyBulkAdd(player, observedStacks, description)` records only newly added states as one transaction.
- `undo(player, count)` and `redo(player, count)` return batch summaries.
- `notePassiveMutation(player)` clears redo only when a passive acquisition actually changes research.

- [ ] Write RED tests for delete -> undo -> redo, bulk-add as one transaction, zero-change operation creating no transaction, and passive mutation invalidating redo.
- [ ] Implement the facade using exact snapshots from Task 1 and persistence from Task 2.
- [ ] Wire passive `ResearchObservationService` to call `notePassiveMutation` only when `unlockStates` returned new endpoints.
- [ ] Run focused/full tests.
- [ ] Commit as `feat: centralize explicit Journey mutations`.

### Task 4: Persistent delete history and restore-deleted

**Files:**
- Create: `src/main/java/dev/gtnhjourney/recovery/DeletionRecord.java`
- Modify: `src/main/java/dev/gtnhjourney/persistence/JourneyRecoveryData.java`
- Modify: `src/main/java/dev/gtnhjourney/recovery/JourneyMutationService.java`
- Test: `src/test/java/dev/gtnhjourney/recovery/DeletionHistoryTest.java`

**Interfaces:**
- D/forget deletion appends an active deletion record.
- `restoreDeleted(player, n)` selects newest active records and records one undoable transaction.
- Natural reacquisition deactivates newest matching active deletion record.
- Hard bound: 1000 records, evict oldest inactive first.

- [ ] Write RED tests for selection order, active/inactive transitions through undo/redo, natural reacquisition, and trimming policy.
- [ ] Implement deletion record persistence and mutation-service integration.
- [ ] Run focused/full tests.
- [ ] Commit as `feat: add deleted research recovery history`.

### Task 5: Snapshot persistence and 2400-tick auto/safety policy

**Files:**
- Create: `src/main/java/dev/gtnhjourney/recovery/JourneySnapshot.java`
- Create: `src/main/java/dev/gtnhjourney/recovery/SnapshotKind.java`
- Create: `src/main/java/dev/gtnhjourney/persistence/JourneySnapshotData.java`
- Create: `src/main/java/dev/gtnhjourney/recovery/JourneySnapshotService.java`
- Test: `src/test/java/dev/gtnhjourney/recovery/JourneySnapshotServiceTest.java`
- Test: `src/test/java/dev/gtnhjourney/persistence/JourneySnapshotDataTest.java`

**Interfaces:**
- Snapshot contains ordered research entries only, never undo/redo/snapshot metadata recursively.
- AUTO and SAFETY share a 20-item ring; MANUAL has separate 10-item ring.
- Auto eligibility: changed since last snapshot, >=2400 server ticks, state loaded, suspicious-drop guard passes.
- Suspicious-drop guard: if last good snapshot has >=100 states and current <25% of it, skip auto and increment diagnostic counter.

- [ ] Write RED tests for rings, cadence, unchanged-state suppression, suspicious-drop guard, manual override, and NBT round-trip.
- [ ] Implement snapshot persistence/service.
- [ ] Run focused/full tests.
- [ ] Commit as `feat: add bounded Journey snapshots`.

### Task 6: Snapshot restore as one undoable transaction

**Files:**
- Modify: `src/main/java/dev/gtnhjourney/recovery/JourneyMutationService.java`
- Modify: `src/main/java/dev/gtnhjourney/recovery/JourneySnapshotService.java`
- Test: `src/test/java/dev/gtnhjourney/recovery/SnapshotRestoreTransactionTest.java`

**Interfaces:**
- `restoreSnapshot(player, idOrName)` validates target first, creates a safety snapshot, applies full replacement, reconciles deletion flags, pushes one undo transaction, clears redo.

- [ ] Write RED test proving restore -> undo returns exact pre-restore state and redo reapplies exact target state including order/templates.
- [ ] Implement validation-before-replacement and full-replacement transaction.
- [ ] Run focused/full tests.
- [ ] Commit as `feat: make snapshot restore undoable`.

### Task 7: D panel mode and server-authoritative exact deletion

**Files:**
- Modify: `src/main/java/dev/gtnhjourney/nei/JourneyViewState.java`
- Modify: `src/main/java/dev/gtnhjourney/nei/JourneyPanelController.java`
- Modify: existing NEI button/click handler classes discovered in branch
- Modify/Create network request for exact delete
- Test: `src/test/java/dev/gtnhjourney/nei/JourneyDeleteModePolicyTest.java`
- Test: network handler policy test

**Interfaces:**
- Add `D` after J/N.
- D list = all researched states newest-first.
- Left-click in D sends delete request and suppresses give/retrieve action.
- Shift/right click does not mass-delete in pre7.

- [ ] Write RED pure tests for J/N/D transitions and D click routing.
- [ ] Add incremental server-validated removal packet and client mirror removal.
- [ ] Implement D button/click path with no global NEI reload.
- [ ] Run focused/full tests.
- [ ] Commit as `feat: add Journey delete mode`.

### Task 8: Commands and user-facing recovery workflow

**Files:**
- Modify: `src/main/java/dev/gtnhjourney/command/CommandJourney.java`
- Modify: `src/main/java/dev/gtnhjourney/GTNHJourney.java`
- Test: command parsing/policy tests

**Interfaces:**
- `/journey undo [N]`, `/journey redo [N]`, N=1 default, clamp 1..100.
- `/journey restore-deleted <N>`, clamp 1..1000.
- `/journey snapshot [name]`, `/journey snapshots`, `/journey restore <id-or-name>`.
- Existing `forget`, `clear`, `prune-missing` route through `JourneyMutationService`.

- [ ] Write RED tests for parsing/clamps and command routing.
- [ ] Replace old single-backup undo behavior with transaction service.
- [ ] Add snapshot commands and one-sync-per-bulk-action behavior.
- [ ] Run focused/full tests.
- [ ] Commit as `feat: expose Journey recovery commands`.

### Task 9: Automatic snapshot ticker, diagnostics, migration from data v7

**Files:**
- Modify: `src/main/java/dev/gtnhjourney/GTNHJourney.java`
- Modify: `src/main/java/dev/gtnhjourney/diagnostics/JourneyDiagnosticDump.java`
- Modify: `src/main/java/dev/gtnhjourney/command/CommandJourney.java`
- Modify: persistence version/migration code as needed
- Test: recovery migration and diagnostics tests

- [ ] Write RED tests that old v7 research loads without data loss and recovery stores begin empty/coherent.
- [ ] Register server-tick snapshot scheduler.
- [ ] Add stats/dump fields: undo, redo, deletion records/active count, snapshot counts/newest, skipped suspicious snapshots, last transaction.
- [ ] Remove/deprecate old single `undoBackups` persistence only after migration test proves no research loss.
- [ ] Run `gradle test build --stacktrace` and static search proving no global NEI reload calls in recovery code.
- [ ] Commit as `feat: complete pre7 recovery infrastructure`.
