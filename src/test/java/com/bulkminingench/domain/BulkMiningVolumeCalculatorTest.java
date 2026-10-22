package com.bulkminingench.domain;

import com.bulkminingench.domain.BulkMiningVolumeCalculator.BoundingBox;
import com.bulkminingench.domain.BulkMiningVolumeCalculator.CubeOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BulkMiningVolumeCalculatorTest {

    @Test
    @DisplayName("TP-01: Solid volumes produce 26 (3x3x3), 124 (5x5x5), and 342 (7x7x7) connected neighbors")
    void shouldCalculateSolidCubeNeighborsAcrossAllLevels() {
        assertEquals(1, BulkMiningVolumeCalculator.radiusForLevel(1));
        assertEquals(3, BulkMiningVolumeCalculator.sideLengthForLevel(1));
        final List<CubeOffset> levelOne = BulkMiningVolumeCalculator.calculateNeighborOffsets(1);
        assertEquals(26, levelOne.size());
        assertFalse(levelOne.contains(new CubeOffset(0, 0, 0)));

        assertEquals(2, BulkMiningVolumeCalculator.radiusForLevel(2));
        assertEquals(5, BulkMiningVolumeCalculator.sideLengthForLevel(2));
        final List<CubeOffset> levelTwo = BulkMiningVolumeCalculator.calculateNeighborOffsets(2);
        assertEquals(124, levelTwo.size());

        assertEquals(3, BulkMiningVolumeCalculator.radiusForLevel(3));
        assertEquals(7, BulkMiningVolumeCalculator.sideLengthForLevel(3));
        final List<CubeOffset> levelThree = BulkMiningVolumeCalculator.calculateNeighborOffsets(3);
        assertEquals(342, levelThree.size());
    }

    @Test
    @DisplayName("TP-02: In 7x7x7 (Level III), mining 3rd or 4th block of height keeps floor at playerFootY [playerFootY, playerFootY + 6]")
    void shouldPreservePlayerFeetFloorWhenMiningThirdOrFourthBlockInSevenBySeven() {
        final int playerFootY = 64;

        // 3rd block of height: originY = 66 (playerFootY + 2)
        final int thirdBlockOriginY = playerFootY + 2;
        final BoundingBox thirdBlockBox = BulkMiningVolumeCalculator.calculateBoundingBox(
                3, thirdBlockOriginY, playerFootY
        );
        assertEquals(-2, thirdBlockBox.minDy());
        assertEquals(4, thirdBlockBox.maxDy());
        assertEquals(playerFootY, thirdBlockOriginY + thirdBlockBox.minDy());
        assertEquals(playerFootY + 6, thirdBlockOriginY + thirdBlockBox.maxDy());

        final List<CubeOffset> thirdBlockNeighbors = BulkMiningVolumeCalculator.findConnectedNeighbors(
                thirdBlockBox, offset -> true
        );
        assertEquals(342, thirdBlockNeighbors.size());
        assertTrue(thirdBlockNeighbors.stream().allMatch(o -> (thirdBlockOriginY + o.dy()) >= playerFootY));

        // 4th block of height: originY = 67 (playerFootY + 3)
        final int fourthBlockOriginY = playerFootY + 3;
        final BoundingBox fourthBlockBox = BulkMiningVolumeCalculator.calculateBoundingBox(
                3, fourthBlockOriginY, playerFootY
        );
        assertEquals(-3, fourthBlockBox.minDy());
        assertEquals(3, fourthBlockBox.maxDy());
        assertEquals(playerFootY, fourthBlockOriginY + fourthBlockBox.minDy());
        assertEquals(playerFootY + 6, fourthBlockOriginY + fourthBlockBox.maxDy());

        // 2nd block of height (playerFootY + 1) preserves floor at playerFootY
        final int secondBlockOriginY = playerFootY + 1;
        final BoundingBox secondBlockBox = BulkMiningVolumeCalculator.calculateBoundingBox(
                3, secondBlockOriginY, playerFootY
        );
        assertEquals(-1, secondBlockBox.minDy());
        assertEquals(playerFootY, secondBlockOriginY + secondBlockBox.minDy());

        // 1st block of height (originY == playerFootY) steps down 1 block (minY = playerFootY - 1) for staircase descent
        for (int level = 1; level <= 3; level++) {
            final BoundingBox firstBlockBox = BulkMiningVolumeCalculator.calculateBoundingBox(
                    level, playerFootY, playerFootY
            );
            assertEquals(-1, firstBlockBox.minDy());
            assertEquals(playerFootY - 1, playerFootY + firstBlockBox.minDy());
        }
    }

    @Test
    @DisplayName("TP-03: 26-directional flood-fill traverses blocks touching by full face or diagonal corner")
    void shouldTraverseFaceAndDiagonalTouchingChain() {
        final BoundingBox box = BulkMiningVolumeCalculator.calculateBoundingBox(3, 70, 60);
        final Set<CubeOffset> diagonalAndFaceChain = Set.of(
                new CubeOffset(1, 1, 1),   // touches origin (0,0,0) diagonally (corner)
                new CubeOffset(2, 2, 1),   // touches (1,1,1) diagonally (edge)
                new CubeOffset(2, 2, 2),   // touches (2,2,1) by full face
                new CubeOffset(3, 3, 3)    // touches (2,2,2) diagonally (corner)
        );

        final List<CubeOffset> broken = BulkMiningVolumeCalculator.findConnectedNeighbors(
                box, diagonalAndFaceChain::contains
        );
        assertEquals(4, broken.size());
        assertTrue(broken.containsAll(diagonalAndFaceChain));
    }

    @Test
    @DisplayName("TE-01: Floating block surrounded by 1-block air gap does not break surrounding blocks")
    void shouldNotBreakSurroundingBlocksWhenOriginBlockIsFloating() {
        final BoundingBox box = BulkMiningVolumeCalculator.calculateBoundingBox(3, 67, 64);

        // All blocks at Chebyshev distance 1 are air; solid blocks exist at distance >= 2
        final List<CubeOffset> broken = BulkMiningVolumeCalculator.findConnectedNeighbors(
                box,
                offset -> Math.max(Math.abs(offset.dx()), Math.max(Math.abs(offset.dy()), Math.abs(offset.dz()))) >= 2
        );
        assertTrue(broken.isEmpty());
    }

    @Test
    @DisplayName("TE-02: 1-block air gap between two walls prevents breaking the disconnected wall")
    void shouldNotJumpOneBlockAirGapBetweenWalls() {
        final BoundingBox box = BulkMiningVolumeCalculator.calculateBoundingBox(3, 67, 64);

        // Wall A is at dx in [-1, 0], air gap at dx == 1, Wall B is at dx in [2, 3]
        final List<CubeOffset> broken = BulkMiningVolumeCalculator.findConnectedNeighbors(
                box,
                offset -> offset.dx() <= 0 || offset.dx() >= 2
        );

        assertFalse(broken.isEmpty());
        assertTrue(broken.stream().allMatch(offset -> offset.dx() <= 0));
        assertTrue(broken.stream().noneMatch(offset -> offset.dx() >= 2));
    }

    @Test
    @DisplayName("TE-05: Out-of-bounds levels <= 0 return empty volume and levels > 3 clamp to 7x7x7 (radius 3)")
    void shouldClampOutOfBoundsEnchantmentLevels() {
        assertEquals(0, BulkMiningVolumeCalculator.radiusForLevel(0));
        assertEquals(0, BulkMiningVolumeCalculator.radiusForLevel(-5));
        assertEquals(0, BulkMiningVolumeCalculator.sideLengthForLevel(0));
        assertTrue(BulkMiningVolumeCalculator.calculateBoundingBox(0, 64, 64).isEmpty());
        assertTrue(BulkMiningVolumeCalculator.calculateNeighborOffsets(0).isEmpty());

        assertEquals(3, BulkMiningVolumeCalculator.radiusForLevel(4));
        assertEquals(3, BulkMiningVolumeCalculator.radiusForLevel(255));
        final Set<CubeOffset> clampedOffsets = new HashSet<>(BulkMiningVolumeCalculator.calculateNeighborOffsets(100));
        assertEquals(342, clampedOffsets.size());
    }
}
