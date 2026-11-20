package com.uncraftbar.easyautocycler.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

final class ConfirmedCycleType {
    private ConfirmedCycleType() {}

    static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> create(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.tryParse("easyautocycler:" + path));
    }
}
