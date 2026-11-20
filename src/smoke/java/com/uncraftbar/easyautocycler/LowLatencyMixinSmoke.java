package com.uncraftbar.easyautocycler;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import java.nio.file.Files;
import java.nio.file.Path;

/** Loads the packaged client mixins and exercises real packet handlers before any window/world. */
public final class LowLatencyMixinSmoke implements PreLaunchEntrypoint {
    @Override public void onPreLaunch() {
        try {
            SharedConstants.tryDetectVersion();
            Bootstrap.bootStrap();
            boolean client = FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
            if (client) {
                Class.forName("net.minecraft.client.multiplayer.ClientPacketListener");
                Class.forName("net.minecraft.world.inventory.MerchantMenu");
                AutomationManager.initialize();
                NamedVillagerSmoke.run();
                if (FabricLoader.getInstance().isModLoaded("visibletraders")) {
                    Class.forName("net.ramixin.visibletraders.client.VisibleTradersCommonClient");
                    ClientTradeReadinessSmoke.run();
                }
            } else {
                if (!FabricLoader.getInstance().isModLoaded("librarian_trade_cycler")) {
                    throw new AssertionError("Confirmed refresh server support should load on a dedicated server");
                }
                Class.forName("net.minecraft.server.level.ServerPlayer");
            }
            String result = "PASS: " + (client ? "client mixins and complete-response gating" : "server offer hook loaded")
                    + ", VisibleTraders=" + FabricLoader.getInstance().isModLoaded("visibletraders") + "\n";
            Files.writeString(Path.of("smoke-result.txt"), result);
            System.out.println(result);
            System.exit(0);
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
    }
}
