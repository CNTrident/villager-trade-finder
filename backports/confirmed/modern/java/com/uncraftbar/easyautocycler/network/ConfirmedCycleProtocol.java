package com.uncraftbar.easyautocycler.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.server.level.ServerPlayer;

final class ConfirmedCycleProtocol {
    private ConfirmedCycleProtocol() {}

    static void initialize(boolean canRefresh) {
        PayloadTypeRegistry.playC2S().register(ConfirmedCycleRequest.TYPE, ConfirmedCycleRequest.CODEC);
        PayloadTypeRegistry.playS2C().register(ConfirmedCycleOffers.TYPE, ConfirmedCycleOffers.CODEC);
        if (canRefresh) {
            ServerPlayNetworking.registerGlobalReceiver(ConfirmedCycleRequest.TYPE, (request, context) ->
                    ConfirmedCycleServer.refresh(context.player(), request.containerId(), request.round(), request.needLaterOffers()));
        }
    }

    static boolean canReply(ServerPlayer player) {
        return ServerPlayNetworking.canSend(player, ConfirmedCycleOffers.TYPE);
    }

    static void reply(ServerPlayer player, long round, ClientboundMerchantOffersPacket offers) {
        ServerPlayNetworking.send(player, new ConfirmedCycleOffers(round, offers));
    }
}
