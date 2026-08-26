# GTNH Journey pre7 Live-Regression Amendment

## Status

Approved through live-test feedback on 2026-08-26. This document supersedes conflicting pre7 wording in older design/implementation documents, especially any statement that `N` is a truncated newest-research tail or that `AREA_16` scans 4096 positions with `-8..+7` bounds.

## N view: authoritative semantics

`J` and `N` have identical researched-state membership. N is never a smaller subset of J and is not limited by `client.newestLimit`.

N ordering is a persistent meaningful-activity chronology:

1. A state genuinely researched for the first time becomes newest in J research chronology and newest in N activity chronology.
2. Re-observing or normally obtaining an already researched state does not change J or N ordering.
3. Successful server-authoritative retrieval by clicking a researched state through J or N leaves J unchanged and touches that state to newest in N.
4. Repeated retrieval of the already-newest N state creates no duplicate.
5. A deleted state that is later genuinely re-researched is a fresh research event and becomes newest again.
6. N activity order persists across relogs/full sync.
7. Missing/corrupt legacy activity data must never make N lose a state that is visible in J; missing activity records are treated as older than known activity.
8. Search constrains the visible Journey list without changing either chronology.

`client.newestLimit` remains readable only as an ignored legacy config key for compatibility with existing cfg files.

## AREA_16: authoritative bounds

`AREA_16` means a true radius of 16 blocks from the player's integer block coordinate in all three axes.

For player position `(px, py, pz)`:

- `x = px - 16 .. px + 16`
- `y = py - 16 .. py + 16`
- `z = pz - 16 .. pz + 16`

All bounds are inclusive. The planned cube is therefore `33 x 33 x 33 = 35,937` coordinate positions before Y/loading filters.

The center remains the player position even if AREA_16 is invoked while pointing at a block. The tool still never force-loads/generates chunks and reads only already-loaded valid positions.

## Live-regression requirements added after first pre7 test

The following are mandatory acceptance criteria:

- NEI item section remains usable in Creative, including Creative tabs where stock NEI hides it.
- A newly researched state updates active J/N without requiring search text or mode toggling.
- Clearing NEI search while J/N is active returns to the Journey-owned list, not unrestricted ordinary NEI.
- Existing vanilla furnace output is observed when the player first interacts with the furnace; taking the result is not required.
- Tracked furnace completion while GUI is closed remains observable for the last user.
- GT++ Hand Pump fluid payload (`mFluid`, `mFluidAmount`, initialization payload) is transient tool state and does not create separate pump research; actual fluid containers remain exact.
- IC2/Vajra partial-charge visual damage does not create charge-step duplicates; BASE uses a stable empty electric-item representation.
- Debug Researcher Tool is never researchable through ordinary observation, login import, bulk migration or rescan, and old persisted mode variants are rejected during migration.
- BLOCK and CONTENTS targeted debug-tool interactions reach the authoritative server action instead of being consumed client-side or stolen by a block GUI.
- Renderer-hostile GregTech volumetric-flask permutations with no fluid icon are prevented from crashing both Journey presentation and vanilla Creative/NEI display without generically stripping valid flask/container states.
- `/journey dump` exposes `creativeUnsafeFlaskVariantsRemoved` and continues to expose migration/acquisition/presentation counters.

## Verification authority

`docs/first-live-test.md` is the current executable live-test checklist. A real Forge/GTNH build remains mandatory before declaring the current HEAD ready for another live jar; pure/local tests do not substitute for the final build gate.
