---
name: minecraft-neoforge-tech-spec
description: >-
  Maps and enforces exact technical requirements, API signatures, build
  toolchains, and resource/data pack conventions for Minecraft 26.3 and
  NeoForge 26.3.x (Java 25 / ModDevGradle). Activate when bootstrapping a mod,
  migrating versions, creating or reviewing items, blocks, registries, data
  components, attachments, client screens, events, loot modifiers, or JSON
  assets/data files.
---

# minecraft-neoforge-tech-spec

## Purpose
Ensure strict API compatibility and zero legacy-version hallucination when developing or auditing Minecraft mods targeting **Minecraft 26.3** and **NeoForge 26.3.x** (with **ModDevGradle 2.0.x** and **Java 25**). This skill provides a portable workflow and reference catalog that can be dropped into any mod's `.agents/skills/` directory.

## Prerequisites
- Access to the target workspace's `gradle.properties`, `build.gradle`, and `src/main/resources/META-INF/neoforge.mods.toml`.
- Complementary skills: `minecraft-clean-code` and `minecraft-design-patterns` (when present in the workspace).

## Step 1 — Audit Build Toolchain & Mod Metadata
Inspect `gradle.properties`, `build.gradle`, and `META-INF/neoforge.mods.toml` to confirm the active technical baseline:
1. **Gradle Plugin & Loader**: Verify `id 'net.neoforged.moddev'` in `build.gradle` and `modLoader="javafml"` in `neoforge.mods.toml`.
2. **Version Coordinates**: Read `minecraft_version` (e.g., `26.3`), `neo_version` (e.g., `26.3.0.95`), `neo_form_version`, and `mod_id`.
3. **Java Toolchain**: Verify `java.toolchain.languageVersion = JavaLanguageVersion.of(25)` (and match any standalone test runners such as `pom-test.xml` or `sdd-stack-contract.md`).
4. **DataGen & Runs**: Confirm `neoForge.runs.data` uses `clientData()` with `--all --output src/generated/resources/` and `sourceSets.main.resources { srcDir 'src/generated/resources' }`.

### Validation
- Confirm `mod_id`, `minecraft_version`, `neo_version`, and Java toolchain version are explicitly identified before writing any code.

## Step 2 — Enforce Minecraft 26.3 & NeoForge 26.3.x Core APIs
Consult [neoforge-26-3-api-guide.md](./references/neoforge-26-3-api-guide.md) and apply these mandatory API contracts:
1. **Resource Identifiers**: Use `net.minecraft.resources.Identifier` (`Identifier.fromNamespaceAndPath(modId, path)`). Never import `ResourceLocation`.
2. **Registries & Pre-Set IDs**:
   - Items: Use `DeferredRegister.Items` (`DeferredRegister.createItems(MOD_ID)`) and `.registerItem(name, MyItem::new)` so the item `ResourceKey` is pre-injected into `Item.Properties` before `super(properties)`.
   - Blocks: Use `DeferredRegister.Blocks` (`DeferredRegister.createBlocks(MOD_ID)`) and `.registerBlock(name, MyBlock::new, () -> BlockBehaviour.Properties.of()...)`.
   - Other registries: Use `DeferredRegister.create(Registries.*, MOD_ID)` returning `DeferredHolder<R, T>` or `Supplier<T>` (never `RegistryObject`).
3. **Data Persistence & Registry Order Safety**:
   - ItemStacks: Use `DataComponentType<T>` backed by Java `record`, `.persistent(Codec<T>)`, and `.networkSynchronized(StreamCodec<ByteBuf, T>)`. **Never** call `ModDataComponents.X.get()` inside an `Item` constructor; read lazily via `stack.get(ModDataComponents.X.get())`.
   - Entities/Players: Use `AttachmentType<T>` registered to `NeoForgeRegistries.ATTACHMENT_TYPES` with `.serialize(MapCodec<T>)`, `.sync(StreamCodec)`, and `.copyOnDeath()`.
4. **Item, Block & Player Signatures**:
   - `Item.use(Level, Player, InteractionHand)` returns `InteractionResult`.
   - `Item.appendHoverText(ItemStack, TooltipContext, TooltipDisplay, Consumer<Component>, TooltipFlag)` adds lines via `tooltipAdder.accept(...)`.
   - `Block.useItemOn(...)` and `Block.useWithoutItem(...)` return `InteractionResult`.
   - Actionbar messages use `player.sendOverlayMessage(Component)`; chat messages use `player.sendSystemMessage(Component)`.

### Validation
- Verify zero references to `ResourceLocation`, `RegistryObject`, `ForgeRegistries`, `InteractionResultHolder`, `ItemInteractionResult`, or `List<Component>` tooltips.

## Step 3 — Enforce Client GUI, Event Bus, Attributes & GLM Contracts
1. **Client Screens (`AbstractContainerScreen`)**:
   - Pass `(menu, playerInventory, title, imageWidth, imageHeight)` to `super(...)`.
   - Use `GuiGraphicsExtractor` (never `GuiGraphics`).
   - Override `extractBackground(GuiGraphicsExtractor, int mouseX, int mouseY, float partialTick)` and `extractTooltip(GuiGraphicsExtractor, int mouseX, int mouseY)` with `extractor.setTooltipForNextFrame(...)`.
2. **Events & `@EventBusSubscriber`**:
   - Use top-level `@EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)` without `bus = ...`.
   - Use `BreakBlockEvent` (in `net.neoforged.neoforge.event.level.block`), `LivingIncomingDamageEvent`, `LivingDamageEvent.Post`, and `PlayerTickEvent.Pre` / `Post`.
   - For client movement keys in `MovementInputUpdateEvent`, read `event.getInput().keyPresses.jump()`.
3. **Attributes, Clock & Global Loot Modifiers**:
   - Construct `AttributeModifier(Identifier id, double amount, AttributeModifier.Operation.ADD_VALUE)`.
   - Read world time via `level.getDefaultClockTime() % 24000` (not `getDayTime()`).
   - Subclass `LootModifier` with constructor `(Optional<Holder<LootItemCondition>> condition, int priority)` and query table ID via `context.getQueriedLootTableId()`.

### Validation
- Verify all client classes are isolated from server imports and use `GuiGraphicsExtractor` and NeoForge 26.3 event classes.

## Step 4 — Validate Resource & Data Pack Layout
Run every new or modified JSON asset/data file through [version-compatibility-checklist.md](./resources/version-compatibility-checklist.md):
1. **Client Item Definitions**: Every item MUST have `assets/<modid>/items/<item_id>.json` (`{"model": {"type": "minecraft:model", "model": "<modid>:item/<item_id>"}}`) in addition to `assets/<modid>/models/item/<item_id>.json`.
2. **Singular Data Pack Directories**: Use `data/<modid>/recipe/`, `data/<modid>/advancement/`, `data/<modid>/loot_table/`, `data/<modid>/tags/item/`, and `data/<modid>/tags/block/`. Keep NeoForge GLMs in `data/<modid>/loot_modifiers/`.

### Confirmation Output
```
✅ Minecraft 26.3 & NeoForge 26.3.x Technical Verification Complete

Target Mod: <mod_id> (MC <minecraft_version> / NeoForge <neo_version> / Java <java_version>)
Checked Categories:
- [x] Toolchain & ModDevGradle configuration
- [x] Identifier & DeferredRegister (pre-set Item/Block ResourceKeys)
- [x] DataComponentType & AttachmentType codecs (lazy component access)
- [x] Item/Block signatures, GuiGraphicsExtractor & NeoForge 26.3 events
- [x] Resource (assets/<modid>/items/) & singular Data Pack directories

Next step: Run `./gradlew compileJava test` to verify compilation and unit tests.
```

## Error Handling
- **If `NullPointerException: Item id not set` or `Block id not set` occurs**: STOP and replace generic `ITEMS.register(...)` / `new Item(new Item.Properties())` with `ITEMS.registerItem(...)` or `BLOCKS.registerBlock(...)` so ModDevGradle/NeoForge injects the `ResourceKey` into `Properties` before construction.
- **If `NullPointerException: Registry Object not present` occurs during item registration**: STOP and remove any `.get()` call on another `DeferredHolder` (such as `ModDataComponents`) from inside `Item` constructors or `Item.Properties` chains.
- **If a target mod uses a different Minecraft or Loader version**: Inspect `gradle.properties` first and report any version delta to the user before generating code.

## Notes
- Detailed code templates (Bad vs. Good): [neoforge-26-3-api-guide.md](./references/neoforge-26-3-api-guide.md)
- Portable verification checklist: [version-compatibility-checklist.md](./resources/version-compatibility-checklist.md)
