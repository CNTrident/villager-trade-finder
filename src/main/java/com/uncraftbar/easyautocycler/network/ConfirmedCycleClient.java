package com.uncraftbar.easyautocycler.network;

import com.uncraftbar.easyautocycler.AutomationManager;
import com.uncraftbar.easyautocycler.EasyAutoCyclerMod;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public final class ConfirmedCycleClient {
    private static boolean registered;

    private ConfirmedCycleClient() {}

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(ConfirmedCycleOffers.TYPE, (reply, context) ->
                AutomationManager.INSTANCE.onConfirmedOffers(reply, context.player().connection));
        registered = true;
    }

    public static boolean isAvailable() {
        return registered && Minecraft.getInstance().getConnection() != null
                && ClientPlayNetworking.canSend(ConfirmedCycleRequest.TYPE);
    }

    public static boolean send(int containerId, long round) {
        try {
            ClientPlayNetworking.send(new ConfirmedCycleRequest(containerId, round));
            return true;
        } catch (RuntimeException error) {
            EasyAutoCyclerMod.LOGGER.error("Could not send confirmed trade refresh", error);
            return false;
        }
    }
}
