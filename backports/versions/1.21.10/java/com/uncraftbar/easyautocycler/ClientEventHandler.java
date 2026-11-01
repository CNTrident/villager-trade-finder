package com.uncraftbar.easyautocycler;

import com.mojang.blaze3d.platform.InputConstants;
import com.uncraftbar.easyautocycler.config.ClientConfig;
import com.uncraftbar.easyautocycler.gui.ConfigScreen;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;

public final class ClientEventHandler {
    public static void onScreenBeforeInit(Minecraft client, Screen screen, int width, int height) {
        VillagerTradeContext.onScreenBeforeInit(screen);
        if (!(screen instanceof MerchantScreen) || !AutomationManager.INSTANCE.isSupportedVillagerTradeScreen(screen)) return;
        ScreenKeyboardEvents.beforeKeyPress(screen).register((s, event) -> {
            var key=KeyBindingHelper.getBoundKeyOf(Keybindings.toggleAutoTradeKey);
            if(key.getType()==InputConstants.Type.KEYSYM && event.key()==key.getValue()) AutomationManager.INSTANCE.toggle();
            var settings=KeyBindingHelper.getBoundKeyOf(Keybindings.openConfigKey);
            if(settings.getType()==InputConstants.Type.KEYSYM && event.key()==settings.getValue()) client.setScreen(new ConfigScreen(s,ConfigScreen.titleFor(s)));
        });
    }

    public static void onScreenInit(Minecraft client, Screen screen, int width, int height) {
        if (!(screen instanceof MerchantScreen merchant) || !AutomationManager.INSTANCE.isSupportedVillagerTradeScreen(screen)) return;
        var config = ClientConfig.load();
        if (config.parsedButtonLocation() == ClientConfig.ButtonLocation.NONE) return;
        int left = (width - 276) / 2;
        int top = (height - 166) / 2;
        int x = (config.parsedButtonLocation() == ClientConfig.ButtonLocation.TOP_RIGHT ? left + 278 : left - 20) + config.buttonOffsetX;
        int y = top + 18 + config.buttonOffsetY;
        Button settings = Button.builder(Component.literal("C"), b -> client.setScreen(new ConfigScreen(screen, ConfigScreen.titleFor(screen))))
                .bounds(x, y, 18, 18).build();
        settings.setTooltip(Tooltip.create(Component.translatable("gui.easyautocycler.button.config.tooltip")));
        Button start = Button.builder(Component.literal("R"), b -> AutomationManager.INSTANCE.toggle())
                .bounds(x, y + 20, 18, 18).build();
        start.setTooltip(Tooltip.create(Component.translatable("gui.easyautocycler.button.toggle.tooltip")));
        Screens.getButtons(screen).add(settings);
        Screens.getButtons(screen).add(start);
    }
}
