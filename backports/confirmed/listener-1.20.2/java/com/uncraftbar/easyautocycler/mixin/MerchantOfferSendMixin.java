package com.uncraftbar.easyautocycler.mixin;

import com.uncraftbar.easyautocycler.network.ConfirmedCycleServer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class MerchantOfferSendMixin {
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void easyAutoCycler$capture(Packet<?> packet, CallbackInfo ci) {
        if (ConfirmedCycleServer.capture(this, packet)) ci.cancel();
    }
}
