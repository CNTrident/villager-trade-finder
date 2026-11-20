package com.uncraftbar.easyautocycler.network;

import com.uncraftbar.easyautocycler.AutomationManager;
import com.uncraftbar.easyautocycler.EasyAutoCyclerMod;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;

public final class ConfirmedCycleClient {
    private static boolean registered;

    private ConfirmedCycleClient() {}

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(ConfirmedCycleProtocol.OFFERS, (client, listener, buffer, response) -> {
            long round = buffer.readVarLong();
            var offers = new ClientboundMerchantOffersPacket(buffer);
            client.execute(() -> AutomationManager.INSTANCE.onConfirmedOffers(round, offers, listener));
        });
        registered = true;
    }

    public static boolean isAvailable() {
        return registered && Minecraft.getInstance().getConnection() != null
                && ClientPlayNetworking.canSend(ConfirmedCycleProtocol.REQUEST);
    }

    public static boolean send(int containerId, long round, boolean needLaterOffers) {
        try {
            var buffer = PacketByteBufs.create();
            buffer.writeVarInt(containerId);
            buffer.writeVarLong(round);
            buffer.writeBoolean(needLaterOffers);
            ClientPlayNetworking.send(ConfirmedCycleProtocol.REQUEST, buffer);
            return true;
        } catch (RuntimeException error) {
            EasyAutoCyclerMod.LOGGER.error("Could not send confirmed trade refresh", error);
            return false;
        }
    }
}
