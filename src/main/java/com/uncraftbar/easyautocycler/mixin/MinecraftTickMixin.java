package com.uncraftbar.easyautocycler.mixin;

import com.uncraftbar.easyautocycler.AutomationManager;
import com.uncraftbar.easyautocycler.InputHandler;
import com.uncraftbar.easyautocycler.VillagerTradeContext;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftTickMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void easyautocycler$onClientTick(CallbackInfo ci) {
        Minecraft client = (Minecraft) (Object) this;
        VillagerTradeContext.clientTick();
        AutomationManager.INSTANCE.clientTick();
        InputHandler.onClientTick(client);
    }
}
