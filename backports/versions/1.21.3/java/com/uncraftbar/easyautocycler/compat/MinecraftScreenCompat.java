package com.uncraftbar.easyautocycler.compat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
public final class MinecraftScreenCompat {
    public static Screen getScreen(Minecraft mc) { return mc.screen; }
    public static void setScreen(Minecraft mc, Screen screen) { mc.setScreen(screen); }
}