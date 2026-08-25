# GTNH Journey pre7 Debug Researcher Tool Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an admin-only Debug Researcher Tool that imports already-existing blocks and inventory contents from a migrated world into Journey without breaking or moving anything.

**Architecture:** Register one custom item with NBT-stored mode and item-first right-click interception. All scans produce copied `ItemStack` candidates, deduplicate them by Journey semantic identity, then pass one bulk operation through the transaction-aware `JourneyMutationService` from the recovery plan. The tool never writes research data directly and never loads chunks.

**Tech Stack:** Java 8 source level on Forge 1.7.10 / GTNH 2.9.0-beta-2, JUnit 4, vanilla/Forge block + `IInventory` APIs, existing Journey research normalization and direct-panel sync.

**Spec:** `docs/superpowers/specs/2026-08-25-pre7-migration-debug-tool-design.md`

## Global Constraints

- Tool is obtained only through `/journey debugtool`; no recipe.
- Tool has permanent enchanted glint, stack size 1, vanilla-stick visual base.
- Shift+right-click cycles `BLOCK -> CONTENTS -> AREA_16 -> BLOCK`.
- Normal right-click performs the current mode.
- Item interaction must take priority over block GUI activation where Forge allows via `onItemUseFirst`.
- Never break/change blocks, mutate inventories, synthesize fluid containers, force-load/generate chunks, or scan the entire world.
- AREA_16 is exactly x/z -8..+7 and y -8..+7 around player's integer block position: 4096 positions before filtering.
- One physical tool action is one recovery transaction and one summary/sync.

---

### Task 1: Tool mode and NBT state

**Files:**
- Create: `src/main/java/dev/gtnhjourney/debug/DebugResearchMode.java`
- Create: `src/main/java/dev/gtnhjourney/debug/DebugResearchToolState.java`
- Test: `src/test/java/dev/gtnhjourney/debug/DebugResearchToolStateTest.java`

**Interfaces:**
- `DebugResearchMode.next()` cycles BLOCK, CONTENTS, AREA_16.
- `DebugResearchToolState.read(ItemStack)` defaults to BLOCK for absent/invalid NBT.
- `write(ItemStack, mode)` changes only Journey-owned mode tag.

- [ ] Write RED tests for exact cycle, default mode, NBT round-trip and unrelated-NBT preservation.
- [ ] Implement pure mode/state helper.
- [ ] Run focused/full tests.
- [ ] Commit as `feat: define debug researcher tool modes`.

### Task 2: Safe placed-block item resolver

**Files:**
- Create: `src/main/java/dev/gtnhjourney/debug/PlacedBlockResearchResolver.java`
- Test: `src/test/java/dev/gtnhjourney/debug/PlacedBlockResearchResolverPolicyTest.java`

**Interfaces:**
- Runtime resolver prefers Forge pick-block representation.
- Safe fallback uses `Item.getItemFromBlock(block)` + world damage/meta.
- Returns null for air/unmapped/broken optional-mod cases.

- [ ] Write RED policy tests with injectable pick/fallback strategies proving preference, fallback and exception isolation.
- [ ] Implement pure policy plus Forge adapter.
- [ ] Run focused/full tests.
- [ ] Commit as `feat: resolve placed blocks for migration research`.

### Task 3: Read-only inventory collector

**Files:**
- Create: `src/main/java/dev/gtnhjourney/debug/InventoryResearchCollector.java`
- Test: `src/test/java/dev/gtnhjourney/debug/InventoryResearchCollectorTest.java`

**Interfaces:**
- Accept `IInventory` and return copies of valid non-empty slot stacks.
- One broken slot read cannot abort remaining slots.
- Never mutate source stacks or inventory.

- [ ] Write RED tests with fake inventory proving defensive copies and slot exception isolation.
- [ ] Implement collector.
- [ ] Run focused/full tests.
- [ ] Commit as `feat: collect migration inventory contents safely`.

### Task 4: Exact AREA_16 coordinate planner and loaded-chunk gate

**Files:**
- Create: `src/main/java/dev/gtnhjourney/debug/Area16Planner.java`
- Create: `src/main/java/dev/gtnhjourney/debug/LoadedWorldAccess.java`
- Test: `src/test/java/dev/gtnhjourney/debug/Area16PlannerTest.java`

**Interfaces:**
- Planner yields exactly 4096 `(x,y,z)` coordinates using -8..+7 bounds.
- Runtime access checks Y validity and already-loaded chunk before block/tile access.
- No method may call APIs that load or generate chunks.

- [ ] Write RED test for 4096 count, corner coordinates, and no duplicates.
- [ ] Implement pure planner.
- [ ] Add loaded-world adapter using existing loaded-chunk checks only.
- [ ] Run focused/full tests.
- [ ] Commit as `feat: add bounded loaded area scan planner`.

### Task 5: Semantic candidate deduplication and scan result model

**Files:**
- Create: `src/main/java/dev/gtnhjourney/debug/DebugResearchScanResult.java`
- Create: `src/main/java/dev/gtnhjourney/debug/ResearchCandidateDeduplicator.java`
- Test: `src/test/java/dev/gtnhjourney/debug/ResearchCandidateDeduplicatorTest.java`

**Interfaces:**
- Deduplicate candidates by Journey semantic identity/endpoints, not object identity or stack count.
- Result tracks positions visited, block candidates, inventories visited, raw stacks, unique candidates, newly unlocked states.

- [ ] Write RED tests proving repeated stone/cable stacks collapse and distinct filled/NBT semantic states survive.
- [ ] Implement deduplicator using existing Journey identity normalization.
- [ ] Run focused/full tests.
- [ ] Commit as `perf: deduplicate migration research candidates`.

### Task 6: BLOCK, CONTENTS and AREA_16 scan service

**Files:**
- Create: `src/main/java/dev/gtnhjourney/debug/DebugResearchScanService.java`
- Test: `src/test/java/dev/gtnhjourney/debug/DebugResearchScanServiceTest.java`

**Interfaces:**
- `scanBlock(player, x,y,z)` returns placed block candidate only.
- `scanContents(player, x,y,z)` returns target `IInventory` contents only.
- `scanArea16(player)` scans loaded positions, block items, and `IInventory` contents.
- Service returns candidates/result; it does not mutate research itself.

- [ ] Write RED tests with fake world adapter proving mode boundaries and no contents in BLOCK/no implicit block in CONTENTS.
- [ ] Implement service by composing Tasks 2-5.
- [ ] Run focused/full tests.
- [ ] Commit as `feat: implement debug migration scans`.

### Task 7: Transactional bulk import integration

**Files:**
- Modify: `src/main/java/dev/gtnhjourney/recovery/JourneyMutationService.java`
- Modify: `src/main/java/dev/gtnhjourney/debug/DebugResearchScanService.java`
- Test: `src/test/java/dev/gtnhjourney/debug/DebugResearchTransactionTest.java`

**Interfaces:**
- One BLOCK/CONTENTS/AREA_16 action calls one bulk-add mutation.
- Zero-new-state scan creates no undo transaction.
- AREA_16 creates safety snapshot before mutation.
- One action produces one Journey sync and one chat summary, not per-item `Unlocked:` spam.

- [ ] Write RED tests for one transaction, undo/redo of exact additions, and zero-change no-op.
- [ ] Wire scan service to recovery facade and snapshot service.
- [ ] Run focused/full tests.
- [ ] Commit as `feat: make debug imports transactional`.

### Task 8: Register custom item and item-first interactions

**Files:**
- Create: `src/main/java/dev/gtnhjourney/debug/ItemDebugResearcherTool.java`
- Create/Modify: item registration in `GTNHJourney`/proxy lifecycle
- Test: pure interaction-routing policy test

**Interfaces:**
- Max stack 1, unlocalized/display name `Debug Researcher Tool`, permanent glint.
- `onItemUseFirst`: Shift cycles mode and consumes action; otherwise executes BLOCK/CONTENTS/AREA_16 and consumes action.
- `onItemRightClick`: Shift cycles mode; AREA_16 can execute in air; BLOCK/CONTENTS in air report no target without mutation.
- Server side performs scans; client side only returns handled result/state without authoritative mutation.

- [ ] Write RED interaction-policy tests for mode switching and consumed right-click behavior.
- [ ] Register item with vanilla stick icon/texture strategy compatible with 1.7.10 resource handling.
- [ ] Implement server-authoritative item hooks with failure isolation.
- [ ] Run focused/full tests.
- [ ] Commit as `feat: add Debug Researcher Tool item`.

### Task 9: `/journey debugtool`, permissions, diagnostics

**Files:**
- Modify: `src/main/java/dev/gtnhjourney/command/CommandJourney.java`
- Modify: `src/main/java/dev/gtnhjourney/diagnostics/JourneyRuntimeCounters.java`
- Modify: `src/main/java/dev/gtnhjourney/diagnostics/JourneyDiagnosticDump.java`
- Test: command permission and diagnostics tests

**Interfaces:**
- `/journey debugtool` gives exactly one tool.
- Integrated singleplayer owner may use it; dedicated server requires operator-level permission for this subcommand.
- Diagnostics count debug scan actions, positions visited, inventories visited, unique candidates, and newly unlocked states.

- [ ] Write RED tests for permission policy and counter accumulation.
- [ ] Implement command routing without raising permission level of harmless `/journey` diagnostics globally.
- [ ] Add compact migration summary messages.
- [ ] Run focused/full tests.
- [ ] Commit as `feat: expose debug migration tool command`.

### Task 10: Live-test checklist and release integration

**Files:**
- Modify: `docs/first-live-test.md`
- Modify: README/version notes as appropriate

- [ ] Add live test: command issuance, mode cycle, machine GUI interception, BLOCK import, CONTENTS import, AREA_16 import, undo, redo, safety snapshot, no inventory/block mutation, no forced chunk load.
- [ ] Add migration workflow recommendation: walk old base with AREA_16, use BLOCK/CONTENTS for targeted misses.
- [ ] Run `gradle test build --stacktrace`.
- [ ] Static-search for forbidden global NEI reload calls and chunk-loading calls in debug package.
- [ ] Commit as `docs: add pre7 migrated world debug workflow`.
