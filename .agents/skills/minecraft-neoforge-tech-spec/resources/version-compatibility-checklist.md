# Minecraft 26.3 & NeoForge 26.3.x Compatibility Checklist

Use this checklist when auditing or scaffolding code in any Minecraft 26.3 / NeoForge 26.3.x mod workspace.

## 1. Build & Environment (`gradle.properties` / `build.gradle` / `neoforge.mods.toml`)
- [ ] `net.neoforged.moddev` plugin (`2.0.148+`) is configured in `build.gradle` (no legacy ForgeGradle).
- [ ] `java.toolchain.languageVersion` is set to `JavaLanguageVersion.of(25)` (and matches any `pom-test.xml` or stack contract docs).
- [ ] `META-INF/neoforge.mods.toml` declares `modLoader="javafml"`, `loaderVersion="[1,)"`, and matching `neoforge` and `minecraft` version ranges.
- [ ] `neoForge.runs.data` uses `clientData()` with `--all --output src/generated/resources/`.

## 2. Registries, Identifiers & Persistence
- [ ] Zero imports of `net.minecraft.resources.ResourceLocation` — all resource IDs use `net.minecraft.resources.Identifier`.
- [ ] Zero references to `ForgeRegistries` or `RegistryObject<T>`.
- [ ] All `Item` registrations use `DeferredRegister.Items` + `.registerItem(...)` so `ResourceKey` is pre-injected into `Item.Properties`.
- [ ] All `Block` registrations use `DeferredRegister.Blocks` + `.registerBlock(...)` so `ResourceKey` is pre-injected into `BlockBehaviour.Properties`.
- [ ] Custom `ItemStack` state uses immutable Java `record` + `DataComponentType<T>` with both `.persistent(Codec)` and `.networkSynchronized(StreamCodec)`.
- [ ] `Item` constructors NEVER call `.get()` on `ModDataComponents` (read lazily via `stack.get(...)` / `stack.getOrDefault(...)`).
- [ ] Player/Entity custom state uses `AttachmentType<T>` on `NeoForgeRegistries.ATTACHMENT_TYPES` with `.serialize(MapCodec<T>)`.

## 3. Item, Block, GUI & Event Signatures
- [ ] `Item.use` returns `InteractionResult` (not `InteractionResultHolder<ItemStack>`).
- [ ] `Item.appendHoverText` uses `(ItemStack, TooltipContext, TooltipDisplay, Consumer<Component>, TooltipFlag)`.
- [ ] `Block` interactions override `useItemOn` and/or `useWithoutItem` returning `InteractionResult` (not `ItemInteractionResult`).
- [ ] Actionbar notifications use `player.sendOverlayMessage(Component)`; chat notifications use `player.sendSystemMessage(Component)`.
- [ ] `AbstractContainerScreen` subclasses use `GuiGraphicsExtractor`, `extractBackground(...)`, and `extractTooltip(...)` (no `GuiGraphics`, `renderBg`, or `renderTooltip`).
- [ ] `@EventBusSubscriber` uses `net.neoforged.fml.common.EventBusSubscriber` without a `bus = ...` parameter.
- [ ] Block break listeners use `net.neoforged.neoforge.event.level.block.BreakBlockEvent`.
- [ ] `AttributeModifier` uses `Identifier` (not `UUID`) and `AttributeModifier.Operation.ADD_VALUE` (`ADD_MULTIPLIED_BASE` / `ADD_MULTIPLIED_TOTAL`).
- [ ] World time checks use `level.getDefaultClockTime()` instead of `level.getDayTime()`.
- [ ] `LootModifier` subclasses use `(Optional<Holder<LootItemCondition>> condition, int priority)` constructor.

## 4. Assets & Data Pack Files
- [ ] Every item has BOTH `assets/<modid>/items/<item_id>.json` AND `assets/<modid>/models/item/<item_id>.json`.
- [ ] Vanilla data pack folders under `data/<modid>/` use singular names: `recipe/`, `advancement/`, `loot_table/`, `tags/item/`, `tags/block/`.
- [ ] Shaped recipe JSONs use direct strings for `"key"` entries (`"G": "minecraft:gold_ingot"`, `"L": "#minecraft:logs"`) and `{"id": "...", "count": 1}` for `"result"`.
- [ ] Both `en_us.json` and `pt_br.json` localization files are updated for all new items, blocks, creative tabs, messages, and advancements.
