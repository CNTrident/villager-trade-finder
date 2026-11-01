package com.uncraftbar.easyautocycler;

import com.mojang.serialization.Lifecycle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.util.Arrays;

/** Exercises the packaged interaction mixin, entity professions and menu/session lifetime. */
final class NamedVillagerSmoke {
    static void run() throws Exception {
        Minecraft previous = Minecraft.getInstance();
        try {
            Minecraft client = allocate(Minecraft.class);
            set(Minecraft.class, null, "instance", client);
            LocalPlayer player = allocate(LocalPlayer.class);
            client.player = player;
            MerchantMenu menu = allocate(MerchantMenu.class);
            MerchantScreen screen = allocate(MerchantScreen.class);
            set(AbstractContainerScreen.class, screen, "menu", menu);
            set(Screen.class, screen, "title", Component.translatable("entity.minecraft.villager.librarian"));
            require(!AutomationManager.INSTANCE.isSupportedVillagerTradeScreen(screen), "title cannot identify an unknown merchant");

            FixtureVillager villager = bindFixture(client, player, menu, "toolsmith");
            // Invoke the handler merged by the real Mixin transformer, rather than a mock callback.
            VillagerTradeContext.clear();
            var hook = Arrays.stream(MultiPlayerGameMode.class.getDeclaredMethods())
                    .filter(method -> method.getName().contains("recordVillager")).findFirst().orElseThrow();
            hook.setAccessible(true);
            hook.invoke(allocate(MultiPlayerGameMode.class), player, villager,
                    new EntityHitResult(villager, Vec3.ZERO), InteractionHand.MAIN_HAND,
                    new CallbackInfoReturnable<>("interact", false));
            VillagerTradeContext.onScreenBeforeInit(screen);
            for (Component title : new Component[]{Component.literal("耐久三号"), Component.empty(),
                    Component.translatable("entity.minecraft.villager.librarian")}) {
                set(Screen.class, screen, "title", title);
                require(id("toolsmith").equals(AutomationManager.INSTANCE.getVillagerProfessionId(screen)),
                        "entity profession must win over renamed, empty and misleading titles");
                require(AutomationManager.INSTANCE.isSupportedVillagerTradeScreen(screen), "renamed villager keeps button/key gate enabled");
            }
            VillagerTradeContext.onScreenBeforeInit(screen);
            require(id("toolsmith").equals(VillagerTradeContext.professionId(menu)), "configuration return preserves menu binding");
            villager.data = data("librarian");
            require(AutomationManager.INSTANCE.isLibrarianTradeScreen(screen), "profession comes from current synchronized entity data");
            for (String excluded : new String[]{"none", "nitwit"}) {
                villager.data = data(excluded);
                require(!AutomationManager.INSTANCE.isSupportedVillagerTradeScreen(screen), "excluded profession " + excluded);
            }

            bindFixture(client, player, menu, "toolsmith");
            MerchantMenu other = allocate(MerchantMenu.class);
            player.containerMenu = other;
            VillagerTradeContext.clientTick();
            player.containerMenu = menu;
            VillagerTradeContext.bindMenu(menu);
            require(VillagerTradeContext.professionId(menu) == null, "closed menus cannot reuse stale entity binding");
            bindFixture(client, player, menu, "toolsmith");
            VillagerTradeContext.recordInteraction(player, player);
            VillagerTradeContext.bindMenu(menu);
            require(VillagerTradeContext.professionId(menu) == null, "non-villager interaction invalidates previous villager");
            villager = bindFixture(client, player, menu, "toolsmith");
            VillagerTradeContext.recordInteraction(player, villager);
            player.tickCount += 101;
            VillagerTradeContext.bindMenu(menu);
            require(VillagerTradeContext.professionId(menu) == null, "expired interaction is not reused");
            bindFixture(client, player, menu, "toolsmith");
            ClientLevel level = client.level;
            client.level = null;
            VillagerTradeContext.clientTick();
            client.level = level;
            require(VillagerTradeContext.professionId(menu) == null, "disconnect clears entity and menu");
            bindFixture(client, player, menu, "toolsmith");
            client.level = allocate(ClientLevel.class);
            require(VillagerTradeContext.professionId(menu) == null, "world change rejects previous world entity");
            System.out.println("NAMED VILLAGER CHECKS PASSED: transformed interaction hook, arbitrary titles, live professions, exclusions, menu close, timeout, disconnect, world change");
        } finally {
            VillagerTradeContext.clear();
            set(Minecraft.class, null, "instance", previous);
        }
    }

    static FixtureVillager bindFixture(Minecraft client, LocalPlayer player, MerchantMenu menu, String profession) throws Exception {
        if (client.level == null) client.level = allocate(ClientLevel.class);
        set(Entity.class, player, "level", client.level);
        player.containerMenu = menu;
        FixtureVillager villager = allocate(FixtureVillager.class);
        set(Entity.class, villager, "level", client.level);
        villager.data = data(profession);
        VillagerTradeContext.recordInteraction(player, villager);
        VillagerTradeContext.bindMenu(menu);
        return villager;
    }

    private static VillagerData data(String profession) {
        var types = new MappedRegistry<>(Registries.VILLAGER_TYPE, Lifecycle.stable());
        var professions = new MappedRegistry<>(Registries.VILLAGER_PROFESSION, Lifecycle.stable());
        return new VillagerData(Holder.Reference.createStandAlone(types, VillagerType.PLAINS),
                Holder.Reference.createStandAlone(professions, ResourceKey.create(Registries.VILLAGER_PROFESSION, id(profession))), 1);
    }

    private static final class FixtureVillager extends Villager {
        private VillagerData data;
        private FixtureVillager() { super(null, null); }
        @Override public VillagerData getVillagerData() { return data; }
        @Override public Component getCustomName() { return Component.literal("耐久三号"); }
    }

    private static Identifier id(String path) { return Identifier.withDefaultNamespace(path); }
    @SuppressWarnings("unchecked")
    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        Field field = unsafe.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (T) unsafe.getMethod("allocateInstance", Class.class).invoke(field.get(null), type);
    }
    private static void set(Class<?> owner, Object instance, String name, Object value) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(instance, value);
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
