package com.bulkminingench.registry;

import com.bulkminingench.BulkMiningEnchMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;

/**
 * Holds data-driven registry keys and tag keys for the Bulk Mining Enchantment mod.
 */
public final class ModEnchantments {

    public static final String BULK_MINING_PATH = "bulk_mining";
    public static final String COMMON_NAMESPACE = "c";
    public static final String ORES_TAG_PATH = "ores";

    public static final Identifier BULK_MINING_ID =
            Identifier.fromNamespaceAndPath(BulkMiningEnchMod.MOD_ID, BULK_MINING_PATH);

    public static final ResourceKey<Enchantment> BULK_MINING =
            ResourceKey.create(Registries.ENCHANTMENT, BULK_MINING_ID);

    public static final TagKey<Block> ORES_TAG =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(COMMON_NAMESPACE, ORES_TAG_PATH));

    private ModEnchantments() {
    }

    public static int getBulkMiningLevel(final LevelReader level, final ItemStack stack) {
        if (level == null || stack == null || stack.isEmpty()) {
            return 0;
        }
        return level.registryAccess()
                .lookup(Registries.ENCHANTMENT)
                .flatMap(registry -> registry.get(BULK_MINING))
                .map(stack::getEnchantmentLevel)
                .orElse(0);
    }
}
