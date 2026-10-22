# Minecraft Modding Design Patterns Catalog

## 1. Strategy Pattern (Pluggable Card / Item Effects)
Instead of giant `switch` statements or deeply nested subclasses for every Tarot Card or magical item, encapsulate the effect logic in a Strategy interface.

```java
@FunctionalInterface
public interface TarotEffectStrategy {
    void apply(ServerLevel level, ServerPlayer player, ItemStack cardStack);
}

public final class TheSunEffect implements TarotEffectStrategy {
    private static final int DURATION_TICKS = 100;

    @Override
    public void apply(ServerLevel level, ServerPlayer player, ItemStack cardStack) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, DURATION_TICKS, 0, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, DURATION_TICKS, 0, true, false));
    }
}

public class TarotCardItem extends Item {
    private final TarotEffectStrategy effectStrategy;

    public TarotCardItem(Properties properties, TarotEffectStrategy effectStrategy) {
        super(properties);
        this.effectStrategy = Objects.requireNonNull(effectStrategy);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;
        if (!(entity instanceof ServerPlayer player)) return;
        if (!isCardActive(stack)) return;

        this.effectStrategy.apply(serverLevel, player, stack);
    }
}
```

---

## 2. Builder Pattern (Card / Item Configuration)
When items or cards share multiple optional attributes (cooldown, mana cost, rarity, passive vs. active effect, tooltip color), use a fluent Builder with a Java `record`.

```java
public record TarotCardDefinition(
    String id,
    Rarity rarity,
    int tickInterval,
    TarotEffectStrategy passiveEffect,
    Optional<TarotEffectStrategy> activeEffect
) {
    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private Rarity rarity = Rarity.UNCOMMON;
        private int tickInterval = 20;
        private TarotEffectStrategy passiveEffect = (lvl, p, stack) -> {};
        private TarotEffectStrategy activeEffect = null;

        private Builder(String id) {
            this.id = Objects.requireNonNull(id);
        }

        public Builder rarity(Rarity rarity) {
            this.rarity = Objects.requireNonNull(rarity);
            return this;
        }

        public Builder tickInterval(int ticks) {
            if (ticks <= 0) throw new IllegalArgumentException("tickInterval must be > 0");
            this.tickInterval = ticks;
            return this;
        }

        public Builder passiveEffect(TarotEffectStrategy effect) {
            this.passiveEffect = Objects.requireNonNull(effect);
            return this;
        }

        public Builder activeEffect(TarotEffectStrategy effect) {
            this.activeEffect = Objects.requireNonNull(effect);
            return this;
        }

        public TarotCardDefinition build() {
            return new TarotCardDefinition(
                id, rarity, tickInterval, passiveEffect, Optional.ofNullable(activeEffect)
            );
        }
    }
}
```

---

## 3. Deferred Factory / Registry Pattern
Never instantiate items or blocks statically. Use lazy suppliers so objects are constructed only when Minecraft fires the appropriate Registry event.

```java
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, TarotCardsMod.MOD_ID);

    public static final RegistryObject<Item> THE_SUN_CARD = registerCard(
        "the_sun",
        () -> TarotCardDefinition.builder("the_sun")
            .rarity(Rarity.RARE)
            .passiveEffect(new TheSunEffect())
            .build()
    );

    private static RegistryObject<Item> registerCard(String name, Supplier<TarotCardDefinition> defSupplier) {
        return ITEMS.register(name, () -> new TarotCardItem(
            new Item.Properties().stacksTo(1),
            defSupplier.get()
        ));
    }

    private ModItems() {}
}
```

---

## 4. Codec / Data-Driven Serialization Pattern
Use Mojang's `Codec` and `StreamCodec` with immutable `record` classes for NBT, Data Components, Datapacks, and Network Payloads.

```java
public record CardStateComponent(boolean active, int charges, long lastUsedGameTime) {
    public static final CardStateComponent DEFAULT = new CardStateComponent(false, 0, 0L);

    public static final Codec<CardStateComponent> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.BOOL.fieldOf("active").forGetter(CardStateComponent::active),
            Codec.INT.fieldOf("charges").forGetter(CardStateComponent::charges),
            Codec.LONG.fieldOf("last_used").forGetter(CardStateComponent::lastUsedGameTime)
        ).apply(instance, CardStateComponent::new)
    );

    public CardStateComponent withToggledActive(long currentGameTime) {
        return new CardStateComponent(!this.active, this.charges, currentGameTime);
    }
}
```

---

## 5. Observer / Thin Event Handler Pattern
Keep event listeners focused on filtering the event and delegating to a dedicated domain handler.

```java
@Mod.EventBusSubscriber(modid = TarotCardsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CombatEventListeners {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;

        float modifiedDamage = TarotCombatService.calculateDamageWithCards(attacker, event.getAmount());
        event.setAmount(modifiedDamage);
    }

    private CombatEventListeners() {}
}
```

---

## 6. Decorator / Interceptor Pattern via Mixin
Keep Mixin classes minimal—never write complex business logic inside a Mixin. Intercept the call and delegate immediately to a static domain service.

```java
@Mixin(Player.class)
public abstract class PlayerMixin {

    @ModifyReturnValue(method = "getDestroySpeed", at = @At("RETURN"))
    private float tarotcards$modifyMiningSpeed(float originalSpeed, BlockState state) {
        Player self = (Player) (Object) this;
        return TarotMiningService.adjustMiningSpeed(self, state, originalSpeed);
    }
}
```
