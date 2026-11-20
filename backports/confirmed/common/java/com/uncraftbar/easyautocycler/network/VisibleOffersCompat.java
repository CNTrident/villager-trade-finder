package com.uncraftbar.easyautocycler.network;

import java.lang.reflect.Method;
import java.util.Arrays;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Completes the combined offer packet for versions whose Trade Cycling sends initial offers only. */
final class VisibleOffersCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger("Villager Trade Finder");
    private static final ClassValue<Generation> GENERATION = new ClassValue<>() {
        @Override
        protected Generation computeValue(Class<?> villagerClass) {
            try {
                return resolveGeneration(villagerClass);
            } catch (ReflectiveOperationException | LinkageError error) {
                LOGGER.warn("No compatible VisibleTraders offer generation API found", error);
                return null;
            }
        }
    };

    private VisibleOffersCompat() {}

    static ClientboundMerchantOffersPacket complete(Merchant villager, ClientboundMerchantOffersPacket original) {
        if (!FabricLoader.getInstance().isModLoaded("visibletraders")) return original;
        int unlocked = original.getVillagerLevel() >>> 8;
        MerchantOffers current = original.getOffers();
        if (unlocked > 0 && hasCompleteLaterOffers(current, unlocked)) return original;

        Generation generation = GENERATION.get(villager.getClass());
        if (generation == null) return original;
        try {
            int initialCount = unlocked > 0 ? unlocked : current.size();
            MerchantOffers later = generation.generate(villager, initialCount);
            if (initialCount <= 0 || initialCount > current.size() || !hasCompleteLaterOffers(later, 0)) {
                return original;
            }
            MerchantOffers combined = new MerchantOffers();
            combined.addAll(current.subList(0, initialCount));
            combined.addAll(later);
            int encodedLevel = (original.getVillagerLevel() & 0xff) | (initialCount << 8);
            return new ClientboundMerchantOffersPacket(original.getContainerId(), combined, encodedLevel,
                    original.getVillagerXp(), original.showProgress(), original.canRestock());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            LOGGER.error("Could not finish VisibleTraders offers for a confirmed refresh", error);
            return original;
        }
    }

    private static boolean hasCompleteLaterOffers(MerchantOffers offers, int initialCount) {
        return offers.size() > initialCount
                && offers.subList(initialCount, offers.size()).stream().noneMatch(offer -> offer.getResult().isEmpty());
    }

    private static Generation resolveGeneration(Class<?> villagerClass) throws ReflectiveOperationException {
        Method regenerate = publicMethod(villagerClass, "visibleTrades$regenerateTrades");
        if (regenerate != null) {
            Method combined = villagerClass.getMethod("visibleTraders$getCombinedOffers");
            return (villager, initialCount) -> {
                regenerate.invoke(villager);
                MerchantOffers all = (MerchantOffers) combined.invoke(villager);
                MerchantOffers later = new MerchantOffers();
                if (all.size() > initialCount) later.addAll(all.subList(initialCount, all.size()));
                return later;
            };
        }

        Method locked = villagerClass.getMethod("visibleTraders$getLockedOffers");
        Method force = publicMethod(villagerClass, "visibleTraders$forceTradeGeneration");
        if (force != null) {
            return (villager, initialCount) -> {
                force.invoke(villager);
                return (MerchantOffers) locked.invoke(villager);
            };
        }

        // VisibleTraders 0.0.4 exposes the locked list but lacks the force method. The same
        // five generation steps were exposed as forceTradeGeneration in the next API.
        Method update = Arrays.stream(villagerClass.getDeclaredMethods())
                .filter(method -> method.getName().endsWith("$updateLockedTradesOnTick")
                        && Arrays.equals(method.getParameterTypes(), new Class<?>[] { CallbackInfo.class }))
                .findFirst().orElseThrow(() -> new NoSuchMethodException("VisibleTraders locked offer tick"));
        update.setAccessible(true);
        return (villager, initialCount) -> {
            for (int step = 0; step < 5; step++) update.invoke(villager, new CallbackInfo("lockedOffers", false));
            return (MerchantOffers) locked.invoke(villager);
        };
    }

    private static Method publicMethod(Class<?> type, String name) {
        try {
            return type.getMethod(name);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    @FunctionalInterface
    private interface Generation {
        MerchantOffers generate(Merchant villager, int initialCount) throws ReflectiveOperationException;
    }
}
