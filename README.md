# GTNH Journey

GT New Horizons 1.7.10 addon that automatically researches item states the player genuinely obtains and allows server-authoritative infinite retrieval through the existing NEI frontend.

## Compatibility target

- Minecraft 1.7.10 / Forge 10.13.4.1614 / MCP stable_12
- **GTNH 2.9.0-beta-2** primary pack baseline
- **NotEnoughItems 2.8.111-GTNH** compile/runtime integration baseline
- Newer 2.9 nightlies are best-effort until separately verified
- Official GTNH Gradle convention plugin
- Development JDK 25 + Jabel syntax mode; Java 8-compatible output bytecode

## Current pre-alpha behavior

### Research authority

- Research belongs to a player UUID and is stored once per save in overworld `WorldSavedData`.
- Real player inventory stacks are researched automatically.
- Crafting and smelting trigger an immediate research attempt.
- Pickups are *not* trusted as proof by themselves; the real server inventory scan validates them, avoiding cancelled/full-inventory ghost unlocks.
- Periodic inventory validation includes main inventory, armor, cursor stack and optional Baubles-Expanded slots.
- Installing Journey into an existing world imports the player's current real inventories before the first full client sync.
- GUI recipe previews, machine output previews and arbitrary open-container slots are never research sources.

### State identity

- Stack size is not part of research identity.
- Registry id, meaningful metadata and canonical semantic NBT are.
- Compound NBT key order is canonicalized deterministically; list order remains significant.
- Classic durability on non-subtyped items is treated as wear, not a new researched item.
- Verified `MetaGeneratedTool` runtime fields `GT.ToolStats.Damage` and `GT.ToolStats.Mode` are ignored in identity and reset in retrieval templates; foreign lookalike NBT stays exact.
- GT fluid/container payload (`GT.FluidContent`) remains exact.
- GT electric items use Journey endpoints: partial charge => BASE; verified max charge => BASE + FULL.
- IC2 `IElectricItem` stacks use the same BASE/FULL model through the IC2 electric API/manager, not a generic `charge`-NBT heuristic.
- CoFH `IEnergyContainerItem` stacks use the same BASE/FULL model through the CoFH energy API; arbitrary `Energy` NBT on foreign items remains exact.
- Tinkers Construct `ToolCore` wear (`InfiTool.Damage/Broken`) is treated as runtime wear while materials, modifiers and other tool payload remain meaningful identity.
- Unknown mod NBT remains exact by default. Compatibility rules fail closed rather than merging unknown states.

### Persistence and recovery

- Exact server retrieval templates are stored alongside research keys.
- Save format migration rebuilds both identity and template through the same current semantic policy.
- Broken NBT-backed entries with no retrieval template are rejected instead of becoming a bare item.
- Removed mod items remain diagnosable as orphan research until explicitly pruned.
- `/journey forget`, `/journey clear confirm` and `/journey prune-missing confirm` create one persisted undo snapshot first.
- `/journey undo` restores exact keys/templates and unlock order, including after a restart.

### Networking / security

- Client is never authoritative for research or spawning.
- Retrieval requests send a fixed SHA-256 research fingerprint, not arbitrary ItemStack/NBT payloads.
- Server resolves the fingerprint against the player's current research again before creating the saved template.
- Retrieval requests are rate/queue limited per player.
- Large research catalogs stream over multiple server ticks with entry-count and actual serialized-ItemStack byte budgets.
- Full sync is epoch-based, expected-count validated and double-buffered client-side; an incomplete/outdated stream never replaces the last complete snapshot.
- Client-bound network mutations are FIFO-queued onto the client tick thread instead of mutating the NEI mirror from Netty callbacks.
- Unlocks occurring during a full sync are deferred and replayed after its matching End packet.
- Oversized or unserializable single NBT states stay server-side rather than risking a disconnect; the GUI reports the server-only count.
- Server-selected semantic compatibility flags are transmitted at Begin-sync so client fingerprints cannot drift from server identity rules.

## NEI GUI

Journey deliberately reuses GTNH's existing NEI item panel instead of adding a second item browser.

- `J` toggles **Journey / Researched** view.
- `N` toggles **Newest** view.
- Exact synchronized NBT variants are injected only while `J`/`N` is active and are removed from normal NEI view.
- Normal NEI search and recipe/usage browsing remain available.
- In Journey/Newest view: left click requests a full stack; right click requests one item.
- In ordinary NEI: Ctrl + left click requests a full researched stack; Ctrl + right click requests one.
- `Journey.Researched` and `Journey.Newest` remain fallback subsets for layouts too narrow to show both buttons.
- Researched tooltips are marked and include Journey retrieval hotkey hints.

The NEI built-in infinite/cheat item path is intentionally **not** used because it would bypass Journey's server-side research validation.

## Commands

- `/journey count`
- `/journey stats`
- `/journey debug`
- `/journey trace [on|off]` - opt-in live unlock chat/log trace for this server session
- `/journey dump` - writes an attachable diagnostic file into `logs/`
- `/journey hotspots [limit]`
- `/journey list [page]`
- `/journey newest [limit]`
- `/journey get <index> [amount]`
- `/journey forget <index>`
- `/journey undo`
- `/journey inspect` - inspect the held stack's normalized identity/GT/IC2 endpoint classification
- `/journey inspect` also reports CoFH, verified GT-tool and TCon-tool classification for compatibility debugging.
- `/journey rescan`
- `/journey prune-missing confirm`
- `/journey clear confirm`

## Configuration

`config/gtnhjourney.cfg`:

- `research.inventoryScanIntervalTicks` (default `2`)
- `research.inventoryFullRescanIntervalTicks` (default `200`, safety deep-rescan while stable slots use cheap signatures)
- `client.newestLimit` (default `64`)
- `compatibility.normalizeGtTransientIdentity` (default `true`)
- `compatibility.resetGtToolTemplateState` (default `true`)
- `compatibility.normalizeGtChargeEndpoints` (default `true`)
- `compatibility.normalizeIc2ChargeEndpoints` (default `true`)
- `compatibility.normalizeTconToolWear` (default `true`)
- `compatibility.normalizeCofhChargeEndpoints` (default `true`)

Security/network hard limits intentionally remain compile-time constants.

## Development verification

Offline regression layers:

```bash
./tools/run-core-tests.sh
./tools/run-adapter-tests.sh
./tools/run-main-smoke.sh
```

They compile with `javac --release 8`. The offline tests/fixtures live outside standard Gradle test source sets so they cannot shadow real Forge/GTNH classes during the actual mod build. A real Forge/GTNH Gradle build still requires dependency network access and is the gate before the first live Prism test.

See `docs/first-live-test.md` for the first-world test matrix.
