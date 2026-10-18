package com.uncraftbar.easyautocycler.gui;

import com.google.gson.JsonParser;
import com.uncraftbar.easyautocycler.EasyAutoCyclerMod;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.alchemy.Potion;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Names and variants of the potions in the server's fletcher tradeable tag. */
public final class PotionNameResolver {
    private final Map<String, ResourceLocation> aliases = new HashMap<>();
    private final Set<String> ambiguousAliases = new HashSet<>();
    private final Map<ResourceLocation, String> displayNames = new HashMap<>();
    private final Map<ResourceLocation, String> preferredNames = new HashMap<>();
    private final List<String> suggestions = new ArrayList<>();

    public PotionNameResolver(RegistryAccess access, ResourceManager resources) {
        Registry<Potion> potions = access.registryOrThrow(Registries.POTION);
        Set<ResourceLocation> allowed = new LinkedHashSet<>();
            resources.getResource(new ResourceLocation("easyautocycler", "tradeable_potions.json"))
                    .ifPresent(resource -> {
                        try (var reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                            JsonParser.parseReader(reader).getAsJsonArray().forEach(value -> {
                                ResourceLocation id = ResourceLocation.tryParse(value.getAsString());
                                if (id != null && potions.containsKey(id)) allowed.add(id);
                            });
                        } catch (Exception exception) {
                            EasyAutoCyclerMod.LOGGER.warn("Could not load fletcher potion candidates", exception);
                        }
                    });
        ClientLanguage english = ClientLanguage.loadFrom(resources, List.of("en_us"), false);
        ClientLanguage chinese = ClientLanguage.loadFrom(resources, List.of("zh_cn"), false);
        for (ResourceLocation id : allowed) {
            Potion potion = potions.get(id);
            if (potion == null) continue;
            String arrowKey = "item.minecraft.tipped_arrow.effect." + id.getPath().replaceFirst("^(long_|strong_)", "");
            String englishName = english.getOrDefault(arrowKey, id.getPath());
            String chineseName = chinese.getOrDefault(arrowKey, id.getPath());
            boolean extended = id.getPath().startsWith("long_");
            int strength = potion.getEffects().stream().mapToInt(effect -> effect.getAmplifier() + 1)
                    .max().orElse(1);
            // Long/strong potions share the base arrow translation. Explicit
            // suffixes keep them distinct in both suggestions and saved targets.
            String englishSuffix = extended ? " (Extended)" : strength > 1 ? " " + strength : "";
            String chineseSuffix = extended ? "（延长）" : strength > 1 ? " " + strength : "";
            String en = englishName + englishSuffix;
            String zh = chineseName + chineseSuffix;
            String display = zh.equals(en) ? en : zh + " / " + en;
            displayNames.put(id, display);
            preferredNames.put(id, display);
            alias(id.toString(), id);
            alias(id.getPath(), id);
            alias(en, id);
            alias(zh, id);
            alias(chineseName.replace("之箭", "") + chineseSuffix, id);
            alias(display, id);
            for (var effect : potion.getEffects()) {
                String key = effect.getEffect().value().getDescriptionId();
                String enEffect = english.getOrDefault(key, "");
                String zhEffect = chinese.getOrDefault(key, "");
                if (!enEffect.isEmpty()) alias(enEffect + englishSuffix, id);
                if (!zhEffect.isEmpty()) alias(zhEffect + chineseSuffix, id);
                if (strength > 1 && !extended) {
                    if (!enEffect.isEmpty()) alias(enEffect + " " + roman(strength), id);
                    if (!zhEffect.isEmpty()) alias("强化" + zhEffect, id);
                }
            }
            if (extended) {
                alias("延长" + chineseName, id);
                alias(englishName + " long", id);
            } else if (strength > 1) {
                alias("强化" + chineseName, id);
                alias(englishName + " strong", id);
                alias(englishName + " " + roman(strength), id);
                alias(chineseName + " " + roman(strength), id);
            }
            String suggestion = display + " (" + id + ")";
            alias(suggestion, id);
            suggestions.add(suggestion);
        }
        suggestions.sort(Comparator.comparing(value -> value.toLowerCase(Locale.ROOT)));
    }

    public ResourceLocation resolve(String input) {
        return aliases.get(normalize(input == null ? "" : input));
    }

    public String preferredInput(ResourceLocation id) {
        return preferredNames.getOrDefault(id, id.toString());
    }

    public String displayName(ResourceLocation id) {
        return displayNames.getOrDefault(id, id.toString());
    }

    public List<String> suggestions() {
        return List.copyOf(suggestions);
    }

    private void alias(String name, ResourceLocation id) {
        String normalized = normalize(name);
        if (normalized.isEmpty() || ambiguousAliases.contains(normalized)) return;
        ResourceLocation previous = aliases.putIfAbsent(normalized, id);
        if (previous != null && !previous.equals(id)) {
            aliases.remove(normalized);
            ambiguousAliases.add(normalized);
        }
    }

    private static String roman(int level) {
        return switch (level) {
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 6 -> "VI";
            default -> String.valueOf(level);
        };
    }

    private static String normalize(String name) {
        StringBuilder result = new StringBuilder();
        Normalizer.normalize(name, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .codePoints().filter(Character::isLetterOrDigit).forEach(result::appendCodePoint);
        return result.toString();
    }
}
