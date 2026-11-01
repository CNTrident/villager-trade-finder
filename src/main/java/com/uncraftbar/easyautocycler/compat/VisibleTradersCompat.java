package com.uncraftbar.easyautocycler.compat;

import com.uncraftbar.easyautocycler.EasyAutoCyclerMod;
import com.uncraftbar.easyautocycler.filter.FilterEntry;
import com.uncraftbar.easyautocycler.mixin.MerchantMenuAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffers;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/** Optional, reflection-based bridge to Ramixin/VisibleTraders. */
public final class VisibleTradersCompat {
    private static final String MOD_ID = "visibletraders";
    private static boolean initialized;
    private static boolean loaded;
    private static Method payloadOffers;
    private static final Map<MerchantMenu, LockedOffers> lockedOffers = new WeakHashMap<>();

    private VisibleTradersCompat() {}

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        loaded = FabricLoader.getInstance().isModLoaded(MOD_ID);
        if (!loaded) {
            EasyAutoCyclerMod.LOGGER.info("VisibleTraders is not loaded; only initial-trade matching is available");
            return;
        }

        try {
            Class<?> payloadClass = Class.forName("net.ramixin.visibletraders.networking.ClientboundLockedTradesPayload");
            payloadOffers = payloadClass.getMethod("offers");
            if (payloadOffers.getReturnType() != Optional.class) throw new NoSuchMethodException("offers: Optional");
            EasyAutoCyclerMod.LOGGER.info("VisibleTraders trade-scope integration enabled");
        } catch (ReflectiveOperationException exception) {
            loaded = false;
            EasyAutoCyclerMod.LOGGER.error("Failed to initialize VisibleTraders integration", exception);
        }
    }

    public static boolean isAvailable() {
        return loaded && payloadOffers != null;
    }

    /** Empty Optional clears the display while generation runs; it is NOT a completed response. */
    public static boolean recordLockedOffers(MerchantMenu menu, Object payload) {
        if (!isAvailable()) return false;
        try {
            Optional<?> offers = (Optional<?>) payloadOffers.invoke(payload);
            boolean ready = offers.isPresent();
            MerchantOffers snapshot = ready ? copy((MerchantOffers) offers.get()) : new MerchantOffers();
            lockedOffers.put(menu, new LockedOffers(snapshot, ready));
            return ready;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            lockedOffers.remove(menu);
            EasyAutoCyclerMod.LOGGER.error("Failed to capture VisibleTraders response", exception);
            return false;
        }
    }

    public static OfferSets captureInitialOffers(MerchantMenu menu) {
        MerchantOffers initial = ((MerchantMenuAccessor) menu).easyAutoCycler$getTrader().getOffers();
        return new OfferSets(initial, new MerchantOffers(), initial, false);
    }

    public static OfferSets captureOffers(MerchantMenu menu) {
        MerchantOffers initial = copy(((MerchantMenuAccessor) menu).easyAutoCycler$getTrader().getOffers());
        LockedOffers snapshot = lockedOffers.get(menu);
        if (!isAvailable() || snapshot == null || !snapshot.ready()) {
            return new OfferSets(initial, new MerchantOffers(), initial, false);
        }
        // Read the actual payload snapshot, never the UI's combined cache from another round.
        MerchantOffers later = copy(snapshot.offers());
        MerchantOffers all = copy(initial);
        all.addAll(later);
        return new OfferSets(initial, later, all, true);
    }

    private static MerchantOffers copy(MerchantOffers source) {
        MerchantOffers result = new MerchantOffers();
        result.addAll(source);
        return result;
    }

    private record LockedOffers(MerchantOffers offers, boolean ready) {}

    public record OfferSets(MerchantOffers initial, MerchantOffers later, MerchantOffers all,
                            boolean lockedOffersReady) {
        public MerchantOffers forScope(FilterEntry.TradeScope scope) {
            return switch (scope) {
                case INITIAL -> initial;
                case LATER -> later;
                case ALL -> all;
            };
        }
    }
}
