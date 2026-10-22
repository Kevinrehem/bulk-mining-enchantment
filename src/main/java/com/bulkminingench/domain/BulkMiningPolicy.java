package com.bulkminingench.domain;

/**
 * Pure domain policy governing when Bulk Mining triggers and which neighbor blocks
 * inside the 3x3x3 / 5x5x5 / 7x7x7 cube are eligible to be broken.
 */
public final class BulkMiningPolicy {

    public static final float INDESTRUCTIBLE_SPEED_THRESHOLD = 0.0F;

    public record TriggerContext(
            boolean isClientSide,
            boolean isShiftKeyDown,
            int enchantmentLevel,
            boolean isPickaxe,
            boolean canHarvestOrigin
    ) {
    }

    public record NeighborContext(
            boolean originIsOre,
            boolean neighborIsOre,
            boolean isAir,
            boolean isMineableByPickaxe,
            boolean canHarvestNeighbor,
            float destroySpeed,
            boolean hasBlockEntity,
            boolean hasWorldPermission
    ) {
    }

    private BulkMiningPolicy() {
    }

    public static boolean shouldTriggerBulkMining(final TriggerContext context) {
        if (context == null || context.isClientSide() || context.isShiftKeyDown()) {
            return false;
        }
        if (BulkMiningVolumeCalculator.radiusForLevel(context.enchantmentLevel()) <= 0) {
            return false;
        }
        return context.isPickaxe() && context.canHarvestOrigin();
    }

    public static boolean canBreakNeighbor(final NeighborContext context) {
        if (context == null || context.isAir() || !context.hasWorldPermission()) {
            return false;
        }
        if (context.hasBlockEntity() || context.destroySpeed() < INDESTRUCTIBLE_SPEED_THRESHOLD) {
            return false;
        }
        if (!context.isMineableByPickaxe() || !context.canHarvestNeighbor()) {
            return false;
        }
        return isOreFilterSatisfied(context.originIsOre(), context.neighborIsOre());
    }

    public static boolean isOreFilterSatisfied(final boolean originIsOre, final boolean neighborIsOre) {
        if (!originIsOre && neighborIsOre) {
            return false;
        }
        return true;
    }
}
