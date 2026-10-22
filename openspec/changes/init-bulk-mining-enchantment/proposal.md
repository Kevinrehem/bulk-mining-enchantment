# Proposal: Initialize Bulk Mining Enchantment Mod (`init-bulk-mining-enchantment`)

> **Change ID:** `init-bulk-mining-enchantment`
> **Status:** approved
> **Author:** @jaca
> **Created:** 2026-10-22
> **Last Updated:** 2026-10-22

---

## Summary

Create the **Bulk Mining Enchantment** (`bulkminingench`) mod for **Minecraft 26.3** and **NeoForge 26.3.0.95** (Java 25). This mod introduces the data-driven enchantment `bulkminingench:bulk_mining` ("Bulk Mining" / "Mineração em Massa") exclusively for pickaxes (`#minecraft:pickaxes`) with 3 levels: Level I (`3x3x3`, radius 1), Level II (`5x5x5`, radius 2), and Level III (`7x7x7`, radius 3). It includes:
1. **Feet-Level Floor Preservation & Staircase Descent:** When mining at the 2nd, 3rd, or 4th block of height (`1 <= originY - playerFootY <= 3`), the vertical mining window never breaks blocks below `playerFootY` (preserving the floor at `playerFootY - 1`), and in Level III (`7x7x7`) mining the 3rd or 4th block of height shifts the 7-block vertical window to `[playerFootY, playerFootY + 6]`. When mining at the 1st block of height (`originY == playerFootY`), the vertical window steps down 1 block (`minY = playerFootY - 1`, `minDy = -1`) to allow horizontal staircase descent.
2. **26-Directional Contiguous Connectivity (Flood-Fill):** Only blocks contiguous to the broken origin block — touching by a full face, edge, or diagonal corner (`max(|Δx|, |Δy|, |Δz|) == 1`) without a 1-block air/unbreakable gap — are broken. Floating blocks do not break surrounding disconnected blocks.
3. **Smart Ore Filtering (`#c:ores`), Sneak Bypass, Single Durability & Infuser Compatibility:** Breaking a non-ore ignores ores (`#c:ores`); breaking an ore breaks both ores and stone; sneaking disables bulk mining; consumes only 1 durability point per action; registered in `#enchantinginfuser:in_enchanting_infuser`, `#enchantinginfuser:in_advanced_enchanting_infuser`, and `#minecraft:in_enchanting_table`.

## Motivation

Mining large underground volumes in Minecraft 26.3 is tedious with 1x1x1 pickaxes, while naive 3x3x3+ hammers suffer from four major usability flaws:
1. Mining at eye level or the 3rd/4th block of height in a `7x7x7` area digs down into the floor beneath the player's feet (`y < playerFootY`).
2. Breaking a floating block (or a pillar separated by a 1-block gap) destroys disconnected walls/floors across the air gap.
3. Clearing stone accidentally destroys valuable ores before switching to Fortune/Silk Touch, or destroys `BlockEntity` containers/machines.
4. Players using **Enchanting Infuser** (`enchantinginfuser` v26.3.0) need the enchantment to appear natively in both the standard Enchanting Infuser and Advanced Enchanting Infuser UIs.

## Scope

### In Scope
- Gradle build configuration for **Minecraft 26.3**, **NeoForge 26.3.0.95** (`neo_form_version=26.3-20261021.000000`), **ModDevGradle 2.0.148**, and **Java 25**.
- Mod entrypoint `com.bulkminingench.BulkMiningEnchMod` and `META-INF/neoforge.mods.toml` declaring optional compatibility with `enchantinginfuser`.
- Data-driven enchantment definition `data/bulkminingench/enchantment/bulk_mining.json` (max level 3, `#minecraft:pickaxes`).
- Enchantment tag registrations (`in_enchanting_infuser`, `in_advanced_enchanting_infuser`, `in_enchanting_table`, `tradeable`, `on_random_loot`, `tooltip_order`) and localizations (`en_us.json`, `pt_br.json`).
- Clean Code & Design Pattern implementation:
  - `ModEnchantments`: holds `ResourceKey<Enchantment>` (`BULK_MINING`) and `#c:ores` TagKey using `Identifier`.
  - `BulkMiningVolumeCalculator`: pure domain calculator computing:
    - Vertical feet-level floor protection (`BoundingBox` clamping `minY >= playerFootY` when `0 <= originY - playerFootY <= 3` and shifting Level III `7x7x7` at 3rd/4th block height to `[playerFootY, playerFootY + 6]`).
    - 26-directional Breadth-First Search (`findConnectedNeighbors`) starting from `(0, 0, 0)` that only propagates through breakable neighbor blocks touching by face, edge, or diagonal corner (`Chebyshev distance == 1`), stopping at 1-block air/non-breakable gaps.
  - `BulkMiningPolicy`: pure domain policy enforcing sneak bypass, enchantment level bounds `[1, 3]`, ore filtering (`originIsOre` vs `neighborIsOre`), pickaxe harvestability, indestructible block protection (`destroySpeed >= 0`), and `BlockEntity` exclusion.
  - `BulkMiningService`: server-side coordinator that discovers connected breakable neighbors via `BulkMiningVolumeCalculator.findConnectedNeighbors` and harvests them with Fortune/Silk Touch and XP drops while consuming only 1 durability point.
  - `BulkMiningEventHandler`: thin NeoForge `BreakBlockEvent` observer registered on `NeoForge.EVENT_BUS`.
- Comprehensive JUnit 5 unit test suite (`BulkMiningVolumeCalculatorTest` and `BulkMiningPolicyTest`).

### Out of Scope
- Custom blocks, items, GUIs, or network packets.
- Applying Bulk Mining to shovels, axes, or hoes (strictly limited to `#minecraft:pickaxes`).
- Breaking blocks containing a `BlockEntity` or jumping across 1-block air gaps.

## Proposed Solution

1. **Feet-Level Bounding Box (`BulkMiningVolumeCalculator.calculateBoundingBox`):**
   - Given `(rawLevel, originY, playerFootY)`, compute `radius = radiusForLevel(rawLevel)` (`1`, `2`, or `3`).
   - Horizontal offsets always span `[-radius, +radius]` on `X` and `Z`.
   - Vertical offsets `(minDy, maxDy)`:
     - Let `heightFromFeet = originY - playerFootY`.
     - If `heightFromFeet == 0` (1st block of height, at player's feet): `minDy = -1` (`minY = playerFootY - 1`, stepping down 1 block for staircase descent) and `maxDy = radius`.
     - If `1 <= heightFromFeet <= 3` (2nd, 3rd, or 4th block of height relative to feet):
       - In `7x7x7` (`radius == 3`) when mining the 3rd or 4th block (`heightFromFeet == 2` or `heightFromFeet == 3`), the vertical range is anchored to `[playerFootY, playerFootY + 6]`, so `minDy = -heightFromFeet` and `maxDy = 6 - heightFromFeet`.
       - For any other case in `1 <= heightFromFeet <= 3` where `originY - radius < playerFootY`, clamp `minDy = -heightFromFeet` (`minY = playerFootY`) and `maxDy = radius`.
     - Otherwise (e.g., player explicitly mining downward below their feet `originY < playerFootY` or high overhead `heightFromFeet > 3`), use standard centered vertical offsets `[-radius, +radius]`.
2. **26-Directional Contiguous Flood-Fill (`BulkMiningVolumeCalculator.findConnectedNeighbors`):**
   - Runs a BFS from `(0, 0, 0)` within the computed `BoundingBox`.
   - At each step, inspects all 26 immediate neighbors `(cx + sx, cy + sy, cz + sz)` where `sx, sy, sz in {-1, 0, 1}` (not all zero, so blocks touch by a full face or diagonally).
   - Evaluates `Predicate<CubeOffset> isBreakable` for each unvisited offset inside the `BoundingBox`.
   - Only offsets where `isBreakable.test(offset)` returns `true` are added to the harvest list and enqueued for further expansion. Air gaps (`1` block of distance) or unbreakable blocks do not propagate the search.
3. **Single-Durability Harvest (`BulkMiningService`):**
   - First resolves the connected list of eligible `BlockPos` targets while all blocks are still in place in the `ServerLevel`, then harvests each target using `Block.dropResources` without extra tool durability loss.

### Impacted Files
| File | Change Type | Notes |
|------|-------------|-------|
| `src/main/java/com/bulkminingench/domain/BulkMiningVolumeCalculator.java` | Modify | Add `BoundingBox`, feet-level vertical anchoring (`playerFootY`), and 26-directional BFS `findConnectedNeighbors` |
| `src/main/java/com/bulkminingench/service/BulkMiningService.java` | Modify | Use `calculateBoundingBox` with `player.blockPosition().getY()` and `findConnectedNeighbors` before harvesting |
| `src/test/java/com/bulkminingench/domain/BulkMiningVolumeCalculatorTest.java` | Modify | Add unit tests for 7x7x7 3rd/4th block feet-level floor preservation, floating block isolation, 1-block gap blocking, and diagonal/face connectivity |

## Acceptance Criteria

- [x] **Given** a project configured with Gradle, **When** inspecting `gradle.properties` and `build.gradle`, **Then** `minecraft_version` is `26.3`, `neo_version` is `26.3.0.95`, `net.neoforged.moddev` is `2.0.148`, and Java toolchain is `25`.
- [x] **Given** a player in a world with Enchanting Infuser installed, **When** placing a Pickaxe (`#minecraft:pickaxes`) into the Enchanting Infuser or Advanced Enchanting Infuser, **Then** the `Bulk Mining` (`bulkminingench:bulk_mining`) enchantment appears as a selectable option with levels I, II, and III.
- [x] **Given** a player standing at `playerFootY` holding a Pickaxe with Bulk Mining III (`7x7x7`), **When** breaking a block at the 3rd (`playerFootY + 2`) or 4th (`playerFootY + 3`) block of height, **Then** the vertical mining window is anchored to `[playerFootY, playerFootY + 6]` and no blocks below `playerFootY` (the floor beneath the player's feet) are broken.
- [x] **Given** a player standing at `playerFootY` holding a Pickaxe with Bulk Mining I, II, or III, **When** breaking a block at the 1st block of height (`originY == playerFootY`), **Then** the vertical mining window steps down 1 block (`minDy = -1`, `minY = playerFootY - 1`) to enable horizontal staircase descent.
- [x] **Given** a floating block in mid-air or a cluster of blocks separated from surrounding walls/floors by a 1-block air gap, **When** the player breaks the block with Bulk Mining I, II, or III, **Then** only blocks contiguous to the origin block (touching by a full face or diagonally via 26-connectivity) are broken, and blocks across the 1-block gap remain untouched.
- [x] **Given** a player holding a pickaxe with Bulk Mining I, II, or III (not sneaking), **When** breaking a non-ore block, **Then** ore blocks (`#c:ores`) are ignored and do not propagate bulk mining into ores, whereas breaking an ore block (`#c:ores`) breaks both connected ores and stone.
- [x] **Given** a player sneaking (holding Shift) or encountering indestructible blocks, non-pickaxe blocks, or blocks with a `BlockEntity`, **Then** bulk mining is bypassed on Shift and special/indestructible blocks are preserved while consuming only 1 durability point.

## Defensive Security & Validation Considerations

### Input Validation
- **Server-Side Authority:** All block breaking and item/XP dropping execute strictly on the logical server (`ServerLevel` / `ServerPlayer`).
- **Level Clamping & Bounded BFS:** Enchantment levels are clamped to `[1, 3]` (maximum `7x7x7` = 343 positions) and the BFS tracks visited positions in a bounded set so cyclic neighbor graphs can never loop infinitely.
- **Block Safety Validation:** Enforces `destroySpeed >= 0`, `level.getBlockEntity(pos) == null`, `serverLevel.mayInteract(serverPlayer, neighborPos)`, and `EventHooks.doPlayerHarvestCheck`.

### Secret Isolation
- No API keys, credentials, tokens, or network endpoints are used in this mod.

### Authorization & Authentication Impact
- Respects server spawn protection / chunk interaction permissions via `serverLevel.mayInteract(serverPlayer, neighborPos)` and `!serverPlayer.blockActionRestricted(...)`.

## Testing Strategy & Shift-Left Plan

### Happy Path Tests
| Test ID | Scenario | Test Type | Expected Result |
|---------|----------|-----------|-----------------|
| TP-01 | Calculate radius and neighbor count for Level I (`3x3x3`), II (`5x5x5`), and III (`7x7x7`) | Unit (JUnit 5) | Produces 26, 124, and 342 connected neighbors when all blocks in the cube are solid |
| TP-02 | Level III (`7x7x7`) vertical alignment when mining 3rd (`playerFootY + 2`) or 4th (`playerFootY + 3`) block of height | Unit (JUnit 5) | Vertical world Y range is `[playerFootY, playerFootY + 6]` and never reaches `< playerFootY` |
| TP-03 | 26-directional flood-fill across blocks touching by full face or diagonal corner | Unit (JUnit 5) | Diagonal and face-connected chains within the bounding box are fully discovered |
| TP-04 | Mining non-ore vs ore block (`#c:ores`) | Unit (JUnit 5) | Non-ore origin ignores ores; ore origin breaks both ores and stone |

### Edge Case & Failure Mode Tests
| Test ID | Scenario | Test Type | Expected Result |
|---------|----------|-----------|-----------------|
| TE-01 | Floating single block surrounded by air (1-block gap to nearby walls/floor) | Unit (JUnit 5) | `findConnectedNeighbors` returns empty list (`0` extra blocks broken) |
| TE-02 | Two walls inside `7x7x7` separated by a 1-block air gap | Unit (JUnit 5) | Only the wall containing the broken origin block is broken; the wall across the 1-block gap is untouched |
| TE-03 | Mining non-ore block (`originIsOre=false`) when neighbor is an ore (`neighborIsOre=true`) | Unit (JUnit 5) | `BulkMiningPolicy.canBreakNeighbor` returns `false` |
| TE-04 | Sneaking (`isShiftKeyDown=true`), client side, `BlockEntity`, or Bedrock (`destroySpeed < 0`) | Unit (JUnit 5) | Policy rejects trigger/neighbor |
| TE-05 | Out-of-bounds enchantment levels (`level <= 0` or `level > 3`) | Unit (JUnit 5) | Clamped safely to `[0, 3]` |

## Risks and Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Removing a neighbor block during BFS iteration could alter adjacency checks for subsequent neighbors | Med | Med | Collect all connected breakable `BlockPos` targets via BFS first, then harvest all collected positions in a second pass |
| Infinite loop in flood-fill | Low | High | Track visited `CubeOffset` coordinates in a `HashSet` bounded strictly to the `BoundingBox` (`<= 343` nodes) |

## References

- [sdd-stack-contract.md](../../specs/sdd-stack-contract.md)
- [AGENTS.md](../../../.agents/rules/AGENTS.md)
- [bulk-mining-enchantment.md](./specs/bulk-mining-enchantment.md)
