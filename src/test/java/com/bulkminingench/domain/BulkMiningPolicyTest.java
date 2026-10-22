package com.bulkminingench.domain;

import com.bulkminingench.domain.BulkMiningPolicy.NeighborContext;
import com.bulkminingench.domain.BulkMiningPolicy.TriggerContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BulkMiningPolicyTest {

    @Test
    @DisplayName("TP-03: Breaking non-ore block allows breaking valid non-ore pickaxe-mineable neighbors")
    void shouldAllowBreakingNonOreNeighborWhenOriginIsNotOre() {
        final TriggerContext trigger = new TriggerContext(false, false, 1, true, true);
        assertTrue(BulkMiningPolicy.shouldTriggerBulkMining(trigger));

        final NeighborContext stoneNeighbor = new NeighborContext(
                false, false, false, true, true, 1.5F, false, true
        );
        assertTrue(BulkMiningPolicy.canBreakNeighbor(stoneNeighbor));
    }

    @Test
    @DisplayName("TP-04: Breaking ore block (#c:ores) allows breaking both ore and non-ore neighbors across all levels")
    void shouldAllowBreakingBothOresAndStoneWhenOriginIsOre() {
        for (int level = 1; level <= 3; level++) {
            assertTrue(BulkMiningPolicy.shouldTriggerBulkMining(new TriggerContext(false, false, level, true, true)));
        }

        final NeighborContext adjacentOre = new NeighborContext(
                true, true, false, true, true, 3.0F, false, true
        );
        final NeighborContext adjacentStone = new NeighborContext(
                true, false, false, true, true, 1.5F, false, true
        );

        assertTrue(BulkMiningPolicy.canBreakNeighbor(adjacentOre));
        assertTrue(BulkMiningPolicy.canBreakNeighbor(adjacentStone));
    }

    @Test
    @DisplayName("TE-01: Breaking non-ore block ignores ore neighbors (#c:ores) across Levels I, II, and III")
    void shouldIgnoreOreNeighborWhenOriginIsNonOre() {
        final NeighborContext oreNeighborWhenMiningStone = new NeighborContext(
                false, true, false, true, true, 3.0F, false, true
        );
        assertFalse(BulkMiningPolicy.canBreakNeighbor(oreNeighborWhenMiningStone));
        assertFalse(BulkMiningPolicy.isOreFilterSatisfied(false, true));
    }

    @Test
    @DisplayName("TE-02: Sneaking (Shift) or client-side execution or non-pickaxe disables bulk mining")
    void shouldDisableBulkMiningWhenSneakingOrClientSideOrInvalidTool() {
        assertFalse(BulkMiningPolicy.shouldTriggerBulkMining(null));
        assertFalse(BulkMiningPolicy.shouldTriggerBulkMining(new TriggerContext(true, false, 1, true, true)));
        assertFalse(BulkMiningPolicy.shouldTriggerBulkMining(new TriggerContext(false, true, 1, true, true)));
        assertFalse(BulkMiningPolicy.shouldTriggerBulkMining(new TriggerContext(false, false, 0, true, true)));
        assertFalse(BulkMiningPolicy.shouldTriggerBulkMining(new TriggerContext(false, false, 2, false, true)));
        assertFalse(BulkMiningPolicy.shouldTriggerBulkMining(new TriggerContext(false, false, 2, true, false)));
    }

    @Test
    @DisplayName("TE-03: Neighbors with BlockEntity or indestructible hardness (< 0, e.g. Bedrock) are rejected")
    void shouldRejectBlockEntitiesAndIndestructibleBlocks() {
        final NeighborContext chestOrInfuser = new NeighborContext(
                true, false, false, true, true, 2.5F, true, true
        );
        final NeighborContext bedrock = new NeighborContext(
                true, false, false, true, true, -1.0F, false, true
        );
        final NeighborContext airBlock = new NeighborContext(
                true, false, true, true, true, 0.0F, false, true
        );

        assertFalse(BulkMiningPolicy.canBreakNeighbor(null));
        assertFalse(BulkMiningPolicy.canBreakNeighbor(chestOrInfuser));
        assertFalse(BulkMiningPolicy.canBreakNeighbor(bedrock));
        assertFalse(BulkMiningPolicy.canBreakNeighbor(airBlock));
    }

    @Test
    @DisplayName("TE-04: Non-pickaxe blocks, insufficient tool tier, or restricted world permissions are rejected")
    void shouldRejectNonPickaxeOrInsufficientTierOrProtectedChunks() {
        final NeighborContext dirtBlock = new NeighborContext(
                false, false, false, false, true, 0.5F, false, true
        );
        final NeighborContext obsidianWithWoodenPickaxe = new NeighborContext(
                false, false, false, true, false, 50.0F, false, true
        );
        final NeighborContext protectedChunkBlock = new NeighborContext(
                false, false, false, true, true, 1.5F, false, false
        );

        assertFalse(BulkMiningPolicy.canBreakNeighbor(dirtBlock));
        assertFalse(BulkMiningPolicy.canBreakNeighbor(obsidianWithWoodenPickaxe));
        assertFalse(BulkMiningPolicy.canBreakNeighbor(protectedChunkBlock));
    }
}
