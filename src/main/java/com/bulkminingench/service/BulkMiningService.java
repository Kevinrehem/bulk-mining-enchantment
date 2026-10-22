package com.bulkminingench.service;

import com.bulkminingench.domain.BulkMiningPolicy;
import com.bulkminingench.domain.BulkMiningPolicy.NeighborContext;
import com.bulkminingench.domain.BulkMiningPolicy.TriggerContext;
import com.bulkminingench.domain.BulkMiningVolumeCalculator;
import com.bulkminingench.domain.BulkMiningVolumeCalculator.BoundingBox;
import com.bulkminingench.domain.BulkMiningVolumeCalculator.CubeOffset;
import com.bulkminingench.registry.ModEnchantments;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;

/**
 * Coordinates server-side 3D bulk mining execution with feet-level floor protection,
 * 26-directional contiguous flood-fill, single-durability loss, and recursion guard.
 */
public final class BulkMiningService {

    private static final Set<UUID> ACTIVE_MINERS = ConcurrentHashMap.newKeySet();

    public record HarvestRequest(
            ServerLevel serverLevel,
            ServerPlayer player,
            BlockPos originPos,
            BlockState originState,
            ItemStack heldTool
    ) {
    }

    private BulkMiningService() {
    }

    public static void executeBulkMining(final HarvestRequest request) {
        final UUID playerId = request.player().getUUID();
        if (!ACTIVE_MINERS.add(playerId)) {
            return;
        }
        try {
            processVolumeIfEligible(request);
        } finally {
            ACTIVE_MINERS.remove(playerId);
        }
    }

    private static void processVolumeIfEligible(final HarvestRequest request) {
        final int enchantmentLevel = ModEnchantments.getBulkMiningLevel(request.serverLevel(), request.heldTool());
        final TriggerContext triggerContext = buildTriggerContext(request, enchantmentLevel);
        if (!BulkMiningPolicy.shouldTriggerBulkMining(triggerContext)) {
            return;
        }
        harvestConnectedNeighbors(request, enchantmentLevel);
    }

    private static TriggerContext buildTriggerContext(final HarvestRequest request, final int enchantmentLevel) {
        final boolean isPickaxe = request.heldTool().is(ItemTags.PICKAXES);
        final boolean canHarvestOrigin = isOriginMineableAndHarvestable(request);
        return new TriggerContext(
                request.serverLevel().isClientSide(),
                request.player().isShiftKeyDown(),
                enchantmentLevel,
                isPickaxe,
                canHarvestOrigin
        );
    }

    private static boolean isOriginMineableAndHarvestable(final HarvestRequest request) {
        final BlockState originState = request.originState();
        return originState.is(BlockTags.MINEABLE_WITH_PICKAXE)
                && EventHooks.doPlayerHarvestCheck(
                        request.player(),
                        originState,
                        request.serverLevel(),
                        request.originPos()
                );
    }

    private static void harvestConnectedNeighbors(final HarvestRequest request, final int enchantmentLevel) {
        final BoundingBox box = BulkMiningVolumeCalculator.calculateBoundingBox(
                enchantmentLevel,
                request.originPos().getY(),
                request.player().blockPosition().getY()
        );
        final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        final List<CubeOffset> connectedOffsets = BulkMiningVolumeCalculator.findConnectedNeighbors(
                box,
                offset -> isOffsetBreakable(request, cursor, offset)
        );
        for (final CubeOffset offset : connectedOffsets) {
            final BlockPos targetPos = request.originPos().offset(offset.dx(), offset.dy(), offset.dz());
            harvestSingleNeighbor(request, targetPos, request.serverLevel().getBlockState(targetPos));
        }
    }

    private static boolean isOffsetBreakable(
            final HarvestRequest request,
            final BlockPos.MutableBlockPos cursor,
            final CubeOffset offset
    ) {
        cursor.setWithOffset(request.originPos(), offset.dx(), offset.dy(), offset.dz());
        final BlockState state = request.serverLevel().getBlockState(cursor);
        final NeighborContext context = buildNeighborContext(request, cursor, state);
        return BulkMiningPolicy.canBreakNeighbor(context);
    }

    private static NeighborContext buildNeighborContext(
            final HarvestRequest request,
            final BlockPos pos,
            final BlockState state
    ) {
        final ServerLevel level = request.serverLevel();
        final ServerPlayer player = request.player();
        return new NeighborContext(
                isOreBlock(request.originState()),
                isOreBlock(state),
                state.isAir(),
                state.is(BlockTags.MINEABLE_WITH_PICKAXE),
                EventHooks.doPlayerHarvestCheck(player, state, level, pos),
                state.getDestroySpeed(level, pos),
                level.getBlockEntity(pos) != null,
                hasInteractionPermission(level, player, pos)
        );
    }

    private static void harvestSingleNeighbor(
            final HarvestRequest request,
            final BlockPos targetPos,
            final BlockState targetState
    ) {
        if (targetState.isAir()) {
            return;
        }
        final ServerLevel level = request.serverLevel();
        final ServerPlayer player = request.player();
        if (!player.isCreative()) {
            Block.dropResources(targetState, level, targetPos, null, player, request.heldTool());
            player.awardStat(Stats.BLOCK_MINED.get(targetState.getBlock()));
        }
        level.removeBlock(targetPos, false);
    }

    private static boolean hasInteractionPermission(
            final ServerLevel level,
            final ServerPlayer player,
            final BlockPos pos
    ) {
        final var gameMode = player.gameMode.getGameModeForPlayer();
        return level.mayInteract(player, pos) && !player.blockActionRestricted(level, pos, gameMode);
    }

    private static boolean isOreBlock(final BlockState state) {
        return state.is(ModEnchantments.ORES_TAG);
    }
}
