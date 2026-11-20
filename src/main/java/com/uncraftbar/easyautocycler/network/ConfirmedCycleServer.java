package com.uncraftbar.easyautocycler.network;

import de.maxhenkel.tradecycling.TradeCyclingMod;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConfirmedCycleServer implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("Villager Trade Finder");
    private static final ThreadLocal<Refresh> ACTIVE_REFRESH = new ThreadLocal<>();

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.serverboundPlay().register(ConfirmedCycleRequest.TYPE, ConfirmedCycleRequest.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ConfirmedCycleOffers.TYPE, ConfirmedCycleOffers.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ConfirmedCycleRequest.TYPE,
                (request, context) -> refresh(context.player(), request));
    }

    private static void refresh(ServerPlayer player, ConfirmedCycleRequest request) {
        if (request.round() <= 0 || !player.isAlive() || player.isSpectator()
                || !(player.containerMenu instanceof MerchantMenu menu) || menu.containerId != request.containerId()
                || !menu.stillValid(player) || !ServerPlayNetworking.canSend(player, ConfirmedCycleOffers.TYPE)) return;

        Refresh refresh = new Refresh(player, request.containerId());
        ACTIVE_REFRESH.set(refresh);
        try {
            TradeCyclingMod.onCycleTrades(player);
        } catch (RuntimeException | LinkageError error) {
            LOGGER.error("Confirmed Trade Cycling refresh failed", error);
            return;
        } finally {
            ACTIVE_REFRESH.remove();
        }
        if (refresh.offers != null && player.containerMenu == menu) {
            ServerPlayNetworking.send(player, new ConfirmedCycleOffers(request.round(), refresh.offers));
        }
    }

    public static boolean captureOffers(ServerPlayer player, int containerId, MerchantOffers offers,
                                        int level, int xp, boolean showProgress, boolean canRestock) {
        Refresh refresh = ACTIVE_REFRESH.get();
        if (refresh == null || refresh.player != player || refresh.containerId != containerId) return false;
        // Trade Cycling can publish interim copies while regenerating and repricing. Its
        // final vanilla packet is the only one sent for this negotiated request.
        refresh.offers = new ClientboundMerchantOffersPacket(containerId, offers, level, xp, showProgress, canRestock);
        return true;
    }

    private static final class Refresh {
        private final ServerPlayer player;
        private final int containerId;
        private ClientboundMerchantOffersPacket offers;

        private Refresh(ServerPlayer player, int containerId) {
            this.player = player;
            this.containerId = containerId;
        }
    }
}
