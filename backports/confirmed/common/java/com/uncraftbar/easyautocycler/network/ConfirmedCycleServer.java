package com.uncraftbar.easyautocycler.network;

import com.uncraftbar.easyautocycler.mixin.MerchantMenuAccessor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import net.fabricmc.api.ModInitializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.MerchantMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Holds the final vanilla offer packet emitted by one native Trade Cycling call. */
public final class ConfirmedCycleServer implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("Villager Trade Finder");
    private static final ThreadLocal<Refresh> ACTIVE_REFRESH = new ThreadLocal<>();
    private static Method nativeCycle;

    @Override
    public void onInitialize() {
        try {
            Method method = Class.forName("de.maxhenkel.tradecycling.TradeCyclingMod")
                    .getMethod("onCycleTrades", ServerPlayer.class);
            if (!Modifier.isStatic(method.getModifiers()) || method.getReturnType() != void.class) {
                throw new NoSuchMethodException(method.toString());
            }
            nativeCycle = method;
        } catch (ReflectiveOperationException | LinkageError error) {
            LOGGER.warn("Trade Cycling has no supported server refresh entrypoint; numbered refresh is unavailable", error);
        }
        ConfirmedCycleProtocol.initialize(nativeCycle != null);
    }

    static void refresh(ServerPlayer player, int containerId, long round, boolean needLaterOffers) {
        if (nativeCycle == null || round <= 0 || !player.isAlive() || player.isSpectator()
                || !(player.containerMenu instanceof MerchantMenu menu) || menu.containerId != containerId
                || !menu.stillValid(player) || !ConfirmedCycleProtocol.canReply(player)) return;

        Refresh refresh = new Refresh(player.connection, menu);
        ACTIVE_REFRESH.set(refresh);
        boolean completed = false;
        try {
            nativeCycle.invoke(null, player);
            completed = true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            LOGGER.error("Confirmed Trade Cycling refresh failed", error);
        } finally {
            ACTIVE_REFRESH.remove();
        }
        if (refresh.offers == null || player.containerMenu != menu) return;
        if (completed) {
            var offers = needLaterOffers
                    ? VisibleOffersCompat.complete(((MerchantMenuAccessor) menu).easyAutoCycler$getTrader(), refresh.offers)
                    : refresh.offers;
            ConfirmedCycleProtocol.reply(player, round, offers);
        } else {
            // Preserve the current menu if native refresh changed it before failing.
            player.connection.send(refresh.offers);
        }
    }

    public static boolean capture(Object listener, Packet<?> packet) {
        Refresh refresh = ACTIVE_REFRESH.get();
        if (refresh == null || refresh.listener != listener
                || !(packet instanceof ClientboundMerchantOffersPacket offers)
                || offers.getContainerId() != refresh.menu.containerId) return false;
        refresh.offers = offers;
        return true;
    }

    private static final class Refresh {
        private final ServerGamePacketListenerImpl listener;
        private final MerchantMenu menu;
        private ClientboundMerchantOffersPacket offers;

        private Refresh(ServerGamePacketListenerImpl listener, MerchantMenu menu) {
            this.listener = listener;
            this.menu = menu;
        }
    }
}
