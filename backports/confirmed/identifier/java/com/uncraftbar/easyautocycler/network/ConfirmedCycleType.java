package com.uncraftbar.easyautocycler.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

final class ConfirmedCycleType {
    private ConfirmedCycleType() {}

    static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> create(String path) {
        return new CustomPacketPayload.Type<>(Identifier.tryParse("easyautocycler:" + path));
    }
}
