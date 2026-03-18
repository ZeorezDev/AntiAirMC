package com.antiair.network;

import com.antiair.entity.Flak38Entity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class Flak38FirePacket {

    public Flak38FirePacket() {
    }

    public Flak38FirePacket(FriendlyByteBuf buf) {
        // Bos paket — sadece "ates et" sinyali
    }

    public void encode(FriendlyByteBuf buf) {
        // Bos paket
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        ServerPlayer player = context.getSender();
        if (player != null && player.getVehicle() instanceof Flak38Entity flak) {
            flak.fireArrow(player);
        }
        context.setPacketHandled(true);
    }
}
