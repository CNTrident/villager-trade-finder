package com.uncraftbar.easyautocycler.compat;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Map;
import java.util.HashMap;
public final class StackCompat {
    public static Map<Identifier,Integer> enchantments(ItemStack stack) {
        Map<Identifier,Integer> result = new HashMap<>();
        var enchantments = stack.getOrDefault(stack.is(Items.ENCHANTED_BOOK)
            ? net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS
            : net.minecraft.core.component.DataComponents.ENCHANTMENTS,
            net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        for (var entry : enchantments.entrySet()) {
            Identifier id = entry.getKey().unwrapKey().map(k -> k.identifier()).orElse(null);
            if (id != null) result.put(id, entry.getIntValue());
        }
        return result;
    }
    public static Identifier potionId(ItemStack stack) {
        var contents = stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        return contents == null ? null : contents.potion().flatMap(p -> p.unwrapKey()).map(k -> k.identifier()).orElse(null);
    }
}