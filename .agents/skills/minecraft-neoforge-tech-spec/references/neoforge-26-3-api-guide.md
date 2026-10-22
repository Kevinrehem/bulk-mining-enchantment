# Minecraft 26.3 & NeoForge 26.3.x API Reference Guide

This reference documents the exact API shifts, method signatures, and JSON formats required by **Minecraft 26.3**, **NeoForge 26.3.0.95+**, **ModDevGradle 2.0.148+**, and **Java 25**.

---

## 1. Build & Toolchain (`gradle.properties` & `build.gradle`)

### Canonical `gradle.properties`
```properties
minecraft_version=26.3
neo_version=26.3.0.95
neo_form_version=26.3-20261021.000000
mod_id=examplemod
mod_name=Example Mod
mod_license=MIT
mod_version=1.0.0
mod_group_id=com.example.examplemod
mod_authors=Author
```

### Canonical `build.gradle` Highlights (ModDevGradle 2.0.x)
```groovy
plugins {
    id 'java-library'
    id 'net.neoforged.moddev' version '2.0.148'
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

neoForge {
    version = project.neo_version

    runs {
        client { client() }
        server {
            server()
            programArgument '--nogui'
        }
        gameTestServer { type = "gameTestServer" }
        data {
            clientData()
            programArguments.addAll '--mod', project.mod_id, '--all',
                '--output', file('src/generated/resources/').getAbsolutePath(),
                '--existing', file('src/main/resources/').getAbsolutePath()
        }
    }
}
```

---

## 2. Resource Identifiers (`Identifier` replaces `ResourceLocation`)

### Bad (Legacy 1.20 / 1.21.1 — Does Not Compile in MC 26.3)
```java
import net.minecraft.resources.ResourceLocation;

public static final ResourceLocation ID = new ResourceLocation("modid", "my_id");
// or ResourceLocation.fromNamespaceAndPath("modid", "my_id");
```

### Good (MC 26.3 Mojmap)
```java
import net.minecraft.resources.Identifier;

public static final Identifier ID = Identifier.fromNamespaceAndPath(MOD_ID, "my_id");
public static final Identifier VANILLA_ID = Identifier.withDefaultNamespace("stone");
```

---

## 3. Registries & Mandatory `Properties` ID Pre-Injection

In Minecraft 26.3, `Item` and `Block` constructors throw `NullPointerException: Item id not set` / `Block id not set` if `Item.Properties.setId(ResourceKey)` / `BlockBehaviour.Properties.setId(ResourceKey)` was not invoked prior to `super(properties)`. `DeferredRegister.Items` and `DeferredRegister.Blocks` handle this automatically when using `registerItem` and `registerBlock`.

### Bad (Legacy `DeferredRegister<Item>` / `RegistryObject<Item>`)
```java
// Fails at runtime with "Item id not set" or fails to compile with RegistryObject
public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
public static final RegistryObject<Item> MY_ITEM = ITEMS.register("my_item", () -> new MyItem(new Item.Properties()));
```

### Good (NeoForge 26.3 `DeferredRegister.Items` & `DeferredRegister.Blocks`)
```java
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// Items
public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);

public static final DeferredItem<MyItem> MY_ITEM =
        ITEMS.registerItem("my_item", MyItem::new); // Passes pre-keyed Item.Properties to MyItem(Item.Properties)

public static final DeferredItem<BlockItem> MY_BLOCK_ITEM =
        ITEMS.registerItem("my_block", properties -> new BlockItem(ModBlocks.MY_BLOCK.get(), properties));

// Blocks
public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);

public static final DeferredBlock<MyBlock> MY_BLOCK = BLOCKS.registerBlock(
        "my_block",
        MyBlock::new,
        () -> BlockBehaviour.Properties.of().strength(2.5F, 6.0F).noOcclusion()
);
```

### Good (Other Registries via `DeferredHolder<R, T>`)
```java
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;

public static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(Registries.MENU, MOD_ID);

public static final DeferredHolder<MenuType<?>, MenuType<MyMenu>> MY_MENU =
        MENUS.register("my_menu", () -> IMenuTypeExtension.create(MyMenu::new));
```

---

## 4. `DataComponentType` (Items) & `AttachmentType` (Entities/Players)

### Good (`DataComponentType` with Record, `Codec`, and `StreamCodec`)
```java
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public record CardData(boolean active, int progress) {
    public static final CardData DEFAULT = new CardData(true, 0);

    public static final Codec<CardData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.fieldOf("active").forGetter(CardData::active),
                    Codec.INT.fieldOf("progress").forGetter(CardData::progress)
            ).apply(instance, CardData::new)
    );

    public static final StreamCodec<ByteBuf, CardData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, CardData::active,
            ByteBufCodecs.VAR_INT, CardData::progress,
            CardData::new
    );
}

// Registration
public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MOD_ID);

public static final DeferredHolder<DataComponentType<?>, DataComponentType<CardData>> CARD_DATA =
        DATA_COMPONENTS.register("card_data", () -> DataComponentType.<CardData>builder()
                .persistent(CardData.CODEC)
                .networkSynchronized(CardData.STREAM_CODEC)
                .build());
```
> **CRITICAL ORDERING RULE**: Never call `ModDataComponents.CARD_DATA.get()` inside an `Item` constructor (`properties.component(ModDataComponents.CARD_DATA.get(), ...)`), because `Registries.ITEM` freezes before `Registries.DATA_COMPONENT_TYPE`. Always read lazily via `stack.getOrDefault(ModDataComponents.CARD_DATA.get(), CardData.DEFAULT)`.

### Good (`AttachmentType` on Entities/Players)
```java
import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import java.util.function.Supplier;

public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MOD_ID);

// Note: .serialize(...) takes a MapCodec<T> (e.g., Codec.INT.fieldOf("offer"))
public static final Supplier<AttachmentType<Integer>> OFFER_ID = ATTACHMENT_TYPES.register(
        "offer_id",
        () -> AttachmentType.builder(() -> -1)
                .serialize(Codec.INT.fieldOf("offer"))
                .sync(ByteBufCodecs.VAR_INT)
                .copyOnDeath()
                .build()
);
```

---

## 5. `Item` & `Block` Method Signatures (MC 26.3)

### Bad (Legacy 1.20 / 1.21.1)
```java
@Override
public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    player.displayClientMessage(Component.translatable("msg"), true);
    return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
}

@Override
public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
    tooltip.add(Component.translatable("tooltip"));
}
```

### Good (MC 26.3)
```java
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

@Override
public InteractionResult use(Level level, Player player, InteractionHand hand) {
    if (level.isClientSide()) {
        return InteractionResult.SUCCESS;
    }
    player.sendOverlayMessage(Component.translatable("msg.actionbar")); // or sendSystemMessage(...)
    return InteractionResult.SUCCESS;
}

@Override
public void appendHoverText(
        ItemStack stack,
        TooltipContext context,
        TooltipDisplay tooltipDisplay,
        Consumer<Component> tooltipAdder,
        TooltipFlag tooltipFlag
) {
    tooltipAdder.accept(Component.translatable("tooltip.line"));
    super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, tooltipFlag);
}
```

### Good (`Block` Interaction in MC 26.3)
```java
@Override
protected InteractionResult useItemOn(
        ItemStack stack, BlockState state, Level level, BlockPos pos,
        Player player, InteractionHand hand, BlockHitResult hitResult
) {
    return InteractionResult.TRY_WITH_EMPTY_HAND; // or InteractionResult.SUCCESS
}

@Override
protected InteractionResult useWithoutItem(
        BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult
) {
    return InteractionResult.SUCCESS;
}
```

---

## 6. Client GUI Rendering (`GuiGraphicsExtractor` replaces `GuiGraphics`)

### Bad (Legacy `GuiGraphics` & `renderBg`)
```java
@Override
protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) { ... }

@Override
public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    super.render(graphics, mouseX, mouseY, partialTick);
    this.renderTooltip(graphics, mouseX, mouseY);
}
```

### Good (MC 26.3 `GuiGraphicsExtractor`, `extractBackground` & `extractTooltip`)
```java
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MyScreen extends AbstractContainerScreen<MyMenu> {

    public MyScreen(MyMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166); // Dimensions passed directly to super
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        super.extractTooltip(extractor, mouseX, mouseY);
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem()) {
            extractor.setTooltipForNextFrame(this.font, Component.translatable("gui.hint"), mouseX, mouseY);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int left = this.leftPos;
        int top = this.topPos;
        extractor.fill(left, top, left + this.imageWidth, top + this.imageHeight, 0xFFC6C6C6);
    }
}
```

---

## 7. Entrypoint, Events, Attributes, World Time & Global Loot Modifiers

### Entrypoint, Config & `@EventBusSubscriber`
```java
@Mod(ExampleMod.MOD_ID)
public class ExampleMod {
    public static final String MOD_ID = "examplemod";

    public ExampleMod(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.ITEMS.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.SYNCED, ExampleConfig.SPEC);
        NeoForge.EVENT_BUS.addListener(this::onPlayerTick);
    }
}

// Client-only subscriber (top-level annotation, no `bus =` parameter)
@EventBusSubscriber(modid = ExampleMod.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void onKeyInput(MovementInputUpdateEvent event) {
        boolean jumping = event.getInput().keyPresses.jump();
    }
}
```

### Key NeoForge 26.3 Events & World Helpers
- **Block Break**: `net.neoforged.neoforge.event.level.block.BreakBlockEvent` (replaces `BlockEvent.BreakEvent`).
- **Entity Damage**: `LivingIncomingDamageEvent` (cancelable pre-mitigation) and `LivingDamageEvent.Post` (`event.getInflictedDamage()`).
- **World Time**: `level.getDefaultClockTime() % 24000` (replaces `level.getDayTime()`).
- **AttributeModifier**:
  ```java
  public static final Identifier HEALTH_BONUS_ID = Identifier.fromNamespaceAndPath(MOD_ID, "health_bonus");
  attrInstance.addPermanentModifier(new AttributeModifier(HEALTH_BONUS_ID, 4.0, AttributeModifier.Operation.ADD_VALUE));
  ```

### Global Loot Modifier (`LootModifier` in NeoForge 26.3)
```java
import com.google.common.base.Suppliers;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

public class MyLootModifier extends LootModifier {
    public static final Supplier<MapCodec<MyLootModifier>> CODEC = Suppliers.memoize(() ->
            RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, MyLootModifier::new))
    );

    public MyLootModifier(Optional<Holder<LootItemCondition>> condition, int priority) {
        super(condition, priority);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        Identifier tableId = context.getQueriedLootTableId();
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
```

---

## 8. Assets & Data Pack Directory Conventions (MC 26.3)

### Mandatory Client Item Definition (`assets/<modid>/items/<item_id>.json`)
Every registered item MUST have both:
1. `assets/<modid>/items/<item_id>.json`:
   ```json
   {
     "model": {
       "type": "minecraft:model",
       "model": "examplemod:item/my_item"
     }
   }
   ```
2. `assets/<modid>/models/item/<item_id>.json`:
   ```json
   {
     "parent": "minecraft:item/generated",
     "textures": {
       "layer0": "examplemod:item/my_item"
     }
   }
   ```

### Singular Data Pack Paths & Recipe Schema (`data/<modid>/`)
- **Recipes** (`data/<modid>/recipe/<name>.json` — singular `recipe`):
  ```json
  {
    "type": "minecraft:crafting_shaped",
    "category": "misc",
    "key": {
      "G": "minecraft:gold_ingot",
      "L": "#minecraft:logs"
    },
    "pattern": [
      "GGG",
      "GLG",
      "GGG"
    ],
    "result": {
      "count": 1,
      "id": "examplemod:my_item"
    }
  }
  ```
- **Advancements**: `data/<modid>/advancement/<name>.json` (icon uses `{"id": "examplemod:my_item"}`).
- **Block Loot Tables**: `data/<modid>/loot_table/blocks/<block_id>.json`.
- **Tags**: `data/<modid>/tags/item/<tag>.json` and `data/minecraft/tags/block/mineable/<tool>.json`.
- **Global Loot Modifiers**: `data/neoforge/loot_modifiers/global_loot_modifiers.json` and `data/<modid>/loot_modifiers/<name>.json` (`{"type": "examplemod:my_loot", "priority": 0}`).
