package com.uncraftbar.easyautocycler.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;

public record ConfirmedCycleOffers(long round, ClientboundMerchantOffersPacket offers) implements CustomPacketPayload {
    public static final Type<ConfirmedCycleOffers> TYPE =
            ConfirmedCycleType.create("confirmed_cycle_offers");
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfirmedCycleOffers> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, ConfirmedCycleOffers::round,
            ClientboundMerchantOffersPacket.STREAM_CODEC, ConfirmedCycleOffers::offers,
            ConfirmedCycleOffers::new);

    @Override
    public Type<ConfirmedCycleOffers> type() {
        return TYPE;
    }
}
