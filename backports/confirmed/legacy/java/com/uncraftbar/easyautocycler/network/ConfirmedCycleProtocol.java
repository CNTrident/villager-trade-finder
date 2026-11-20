package com.uncraftbar.easyautocycler.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

final class ConfirmedCycleProtocol {
    static final ResourceLocation REQUEST = new ResourceLocation("easyautocycler", "confirmed_cycle_request");
    static final ResourceLocation OFFERS = new ResourceLocation("easyautocycler", "confirmed_cycle_offers");

    private ConfirmedCycleProtocol() {}

    static void initialize(boolean canRefresh) {
        if (!canRefresh) return;
        ServerPlayNetworking.registerGlobalReceiver(REQUEST, (server, player, listener, buffer, response) -> {
            int containerId = buffer.readVarInt();
            long round = buffer.readVarLong();
            boolean needLaterOffers = buffer.readBoolean();
            server.execute(() -> ConfirmedCycleServer.refresh(player, containerId, round, needLaterOffers));
        });
    }

    static boolean canReply(ServerPlayer player) {
        return ServerPlayNetworking.canSend(player, OFFERS);
    }

    static void reply(ServerPlayer player, long round, ClientboundMerchantOffersPacket offers) {
        var buffer = PacketByteBufs.create();
        buffer.writeVarLong(round);
        offers.write(buffer);
        ServerPlayNetworking.send(player, OFFERS, buffer);
    }
}
