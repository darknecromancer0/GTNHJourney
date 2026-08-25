# GTNH Journey pre7 Semantic States, Migration, and Release Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Preserve meaningful filled-container and electric states, eliminate verified equip/runtime-NBT duplicates, migrate pre6 data deterministically without losing drills/fluid cells, then produce a fully version-consistent `0.1.0-pre7` live-test jar.

**Architecture:** Keep unknown NBT exact by default. Add only narrowly scoped wearable transient policies for the exact live-observed families. Run those policies through both identity and retrieval-template normalization so keys/templates cannot drift. Bump persisted data version so old duplicate keys are recanonicalized; because persisted entries are oldest-first, the first valid resolved entry for a collapsed key preserves the earliest chronology. Direct Journey panel ownership from plan 1 makes IC2 drills/filled cells visible without global NEI variants.

**Tech Stack:** Forge/FML 1.7.10 item/NBT APIs, existing Journey semantic policies and WorldSavedData migration, JUnit 5, Gradle/GTNH convention build, GitHub Actions artifact version guard.

**Spec:** `docs/superpowers/specs/2026-08-25-pre7-architecture-design.md`

## Global Constraints

- Filled fluid identity is meaningful unless a specific verified policy proves otherwise.
- Do not add generic `Fluid`, `Energy`, `charge`, armor, or unknown-NBT stripping.
- Existing IC2 Drill/Diamond Drill/Iridium Drill and filled `IC2:itemFluidCell` research must survive migration.
- Equip/runtime normalization must be registry/class scoped and preserve enchantments, upgrades, configurations and persistent payload.
- Existing duplicate keys that collapse under pre7 keep the earliest valid timeline position.
- Display sanitization never changes the server retrieval template.
- GregTech's global Creative Inventory renderer crash is a non-goal; Journey only protects its own display stacks.

---

### Task 1: Lock filled-container exactness and drill visibility into regression contracts

**Files:**
- Create: `src/test/java/dev/gtnhjourney/minecraft/FilledContainerIdentityTest.java`
- Create: `src/test/java/dev/gtnhjourney/nei/StoredResearchVisibilityTest.java`
- Modify: `src/test/java/dev/gtnhjourney/Pre6RegressionContractTest.java`

- [ ] **Step 1: Add filled-fluid exactness tests**

Build representative NBT compounds:

```java
NBTTagCompound orundum = fluidTag("molten.orundum", 1000);
NBTTagCompound vanadium = fluidTag("molten.vanadium", 1000);
assertNotEquals(
    NbtCanonicalizer.canonicalize(orundum),
    NbtCanonicalizer.canonicalize(vanadium));
assertEquals(
    NbtCanonicalizer.canonicalize(orundum),
    NbtCanonicalizer.canonicalize(ResearchTemplateNormalizer.normalize(orundum)));
```

Also prove empty tag and filled tag differ. This test encodes that tag-only/unknown container payload remains exact.

- [ ] **Step 2: Add panel-planner visibility regression for stored keys**

Use synthetic keys matching the live dump:

```java
ResearchKey drillBase = new ResearchKey("IC2:itemToolDrill", 26, "");
ResearchKey drillFull = new ResearchKey("IC2:itemToolDrill", 1, "10{6:charge=6:30000.0d;}");
ResearchKey filledCell = new ResearchKey(
    "IC2:itemFluidCell",
    0,
    "10{5:Fluid=10{6:Amount=3:1000;9:FluidName=8:\"molten.orundum\";};}");
```

Assert J/N chronology planners include these keys based solely on stored research order; no native NEI-permutation predicate is consulted.

- [ ] **Step 3: Run tests**

```bash
gradle test --tests dev.gtnhjourney.minecraft.FilledContainerIdentityTest --tests dev.gtnhjourney.nei.StoredResearchVisibilityTest
```

Expected: PASS after panel plan 1 is implemented. If filled exactness fails, stop and fix semantic over-normalization before continuing.

- [ ] **Step 4: Commit**

```bash
git add src/test/java/dev/gtnhjourney/minecraft/FilledContainerIdentityTest.java src/test/java/dev/gtnhjourney/nei/StoredResearchVisibilityTest.java src/test/java/dev/gtnhjourney/Pre6RegressionContractTest.java
git commit -m "test: preserve drills and filled container research"
```

---

### Task 2: Add narrowly scoped wearable transient normalization

**Files:**
- Create: `src/main/java/dev/gtnhjourney/minecraft/WearableTransientStatePolicy.java`
- Modify: `src/main/java/dev/gtnhjourney/minecraft/ResearchNbtIdentity.java`
- Modify: `src/main/java/dev/gtnhjourney/minecraft/ResearchTemplateNormalizer.java`
- Create: `src/test/java/dev/gtnhjourney/minecraft/WearableTransientStatePolicyTest.java`

**Verified live targets from pre6 dump:**
- `EMT:itemArmorQuantumChestplate`: duplicate runtime tag `{unequip:0b, wing:0b}` versus BASE.
- `DraconicEvolution:wyvernChest`: duplicate runtime `ProtectionPoints` / `ShieldEntropy` variants.

The policy must match Forge registry id, not merely NBT key names.

- [ ] **Step 1: Write RED tests for EMT zero-state cleanup**

Pure helper API:

```java
static void normalize(String registryId, NBTTagCompound tag)
```

Test:

```java
NBTTagCompound tag = new NBTTagCompound();
tag.setByte("unequip", (byte) 0);
tag.setByte("wing", (byte) 0);
tag.setString("PersistentUpgrade", "keep");
WearableTransientStatePolicy.normalize("EMT:itemArmorQuantumChestplate", tag);
assertFalse(tag.hasKey("unequip"));
assertFalse(tag.hasKey("wing"));
assertEquals("keep", tag.getString("PersistentUpgrade"));
```

Also prove non-zero `unequip` or `wing` remains exact. This fail-closed rule removes only the observed zero initialization/equip noise.

- [ ] **Step 2: Write RED tests for Wyvern runtime shield cleanup**

```java
NBTTagCompound tag = new NBTTagCompound();
tag.setInteger("Energy", 1000000);
tag.setFloat("ProtectionPoints", 80.0F);
tag.setFloat("ShieldEntropy", 0.25F);
tag.setString("PersistentUpgrade", "keep");
WearableTransientStatePolicy.normalize("DraconicEvolution:wyvernChest", tag);
assertFalse(tag.hasKey("ProtectionPoints"));
assertFalse(tag.hasKey("ShieldEntropy"));
assertEquals(1000000, tag.getInteger("Energy"));
assertEquals("keep", tag.getString("PersistentUpgrade"));
```

Do not remove `Energy`, non-empty config profiles, enchantments or unrelated tags.

- [ ] **Step 3: Prove foreign lookalikes stay exact**

The same NBT keys on `test:foreignArmor` remain untouched.

- [ ] **Step 4: Run focused tests and prove RED**

```bash
gradle test --tests dev.gtnhjourney.minecraft.WearableTransientStatePolicyTest
```

Expected: missing policy.

- [ ] **Step 5: Implement registry-id-scoped policy**

Resolve the owner registry id from `GameRegistry.findUniqueIdentifierFor(stack.getItem())` in a defensive `registryId(ItemStack)` helper. `normalize(ItemStack, tag)` delegates to the pure string-id normalizer.

Rules are exactly:
- EMT quantum chestplate: remove `unequip` only when numeric value is 0; remove `wing` only when numeric value is 0.
- Wyvern chest: remove `ProtectionPoints` and `ShieldEntropy`.
- all other ids: no change.

- [ ] **Step 6: Apply the policy to identity and template normalization**

Call `WearableTransientStatePolicy.normalize(stack, identityTag)` in `ResearchNbtIdentity.canonicalize(ItemStack)` and the same policy in `ResearchTemplateNormalizer.normalize(ItemStack)`.

This guarantees a key that collapses equip runtime NBT also stores a retrieval template without that same transient payload.

- [ ] **Step 7: Run focused/full tests**

```bash
gradle test --tests dev.gtnhjourney.minecraft.WearableTransientStatePolicyTest
gradle test
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/dev/gtnhjourney/minecraft/WearableTransientStatePolicy.java src/main/java/dev/gtnhjourney/minecraft/ResearchNbtIdentity.java src/main/java/dev/gtnhjourney/minecraft/ResearchTemplateNormalizer.java src/test/java/dev/gtnhjourney/minecraft/WearableTransientStatePolicyTest.java
git commit -m "fix: normalize verified wearable runtime state"
```

---

### Task 3: Make pre6 -> pre7 migration's earliest-entry rule explicit and tested

**Files:**
- Create: `src/main/java/dev/gtnhjourney/persistence/MigratedEntryAccumulator.java`
- Modify: `src/main/java/dev/gtnhjourney/persistence/JourneyResearchData.java`
- Create: `src/test/java/dev/gtnhjourney/persistence/MigratedEntryAccumulatorTest.java`

- [ ] **Step 1: Write RED pure migration tests**

Required behavior:

```java
MigratedEntryAccumulator acc = new MigratedEntryAccumulator();
assertTrue(acc.accept(collapsedKey, earliestTemplate));
assertFalse(acc.accept(collapsedKey, laterTemplate));
assertEquals(Arrays.asList(collapsedKey), acc.keys());
assertEquals(earliestTemplate, acc.template(collapsedKey));
```

Also test:
- distinct drill/cell keys remain distinct;
- null/unresolved entries are skipped by the caller and do not reserve a key;
- therefore if earliest persisted entry is invalid/unresolvable, the first later valid entry becomes the survivor.

- [ ] **Step 2: Run test and prove RED**

```bash
gradle test --tests dev.gtnhjourney.persistence.MigratedEntryAccumulatorTest
```

- [ ] **Step 3: Implement accumulator**

Use `LinkedHashMap<ResearchKey, NBTTagCompound>` so first valid occurrence wins and insertion order is the earliest logical chronology. Deep-copy templates on input/output.

- [ ] **Step 4: Integrate both normal and undo migration loops**

In `JourneyResearchData.readFromNBT(...)`, replace ad-hoc `keys + containsKey` duplication logic with `MigratedEntryAccumulator` for each player's `Entries` and `UndoPlayers` entries. Set `migrated=true` whenever `accept(...)` rejects a later duplicate.

Bump:

```java
private static final int DATA_VERSION = 8;
```

Do not emit network unlocks/notifications from migration.

- [ ] **Step 5: Add persistence-facing regression**

Create NBT fixture logic that serializes two persisted entries which resolve to one key under the new wearable policy and assert the read result contains one key at the earliest position. Also include synthetic persisted drill/cell entries that remain present when they do not collapse.

If direct WorldSavedData construction makes item-registry reconstruction unavailable in unit tests, test `MigratedEntryAccumulator` plus `PersistedResearchEntryResolver` separately rather than weakening the contract.

- [ ] **Step 6: Run tests**

```bash
gradle test --tests dev.gtnhjourney.persistence.MigratedEntryAccumulatorTest
gradle test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/dev/gtnhjourney/persistence src/test/java/dev/gtnhjourney/persistence
git commit -m "fix: migrate pre7 research with earliest chronology"
```

---

### Task 4: Strengthen Journey-only safe display fallback without changing retrieval data

**Files:**
- Modify: `src/main/java/dev/gtnhjourney/nei/JourneyPresentationSafety.java`
- Modify: `src/main/java/dev/gtnhjourney/nei/JourneyPanelController.java`
- Create: `src/test/java/dev/gtnhjourney/nei/JourneyPresentationSafetyTest.java`
- Modify: `src/test/java/dev/gtnhjourney/Pre6RegressionContractTest.java`

- [ ] **Step 1: Extend renderer-safety regression tests**

Retain the existing Volumetric Flask test and add:
- safe fluid/icon state returns a copy with Fluid intact;
- unsafe/missing-icon state returns a sanitized display copy with Fluid removed;
- source authoritative tag remains byte-for-byte/canonical unchanged;
- null/throwing presentation failure does not imply research deletion.

- [ ] **Step 2: Run focused tests**

```bash
gradle test --tests dev.gtnhjourney.nei.JourneyPresentationSafetyTest --tests dev.gtnhjourney.Pre6RegressionContractTest
```

- [ ] **Step 3: Harden controller boundary**

`JourneyPanelController` wraps each `JourneyPresentationSafety.forNei(...)` call in `try/catch(Throwable)` at the third-party renderer/data boundary. On failure:
- record a bounded diagnostic presentation failure;
- omit only that display entry for the current panel refresh;
- leave `ClientStackMirror` and server research untouched.

Do not patch `GuiContainerCreative`, GregTech's renderer, or global Forge renderer registration.

- [ ] **Step 4: Run full tests/build**

```bash
gradle test build --stacktrace
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/dev/gtnhjourney/nei/JourneyPresentationSafety.java src/main/java/dev/gtnhjourney/nei/JourneyPanelController.java src/test/java/dev/gtnhjourney/nei/JourneyPresentationSafetyTest.java src/test/java/dev/gtnhjourney/Pre6RegressionContractTest.java
git commit -m "fix: fail safe on broken Journey presentation states"
```

---

### Task 5: Update diagnostics for new semantic/runtime behavior

**Files:**
- Modify: `src/main/java/dev/gtnhjourney/diagnostics/SemanticDiagnosticSnapshot.java`
- Modify: `src/main/java/dev/gtnhjourney/diagnostics/JourneyDiagnosticDump.java`
- Modify: `src/main/java/dev/gtnhjourney/diagnostics/RuntimeCompatibilityReport.java`
- Modify: `src/test/java/dev/gtnhjourney/diagnostics/SemanticDiagnosticSnapshotTest.java`

- [ ] **Step 1: Write RED diagnostic assertions**

Extend snapshot reporting with a `wearable-transient` semantic match boolean and verify it appears in `matchedPoliciesCsv()` only for supported normalized stacks.

Add dump assertions/helper tests for the runtime counters introduced in panel/acquisition plans.

- [ ] **Step 2: Implement diagnostics**

Update semantic construction in `JourneyDiagnosticDump` so supported wearable runtime states are not reported as unknown exact NBT after normalization. Add runtime counter section if not already added by plan 1.

`RuntimeCompatibilityReport` should report the 20-tick scan default/effective config naturally through existing config formatting; no duplicate special line is needed.

- [ ] **Step 3: Run diagnostics tests/full tests**

```bash
gradle test --tests dev.gtnhjourney.diagnostics.SemanticDiagnosticSnapshotTest
gradle test
```

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/dev/gtnhjourney/diagnostics src/test/java/dev/gtnhjourney/diagnostics
git commit -m "diagnostics: report pre7 wearable and fast-path state"
```

---

### Task 6: Version the release consistently as 0.1.0-pre7

**Files:**
- Modify: `src/main/java/dev/gtnhjourney/GTNHJourney.java`
- Modify: `build.gradle.kts`
- Modify: `src/main/resources/mcmod.info`
- Modify: `README.md`
- Modify: `docs/first-live-test.md`
- Modify if needed: `.github/workflows/build.yml`

- [ ] **Step 1: Update all three version sources together**

Set exactly `0.1.0-pre7` in:

```java
GTNHJourney.VERSION
```

```kotlin
version = "0.1.0-pre7"
```

```json
"version": "0.1.0-pre7"
```

The existing GitHub Actions `Verify artifact version` step must continue to require the runtime jar name and `mcmod.info` to match Java `VERSION`.

- [ ] **Step 2: Update README behavior**

Document:
- J and N newest-first;
- newest state appears top-left page 1;
- direct Journey panel path/no ordinary full NEI reload;
- 20-tick fallback scan;
- tracked closed-furnace research;
- `Unlocked: <name>` notifications;
- filled-container/drill visibility;
- wearable transient normalization;
- Creative mode support;
- global GregTech Creative flask renderer crash remains outside Journey scope.

Remove obsolete pre6 statements about J oldest-first or global variant injection.

- [ ] **Step 3: Rewrite live-test matrix for pre7**

Required ordered checks:
1. startup/version;
2. J newest-first, newest item upper-left;
3. N newest-first/limit, newest item upper-left;
4. unlock performance/no visible full-NEI freeze;
5. IC2 Drill/Diamond/Iridium states;
6. filled Universal Fluid Cells with contents;
7. closed-GUI furnace automatic research;
8. one `Unlocked:` message/no login spam;
9. equip/unequip armor no duplicates;
10. Survival <-> Creative Journey continuity;
11. Journey-safe flask display without claiming to fix global Creative renderer;
12. `/journey dump` counters.

- [ ] **Step 4: Run version guard locally**

```bash
gradle spotlessApply
gradle test build --stacktrace
VERSION=$(sed -n 's/.*public static final String VERSION = "\([^"]*\)";.*/\1/p' src/main/java/dev/gtnhjourney/GTNHJourney.java)
test "$VERSION" = "0.1.0-pre7"
RUNTIME_JAR="build/libs/gtnhjourney-${VERSION}.jar"
test -f "$RUNTIME_JAR"
unzip -p "$RUNTIME_JAR" mcmod.info | grep -F '"version": "'"$VERSION"'"'
```

Expected: all commands succeed.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/dev/gtnhjourney/GTNHJourney.java build.gradle.kts src/main/resources/mcmod.info README.md docs/first-live-test.md .github/workflows/build.yml
git commit -m "release: prepare GTNH Journey 0.1.0-pre7"
```

---

### Task 7: Final pre7 verification and artifact evidence

- [ ] **Step 1: Full clean verification**

```bash
gradle clean spotlessApply test build --stacktrace
```

Expected: all tests/checkstyle/Forge build pass from a clean build directory.

- [ ] **Step 2: Verify forbidden regressions**

```bash
grep -R "ItemList\.loadItems\.restart" -n src/main/java/dev/gtnhjourney || true
grep -R "JourneyNEIOrderBridge\|JourneyNEIVariantBridge\|JourneyVariantScope" -n src/main/java src/test/java || true
grep -R "Current development version: `0.1.0-pre6`" -n README.md docs || true
```

Expected: no pre7 Journey unlock/panel path uses full NEI load; obsolete bridges and stale pre6 current-version text are absent.

- [ ] **Step 3: Verify jar metadata and checksum**

```bash
RUNTIME_JAR=build/libs/gtnhjourney-0.1.0-pre7.jar
unzip -t "$RUNTIME_JAR"
unzip -p "$RUNTIME_JAR" mcmod.info | grep -F '"version": "0.1.0-pre7"'
sha256sum "$RUNTIME_JAR"
```

Record the SHA-256 in the PR/live-test handoff.

- [ ] **Step 4: Push and require GitHub Actions green on the exact final HEAD**

Do not call the release ready based on an older green run. Verify the final workflow run includes:
- formatting;
- Forge build/test/checkstyle;
- artifact version guard;
- artifact upload.

- [ ] **Step 5: Download the final CI artifact and independently inspect it**

Confirm:
- runtime jar file name is `gtnhjourney-0.1.0-pre7.jar`;
- internal `mcmod.info` says `0.1.0-pre7`;
- compiled `GTNHJourney.VERSION` is `0.1.0-pre7`;
- `unzip -t` passes;
- checksum is recorded.

Only this jar is handed to the user for live testing.
