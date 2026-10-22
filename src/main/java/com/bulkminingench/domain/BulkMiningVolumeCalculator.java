package com.bulkminingench.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Pure domain calculator for 3D cubic bulk-mining volumes (3x3x3, 5x5x5, 7x7x7),
 * feet-level floor preservation, and 26-directional contiguous flood-fill (face + diagonal).
 */
public final class BulkMiningVolumeCalculator {

    public static final int MIN_ENCHANTMENT_LEVEL = 1;
    public static final int MAX_ENCHANTMENT_LEVEL = 3;
    public static final int LEVEL_THREE_RADIUS = 3;
    public static final int DISABLED_RADIUS = 0;

    public static final int FIRST_BLOCK_HEIGHT_OFFSET = 0;
    public static final int SECOND_BLOCK_HEIGHT_OFFSET = 1;
    public static final int THIRD_BLOCK_HEIGHT_OFFSET = 2;
    public static final int FOURTH_BLOCK_HEIGHT_OFFSET = 3;
    public static final int SEVEN_BY_SEVEN_MAX_HEIGHT_OFFSET = 6;
    public static final int STAIRCASE_DESCENT_MIN_DY = -1;

    private static final int STEP_MIN = -1;
    private static final int STEP_MAX = 1;
    private static final CubeOffset ORIGIN = new CubeOffset(0, 0, 0);
    private static final List<CubeOffset> TWENTY_SIX_DIRECTIONS = buildUnitDirections();

    public record CubeOffset(int dx, int dy, int dz) {
        public boolean isOrigin() {
            return this.dx == 0 && this.dy == 0 && this.dz == 0;
        }

        public CubeOffset add(final CubeOffset delta) {
            return new CubeOffset(this.dx + delta.dx(), this.dy + delta.dy(), this.dz + delta.dz());
        }
    }

    public record BoundingBox(int minDx, int maxDx, int minDy, int maxDy, int minDz, int maxDz) {
        public static final BoundingBox EMPTY = new BoundingBox(0, -1, 0, -1, 0, -1);

        public boolean isEmpty() {
            return this.minDx > this.maxDx || this.minDy > this.maxDy || this.minDz > this.maxDz;
        }

        public boolean contains(final CubeOffset offset) {
            if (offset == null || this.isEmpty()) {
                return false;
            }
            final boolean inX = offset.dx() >= this.minDx && offset.dx() <= this.maxDx;
            final boolean inY = offset.dy() >= this.minDy && offset.dy() <= this.maxDy;
            final boolean inZ = offset.dz() >= this.minDz && offset.dz() <= this.maxDz;
            return inX && inY && inZ;
        }
    }

    private record BfsState(
            BoundingBox box,
            Predicate<CubeOffset> isBreakable,
            Queue<CubeOffset> queue,
            Set<CubeOffset> visited,
            List<CubeOffset> connected
    ) {
    }

    private BulkMiningVolumeCalculator() {
    }

    public static int radiusForLevel(final int rawLevel) {
        if (rawLevel < MIN_ENCHANTMENT_LEVEL) {
            return DISABLED_RADIUS;
        }
        return Math.min(rawLevel, MAX_ENCHANTMENT_LEVEL);
    }

    public static int sideLengthForLevel(final int rawLevel) {
        final int radius = radiusForLevel(rawLevel);
        if (radius == DISABLED_RADIUS) {
            return DISABLED_RADIUS;
        }
        return (radius * 2) + 1;
    }

    public static BoundingBox calculateBoundingBox(
            final int rawLevel,
            final int originY,
            final int playerFootY
    ) {
        final int radius = radiusForLevel(rawLevel);
        if (radius == DISABLED_RADIUS) {
            return BoundingBox.EMPTY;
        }
        final int heightFromFeet = originY - playerFootY;
        final int minDy = calculateMinDy(radius, heightFromFeet);
        final int maxDy = calculateMaxDy(radius, heightFromFeet);
        return new BoundingBox(-radius, radius, minDy, maxDy, -radius, radius);
    }

    public static List<CubeOffset> findConnectedNeighbors(
            final BoundingBox box,
            final Predicate<CubeOffset> isBreakable
    ) {
        if (box == null || box.isEmpty() || isBreakable == null) {
            return List.of();
        }
        final BfsState state = new BfsState(
                box,
                isBreakable,
                new ArrayDeque<>(),
                new HashSet<>(),
                new ArrayList<>()
        );
        state.visited().add(ORIGIN);
        state.queue().add(ORIGIN);
        while (!state.queue().isEmpty()) {
            exploreAdjacentOffsets(state.queue().poll(), state);
        }
        return Collections.unmodifiableList(state.connected());
    }

    public static List<CubeOffset> calculateNeighborOffsets(final int rawLevel) {
        final int radius = radiusForLevel(rawLevel);
        if (radius == DISABLED_RADIUS) {
            return List.of();
        }
        final BoundingBox centeredBox = new BoundingBox(-radius, radius, -radius, radius, -radius, radius);
        return findConnectedNeighbors(centeredBox, offset -> true);
    }

    private static int calculateMinDy(final int radius, final int heightFromFeet) {
        if (heightFromFeet == FIRST_BLOCK_HEIGHT_OFFSET) {
            return STAIRCASE_DESCENT_MIN_DY;
        }
        if (isWithinFeetProtectionZone(heightFromFeet)) {
            return Math.max(-radius, -heightFromFeet);
        }
        return -radius;
    }

    private static int calculateMaxDy(final int radius, final int heightFromFeet) {
        if (isLevelThreeThirdOrFourthBlock(radius, heightFromFeet)) {
            return SEVEN_BY_SEVEN_MAX_HEIGHT_OFFSET - heightFromFeet;
        }
        return radius;
    }

    private static boolean isWithinFeetProtectionZone(final int heightFromFeet) {
        return heightFromFeet >= SECOND_BLOCK_HEIGHT_OFFSET && heightFromFeet <= FOURTH_BLOCK_HEIGHT_OFFSET;
    }

    private static boolean isLevelThreeThirdOrFourthBlock(final int radius, final int heightFromFeet) {
        if (radius != LEVEL_THREE_RADIUS) {
            return false;
        }
        return heightFromFeet == THIRD_BLOCK_HEIGHT_OFFSET || heightFromFeet == FOURTH_BLOCK_HEIGHT_OFFSET;
    }

    private static void exploreAdjacentOffsets(final CubeOffset current, final BfsState state) {
        for (final CubeOffset step : TWENTY_SIX_DIRECTIONS) {
            tryVisitNeighbor(current.add(step), state);
        }
    }

    private static void tryVisitNeighbor(final CubeOffset candidate, final BfsState state) {
        if (!state.box().contains(candidate) || !state.visited().add(candidate)) {
            return;
        }
        if (state.isBreakable().test(candidate)) {
            state.connected().add(candidate);
            state.queue().add(candidate);
        }
    }

    private static List<CubeOffset> buildUnitDirections() {
        final List<CubeOffset> directions = new ArrayList<>(26);
        for (int dx = STEP_MIN; dx <= STEP_MAX; dx++) {
            appendDirectionPlane(dx, directions);
        }
        return Collections.unmodifiableList(directions);
    }

    private static void appendDirectionPlane(final int dx, final List<CubeOffset> directions) {
        for (int dy = STEP_MIN; dy <= STEP_MAX; dy++) {
            for (int dz = STEP_MIN; dz <= STEP_MAX; dz++) {
                addIfNonOriginDirection(new CubeOffset(dx, dy, dz), directions);
            }
        }
    }

    private static void addIfNonOriginDirection(final CubeOffset offset, final List<CubeOffset> directions) {
        if (!offset.isOrigin()) {
            directions.add(offset);
        }
    }
}
