# Tasks: Initialize Bulk Mining Enchantment Mod

> **Change ID:** `init-bulk-mining-enchantment`
> **Linked Proposal:** [proposal.md](./proposal.md)
> **Status:** complete
>
> All tasks must be `[x]` before this change can be archived via `sdd-spec-archive`.

---

## Implementation Tasks

- [x] **T-01:** Create Gradle build scaffold (`settings.gradle`, `gradle.properties`, `build.gradle`) pinned to Minecraft `26.3`, NeoForge `26.3.0.95` (`neo_form_version=26.3-20261021.000000`), ModDevGradle `2.0.148`, and Java `25`.
- [x] **T-02:** Create mod metadata (`src/main/resources/META-INF/neoforge.mods.toml`, `pack.mcmeta`) and localization files (`en_us.json`, `pt_br.json`).
- [x] **T-03:** Create data-driven enchantment `data/bulkminingench/enchantment/bulk_mining.json` (3 levels, `#minecraft:pickaxes`) and register tags for Enchanting Infuser (`in_enchanting_infuser.json`, `in_advanced_enchanting_infuser.json`) and Vanilla (`in_enchanting_table.json`, `tradeable.json`, `on_random_loot.json`, `tooltip_order.json`).
- [x] **T-04:** Implement `ModEnchantments` registry holder keys (`bulkminingench:bulk_mining` and `#c:ores`) using `net.minecraft.resources.Identifier`.
- [x] **T-05:** Update `BulkMiningVolumeCalculator` with feet-level floor preservation (`calculateBoundingBox` anchoring `[playerFootY, playerFootY + 6]` when mining the 3rd/4th block in `7x7x7`, clamping `minY >= playerFootY` for blocks 2–4, and stepping down 1 block `minDy = -1` at block 1 `originY == playerFootY` for staircase descent) and 26-directional contiguous BFS flood-fill (`findConnectedNeighbors`).
- [x] **T-06:** Update `BulkMiningService` to resolve connected neighbor blocks via `BulkMiningVolumeCalculator.findConnectedNeighbors` before harvesting them in a second pass.
- [x] **T-07:** Implement `BulkMiningEnchMod` `@Mod("bulkminingench")` entrypoint wiring the event handler.

## Security Validation Tasks

- [x] **SV-01:** Verify server-side guard (`!level.isClientSide()` and `ServerPlayer`) and world interaction permission checks (`serverLevel.mayInteract` and `!serverPlayer.blockActionRestricted`) are enforced before breaking neighbor blocks.
- [x] **SV-02:** Verify enchantment level is clamped to `[1, 3]` and BFS visited set is strictly bounded to prevent DoS/infinite loops.
- [x] **SV-03:** Confirm no hardcoded secrets, tokens, or client-only imports (`net.minecraft.client.*`) exist in common/server classes.

## Shift-Left Test Suite Tasks

### Happy Path
- [x] **TP-01:** Verify `3x3x3` (26), `5x5x5` (124), and `7x7x7` (342) connected neighbor discovery when the volume is solid.
- [x] **TP-02:** Write unit test in `BulkMiningVolumeCalculatorTest` verifying Level III (`7x7x7`) anchors vertical Y to `[playerFootY, playerFootY + 6]` when mining the 3rd (`playerFootY + 2`) or 4th (`playerFootY + 3`) block of height, preserving `y < playerFootY`.
- [x] **TP-03:** Write unit test in `BulkMiningVolumeCalculatorTest` verifying 26-directional flood-fill traverses blocks touching by a full face or diagonally.
- [x] **TP-04:** Verify non-ore vs ore (`#c:ores`) policy behavior in `BulkMiningPolicyTest`.

### Edge Cases & Failure Modes
- [x] **TE-01:** Write unit test in `BulkMiningVolumeCalculatorTest` verifying a floating block surrounded by air returns `0` neighbors even if walls exist 1 block away.
- [x] **TE-02:** Write unit test in `BulkMiningVolumeCalculatorTest` verifying a 1-block air gap between two walls stops flood-fill from breaking the second wall.
- [x] **TE-03:** Verify non-ore origin ignores ore neighbors (`#c:ores`) in `BulkMiningPolicyTest`.
- [x] **TE-04:** Verify sneaking, client-side, `BlockEntity`, and indestructible blocks are rejected in `BulkMiningPolicyTest`.
- [x] **TE-05:** Verify out-of-bounds enchantment levels clamp safely in `BulkMiningVolumeCalculatorTest`.

## Documentation Tasks

- [x] **DOC-01:** Update domain specification `openspec/changes/init-bulk-mining-enchantment/specs/bulk-mining-enchantment.md`.

## Review Gate

- [x] All implementation tasks complete
- [x] All security validation tasks complete
- [x] All tests passing (`./gradlew test build`)
- [x] Proposal acceptance criteria verified (run `sdd-spec-validate`)
- [x] Ready for `sdd-spec-archive`
