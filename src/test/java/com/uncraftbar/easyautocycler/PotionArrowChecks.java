package com.uncraftbar.easyautocycler;

import com.uncraftbar.easyautocycler.config.FilterConfig;
import com.uncraftbar.easyautocycler.compat.VisibleTradersCompat;
import com.uncraftbar.easyautocycler.compat.MinecraftApiCompat;
import com.mojang.blaze3d.platform.InputConstants;
import com.uncraftbar.easyautocycler.filter.FilterEntry;
import com.uncraftbar.easyautocycler.gui.PotionNameResolver;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/** Headless regression checks using real Minecraft potion/item components. */
public final class PotionArrowChecks {
    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        boolean sdl = MinecraftApiCompat.keyboardType().name().equals("KEYBOARD");
        require(InputConstants.KEY_R == (sdl ? 21 : 82) && InputConstants.KEY_C == (sdl ? 6 : 67),
                "default shortcut codes must use the current keyboard backend");
        require(InputConstants.KEY_PAGEDOWN == (sdl ? 78 : 267)
                && InputConstants.KEY_RETURN == (sdl ? 40 : 257), "completion navigation key codes");
        require(InputConstants.MOUSE_BUTTON_LEFT == (sdl ? 1 : 0), "completion click button code");
        var tradeEntries = net.minecraft.core.HolderSet.<net.minecraft.world.item.trading.VillagerTrade>direct();
        var tradeSet = new net.minecraft.world.item.trading.TradeSet(tradeEntries, null, false, Optional.empty());
        require(MinecraftApiCompat.trades(tradeSet) == tradeEntries, "version-specific trade list accessor");
        Class<?> cycleButton = Class.forName("de.maxhenkel.tradecycling.gui.CycleTradesButton", false,
                PotionArrowChecks.class.getClassLoader());
        require(cycleButton.getMethod("canCycle", net.minecraft.world.inventory.MerchantMenu.class)
                .getReturnType() == boolean.class, "Trade Cycling canCycle contract");
        Class<?> cyclePacket = Class.forName("de.maxhenkel.tradecycling.net.CycleTradesPacket", false,
                PotionArrowChecks.class.getClassLoader());
        cyclePacket.getDeclaredConstructor();
        require(net.minecraft.network.protocol.common.custom.CustomPacketPayload.class.isAssignableFrom(cyclePacket),
                "Trade Cycling payload contract");
        // A headless bootstrap does not load the data pack's item components.
        // These checks only need real item identity and explicit potion contents.
        for (var item : List.of(Items.TIPPED_ARROW, Items.POTION, Items.BOW, Items.EMERALD, Items.ARROW)) {
            var holder = BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getKey(item)).orElseThrow();
            if (!holder.areComponentsBound()) holder.bindComponents(DataComponentMap.EMPTY);
        }
        FilterEntry target = new FilterEntry();
        target.setItemId(id("tipped_arrow"));
        target.setPotionId(id("strong_poison"));
        ItemStack strong = PotionContents.createItemStack(Items.TIPPED_ARROW, Potions.STRONG_POISON);
        ItemStack normal = PotionContents.createItemStack(Items.TIPPED_ARROW, Potions.POISON);
        ItemStack extended = PotionContents.createItemStack(Items.TIPPED_ARROW, Potions.LONG_POISON);
        require(target.matchesPotion(strong), "specific strong potion must match");
        require(!target.matchesPotion(normal), "normal potion must not match strong target");
        require(!target.matchesPotion(extended), "extended potion must not match strong target");
        require(!target.matchesPotion(new ItemStack(Items.TIPPED_ARROW)), "missing potion must not match");
        require(!target.matchesPotion(PotionContents.createItemStack(Items.POTION, Potions.STRONG_POISON)),
                "potion bottle must not match an arrow target");
        FilterEntry clone = new FilterEntry(target);
        require(id("strong_poison").equals(clone.getPotionId()), "copy must retain potion criteria");
        target.setTradeScope(FilterEntry.TradeScope.LATER);
        target.setMode(FilterEntry.Mode.AUTO);
        FilterConfig.saveFilters(List.of(), true, List.of(), true,
                Map.of("minecraft:fletcher", List.of(target)), Map.of("minecraft:fletcher", true));
        var loaded = FilterConfig.dataToFilters(FilterConfig.loadFilters().professionFilters.get("minecraft:fletcher"));
        require(loaded.size() == 1 && loaded.getFirst().matchesPotion(strong)
                && !loaded.getFirst().matchesPotion(normal), "saved potion must round-trip exactly");
        require(loaded.getFirst().getMode() == FilterEntry.Mode.AUTO
                && loaded.getFirst().getTradeScope() == FilterEntry.TradeScope.LATER, "mode/scope must survive");
        FilterConfig.FilterData legacy = new FilterConfig.FilterData();
        legacy.itemId = "minecraft:tipped_arrow";
        var oldTarget = FilterConfig.dataToFilters(List.of(legacy)).getFirst();
        require(oldTarget.getPotionId() == null && oldTarget.matchesPotion(normal)
                && oldTarget.matchesPotion(strong), "legacy arrows must still match any potion");
        legacy.potionId = "bad potion id!";
        require(FilterConfig.dataToFilters(List.of(legacy)).isEmpty(), "invalid potion must not become a wildcard");
        clone.setItemId(id("bow"));
        require(clone.getPotionId() == null, "switching item must clear hidden potion criteria");

        target.setMinCount(5);
        target.setMinPrice(2);
        target.setMaxPrice(2);
        strong.setCount(5);
        normal.setCount(5);
        MerchantOffers initial = new MerchantOffers();
        MerchantOffers later = new MerchantOffers();
        initial.add(new MerchantOffer(new ItemCost(Items.EMERALD, 2), Optional.of(new ItemCost(Items.ARROW, 5)),
                normal, 12, 30, 0.05F));
        later.add(new MerchantOffer(new ItemCost(Items.EMERALD, 2), Optional.of(new ItemCost(Items.ARROW, 5)),
                strong, 12, 30, 0.05F));
        MerchantOffers combined = new MerchantOffers();
        combined.addAll(initial);
        combined.addAll(later);
        var offers = new VisibleTradersCompat.OfferSets(initial, later, combined, true);
        var matchTrade = AutomationManager.class.getDeclaredMethod("checkTradeWithFilter", MerchantOffers.class, FilterEntry.class);
        matchTrade.setAccessible(true);
        require(!(boolean) matchTrade.invoke(AutomationManager.INSTANCE, offers.forScope(FilterEntry.TradeScope.INITIAL), target),
                "initial normal potion must not satisfy a strong later target");
        require((boolean) matchTrade.invoke(AutomationManager.INSTANCE, offers.forScope(FilterEntry.TradeScope.LATER), target),
                "later strong arrow and both payment stacks must match");
        require((boolean) matchTrade.invoke(AutomationManager.INSTANCE, offers.forScope(FilterEntry.TradeScope.ALL), target),
                "all-trades scope must include the later potion");
        target.setMaxPrice(1);
        require(!(boolean) matchTrade.invoke(AutomationManager.INSTANCE, later, target), "potion match must still respect price");
        target.setMinPrice(1);
        target.setMaxPrice(64);
        target.setMinCount(6);
        require(!(boolean) matchTrade.invoke(AutomationManager.INSTANCE, later, target), "potion match must still respect count");

        PotionNameResolver names = new PotionNameResolver(
                RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY), FixtureResources.create());
        require(id("poison").equals(names.resolve("剧毒之箭")), "Chinese base arrow name");
        require(id("strong_poison").equals(names.resolve("Arrow of Poison II")), "English strong arrow name");
        require(id("long_poison").equals(names.resolve("剧毒之箭（延长）")), "Chinese extended variant");
        require(id("poison").equals(names.resolve("Poison")), "English effect name");
        require(id("poison").equals(names.resolve("剧毒")), "Chinese shortened potion name");
        require(id("strong_poison").equals(names.resolve("Poison II")), "English roman-numeral effect name");
        require(id("strong_poison").equals(names.resolve("中毒 2")), "Chinese strong effect name");
        require(id("strong_poison").equals(names.resolve("minecraft:strong_poison")), "namespaced ID");
        require(names.resolve("water") == null && names.resolve("luck") == null, "non-tradeable potions excluded");
        require(names.suggestions().size() == 41, "all vanilla tradeable variants present");
        for (String suggestion : names.suggestions()) {
            require(names.resolve(suggestion) != null, "every suggestion must resolve: " + suggestion);
        }
        for (String variant : List.of("poison", "long_poison", "strong_poison")) {
            require(id(variant).equals(names.resolve(names.preferredInput(id(variant)))), "editable display round-trip");
        }
        System.out.println("Potion arrow regression checks passed: matching, variants, configuration, legacy targets and bilingual candidates.");
    }

    private static Identifier id(String path) { return Identifier.withDefaultNamespace(path); }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class FixtureResources implements java.lang.reflect.InvocationHandler {
        static ResourceManager create() {
            return (ResourceManager) java.lang.reflect.Proxy.newProxyInstance(
                    ResourceManager.class.getClassLoader(), new Class<?>[]{ResourceManager.class}, new FixtureResources());
        }

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
            return switch (method.getName()) {
                case "getNamespaces" -> Set.of("minecraft", "easyautocycler");
                case "getResource" -> getResource((Identifier) args[0]);
                case "getResourceStack" -> getResource((Identifier) args[0]).map(List::of).orElse(List.of());
                case "listResources", "listResourceStacks" -> Map.of();
                case "listPacks" -> Stream.empty();
                case "close" -> null;
                case "toString" -> "PotionArrowChecks.FixtureResources";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(method.toString());
            };
        }

        public Optional<Resource> getResource(Identifier id) {
            String json = switch (id.toString()) {
                case "minecraft:lang/en_us.json" -> "{\"item.minecraft.tipped_arrow.effect.poison\":\"Arrow of Poison\",\"effect.minecraft.poison\":\"Poison\"}";
                case "minecraft:lang/zh_cn.json" -> "{\"item.minecraft.tipped_arrow.effect.poison\":\"剧毒之箭\",\"effect.minecraft.poison\":\"中毒\"}";
                default -> null;
            };
            if (json != null) return Optional.of(new Resource(null,
                    () -> new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))));
            if (id.toString().equals("easyautocycler:tradeable_potions.json")) {
                return Optional.of(new Resource(null, () -> PotionArrowChecks.class.getResourceAsStream(
                        "/assets/easyautocycler/tradeable_potions.json")));
            }
            return Optional.empty();
        }
    }
}
