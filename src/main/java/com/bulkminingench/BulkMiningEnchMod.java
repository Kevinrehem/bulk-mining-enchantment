package com.bulkminingench;

import com.bulkminingench.event.BulkMiningEventHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Main mod entrypoint for Bulk Mining Enchantment (Minecraft 26.3 / NeoForge 26.3.0.95).
 */
@Mod(BulkMiningEnchMod.MOD_ID)
public final class BulkMiningEnchMod {

    public static final String MOD_ID = "bulkminingench";

    public BulkMiningEnchMod(final IEventBus modEventBus, final ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(BulkMiningEventHandler.class);
    }
}
