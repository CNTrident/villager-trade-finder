package com.uncraftbar.easyautocycler.compat;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Read actual static trade templates without generating offers or touching a villager. */
public final class TradeItemsCompat {
    public static Set<Identifier> collect(RegistryAccess access, Identifier professionId) {
        Set<Identifier> result = new LinkedHashSet<>();
        Registry<Item> items = access.lookupOrThrow(Registries.ITEM);
        Object profession = access.lookupOrThrow(Registries.VILLAGER_PROFESSION).getValue(professionId);
        Object listings = VillagerTrades.TRADES.get(profession);
        if(listings == null) listings=VillagerTrades.TRADES.get(net.minecraft.resources.ResourceKey.create(Registries.VILLAGER_PROFESSION,professionId));
        collectValue(listings,items,result,Collections.newSetFromMap(new IdentityHashMap<>()),0);
        return result;
    }
    private static void collectValue(Object value, Registry<Item> items, Set<Identifier> result,Set<Object> seen,int depth) {
        if(value==null || depth>7 || !seen.add(value))return;
        if(value instanceof ItemStack stack){collectValue(stack.getItem(),items,result,seen,depth+1);return;}
        if(value instanceof Item item){if(item!=Items.EMERALD)result.add(items.getKey(item));return;}
        if(value instanceof Holder<?> holder){if(holder.isBound())collectValue(holder.value(),items,result,seen,depth+1);return;}
        if(value instanceof Map<?,?> map){map.values().forEach(v->collectValue(v,items,result,seen,depth+1));return;}
        if(value instanceof Iterable<?> list){list.forEach(v->collectValue(v,items,result,seen,depth+1));return;}
        if(value.getClass().isArray()){
            for(int i=0;i<Array.getLength(value);i++)collectValue(Array.get(value,i),items,result,seen,depth+1);return;
        }
        if(!value.getClass().getName().startsWith("net.minecraft."))return;
        // Match field values by their stable public types, never by obfuscated names.
        for(var field:value.getClass().getDeclaredFields()){
            if(Modifier.isStatic(field.getModifiers())||field.getType().isPrimitive())continue;
            try{field.setAccessible(true);collectValue(field.get(value),items,result,seen,depth+1);}
            catch(ReflectiveOperationException|RuntimeException ignored){}
        }
    }
}
