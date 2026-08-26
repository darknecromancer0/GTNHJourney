# GTNHJourney v1.0 Inventory Recovery Release Plan

**Goal:** Ship 1.0.0 with a 5-tick fallback scan, reliable manual inventory recovery, safe empty-container research/retrieval, S/T NEI controls, concise help, and `/journey research`.

## Tasks

1. Add conservative embedded-inventory NBT detection and normalization. Lunch Bag strips `Inventory`, `Open`, `UUID`; IC2 Toolbox strips `Items`, `uid`; generic recognized serialized `Items` lists are removed from research identity/template while unrelated NBT stays exact.
2. Add bounded recursive inventory recovery that collects top-level player-owned stacks plus embedded ItemStack lists, with depth/item limits and broken-entry isolation.
3. Add server-authoritative scan/debug-tool request packets and NEI `S`/`T` buttons. S always performs one full research sync after its scan. T reuses existing DebugToolPermissionPolicy.
4. Make `/journey rescan` use the same deep scanner and always full-sync. Keep `/journey research` as explicit held-item refresh and `/journey help` compact/descriptive.
5. Bump persisted research data version so old Lunch Bag/Toolbox content-bearing keys recanonicalize safely, preserving earliest chronology when duplicates collapse.
6. Set fallback scan default/minimum to 5 ticks and migrate saved values below 5 at runtime.
7. Version all release sources as `1.0.0`, update README/live-test notes, run exact-SHA Forge CI, inspect artifact metadata and hashes.

## Safety constraints

- Never research arbitrary open machine/container GUI slots.
- Never preserve embedded inventory contents in a retrievable Journey template.
- Do not generically strip fluid, charge, enchantment, custom-name, tool-material, or unknown NBT.
- Recursive scan is manual only; normal 5t fallback remains shallow/cheap.
- Bounded recursion must fail closed on malformed optional-mod NBT and must not mutate the real held/inventory stack.
