package com.uncraftbar.easyautocycler.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.uncraftbar.easyautocycler.EasyAutoCyclerMod;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.Registries;
import org.jspecify.annotations.Nullable;

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

/** Resolves items on either side of one villager profession's trades. */
public final class ItemNameResolver {
    private static final ResourceLocation VANILLA_FALLBACK = ResourceLocation.fromNamespaceAndPath(
            "easyautocycler", "villager_trade_items.json");
    private final Registry<Item> itemRegistry;
    private final Set<ResourceLocation> allowedItems;
    private final Map<String, ResourceLocation> aliases = new HashMap<>();
    private final Set<String> ambiguousAliases = new HashSet<>();
    private final Map<ResourceLocation, String> preferredNames = new HashMap<>();
    private final Map<ResourceLocation, String> displayNames = new HashMap<>();
    private final List<String> suggestions;

    public ItemNameResolver(RegistryAccess registryAccess, ResourceLocation professionId,
                            ResourceManager resourceManager) {
        this.itemRegistry = registryAccess.registryOrThrow(Registries.ITEM);
        this.allowedItems = collectTradeItems(registryAccess, professionId, resourceManager);
        Set<String> suggestionSet = new LinkedHashSet<>();
        ClientLanguage english = ClientLanguage.loadFrom(resourceManager, List.of("en_us"), false);
        ClientLanguage chinese = ClientLanguage.loadFrom(resourceManager, List.of("zh_cn"), false);

        for (ResourceLocation id : allowedItems) {
            Item item = itemRegistry.get(id);
            if (item == null) continue;
            String key = item.getDescriptionId();
            String fallback = humanizePath(id.getPath());
            String englishName = english.getOrDefault(key, fallback).trim();
            String chineseName = chinese.getOrDefault(key, "").trim();
            String localizedName = Component.translatable(key).getString().trim();

            registerAlias(id.toString(), id);
            registerAlias(id.getPath(), id);
            registerAlias(englishName, id);
            registerAlias(chineseName, id);
            registerAlias(localizedName, id);
            if (id.equals(ResourceLocation.withDefaultNamespace("tipped_arrow"))) {
                registerAlias("药水箭", id);
            }

            preferredNames.put(id, localizedName.isEmpty() ? englishName : localizedName);
            displayNames.put(id, bilingualName(chineseName, englishName, fallback));
            addNamedSuggestion(suggestionSet, localizedName, id);
            addNamedSuggestion(suggestionSet, chineseName, id);
            addNamedSuggestion(suggestionSet, englishName, id);
            suggestionSet.add(id.toString());
        }

        this.suggestions = suggestionSet.stream()
                .sorted(Comparator.comparing(value -> value.toLowerCase(Locale.ROOT)))
                .toList();
    }

    private static Set<ResourceLocation> collectTradeItems(RegistryAccess access, ResourceLocation professionId,
                                                     ResourceManager resources) {
        Set<ResourceLocation> result = com.uncraftbar.easyautocycler.compat.TradeItemsCompat.collect(access, professionId);
        addVanillaFallback(result, access.registryOrThrow(Registries.ITEM), professionId, resources);
        return result;
    }



    private static void addItem(Set<ResourceLocation> result, Registry<Item> items, Item item) {
        if (item == Items.EMERALD) return;
        ResourceLocation id = items.getKey(item);
        if (id != null) result.add(id);
    }

    private static void addVanillaFallback(Set<ResourceLocation> result, Registry<Item> items,
                                           ResourceLocation professionId, ResourceManager resourceManager) {
        if (!"minecraft".equals(professionId.getNamespace())) return;
        resourceManager.getResource(VANILLA_FALLBACK).ifPresent(resource -> {
            try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray ids = root.getAsJsonArray(professionId.getPath());
                if (ids == null) return;
                for (JsonElement element : ids) {
                    ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
                    if (id != null && items.containsKey(id) && !id.equals(ResourceLocation.withDefaultNamespace("emerald"))) {
                        result.add(id);
                    }
                }
            } catch (Exception exception) {
                EasyAutoCyclerMod.LOGGER.warn("Could not load bundled trade candidates for {}: {}",
                        professionId, exception.getMessage());
            }
        });
    }

    @Nullable
    public ResourceLocation resolve(String input) {
        String trimmed = input == null ? "" : input.trim();
        if (trimmed.isEmpty()) return null;
        try {
            ResourceLocation id = ResourceLocation.parse(trimmed);
            if (allowedItems.contains(id)) return id;
        } catch (RuntimeException ignored) {
        }
        String normalized = normalize(trimmed);
        return ambiguousAliases.contains(normalized) ? null : aliases.get(normalized);
    }

    public String preferredInput(ResourceLocation id) {
        return preferredNames.getOrDefault(id, humanizePath(id.getPath()));
    }

    public String displayName(ResourceLocation id) {
        return displayNames.getOrDefault(id, preferredInput(id));
    }

    public List<String> suggestions() {
        return new ArrayList<>(suggestions);
    }

    public ItemStack defaultStack(ResourceLocation id) {
        Item item = itemRegistry.get(id);
        return item == null ? ItemStack.EMPTY : item.getDefaultInstance();
    }

    private void registerAlias(String alias, ResourceLocation id) {
        String normalized = normalize(alias);
        if (normalized.isEmpty() || ambiguousAliases.contains(normalized)) return;
        ResourceLocation previous = aliases.putIfAbsent(normalized, id);
        if (previous != null && !previous.equals(id)) {
            aliases.remove(normalized);
            ambiguousAliases.add(normalized);
        }
    }

    private void addNamedSuggestion(Set<String> target, String name, ResourceLocation id) {
        if (name.isEmpty()) return;
        String suggestion = name + " (" + id + ")";
        target.add(suggestion);
        registerAlias(suggestion, id);
    }

    private static String bilingualName(String chinese, String english, String fallback) {
        if (!chinese.isEmpty() && !english.isEmpty() && !chinese.equalsIgnoreCase(english)) {
            return chinese + " / " + english;
        }
        if (!chinese.isEmpty()) return chinese;
        if (!english.isEmpty()) return english;
        return fallback;
    }

    private static String humanizePath(String path) {
        String[] words = path.split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) result.append(word.substring(1));
        }
        return result.toString();
    }

    private static String normalize(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder();
        normalized.codePoints().filter(Character::isLetterOrDigit).forEach(result::appendCodePoint);
        return result.toString();
    }
}
