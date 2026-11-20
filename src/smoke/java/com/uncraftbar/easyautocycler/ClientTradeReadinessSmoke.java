package com.uncraftbar.easyautocycler;

import com.mojang.serialization.Lifecycle;
import com.uncraftbar.easyautocycler.filter.FilterEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketProcessor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

/** Real packet handlers -> real automation -> captured outgoing requests, without a world/window. */
final class ClientTradeReadinessSmoke {
    static void run() throws Exception {
        Minecraft previousClient = Minecraft.getInstance();
        AutomationManager manager = AutomationManager.INSTANCE;
        try {
            for (var item : List.of(Items.EMERALD, Items.COAL, Items.DIAMOND_PICKAXE)) {
                var holder = BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getKey(item)).orElseThrow();
                if (!holder.areComponentsBound()) holder.bindComponents(DataComponentMap.EMPTY);
            }
            Minecraft client = allocate(Minecraft.class);
            set(Minecraft.class, null, "instance", client);
            Gui gui = allocate(Gui.class);
            set(Minecraft.class, client, "gui", gui);
            set(Minecraft.class, client, "packetProcessor", new PacketProcessor(Thread.currentThread()));
            RecordingPlayer player = allocate(RecordingPlayer.class);
            client.player = player;
            Inventory inventory = new Inventory(player, new EntityEquipment());
            MerchantMenu menu = new MerchantMenu(7, inventory);
            menu.setShowProgressBar(true);
            menu.setMerchantLevel(1);
            player.containerMenu = menu;
            MerchantScreen screen = allocate(MerchantScreen.class);
            set(AbstractContainerScreen.class, screen, "menu", menu);
            set(Screen.class, screen, "title", Component.literal("耐久三号"));
            NamedVillagerSmoke.bindFixture(client, player, menu, "toolsmith");
            set(Gui.class, gui, "screen", screen);

            RecordingConnection connection = new RecordingConnection();
            ClientPacketListener listener = allocate(ClientPacketListener.class);
            set(ClientCommonPacketListenerImpl.class, listener, "connection", connection);
            set(ClientCommonPacketListenerImpl.class, listener, "minecraft", client);
            set(LocalPlayer.class, player, "connection", listener);

            Class<?> payloadClass = Class.forName("net.ramixin.visibletraders.networking.ClientboundLockedTradesPayload");
            Constructor<?> payload = payloadClass.getConstructor(Optional.class);
            Method handle = Class.forName("net.ramixin.visibletraders.client.VisibleTradersCommonClient")
                    .getMethod("handleClientboundTradesPayload", payloadClass);

            MappedRegistry<Enchantment> owner = new MappedRegistry<>(Registries.ENCHANTMENT, Lifecycle.stable());
            Holder<Enchantment> unbreaking = Holder.Reference.createStandAlone(owner,
                    ResourceKey.create(Registries.ENCHANTMENT, id("unbreaking")));
            MerchantOffers initial = new MerchantOffers();
            initial.add(new MerchantOffer(new ItemCost(Items.EMERALD, 1), new ItemStack(Items.COAL), 12, 1, 0.05F));
            MerchantOffers miss = pickaxe(unbreaking, 2);
            MerchantOffers hit = pickaxe(unbreaking, 3);
            FilterEntry filter = new FilterEntry();
            filter.setItemId(id("diamond_pickaxe"));
            filter.setItemEnchantments(List.of(new FilterEntry.EnchantmentTarget(id("unbreaking"), 3)));
            filter.setTradeScope(FilterEntry.TradeScope.LATER);
            filter.setMinPrice(26);
            filter.setMaxPrice(30);
            filter.setMode(FilterEntry.Mode.ON);
            manager.setFilterEntries(id("toolsmith"), List.of(filter));
            manager.setMatchAny(id("toolsmith"), false);

            for (var scope : List.of(FilterEntry.TradeScope.LATER, FilterEntry.TradeScope.ALL)) {
                filter.setTradeScope(scope);
                manager.setFilterEntries(id("toolsmith"), List.of(filter));
                for (int order = 0; order < 3; order++) {
                    menu.setOffers(initial);
                    handle.invoke(null, payload.newInstance(Optional.of(miss)));
                    int before = connection.requests;
                    manager.toggle();
                    require(manager.isRunning() && connection.requests == before + 1, "first request is sent once");
                    if (order == 1) listener.handleMerchantOffers(initialPacket(initial));
                    handle.invoke(null, payload.newInstance(Optional.empty()));
                    require(manager.isRunning() && connection.requests == before + 1,
                            "clear marker must never trigger another refresh");
                    if (order == 0) listener.handleMerchantOffers(initialPacket(initial));
                    require(manager.isRunning() && connection.requests == before + 1,
                            "initial plus clear must wait for generated future trades");
                    handle.invoke(null, payload.newInstance(Optional.of(hit)));
                    if (order == 2) {
                        require(manager.isRunning() && connection.requests == before + 1,
                                "locked-first hit waits for its matching initial packet");
                        listener.handleMerchantOffers(initialPacket(initial));
                    }
                    require(manager.isRunning() && connection.requests == before + 1,
                            "receiving the last required packet must leave time for the rest of its burst");
                    settle(manager);
                    require(!manager.isRunning() && connection.requests == before + 1,
                            "Unbreaking III pickaxe must stop without sending the next request");
                }
            }
            filter.setTradeScope(FilterEntry.TradeScope.LATER);
            manager.setFilterEntries(id("toolsmith"), List.of(filter));
            menu.setOffers(initial);
            handle.invoke(null, payload.newInstance(Optional.of(miss)));
            int before = connection.requests;
            manager.toggle();
            handle.invoke(null, payload.newInstance(Optional.empty()));
            listener.handleMerchantOffers(initialPacket(initial));
            handle.invoke(null, payload.newInstance(Optional.of(miss)));
            settle(manager);
            require(manager.isRunning() && connection.requests == before + 2,
                    "actual non-matching completed response sends exactly one next request");

            manager.toggle();
            manager.toggle();
            require(manager.isRunning() && connection.requests == before + 2,
                    "restarting in the same menu keeps the previous request in flight");
            handle.invoke(null, payload.newInstance(Optional.empty()));
            listener.handleMerchantOffers(initialPacket(initial));
            handle.invoke(null, payload.newInstance(Optional.of(hit)));
            settle(manager);
            require(!manager.isRunning() && connection.requests == before + 2,
                    "the outstanding round can safely satisfy the restarted run");

            filter.setTradeScope(FilterEntry.TradeScope.INITIAL);
            manager.setFilterEntries(id("toolsmith"), List.of(filter));
            int start = connection.requests;
            manager.toggle();
            require(manager.isRunning() && connection.requests == start + 1, "send the first initial-only request");
            listener.handleMerchantOffers(initialPacket(miss));
            listener.handleMerchantOffers(initialPacket(miss));
            manager.clientTick();
            listener.handleMerchantOffers(initialPacket(miss));
            manager.clientTick();
            require(connection.requests == start + 1, "no new request halfway through the three-packet burst");
            settle(manager);
            require(connection.requests == start + 2, "one new request after the whole burst settles");
            listener.handleMerchantOffers(initialPacket(hit));
            listener.handleMerchantOffers(initialPacket(hit));
            listener.handleMerchantOffers(initialPacket(hit));
            settle(manager);
            require(!manager.isRunning() && connection.requests == start + 2,
                    "the three-packet hit stops without a request in flight");
            System.out.println("CLIENT TRADE READINESS PASSED: real handlers, initial/locked/all scopes, duplicate burst, restart, and zero extra refreshes after a match");
        } finally {
            VillagerTradeContext.clear();
            manager.stop("readiness cleanup");
            Minecraft current = Minecraft.getInstance();
            if (current != null && current != previousClient && current.player != null) {
                current.player.containerMenu = null;
                manager.clientTick();
            }
            set(Minecraft.class, null, "instance", previousClient);
        }
    }

    private static void settle(AutomationManager manager) throws InterruptedException {
        Thread.sleep(60);
        manager.clientTick();
    }

    private static ClientboundMerchantOffersPacket initialPacket(MerchantOffers offers) {
        return new ClientboundMerchantOffersPacket(7, offers, 1, 0, true, true);
    }

    private static MerchantOffers pickaxe(Holder<Enchantment> unbreaking, int level) {
        ItemStack stack = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        enchantments.set(unbreaking, level);
        stack.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
        MerchantOffers offers = new MerchantOffers();
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 28), stack, 3, 15, 0.2F));
        return offers;
    }

    private static final class RecordingConnection extends Connection {
        private int requests;
        private RecordingConnection() { super(PacketFlow.CLIENTBOUND); }
        @Override public void send(Packet<?> packet) {
            require(packet instanceof ServerboundCustomPayloadPacket, "unexpected outgoing packet");
            requests++;
        }
    }

    private static final class RecordingPlayer extends LocalPlayer {
        private RecordingPlayer() { super(null, null, null, null, null, null, false, null); }
        @Override public void sendSystemMessage(Component message) { /* No GUI in this fixture. */ }
    }

    private static Identifier id(String path) { return Identifier.withDefaultNamespace(path); }

    @SuppressWarnings("unchecked")
    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field field = unsafeClass.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (T) unsafeClass.getMethod("allocateInstance", Class.class).invoke(field.get(null), type);
    }

    private static void set(Class<?> owner, Object instance, String name, Object value) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(instance, value);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
