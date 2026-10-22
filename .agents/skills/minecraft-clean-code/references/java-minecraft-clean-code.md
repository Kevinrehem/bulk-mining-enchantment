# Java & Minecraft Modding Clean Code Reference

## 1. Guard Clauses & Early Returns (Logical Side Check)

### Bad (Deep Nesting & Magic Numbers)
```java
public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
    if (!level.isClientSide()) {
        if (entity instanceof Player player) {
            if (level.getGameTime() % 20 == 0) {
                if (stack.hasTag() && stack.getTag().getBoolean("active")) {
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
                }
            }
        }
    }
}
```

### Good (Flat Guard Clauses & Named Constants)
```java
private static final int EFFECT_CHECK_INTERVAL_TICKS = 20;
private static final int REGEN_DURATION_TICKS = 60;

public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
    if (level.isClientSide()) return;
    if (level.getGameTime() % EFFECT_CHECK_INTERVAL_TICKS != 0) return;
    if (!(entity instanceof ServerPlayer player)) return;
    if (!isActive(stack)) return;

    applyPassiveEffect(player);
}

private void applyPassiveEffect(ServerPlayer player) {
    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGEN_DURATION_TICKS, 0, true, false));
}
```

---

## 2. Singleton Item/Block State Prohibited

### Bad (Mutable Field in Item Class)
```java
public class TarotCardItem extends Item {
    // BUG: Shared across all players and all stacks on the server and client!
    private boolean isActivated = false;
    private int cooldown = 0;
}
```

### Good (State Stored on ItemStack / Data Components / Player Attachment)
```java
public class TarotCardItem extends Item {
    public TarotCardItem(Properties properties) {
        super(properties);
    }

    public boolean isActivated(ItemStack stack) {
        // Read state from ItemStack DataComponents or NBT
        return stack.getOrDefault(ModDataComponents.CARD_ACTIVATED, false);
    }
}
```

---

## 3. Zero-Allocation Tick Loops & BlockPos Iteration

### Bad (Allocating thousands of BlockPos and Streams per tick)
```java
public void scanArea(ServerLevel level, BlockPos center, int radius) {
    List<BlockPos> list = new ArrayList<>();
    for (int x = -radius; x <= radius; x++) {
        for (int y = -radius; y <= radius; y++) {
            for (int z = -radius; z <= radius; z++) {
                list.add(new BlockPos(center.getX() + x, center.getY() + y, center.getZ() + z));
            }
        }
    }
    list.stream()
        .filter(pos -> level.getBlockState(pos).is(Blocks.GOLD_BLOCK))
        .forEach(pos -> spawnParticles(level, pos));
}
```

### Good (MutableBlockPos Reuse)
```java
public void scanArea(ServerLevel level, BlockPos center, int radius) {
    BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    for (int x = -radius; x <= radius; x++) {
        for (int y = -radius; y <= radius; y++) {
            for (int z = -radius; z <= radius; z++) {
                cursor.setWithOffset(center, x, y, z);
                if (level.getBlockState(cursor).is(Blocks.GOLD_BLOCK)) {
                    spawnParticles(level, cursor);
                }
            }
        }
    }
}
```

---

## 4. Safe Mixin Declarations

### Bad (Destructive `@Overwrite` and unprefixed methods)
```java
@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    private int bonusArmor; // Collision risk with other mods!

    @Overwrite // Breaks every other mod hooking getArmorValue!
    public int getArmorValue() {
        return 10 + this.bonusArmor;
    }
}
```

### Good (Composable `@ModifyReturnValue` and `modid$` prefix)
```java
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Unique
    private int tarotcards$bonusArmor;

    @ModifyReturnValue(method = "getArmorValue", at = @At("RETURN"))
    private int tarotcards$applyTarotArmorBonus(int original) {
        return original + this.tarotcards$bonusArmor;
    }
}
```
