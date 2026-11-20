package com.uncraftbar.easyautocycler.mixin;

import com.uncraftbar.easyautocycler.network.ConfirmedCycleServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerOffersMixin {
    @Inject(method = "sendMerchantOffers", at = @At("HEAD"), cancellable = true)
    private void easyAutoCycler$captureOffers(int containerId, MerchantOffers offers, int level, int xp,
                                               boolean showProgress, boolean canRestock, CallbackInfo ci) {
        if (ConfirmedCycleServer.captureOffers((ServerPlayer) (Object) this,
                containerId, offers, level, xp, showProgress, canRestock)) {
            ci.cancel();
        }
    }
}
