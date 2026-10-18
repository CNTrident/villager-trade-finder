package com.uncraftbar.easyautocycler.compat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.core.HolderSet;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.VillagerTrade;

public final class MinecraftApiCompat {
    private MinecraftApiCompat() {}

    public static InputConstants.Type keyboardType() { return InputConstants.Type.KEYBOARD; }

    public static HolderSet<VillagerTrade> trades(TradeSet set) { return set.trades(); }
}
