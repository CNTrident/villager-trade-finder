package com.uncraftbar.easyautocycler.compat;

import com.uncraftbar.easyautocycler.EasyAutoCyclerMod;
import com.uncraftbar.easyautocycler.filter.FilterEntry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffers;
import java.lang.reflect.Method;

/** 1.20-1.21 VisibleTraders marks the unlocked prefix in its menu duck. */
public final class VisibleTradersCompat {
    private static boolean initialized;
    private static Method shouldAllowTrade;
    public static void initialize() {
        if(initialized)return;initialized=true;
        if(!FabricLoader.getInstance().isModLoaded("visibletraders"))return;
        for(String name : new String[]{"net.ramixin.visibletraders.ducks.MerchantMenuDuck",
                "net.ramgames.visibletraders.MerchantMenuDuck",
                "net.ramgames.visibletraders.ducks.MerchantMenuDuck"}) {
            try { shouldAllowTrade=Class.forName(name).getMethod("visibleTraders$shouldAllowTrade",int.class);return; }
            catch(ReflectiveOperationException ignored) {}
        }
        EasyAutoCyclerMod.LOGGER.warn("Unsupported VisibleTraders menu API; future-trade matching is disabled");
    }
    public static boolean isAvailable(){return shouldAllowTrade!=null;}
    // Legacy VisibleTraders returns initial and future offers in ONE vanilla packet.
    public static OfferSets captureInitialOffers(MerchantMenu menu) { return captureOffers(menu); }
    public static OfferSets captureOffers(MerchantMenu menu) {
        MerchantOffers all=copy(menu.getOffers());
        if(!isAvailable())return new OfferSets(all,new MerchantOffers(),all,false);
        MerchantOffers initial=new MerchantOffers(),later=new MerchantOffers();
        try {
            for(int i=0;i<all.size();i++) {
                if((boolean)shouldAllowTrade.invoke(menu,i))initial.add(all.get(i));
                else later.add(all.get(i));
            }
            return new OfferSets(initial,later,all,!later.isEmpty());
        } catch(ReflectiveOperationException exception) {
            EasyAutoCyclerMod.LOGGER.error("Could not read VisibleTraders trade levels",exception);
            return new OfferSets(all,new MerchantOffers(),all,false);
        }
    }
    private static MerchantOffers copy(MerchantOffers offers){MerchantOffers result=new MerchantOffers();result.addAll(offers);return result;}
    public record OfferSets(MerchantOffers initial,MerchantOffers later,MerchantOffers all,boolean lockedOffersReady) {
        public MerchantOffers forScope(FilterEntry.TradeScope scope) {
            return switch(scope){case INITIAL->initial;case LATER->later;case ALL->all;};
        }
    }
}
