---
name: minecraft-design-patterns
description: >-
  Guides the selection and implementation of GoF and Minecraft-specific Design
  Patterns (Deferred Registration, Strategy for Item/Card effects, Builder for
  Properties/Components, Codec Data-Driven Serialization, Event-Driven Observer,
  and Mixin Decorator) in Java mods. Activate when architecting new mod features,
  designing extensible systems, or refactoring complex classes.
---

# minecraft-design-patterns

## Purpose
Provide a structured methodology for selecting and applying software Design Patterns (Gang of Four and Minecraft ecosystem idioms) so that mod features remain decoupled, testable, data-driven, and compatible across mod loaders and game updates.

## Prerequisites
- The feature requirements or refactoring target are understood.
- Combine with `minecraft-clean-code` when writing the final Java implementation.

## Step 1 — Classify the Architectural Problem
Match the feature requirement to the appropriate design pattern using [pattern-decision-matrix.md](./resources/pattern-decision-matrix.md):

1. **Object Registration & Lifecycle** → **Deferred Registration / Registry Holder Pattern** (`DeferredRegister`, `RegistryObject`, `Holder<T>`).
2. **Interchangeable Item / Card / Spell Behaviors** → **Strategy Pattern** (interface-driven effect handlers decoupled from the `Item` class).
3. **Complex Object or Component Construction** → **Builder Pattern** (fluent builders for Item properties, Tarot card definitions, or packet payloads).
4. **Cross-Cutting Gameplay Reactions** → **Observer / Event Bus Pattern** (loader event listeners decoupled from domain logic services).
5. **Serialization, Networking & Datapacks** → **Codec & StreamCodec Pattern** (declarative immutable serialization via Mojang's `RecordCodecBuilder`).
6. **Extending Vanilla Behavior Safely** → **Decorator / Interceptor Pattern via Mixin** (non-destructive wrapping of vanilla hooks delegating immediately to a domain class).

### Validation
- Confirm the selected pattern directly addresses the problem without introducing speculative over-engineering (YAGNI).

## Step 2 — Separate Domain Logic from Minecraft Plumbing
Structure classes to isolate core mod mechanics from Minecraft boilerplate:
1. **Thin Minecraft Adapters**: Keep `Item`, `Block`, `MobEffect`, Event Listeners, and `Mixin` classes thin. Their only job is to extract context (`ServerLevel`, `ServerPlayer`, `ItemStack`) and delegate to a dedicated domain service or strategy.
2. **Pure Domain Services / Strategies**: Implement calculation, effect resolution, cooldown formulas, and state transitions in focused classes that are easy to unit-test and reason about.
3. **Composition Over Inheritance**: Avoid deep `AbstractBaseModItem` class hierarchies. Compose behaviors using functional interfaces, Strategy objects, or Data Components.

Consult [minecraft-patterns-catalog.md](./references/minecraft-patterns-catalog.md) for canonical code templates of each pattern.

### Validation
- Verify that adding a new variant (e.g., a new Tarot Card effect or passive buff) satisfies the **Open/Closed Principle (OCP)**: new behavior can be added by registering a new Strategy/entry without modifying existing `switch`/`if-else` chains.

## Step 3 — Verify Testability & Lifecycle Safety
Before finalizing the pattern implementation, verify:
1. **Registry Order Independence**: Ensure strategies or builders that reference registered objects (`MobEffect`, `Item`, `SoundEvent`) accept `Supplier<T>` or `Holder<T>` so they never trigger premature class-loading or null registry references.
2. **Thread & Side Isolation**: Ensure Singletons or Registry caches do not leak client-side state into server threads or persist stale `Level`/`Entity` references across world unload boundaries (avoid memory leaks by never caching `Entity` or `Level` in static fields; store `UUID` or `ResourceKey<Level>` instead).

### Validation
- Check that no static collection or singleton holds strong references to `Entity`, `Player`, or `Level` instances.

## Step 4 — Document Architectural Decision
Present the selected design pattern(s) and class responsibilities to the user.

### Confirmation Output
```
✅ Architecture & Design Pattern Plan (`minecraft-design-patterns`)

Selected Pattern(s):
  - [Pattern Name]: [1-sentence justification]

Class Responsibilities:
  - [Class/Interface Name]: [Role in the pattern]

Lifecycle & Memory Safety:
  - Registry lazy evaluation (`Supplier`/`Holder`): VERIFIED
  - Zero static Entity/Level retention: VERIFIED

Next step: Proceed with implementation following `minecraft-clean-code`.
```

## Error Handling
- If a proposed design requires a deep inheritance hierarchy (> 2 levels of custom abstract classes), STOP and refactor to Composition/Strategy.
- If a design stores `Player` or `Level` references inside a static Map or Singleton, STOP immediately (causes severe memory leaks on dimension change/world reload) and replace with `UUID` Lookup or `SavedData`/Attachments.

## Notes
- See [pattern-decision-matrix.md](./resources/pattern-decision-matrix.md) to quickly choose the right pattern.
- See [minecraft-patterns-catalog.md](./references/minecraft-patterns-catalog.md) for ready-to-use Java implementations tailored for Minecraft mods.
