package com.uncraftbar.easyautocycler.compat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Map;
import java.util.HashMap;
public final class StackCompat {
    public static Map<ResourceLocation,Integer> enchantments(ItemStack stack) {
        Map<ResourceLocation,Integer> result = new HashMap<>();
        var enchantments = stack.getOrDefault(stack.is(Items.ENCHANTED_BOOK)
            ? net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS
            : net.minecraft.core.component.DataComponents.ENCHANTMENTS,
            net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        for (var entry : enchantments.entrySet()) {
            ResourceLocation id = entry.getKey().unwrapKey().map(k -> k.location()).orElse(null);
            if (id != null) result.put(id, entry.getIntValue());
        }
        return result;
    }
    public static ResourceLocation potionId(ItemStack stack) {
        var contents = stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        return contents == null ? null : contents.potion().flatMap(p -> p.unwrapKey()).map(k -> k.location()).orElse(null);
    }
}