# Native backup shutdown save-safety plan

## Problem

GTNH ServerUtilities 2.4.1 temporarily sets every `WorldServer.levelSaving = true` while its background backup thread copies the world. If the integrated server stops during that window, ServerUtilities 2.4.1 does not wait for/cancel the worker and does not restore the prior `levelSaving` values. Minecraft still prints `Saving chunks...`, but those saves can be skipped, so progress after backup start can disappear on the next launch.

GTNH Journey delegates world backups to ServerUtilities when that mod is present, so Journey must provide shutdown compatibility for affected native-backup versions without taking ownership of ordinary backup scheduling.

## Implementation

1. Add a dependency-free reflective `ServerUtilitiesBackupSaveSafety` helper.
2. On `FMLServerStoppingEvent`, inspect ServerUtilities `BackupTask.thread` and `dimSaveStates`.
3. If a native backup worker is alive, request interruption and wait for it to finish for a bounded period. Legacy 2.4.1 is non-cooperative, so waiting is expected during Save & Quit.
4. Restore each loaded world's `levelSaving` from the exact state captured by ServerUtilities, using the same world-array index semantics as ServerUtilities 2.4.1.
5. Clear a dead/stale native backup thread reference so future backups are not silently disabled in the same client session.
6. If the worker exceeds the wait bound, prioritize preserving the live world: re-enable the recorded save state and report that the in-flight native archive may be invalid.
7. Run this compatibility guard before Journey's own `WORLD_BACKUPS.finishForShutdown()`.
8. Add diagnostics for native backup worker/save-suppression state and shutdown repairs if practical.

## Tests

- RED contract: server stopping must invoke the native save-safety guard before Journey backup shutdown.
- RED policy tests: restore captured false/true save states by array index; only mismatched suppressed worlds are repaired.
- RED reflection/source contract: helper must target `serverutils.task.backup.BackupTask.thread` and `dimSaveStates` without a compile-time ServerUtilities dependency.
- Full Forge build/test/refmap/version gate.
- Only after functional GREEN, bump runtime/build metadata to 1.1.34 and run the release gate again on the exact release SHA.
