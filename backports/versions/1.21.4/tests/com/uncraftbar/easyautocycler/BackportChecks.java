package com.uncraftbar.easyautocycler;
import com.uncraftbar.easyautocycler.filter.FilterEntry;
import com.uncraftbar.easyautocycler.config.FilterConfig;
import com.uncraftbar.easyautocycler.compat.StackCompat;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import java.util.Map;
import java.util.List;
import java.util.Optional;
public final class BackportChecks {
    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();Bootstrap.bootStrap();
        var access=net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY);
        for(String profession:List.of("armorer","toolsmith","weaponsmith")) {
            var candidates=com.uncraftbar.easyautocycler.compat.TradeItemsCompat.collect(access,id(profession));
            require(candidates.contains(id("coal")) && candidates.contains(id("iron_ingot")),"profession trade templates: "+profession);
        }
        FilterEntry filter=new FilterEntry();filter.setItemId(id("tipped_arrow"));filter.setPotionId(id("strong_poison"));
        require(filter.matchesPotion(arrow(Potions.STRONG_POISON)),"strong poison match");
        require(!filter.matchesPotion(arrow(Potions.POISON)),"normal poison exclusion");
        require(!filter.matchesPotion(arrow(Potions.LONG_POISON)),"extended poison exclusion");
        require(!filter.matchesPotion(new ItemStack(Items.ARROW)),"plain arrow exclusion");
        FilterConfig.saveFilters(List.of(),true,List.of(),true,Map.of("minecraft:fletcher",List.of(filter)),Map.of());
        var restored=FilterConfig.dataToFilters(FilterConfig.loadFilters().professionFilters.get("minecraft:fletcher")).get(0);
        require(filter.getPotionId().equals(restored.getPotionId()),"configuration round-trip");
        FilterConfig.FilterData legacy=new FilterConfig.FilterData();legacy.itemId="minecraft:tipped_arrow";
        require(FilterConfig.dataToFilters(List.of(legacy)).get(0).matchesPotion(arrow(Potions.POISON)),"legacy wildcard");
        var arrow=arrow(Potions.STRONG_POISON);arrow.setCount(5);
        var offer=new MerchantOffer(new ItemCost(Items.EMERALD, 2), Optional.of(new ItemCost(Items.ARROW, 5)), arrow, 12, 30, 0.05F);
        var offers=new MerchantOffers();offers.add(offer);
        filter.setMinPrice(2);filter.setMaxPrice(2);filter.setMinCount(5);
        var method=AutomationManager.class.getDeclaredMethod("checkTradeWithFilter",MerchantOffers.class,FilterEntry.class);method.setAccessible(true);
        require((boolean)method.invoke(AutomationManager.INSTANCE,offers,filter),"real merchant offer matching");
        filter.setPotionId(id("long_poison"));
        require(!(boolean)method.invoke(AutomationManager.INSTANCE,offers,filter),"wrong merchant potion exclusion");
        var sword=new ItemStack(Items.DIAMOND_SWORD);
        
        
        System.out.println("BACKPORT CHECKS PASSED: "+SharedConstants.getCurrentVersion().getName());
    }
    private static ItemStack arrow(net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> potion){return net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.TIPPED_ARROW, potion);}
    private static ResourceLocation id(String path){return ResourceLocation.withDefaultNamespace(path);}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
