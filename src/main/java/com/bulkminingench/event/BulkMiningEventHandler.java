package com.bulkminingench.event;

import com.bulkminingench.service.BulkMiningService;
import com.bulkminingench.service.BulkMiningService.HarvestRequest;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/**
 * Thin NeoForge event adapter that listens to block breaks and delegates to {@link BulkMiningService}.
 */
public final class BulkMiningEventHandler {

    private BulkMiningEventHandler() {
    }

    @SubscribeEvent
    public static void onBlockBreak(final BreakBlockEvent event) {
        if (event.isCanceled() || event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        final HarvestRequest request = new HarvestRequest(
                serverLevel,
                serverPlayer,
                event.getPos().immutable(),
                event.getState(),
                serverPlayer.getMainHandItem()
        );
        BulkMiningService.executeBulkMining(request);
    }
}
