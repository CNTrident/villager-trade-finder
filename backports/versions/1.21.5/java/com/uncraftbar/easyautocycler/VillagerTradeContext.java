package com.uncraftbar.easyautocycler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/** Associates a merchant menu with the villager actually interacted with on this client. */
public final class VillagerTradeContext {
    private static final int OPEN_TIMEOUT_TICKS = 100;
    @Nullable private static Player player;
    @Nullable private static Level level;
    @Nullable private static Villager pendingVillager;
    private static int interactionTick;
    @Nullable private static MerchantMenu boundMenu;
    @Nullable private static Villager boundVillager;

    private VillagerTradeContext() {}

    public static void recordInteraction(Player interactingPlayer, Entity target) {
        clear();
        Minecraft client = Minecraft.getInstance();
        if (interactingPlayer != client.player || client.level == null
                || !(target instanceof Villager villager) || villager.isRemoved()
                || villager.level() != client.level) return;
        player = interactingPlayer;
        level = client.level;
        pendingVillager = villager;
        interactionTick = interactingPlayer.tickCount;
    }

    public static void onScreenBeforeInit(Screen screen) {
        if (screen instanceof MerchantScreen merchantScreen) {
            bindMenu(merchantScreen.getMenu());
        } else if (screen instanceof AbstractContainerScreen<?>) {
            clear();
        }
    }

    static void bindMenu(MerchantMenu menu) {
        if (!validSession()) {
            clear();
            return;
        }
        if (boundMenu == menu) return; // Returning from configuration keeps the same menu.
        boundMenu = null;
        boundVillager = null;
        int age = player.tickCount - interactionTick;
        if (pendingVillager != null && age >= 0 && age <= OPEN_TIMEOUT_TICKS
                && !pendingVillager.isRemoved() && player.containerMenu == menu) {
            boundMenu = menu;
            boundVillager = pendingVillager;
        }
        pendingVillager = null; // An interaction may only identify one newly opened menu.
    }

    @Nullable
    public static ResourceLocation professionId(MerchantMenu menu) {
        if (!validSession() || menu != boundMenu || player.containerMenu != menu
                || boundVillager == null || boundVillager.isRemoved()
                || boundVillager.level() != level) return null;
        return boundVillager.getVillagerData().profession().unwrapKey()
                .map(key -> key.location()).orElse(null);
    }

    public static void clientTick() {
        if (!validSession() || (boundMenu != null && player.containerMenu != boundMenu)
                || (pendingVillager != null && (player.tickCount - interactionTick < 0
                || player.tickCount - interactionTick > OPEN_TIMEOUT_TICKS))) clear();
    }

    private static boolean validSession() {
        Minecraft client = Minecraft.getInstance();
        return player != null && level != null && client.player == player && client.level == level;
    }

    static void clear() {
        player = null;
        level = null;
        pendingVillager = null;
        boundMenu = null;
        boundVillager = null;
    }
}
