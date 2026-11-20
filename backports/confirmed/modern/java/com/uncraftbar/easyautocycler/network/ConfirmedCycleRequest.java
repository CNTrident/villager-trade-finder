package com.uncraftbar.easyautocycler.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ConfirmedCycleRequest(int containerId, long round, boolean needLaterOffers) implements CustomPacketPayload {
    public static final Type<ConfirmedCycleRequest> TYPE =
            ConfirmedCycleType.create("confirmed_cycle_request");
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfirmedCycleRequest> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ConfirmedCycleRequest::containerId,
            ByteBufCodecs.VAR_LONG, ConfirmedCycleRequest::round,
            ByteBufCodecs.BOOL, ConfirmedCycleRequest::needLaterOffers,
            ConfirmedCycleRequest::new);

    @Override
    public Type<ConfirmedCycleRequest> type() {
        return TYPE;
    }
}
