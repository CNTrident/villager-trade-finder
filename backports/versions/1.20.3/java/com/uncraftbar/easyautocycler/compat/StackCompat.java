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
        net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(stack)
                .forEach((enchantment, level) -> result.put(BuiltInRegistries.ENCHANTMENT.getKey(enchantment), level));
        return result;
    }
    public static ResourceLocation potionId(ItemStack stack) {
        return BuiltInRegistries.POTION.getKey(net.minecraft.world.item.alchemy.PotionUtils.getPotion(stack));
    }
}