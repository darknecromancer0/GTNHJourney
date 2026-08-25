# GTNH Journey pre7 Migration Debug Researcher Tool Design

## Status

Approved in chat for implementation as part of the pre7 migration/recovery work.

## Goal

Let an existing migrated GTNH world seed Journey research from blocks and inventories that already exist, without breaking machines or physically moving items.

The feature is intentionally an explicit migration/debug tool, not part of normal research progression.

## User-facing item

The tool is a registered custom item named **Debug Researcher Tool**.

- Visual base: vanilla stick texture.
- Visual effect: permanent enchanted glint.
- Max stack size: 1.
- No crafting recipe.
- Not added as a normal progression reward.
- Primary acquisition: `/journey debugtool`.
- The command is server-authoritative and returns exactly one tool to the invoking player, dropping it at the player only if inventory insertion cannot fit it.

The tool stores its current mode in its own NBT so the mode follows the physical tool across relogs.

## Interaction priority

The tool must take priority over machine/chest/block GUIs while held.

Implementation should use the 1.7.10 item-first interaction hook (`Item#onItemUseFirst`) so a successful debug-tool action returns `true` before normal `Block#onBlockActivated` handling. Right-click-air behavior uses `Item#onItemRightClick`.

The tool must never:

- break the target block;
- change block metadata;
- open or mutate the target inventory;
- consume, move, insert, extract, charge, fill, drain, or otherwise alter item stacks;
- force-load or generate chunks.

## Modes

Shift + right-click cycles modes in this exact order:

1. `BLOCK`
2. `CONTENTS`
3. `AREA_16`
4. back to `BLOCK`

Mode changes are shown in chat as one short line, for example:

`[Journey] Debug Researcher Tool: CONTENTS`

A normal right-click performs the current mode.

### BLOCK

Right-click a block to research the item representation of that exact placed block.

Resolution order:

1. Ask the target block for its Forge pick-block representation so modded machine blocks can expose the correct item/meta state.
2. If pick-block fails or returns null, fall back to `Item.getItemFromBlock(block)` plus the block's world damage/meta value.
3. Air, invalid item mappings, and mod exceptions are skipped safely.

Only the block item itself is observed. Tile inventory contents are not scanned in this mode.

Expected summary:

`[Journey] Migration BLOCK: +1 state` or `[Journey] Migration BLOCK: +0 states`

### CONTENTS

Right-click a block to inspect the target TileEntity without opening its GUI.

Behavior:

- If the TileEntity implements `IInventory`, copy each non-null stack from every inventory slot.
- `ISidedInventory` is naturally covered through `IInventory`.
- Slot reads are isolated so one broken modded slot cannot abort the entire scan.
- The physical inventory remains unchanged.
- The target block itself is not implicitly researched in CONTENTS mode; BLOCK exists for that purpose.
- Fluid tanks are not converted into invented fluid-container items in pre7. Only actual item stacks exposed by the TileEntity inventory are researched.

Expected summary:

`[Journey] Migration CONTENTS: 27 stacks scanned, +8 states`

### AREA_16

Right-click either air or a block to scan a **16 x 16 x 16** cube centered on the player's current integer block position.

Exact bounds for player block coordinate `(px, py, pz)` are:

- `x = px - 8 .. px + 7`
- `y = py - 8 .. py + 7`
- `z = pz - 8 .. pz + 7`

That is exactly 4096 block positions.

For each position:

1. Skip coordinates outside the world's valid Y range.
2. Skip positions whose chunk is not already loaded. Never load/generate a chunk for this tool.
3. Resolve and collect the placed block item using the same resolver as BLOCK mode.
4. If the loaded TileEntity implements `IInventory`, collect copies of all non-null item stacks.

Before applying research, candidate observations are deduplicated by Journey semantic identity so thousands of repeated stone/cables/machines do not create thousands of research attempts.

The scan must not enumerate the entire world, region files, unloaded chunks, entities, dropped items, or other players.

Expected summary:

`[Journey] Migration AREA_16: 4096 positions, 318 block candidates, 14 inventories, +63 states`

## Research integration

Debug-tool imports use the same authoritative `PlayerResearchService` and semantic expansion/normalization rules as normal Journey research. The tool does not write research keys or templates directly.

Bulk migration operations must not send one network refresh per candidate. Add a bulk observation path that:

1. validates and deduplicates observed stacks;
2. applies all semantic endpoints server-side;
3. records one explicit migration transaction in the recovery journal containing only states newly added by that action;
4. performs one Journey research sync after the operation;
5. performs one direct-panel refresh on the client;
6. emits one migration summary line instead of hundreds of `Unlocked:` notifications.

Normal gameplay acquisition keeps its normal incremental `Unlocked: <name>` behavior.

## Undo/redo integration

Each physical tool action is one explicit transaction:

- one BLOCK click = one transaction;
- one CONTENTS click = one transaction;
- one AREA_16 click = one transaction.

If an action adds zero new states, no undo transaction is created.

`/journey undo` removes exactly the states newly added by the last explicit migration action, without deleting unrelated research that existed before it.

`/journey redo` reapplies that transaction if no later research mutation invalidated the redo branch.

## Permissions and multiplayer safety

`/journey debugtool` is a debug/admin-style command. The server validates permission for this subcommand independently of harmless `/journey` diagnostic commands.

On an integrated singleplayer server the world owner can use it. On a dedicated multiplayer server it must require operator-level permission.

Research remains scoped to the player using the tool. AREA_16 never grants research to nearby players.

## Performance and safety

AREA_16 has a hard fixed scan volume of 4096 positions. No configurable unbounded radius is part of pre7.

The implementation must:

- operate server-side for authoritative world/inventory reads;
- use copies of observed ItemStacks;
- catch modded block/TileEntity runtime and linkage failures at the smallest practical boundary;
- avoid full NEI item-list rebuilds;
- avoid chunk loads;
- deduplicate candidate stacks before bulk research;
- expose scan counters in diagnostics so live-test dumps show debug scan count, positions visited, inventories visited, candidate identities, and newly unlocked states.

## Explicit non-goals for pre7

- Reconstructing every item the player ever held historically from save history. GTNH/vanilla saves do not provide a complete lifetime ItemStack journal.
- Offline scanning of every region/chunk file in the world.
- Synthesizing fluid-container items from machine fluid tanks.
- Automatically granting all recipes/NEI items.
- Destructive block replacement or inventory extraction.

The intended migration workflow is to walk through the existing base with AREA_16 and use BLOCK/CONTENTS for targeted recovery where needed.

## Regression criteria

Tests must prove at minimum:

- mode cycle is BLOCK -> CONTENTS -> AREA_16 -> BLOCK;
- mode is stored in tool NBT;
- BLOCK resolver prefers pick-block and has a safe fallback;
- CONTENTS copies stacks and never mutates source inventory;
- AREA_16 produces exactly 4096 coordinate positions before world-bound/chunk filtering;
- unloaded chunks are skipped without loading them;
- repeated semantic candidates are deduplicated;
- one bulk action produces one transaction and one summary;
- zero-new-state actions produce no undo transaction;
- undo removes only states added by that tool action;
- redo reapplies them;
- a new mutation after undo invalidates redo;
- the tool interaction consumes the right-click before normal block activation.
