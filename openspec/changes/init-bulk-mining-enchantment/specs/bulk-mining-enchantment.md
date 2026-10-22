---
id: "bulk-mining-enchantment"
title: "Bulk Mining Enchantment — Living Specification"
version: "1.1.0"
status: "draft"
owners:
  - "@jaca"
scope:
  paths:
    - "src/main/java/com/bulkminingench/**"
    - "src/main/resources/data/**"
    - "src/main/resources/assets/**"
resources: []
---

# Bulk Mining Enchantment — Living Specification

> This is the authoritative source of truth for this module's current functional state.
> Update this file only after merging a completed change from `openspec/changes/`.

---

## Context & Objectives

The `bulkminingench` mod provides a 3-tier pickaxe enchantment (`bulkminingench:bulk_mining`) for **Minecraft 26.3** (NeoForge `26.3.0.95`) that enables cubic bulk mining (`3x3x3`, `5x5x5`, `7x7x7`) with feet-level floor protection, 26-directional contiguous flood-fill (no air-gap jumping), smart ore protection, and full compatibility with **Enchanting Infuser** (`enchantinginfuser`).

## Functional Requirements

- **[RF-01] Data-Driven Pickaxe Enchantment:** `bulkminingench:bulk_mining` supports `#minecraft:pickaxes` as primary and supported items, has `max_level: 3`, and has no exclusive set (fully compatible with Fortune, Silk Touch, Efficiency, Unbreaking, and Mending).
- **[RF-02] Infuser & Vanilla Discovery:** Registered in `#enchantinginfuser:in_enchanting_infuser`, `#enchantinginfuser:in_advanced_enchanting_infuser`, `#minecraft:in_enchanting_table`, `#minecraft:tradeable`, `#minecraft:on_random_loot`, and `#minecraft:tooltip_order`.
- **[RF-03] Cubic Volume, Feet-Level Floor Preservation & Staircase Descent:**
  - Level I: `3x3x3` ($r = 1$), Level II: `5x5x5` ($r = 2$), Level III: `7x7x7` ($r = 3$).
  - When mining at the 1st block of height (`originY == playerFootY`), `minDy = -1` (`minY = playerFootY - 1`), stepping down 1 block to allow staircase descent.
  - When mining from the 2nd to the 4th block of height (`1 <= originY - playerFootY <= 3`), the vertical bounding box never breaks below `playerFootY` (`minY >= playerFootY`). In Level III (`7x7x7`), mining the 3rd or 4th block of height (`originY - playerFootY` in `{2, 3}`) anchors the 7-block vertical window to `[playerFootY, playerFootY + 6]`.
- **[RF-04] 26-Directional Contiguous Flood-Fill:** Only blocks within the bounding box that are contiguous to the broken origin block — touching by a full face, edge, or diagonal corner (`max(|Δx|, |Δy|, |Δz|) == 1`) through breakable blocks — are broken. Floating blocks or blocks separated by at least 1 block of air/non-breakable space are never broken.
- **[RF-05] Smart Ore Filtering (`#c:ores`):** Across all 3 levels, if the origin broken block is NOT in `#c:ores`, neighbor blocks in `#c:ores` are skipped. If the origin broken block IS in `#c:ores`, both ore and non-ore valid neighbor blocks are broken.
- **[RF-06] Sneak Bypass:** Holding Shift (`player.isShiftKeyDown()`) disables bulk mining and breaks only the target block (`1x1x1`).
- **[RF-07] Neighbor Block Safety & Tool Tier:** Neighbor blocks are broken only if they are in `#minecraft:mineable/pickaxe`, the held pickaxe satisfies `EventHooks.doPlayerHarvestCheck`, `destroySpeed >= 0`, `level.getBlockEntity(pos) == null`, and the player has interaction permission.
- **[RF-08] Single Durability & Enchanted Drops:** Neighbor blocks drop items and XP applying the held pickaxe's enchantments (Fortune / Silk Touch), while the entire bulk-mining action consumes only the 1 durability point from the origin block break.

## Non-Functional Requirements

- **[RNF-01] Version & Toolchain Compliance:** Strictly targets Minecraft `26.3`, NeoForge `26.3.0.95`, ModDevGradle `2.0.148`, and Java `25` using `net.minecraft.resources.Identifier`.
- **[RNF-02] Tick Performance & Re-Entrancy Safety:** Uses bounded BFS (`<= 343` nodes), clamps enchantment level to `3`, and prevents recursive event loops via a per-player guard.

## Out of Scope

- Non-pickaxe tools (shovels, axes, hoes).
- Breaking blocks that own a `BlockEntity` or blocks across an air gap.

## Revision History

| Version | Date | Author | Summary |
|---------|------|--------|---------|
| 1.0.0 | 2026-10-22 | @jaca | Initial specification for `bulkminingench:bulk_mining` |
| 1.1.0 | 2026-10-22 | @jaca | Add feet-level floor preservation for 7x7x7 (blocks 1–4) and 26-directional contiguous flood-fill |
