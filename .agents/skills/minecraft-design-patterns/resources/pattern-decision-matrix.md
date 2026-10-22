# Minecraft Modding Pattern Decision Matrix

Use this matrix to select the simplest, most effective design pattern for a given modding problem.

| Problem / Scenario | Recommended Pattern | Anti-Pattern to Avoid |
| :--- | :--- | :--- |
| Multiple items/cards share the same base class but execute different passive or active effects | **Strategy Pattern** (`TarotEffectStrategy` injected into `TarotCardItem`) | Giant `switch (cardType)` or creating 22 separate `Item` subclasses with duplicated `inventoryTick` boilerplate |
| Constructing items, cards, or spells with many optional parameters (cooldown, rarity, mana, effects) | **Builder Pattern** (`TarotCardDefinition.Builder` producing an immutable `record`) | Telescoping constructors (`new CardItem(p, true, false, 20, 100, null, 0.5f)`) |
| Registering Blocks, Items, Effects, Sounds, or Creative Tabs | **Deferred Factory / Registry Holder** (`DeferredRegister` + `Supplier<T>`) | Static field instantiation (`public static final Item CARD = new Item(...)`) before registry events |
| Saving custom data on an `ItemStack`, `Entity`, `Chunk`, or syncing over network | **Codec / Immutable Data Record** (`RecordCodecBuilder` + `record`) | Manual string-keyed `CompoundTag` manipulation scattered across business classes |
| Reacting to player combat, dimension changes, loot drops, or tick updates | **Observer / Thin Event Subscriber** (Event listener filters & delegates to a Domain Service) | Putting 150 lines of game logic directly inside `@SubscribeEvent` methods |
| Modifying vanilla mechanics where no Loader Event exists | **Decorator / Interceptor via Mixin** (`@ModifyReturnValue` / `@WrapOperation` delegating to a Service) | Using `@Overwrite` or writing complex business logic directly inside the `@Mixin` class |
| Persisting global or dimension-wide mod state across server restarts | **Repository / SavedData Pattern** (`DimensionDataStorage` + `SavedData`) | Storing state in `static Map<UUID, ...>` fields that leak across world loads in Singleplayer |
