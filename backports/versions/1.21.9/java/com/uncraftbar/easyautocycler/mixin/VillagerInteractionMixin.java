package com.uncraftbar.easyautocycler.mixin;

import com.uncraftbar.easyautocycler.VillagerTradeContext;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class VillagerInteractionMixin {
    @Inject(method = "interact", at = @At("HEAD"))
    private void easyautocycler$recordVillager(Player player, Entity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> ci) {
        VillagerTradeContext.recordInteraction(player, entity);
    }

    @Inject(method = "interactAt", at = @At("HEAD"))
    private void easyautocycler$recordVillagerAt(Player player, Entity entity, EntityHitResult hit,
                                               InteractionHand hand, CallbackInfoReturnable<InteractionResult> ci) {
        VillagerTradeContext.recordInteraction(player, entity);
    }
}
