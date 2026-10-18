package com.uncraftbar.easyautocycler.compat;

import com.uncraftbar.easyautocycler.EasyAutoCyclerMod;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.inventory.MerchantMenu;

/** Dispatch via Trade Cycling's own button so each version uses its native protocol. */
public final class NativeCycleCompat {
    public static boolean canCycle(MerchantMenu menu) {
        try {
            Class<?> type = Class.forName("de.maxhenkel.tradecycling.gui.CycleTradesButton");
            return (boolean) type.getMethod("canCycle", MerchantMenu.class).invoke(null, menu);
        } catch (ReflectiveOperationException exception) {
            EasyAutoCyclerMod.LOGGER.warn("Cannot query Trade Cycling", exception);
            return false;
        }
    }

    public static boolean cycle() {
        if (!(Minecraft.getInstance().screen instanceof MerchantScreen screen)
                || !canCycle(screen.getMenu())) return false;
        for (var widget : Screens.getButtons(screen)) {
            if (widget instanceof AbstractButton button && widget.getClass().getName()
                    .equals("de.maxhenkel.tradecycling.gui.CycleTradesButton")) {
                button.onPress(new net.minecraft.client.input.KeyEvent(82,0,0));
                return true;
            }
        }
        return false;
    }
}
