---
name: minecraft-clean-code
description: >-
  Applies Java and Minecraft modding Clean Code principles, SOLID guidelines,
  side-safety (isClientSide), tick performance optimization, and immutability
  standards during code creation, refactoring, or review. Activate when writing,
  refactoring, or reviewing Java mod code, items, blocks, entities, events, or mixins.
---

# minecraft-clean-code

## Purpose
Ensure all Java and Minecraft mod source code adheres to strict Clean Code standards, SOLID principles, modern Java idioms (Java 17/21), and Minecraft-specific invariants (Logical vs. Physical Side safety, Singleton Item/Block immutability, zero-allocation tick loops, and minimally invasive Mixins).

## Prerequisites
- The target workspace is a Java-based Minecraft mod project.
- An approved SDD specification or change plan exists before applying structural code changes.

## Step 1 — Enforce Modern Java & SOLID Principles
Apply clean Java conventions across all classes and methods:
1. **Single Responsibility & Method Size**: Keep methods focused on a single task (ideally < 20 lines) with at most 3 parameters (group related parameters into `record` objects or context holders).
2. **Guard Clauses Over Nesting**: Replace deep `if/else` pyramids with early returns (`if (level.isClientSide()) return;`). Maximum indentation depth inside a method is 2 levels.
3. **Immutability & Modern Java**:
   - Use Java `record` for DTOs, network payloads, Data Components, and configuration structures.
   - Prefer `final` fields and local variables where practical.
   - Never return `null` from collections or query methods; return empty collections (`List.of()`) or `Optional<T>`.
4. **Naming & Self-Documenting Code**:
   - Avoid magic numbers (`20`, `0xFF0000`, `16`). Extract named constants (`TICKS_PER_SECOND = 20`).
   - Name classes, methods, and variables after domain intent following official Mojmap terminology (`ServerLevel`, `ResourceLocation`, `BlockPos`, `ItemStack`).

### Validation
- Verify no method exceeds 25 lines or 2 levels of nesting without justification.
- Verify no raw magic numbers exist in business or game logic.

## Step 2 — Enforce Minecraft Side-Safety & Singleton Invariants
Inspect and enforce Minecraft runtime architecture boundaries:
1. **Singleton `Item` and `Block` Classes**:
   - `Item`, `Block`, `MobEffect`, and `Enchantment` instances are **global singletons**.
   - **NEVER** store mutable per-player or per-instance state as fields in `Item` or `Block` subclasses.
   - Store state in `ItemStack` (via Data Components / NBT), `BlockEntity`, `Entity` attachments/capabilities, or `SavedData`.
2. **Logical vs. Physical Side Separation**:
   - Always guard authoritative game logic (spawning, inventory mutation, damage, buffs, networking) with `if (level.isClientSide()) return;` (or `if (!(level instanceof ServerLevel serverLevel)) return;`).
   - **NEVER** import `net.minecraft.client.*` classes inside common or server packages (`NoClassDefFoundError` on dedicated servers). Isolate client rendering, keymappings, particles, and screens in client-only entrypoints/classes.

### Validation
- Confirm zero `net.minecraft.client.*` imports exist in common/server classes.
- Confirm zero mutable fields exist on `Item`, `Block`, or `MobEffect` classes.

## Step 3 — Optimize Tick Loops, Registries & Mixins
Ensure runtime performance (20 TPS / 50ms budget per tick) and mod compatibility:
1. **Tick Loop Discipline (`tick()`, `inventoryTick()`, `EntityTickEvent`)**:
   - Throttle periodic checks using modulo game time: `if (level.getGameTime() % 20 != 0) return;`.
   - Avoid heavy allocations (`Stream` pipelines, regex, reflection, uncached AABB searches) inside every-tick paths.
   - Use `BlockPos.MutableBlockPos` when iterating over 3D coordinate volumes.
2. **Safe Registry & Resource Identifiers**:
   - Never instantiate registered objects in static initializers before registry events fire.
   - Always construct identifiers using `ResourceLocation.fromNamespaceAndPath(MOD_ID, path)` instead of string concatenation.
3. **Mixin Hygiene**:
   - Prefer standard Loader Events or APIs before resorting to a Mixin.
   - Prefer `@Inject`, `@ModifyReturnValue`, or `@WrapOperation` (MixinExtras) over `@Overwrite` or `@Redirect` to preserve compatibility with other mods.
   - Prefix all injected fields and helper methods in Mixins with `modid$` (e.g., `tarotcards$ getActiveDeck()`).

For detailed code examples (bad vs. good), consult [java-minecraft-clean-code.md](./references/java-minecraft-clean-code.md).

### Validation
- Run through the complete checklist in [clean-code-checklist.md](./resources/clean-code-checklist.md) against all created or modified files.

## Step 4 — Report Clean Code Compliance
Present a concise compliance summary to the user after generating, refactoring, or reviewing code.

### Confirmation Output
```
✅ Clean Code Audit Complete (`minecraft-clean-code`)

Checked artifacts:
  - [List of inspected/modified Java classes]

Compliance Summary:
  - Java & SOLID (Guard clauses, Records, Constants): PASS
  - Minecraft Invariants (Stateless Items/Blocks, Side-Safety): PASS
  - Tick Performance & Mixin Compatibility: PASS

Next step: Run automated tests or proceed with `minecraft-design-patterns` if architectural refactoring is needed.
```

## Error Handling
- If a class in a common package requires client-only APIs (e.g., `Minecraft.getInstance()`), STOP and refactor using a client proxy/sided distributor or event listener.
- If mutable fields are found on an `Item` or `Block` class, STOP and migrate the state to `ItemStack` Data Components/NBT or `BlockEntity`.

## Notes
- Refer to [java-minecraft-clean-code.md](./references/java-minecraft-clean-code.md) for concrete refactoring patterns.
- Refer to [clean-code-checklist.md](./resources/clean-code-checklist.md) during code reviews and PR checks.
