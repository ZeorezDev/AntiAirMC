package com.antiair.network;

import com.antiair.AntiAirMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AntiAirMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        CHANNEL.messageBuilder(Flak38FirePacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(Flak38FirePacket::encode)
                .decoder(Flak38FirePacket::new)
                .consumerMainThread(Flak38FirePacket::handle)
                .add();
    }
}
