# GTNH Journey pre7 Acquisition and Notifications Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make research acquisition event-first, detect tracked furnace outputs while the GUI is closed, reduce fallback inventory scan pressure, keep Creative mode supported, and emit exactly one human unlock notification per newly acquired logical item.

**Architecture:** Extract a shared `ResearchObservationService` so inventory events and furnace tracking enter the same server-authoritative research path. Keep the inventory scanner as reconciliation rather than the primary trigger. Track only furnaces the player actually touched and poll their output signatures. Notifications are a separate incremental network message, never part of full login sync.

**Tech Stack:** Forge/FML 1.7.10 event buses, `TileEntityFurnace`, SimpleNetworkWrapper, Java 8-compatible source, JUnit 5, existing Journey persistence/network layers.

**Spec:** `docs/superpowers/specs/2026-08-25-pre7-architecture-design.md`

## Global Constraints

- Furnace ownership rule: last player who opened/used the furnace owns its automatic output research.
- Current target use case is singleplayer, but the ownership implementation must remain deterministic on a server.
- Do not scan every loaded furnace or tile entity globally.
- Pickup events are hints, not ownership proof; reconcile the real server inventory after the event rather than researching a possibly-cancelled entity stack directly.
- Full login sync never emits `Unlocked:` messages.
- One observation that creates BASE + FULL endpoints emits one notification, not one per endpoint.
- Research remains gamemode-independent.
- Default incremental inventory scan becomes 20 ticks; existing forced full-rescan remains the deeper safety net.

---

### Task 1: Extract one shared server observation service

**Files:**
- Create: `src/main/java/dev/gtnhjourney/acquisition/ResearchObservationService.java`
- Modify: `src/main/java/dev/gtnhjourney/acquisition/InventoryResearchTracker.java`
- Modify: `src/main/java/dev/gtnhjourney/GTNHJourney.java`
- Create: `src/test/java/dev/gtnhjourney/acquisition/ResearchObservationResultTest.java`

**Interface:**

```java
public final class ResearchObservationService {
    public ResearchObservationService(PlayerResearchService research);
    public List<ItemStack> observe(EntityPlayerMP player, ItemStack observed);
}
```

- [ ] **Step 1: Add a pure result/coalescing test**

Introduce a tiny pure helper/result type if needed so tests can prove:
- zero newly unlocked endpoints -> no logical unlock;
- one endpoint -> logical unlock true;
- two endpoints from one observation -> still one logical unlock/notification decision.

Example contract:

```java
ResearchObservationResult result = ResearchObservationResult.of(Arrays.asList(base, full));
assertTrue(result.isNewLogicalUnlock());
assertEquals(2, result.endpointCount());
assertEquals(1, result.notificationCount());
```

- [ ] **Step 2: Run focused test and prove RED**

```bash
gradle test --tests dev.gtnhjourney.acquisition.ResearchObservationResultTest
```

Expected: missing helper/service.

- [ ] **Step 3: Implement `ResearchObservationService`**

Move the current private `InventoryResearchTracker.unlock(...)` behavior into this service:

```java
List<ItemStack> unlocked = research.unlockStates(player, observed);
for (ItemStack endpoint : unlocked) {
    ResearchTrace.unlocked(player, endpoint);
    JourneyNetwork.sendUnlock(player, endpoint);
}
if (!unlocked.isEmpty()) JourneyNetwork.sendUnlockNotification(player, observed);
return unlocked;
```

All calls are defensive against null/broken third-party stacks exactly as the current tracker is.

- [ ] **Step 4: Rewire `InventoryResearchTracker`**

Constructor becomes:

```java
InventoryResearchTracker(PlayerResearchService research, ResearchObservationService observations)
```

or only accepts `ResearchObservationService` plus the raw research service for login import if needed. Crafted/smelted/inventory-change paths call `observations.observe(...)`. Login import remains notification-free by calling persistence directly before full sync.

- [ ] **Step 5: Run tests and build**

```bash
gradle test
gradle compileJava
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/dev/gtnhjourney/acquisition src/main/java/dev/gtnhjourney/GTNHJourney.java src/test/java/dev/gtnhjourney/acquisition
git commit -m "refactor: centralize Journey research observations"
```

---

### Task 2: Slow the fallback scanner and make pickup event-first without ghost unlocks

**Files:**
- Modify: `src/main/java/dev/gtnhjourney/config/JourneyConfig.java`
- Modify: `src/main/java/dev/gtnhjourney/acquisition/InventoryResearchTracker.java`
- Modify: `src/main/java/dev/gtnhjourney/GTNHJourney.java`
- Create: `src/test/java/dev/gtnhjourney/acquisition/ReconcileRequestSetTest.java`

- [ ] **Step 1: Write RED tests for forced reconciliation scheduling**

Create a small pure `ReconcileRequestSet` or equivalent package-private helper with:

```java
request(playerId);
assertTrue(consume(playerId));
assertFalse(consume(playerId));
```

This ensures multiple pickup hints collapse into one next-tick forced inventory reconciliation.

- [ ] **Step 2: Change default incremental scan interval**

In `JourneyConfig` set:

```java
private static volatile int inventoryScanIntervalTicks = 20;
```

and use default `20` in `config.getInt(...)`. Keep allowed range and `inventoryFullRescanIntervalTicks=200` unchanged.

- [ ] **Step 3: Add Forge pickup event handler**

Handle `net.minecraftforge.event.entity.player.EntityItemPickupEvent` in `InventoryResearchTracker`:
- only `EntityPlayerMP` on server side;
- do not unlock `event.item.getEntityItem()` directly;
- request a forced reconciliation for that player;
- on the next server player tick, consume the request and run `scanChanged(player, true, true)` regardless of normal 20-tick cadence.

Register the tracker on both:

```java
FMLCommonHandler.instance().bus()
MinecraftForge.EVENT_BUS
```

because tick/craft events and Forge entity pickup events live on different buses.

- [ ] **Step 4: Run tests**

```bash
gradle test --tests dev.gtnhjourney.acquisition.ReconcileRequestSetTest
gradle test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/dev/gtnhjourney/config/JourneyConfig.java src/main/java/dev/gtnhjourney/acquisition/InventoryResearchTracker.java src/main/java/dev/gtnhjourney/GTNHJourney.java src/test/java/dev/gtnhjourney/acquisition/ReconcileRequestSetTest.java
git commit -m "perf: make inventory scanning a reconciliation fallback"
```

---

### Task 3: Implement last-user furnace output tracking

**Files:**
- Create: `src/main/java/dev/gtnhjourney/acquisition/FurnaceKey.java`
- Create: `src/main/java/dev/gtnhjourney/acquisition/FurnaceOutputGate.java`
- Create: `src/main/java/dev/gtnhjourney/acquisition/FurnaceOwnershipTracker.java`
- Modify: `src/main/java/dev/gtnhjourney/GTNHJourney.java`
- Create: `src/test/java/dev/gtnhjourney/acquisition/FurnaceOutputGateTest.java`
- Create: `src/test/java/dev/gtnhjourney/acquisition/FurnaceKeyTest.java`

- [ ] **Step 1: Write RED pure tests**

`FurnaceKey` equality/hash includes dimension + x + y + z.

`FurnaceOutputGate` behavior:

```java
FurnaceOutputGate gate = new FurnaceOutputGate();
gate.prime(0, false);
assertTrue(gate.observe(12345, true));
assertFalse(gate.observe(12345, true));
assertFalse(gate.observe(0, false));
assertTrue(gate.observe(67890, true));
```

The signature is identity-oriented and excludes stack count through existing `InventoryStackSignature.of` behavior, so producing more of the same output does not spam research attempts.

- [ ] **Step 2: Run focused tests and prove RED**

```bash
gradle test --tests dev.gtnhjourney.acquisition.FurnaceOutputGateTest --tests dev.gtnhjourney.acquisition.FurnaceKeyTest
```

- [ ] **Step 3: Implement `FurnaceOwnershipTracker` interaction hook**

Register on `MinecraftForge.EVENT_BUS` for `PlayerInteractEvent`.

On server-side `RIGHT_CLICK_BLOCK`:
1. Resolve tile at event x/y/z.
2. Continue only for `TileEntityFurnace`.
3. Create `FurnaceKey(dimensionId, x, y, z)`.
4. Store/replace entry owner with the current `EntityPlayerMP` (last-user rule).
5. Prime its `FurnaceOutputGate` with slot 2's current `InventoryStackSignature.of(...)` and emptiness so merely opening a furnace with old output does not fabricate a new completion transition.

- [ ] **Step 4: Implement tracked-furnace server polling**

Register the same tracker on `FMLCommonHandler.instance().bus()` for `ServerTickEvent.END`.

Each tick iterate only the tracked map:
- resolve `WorldServer` with `DimensionManager.getWorld(key.dimension())`;
- if world/chunk/tile is unavailable or tile is invalid/not `TileEntityFurnace`, remove entry;
- read output slot 2;
- increment `JourneyRuntimeCounters.furnaceOutputObservation()` for valid tracked observations;
- if gate reports a new non-empty signature, call `observations.observe(owner, output.copy())`;
- if at least one endpoint was newly unlocked, increment `furnaceOutputUnlock()`.

Owner validity check:

```java
owner != null && !owner.isDead && owner.playerNetServerHandler != null
```

On player logout remove entries whose owner UUID matches. Add `clear()` and call it on server stop.

- [ ] **Step 5: Run focused/full tests**

```bash
gradle test --tests dev.gtnhjourney.acquisition.FurnaceOutputGateTest --tests dev.gtnhjourney.acquisition.FurnaceKeyTest
gradle test build --stacktrace
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/dev/gtnhjourney/acquisition/Furnace*.java src/main/java/dev/gtnhjourney/GTNHJourney.java src/test/java/dev/gtnhjourney/acquisition/Furnace*Test.java
git commit -m "feat: research completed tracked furnace outputs"
```

---

### Task 4: Add one incremental unlock notification per logical acquisition

**Files:**
- Create: `src/main/java/dev/gtnhjourney/network/ResearchUnlockNotificationMessage.java`
- Create: `src/main/java/dev/gtnhjourney/client/UnlockNotificationService.java`
- Modify: `src/main/java/dev/gtnhjourney/network/JourneyNetwork.java`
- Modify: `src/main/java/dev/gtnhjourney/acquisition/ResearchObservationService.java`
- Create: `src/test/java/dev/gtnhjourney/client/UnlockNotificationTextTest.java`
- Create: `src/test/java/dev/gtnhjourney/network/UnlockNotificationPolicyTest.java`

- [ ] **Step 1: Write RED tests for notification policy/text**

Required pure policy:

```java
assertFalse(UnlockNotificationPolicy.shouldNotify(0));
assertTrue(UnlockNotificationPolicy.shouldNotify(1));
assertTrue(UnlockNotificationPolicy.shouldNotify(2));
assertEquals("Unlocked: Universal Fluid Cell", UnlockNotificationText.format("Universal Fluid Cell"));
```

Endpoint count 2 still means one logical notification.

- [ ] **Step 2: Implement message registration**

Register discriminator `6`, client side:

```java
CHANNEL.registerMessage(
    ResearchUnlockNotificationMessage.Handler.class,
    ResearchUnlockNotificationMessage.class,
    6,
    Side.CLIENT);
```

Message payload is a bounded UTF-8 display string. Clamp/sanitize to a conservative maximum of 256 characters before sending. `sendUnlockNotification(player, observed)` computes a safe display name with defensive exception handling; fallback is the Forge registry identifier or `"item"`.

- [ ] **Step 3: Implement client notification service**

On the client tick thread:

```java
Minecraft.getMinecraft().thePlayer.addChatMessage(
    new ChatComponentText(UnlockNotificationText.format(displayName)));
JourneyRuntimeCounters.unlockNotification();
```

If player/chat is unavailable, skip safely.

- [ ] **Step 4: Prove full sync cannot notify**

The only notification send call must be from `ResearchObservationService` after a non-empty result. `ResearchSyncBeginMessage`, `ResearchSyncChunkMessage`, `ResearchSyncEndMessage`, and `ServerResearchSyncQueue` must not call `sendUnlockNotification`.

Add a regression/source contract or pure flow test asserting login/full-sync flow has zero notification action.

- [ ] **Step 5: Run tests**

```bash
gradle test --tests dev.gtnhjourney.client.UnlockNotificationTextTest --tests dev.gtnhjourney.network.UnlockNotificationPolicyTest
gradle test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/dev/gtnhjourney/network src/main/java/dev/gtnhjourney/client src/main/java/dev/gtnhjourney/acquisition/ResearchObservationService.java src/test/java/dev/gtnhjourney/client src/test/java/dev/gtnhjourney/network
git commit -m "feat: notify once for new Journey research"
```

---

### Task 5: Lock Creative-mode independence into regression tests

**Files:**
- Modify: `src/test/java/dev/gtnhjourney/nei/JourneyViewStateTest.java`
- Create: `src/test/java/dev/gtnhjourney/nei/JourneyGamemodeIndependenceTest.java`
- Modify only if needed: `src/main/java/dev/gtnhjourney/nei/JourneyPanelController.java`

- [ ] **Step 1: Add tests that encode the actual invariant**

Journey mode/mirror has no gamemode input. Test that setting/toggling `JourneyViewState` remains stable across a simulated lifecycle call that does not represent connection reset. The production audit test should assert no Journey panel/controller class references `GameType`, `isCreative`, `capabilities.isCreativeMode`, or `GuiContainerCreative` as an enable/disable condition.

- [ ] **Step 2: Run focused tests**

```bash
gradle test --tests dev.gtnhjourney.nei.JourneyGamemodeIndependenceTest --tests dev.gtnhjourney.nei.JourneyViewStateTest
```

If this exposes a real gamemode gate, remove only that gate. Do not change normal NEI cheat permission logic.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/dev/gtnhjourney/nei src/main/java/dev/gtnhjourney/nei/JourneyPanelController.java
git commit -m "test: keep Journey available in creative mode"
```

---

### Task 6: Verify acquisition phase

- [ ] **Step 1: Format and full build**

```bash
gradle spotlessApply
gradle test build --stacktrace
```

Expected: PASS.

- [ ] **Step 2: Audit event registration and notification isolation**

```bash
grep -R "EntityItemPickupEvent\|PlayerInteractEvent\|ServerTickEvent" -n src/main/java/dev/gtnhjourney/acquisition
grep -R "sendUnlockNotification" -n src/main/java/dev/gtnhjourney
```

Expected:
- pickup reconciliation and tracked furnace interaction/tick paths exist;
- notification send appears in the shared logical observation path, not full-sync code.

- [ ] **Step 3: Check default scan config**

```bash
grep -n "inventoryScanIntervalTicks" src/main/java/dev/gtnhjourney/config/JourneyConfig.java
```

Expected default: `20` ticks.

- [ ] **Step 4: Commit formatting only if needed**

```bash
git add -A
git commit -m "test: verify pre7 acquisition fast path"
```

Do not create an empty commit.
