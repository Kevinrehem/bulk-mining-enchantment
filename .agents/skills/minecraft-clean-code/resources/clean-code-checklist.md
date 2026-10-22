# Minecraft Modding Clean Code Checklist

Use this checklist before finalizing any Java class creation or refactoring.

## 1. Java & SOLID Fundamentals
- [ ] **Single Responsibility**: Does each class and method have a single, well-defined reason to change?
- [ ] **Method Length & Nesting**: Are methods <= 20-25 lines with <= 2 levels of indentation (using early return guard clauses)?
- [ ] **No Magic Values**: Are tick durations, colors, probabilities, multipliers, and slot indices extracted to `private static final` constants?
- [ ] **Immutability**: Are DTOs, configurations, and network packets implemented as Java `record`s?
- [ ] **Null Safety**: Are `@Nullable` annotations or `Optional<T>` used explicitly instead of returning silent `null` references?

## 2. Minecraft Architecture & Side-Safety
- [ ] **Stateless Singletons**: Do `Item`, `Block`, `MobEffect`, and `Enchantment` subclasses contain **zero** mutable instance fields?
- [ ] **Client Isolation**: Are common/server classes completely free of `net.minecraft.client.*` imports?
- [ ] **Logical Server Guard**: Do gameplay mutations check `if (level.isClientSide()) return;` or pattern-match `level instanceof ServerLevel`?
- [ ] **Translation Keys**: Are all user-facing strings using `Component.translatable("...")` instead of hardcoded `Component.literal("...")`?

## 3. Performance & Mod Compatibility
- [ ] **Tick Throttling**: Are passive effects and entity checks throttled (e.g., `level.getGameTime() % N == 0`) rather than running every tick?
- [ ] **Zero Hot-Path Allocations**: Are `Stream`, regex, reflection, and unnecessary object allocations avoided inside `tick()` methods?
- [ ] **Mutable Coordinates**: Is `BlockPos.MutableBlockPos` used when scanning 3D block regions?
- [ ] **Mixin Restraint**: Are `@Overwrite` and `@Redirect` avoided in favor of events, `@Inject`, `@ModifyReturnValue`, or `@WrapOperation`?
- [ ] **Mixin Namespace**: Are all `@Unique` fields and helper methods in Mixins prefixed with `<modid>$`?
