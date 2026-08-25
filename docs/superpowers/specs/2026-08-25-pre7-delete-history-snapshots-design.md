# GTNH Journey pre7 Delete Mode, Undo/Redo, and Snapshots Design

## Status

Approved in chat for implementation as part of the pre7 migration/recovery work.

## Goal

Add a safe recovery layer for a migrated long-lived world: interactive deletion from Journey, reversible explicit mutations, persistent deleted-item recovery, and small rotating snapshots.

The system must make destructive operations easy to undo without turning normal passive research acquisition into a confusing editor-style history.

## Journey panel mode D

Add a third Journey button after J and N:

`J  N  D`

Behavior:

- J: all researched states, newest-first.
- N: newest subset, newest-first.
- D: delete mode over all researched states, newest-first.
- In every mode, the newest state is in the top-left slot of page 1.

D is a destructive interaction mode, not a normal NEI cheat/get view.

While D is active:

- clicking a displayed research state deletes exactly that displayed research state;
- the click must not retrieve/cheat/give the item;
- the delete request is validated server-side against the player's authoritative research registry;
- the deleted state disappears from the direct Journey panel immediately after server confirmation;
- no full NEI item-list reload occurs.

Pre7 deliberately does **not** assign Shift+click to family-wide deletion. Exact-state deletion is safer for a migrated save and avoids accidental removal of all charge/fluid/NBT variants.

## Explicit mutation model

The recovery system tracks **explicit Journey mutations** as transactions.

Transactions include:

- D-mode deletion;
- `/journey forget ...`;
- `/journey clear confirm`;
- `/journey prune-missing confirm`;
- Debug Researcher Tool BLOCK/CONTENTS/AREA_16 imports;
- `/journey restore-deleted N`;
- snapshot restore;
- future explicit migration/recovery commands that opt into the transaction API.

Normal passive gameplay research such as pickup, crafting, smelting, inventory reconciliation, or ordinary `Unlocked:` events is **not** pushed onto the undo stack. However, any new research mutation after an undo invalidates the redo branch, matching standard editor semantics and preventing redo from applying to a state that has diverged.

## Transaction representation

Do not store a full copy of the entire research database for every ordinary action.

Use a compact transaction delta containing:

- transaction id;
- timestamp;
- human-readable type/description;
- `added` entry snapshots;
- `removed` entry snapshots;
- each entry snapshot contains ResearchKey, retrieval template NBT, and the timeline position needed to restore ordering;
- optional delete-history state changes needed by `/journey restore-deleted`.

Forward application removes `removed` entries then inserts `added` entries at their recorded positions.

Reverse application removes `added` entries then restores `removed` entries at their recorded positions.

For a snapshot restore, where many common keys may also change order, it is acceptable for the transaction to represent the operation as a full replacement delta: all current entries are `removed`, all target entries are `added`. Snapshot restore is rare and correctness is more important than minimizing that one transaction.

Transaction application must be idempotent enough to skip entries already in the desired state rather than corrupting unrelated research.

## Undo

Command:

`/journey undo [N]`

- Default N = 1.
- N is clamped to a safe bounded maximum, recommended 100 per command.
- Applies reverse transactions newest-first.
- Moves each successfully reversed transaction to the redo stack.
- One Debug Researcher Tool AREA_16 scan is one transaction even if it added hundreds of states.
- One `/journey restore-deleted 20` is one transaction even if it restored 20 states.
- One snapshot restore is one transaction.

After the command, perform one authoritative Journey research sync and one direct-panel refresh.

Example:

`[Journey] Undo: Migration AREA_16 (-63 states)`

## Redo

Command:

`/journey redo [N]`

- Default N = 1.
- Reapplies the most recently undone transactions in correct order.
- Moves each successfully replayed transaction back to the undo stack.
- A new research mutation after undo clears the redo stack.
- Redo must reverse the consequences of undo for D deletions, migration scans, restore-deleted, clear/prune, and snapshot restore.

Example:

`[Journey] Redo: Migration AREA_16 (+63 states)`

## Persistent delete history

Keep a separate persistent deletion history so users can recover recently deleted research even if other explicit actions happened afterward.

Each deletion record stores:

- unique deletion record id;
- timestamp;
- deleted entry snapshot;
- whether that deletion is currently active (the research state is still absent because of that deletion).

D-mode deletion and `/journey forget` append deletion records.

If the same research state is naturally reacquired later, the newest matching active deletion record becomes inactive so `/journey restore-deleted` does not try to restore an item that is already present.

Keep at most the newest 1000 deletion records. When trimming, evict the oldest inactive records first, then the oldest active records only if the hard limit still requires it.

## Restore deleted

Command:

`/journey restore-deleted <N>`

- Restores up to N newest **active** deletion records.
- N is clamped to 1..1000.
- Restores exact ResearchKey + template + timeline placement where possible.
- Marks the selected deletion records inactive.
- Records the whole command as one undoable transaction.

Undoing that transaction deletes those restored states again and marks those deletion records active again.

Redoing it restores the same states and marks the records inactive again.

This is separate from snapshot restore, avoiding command ambiguity.

## Snapshot system

Snapshots are a second recovery layer, independent of undo/redo.

A snapshot contains only the authoritative Journey research state required for exact restoration:

- ordered research keys;
- retrieval template NBT for each key;
- timeline order.

Snapshots do **not** embed the undo stack, redo stack, or snapshot list recursively.

After snapshot restoration, delete-history active/inactive flags are reconciled against the restored research set: a deletion record for a currently present key is inactive; one for an absent key remains active.

## Automatic snapshots

Create an automatic snapshot approximately every 120 seconds when all of the following are true:

- the server/world research data is fully loaded;
- Journey research changed since the previous automatic snapshot;
- the current state passes the suspicious-drop safety check.

Keep the newest **20 automatic snapshots** in a ring.

Automatic snapshots use generated names containing local world/server time or a monotonic index.

### Suspicious-drop guard

Do not let a corruption event immediately overwrite the good rotating history with nearly empty snapshots.

If the most recent good snapshot has at least 100 states and the current research count is below 25% of that snapshot, skip the automatic snapshot and emit a diagnostic warning.

Explicit manual snapshots are still allowed because the user deliberately requested them.

## Manual snapshots

Command:

`/journey snapshot [name]`

- Without a name, generate a timestamp/index name.
- With a name, sanitize it to a conservative filename/display identifier.
- Keep at most **10 manual snapshots**.
- If a new manual snapshot exceeds the limit, evict the oldest manual snapshot.

Command:

`/journey snapshots`

Lists manual and automatic snapshots with id/name, age/time, and research-state count.

## Safety snapshots

Before potentially destructive bulk actions, create a safety snapshot if the research state is non-empty:

- `clear`;
- `prune-missing`;
- snapshot restore;
- Debug Researcher Tool AREA_16 scan;
- future bulk-delete operations.

Safety snapshots use the same rotating automatic/safety storage budget rather than creating an unbounded third archive.

## Snapshot restore

Command:

`/journey restore <snapshot-id-or-name>`

Behavior:

1. Resolve an exact manual/automatic snapshot.
2. Create a safety snapshot of the current state first.
3. Build one explicit full-replacement transaction from current state to target state.
4. Apply the target snapshot authoritatively.
5. Reconcile delete-history active flags.
6. Clear any old redo branch and push the restore transaction onto undo.
7. Perform one Journey full research sync and one direct-panel refresh.

Therefore:

- `/journey undo` after restore returns to the exact state immediately before restore;
- `/journey redo` reapplies the restored snapshot.

## Persistence boundaries

Keep recovery metadata separate from primary research data so recovery corruption cannot directly destroy the authoritative registry.

Recommended storage split:

- existing `JourneyResearchData`: authoritative current research;
- new `JourneyRecoveryData`: undo stack, redo stack, deletion history;
- new `JourneySnapshotData`: rotating/manual snapshot archive.

All are world-scoped and player-keyed where appropriate.

Primary research mutation should be performed through one transaction-aware facade rather than allowing D, commands, and debug tools to edit `JourneyResearchData` independently.

## History limits

Use bounded persistent history:

- undo stack: newest 100 explicit transactions;
- redo stack: newest 100 transactions;
- deletion records: 1000;
- automatic/safety snapshots: 20;
- manual snapshots: 10.

When limits are exceeded, evict oldest entries first.

These are intentionally small recovery rings, not an audit archive of the entire playthrough.

## Networking and panel refresh

Server remains authoritative.

Single-state D deletion should use an incremental removal sync so deleting one state does not resend thousands of research entries.

Bulk operations (`undo N`, `redo N`, restore-deleted, snapshot restore, clear/prune) may perform one full Journey research sync after the transaction batch. A Journey full sync is allowed because pre7 direct-panel refresh does not call a full NEI registry reload.

No recovery operation may call `ItemList.loadItems.restart()` or equivalent global NEI item loading.

## Diagnostics

`/journey stats` and diagnostic dump should expose:

- undo depth;
- redo depth;
- active delete-history count;
- total deletion records;
- automatic snapshot count;
- manual snapshot count;
- newest snapshot age/id;
- last recovery transaction description;
- skipped automatic snapshots due to suspicious-drop guard.

## Failure behavior

Recovery operations fail closed:

- if an entry snapshot cannot reconstruct its item in the current pack, skip that entry and report the count;
- never partially corrupt the registry because one optional mod item fails to deserialize;
- snapshot restore should validate/build the target state before replacing current authoritative state;
- if validation cannot produce a coherent target, leave current research untouched and report failure;
- undo/redo only move a transaction between stacks after the intended mutation has been applied successfully enough to produce a coherent state.

## Regression criteria

Tests must prove at minimum:

- D click deletes exact displayed state only;
- D click cannot retrieve/give the item;
- transaction forward/reverse restores exact templates and timeline position;
- undo of one migration scan removes only that scan's additions;
- redo restores them;
- new mutation after undo clears redo;
- restore-deleted selects newest active records only;
- undo restore-deleted reactivates those deletion records;
- redo restore-deleted deactivates them again;
- natural reacquisition deactivates a matching active deletion record;
- transaction and deletion-history bounds are enforced;
- automatic snapshot interval/ring behavior is bounded;
- suspicious-drop guard skips dangerous auto snapshots;
- manual snapshots can still be made explicitly;
- snapshot restore is undoable and redoable;
- snapshot restore does not recursively restore old undo/redo history;
- recovery storage survives write/read NBT round-trip;
- all bulk mutations cause one Journey sync/refresh rather than global NEI reload.
