package com.uncraftbar.easyautocycler.mixin;

import com.uncraftbar.easyautocycler.AutomationManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional hook: runs only after VisibleTraders has applied the locked offers. */
@Pseudo
@Mixin(targets = "net.ramixin.visibletraders.client.VisibleTradersCommonClient", remap = false)
public abstract class VisibleTradersClientMixin {
    @Inject(method = "handleClientboundTradesPayload", at = @At("TAIL"))
    private static void easyAutoCycler$afterLockedOffersApplied(@Coerce Object payload, CallbackInfo ci) {
        AutomationManager.INSTANCE.onVisibleTradersOffersUpdated(payload);
    }
}
