package com.antiair.client;

import com.antiair.AntiAirMod;
import com.antiair.entity.Flak38Entity;
import com.antiair.network.ModNetwork;
import com.antiair.network.Flak38FirePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AntiAirMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class Flak38InputHandler {

    private static boolean wasRiding = false;
    private static int mountGraceTicks = 0;

    // Binme animasyonu sonrasi bekleme suresi (tick)
    // Sag tikla bindigimiz icin, parmak hala basili — bu sure boyunca ates etme
    private static final int MOUNT_GRACE_PERIOD = 5;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        boolean isRiding = player.getVehicle() instanceof Flak38Entity;

        // Yeni bindiyse grace period baslat
        if (isRiding && !wasRiding) {
            mountGraceTicks = MOUNT_GRACE_PERIOD;
        }
        wasRiding = isRiding;

        if (!isRiding) {
            mountGraceTicks = 0;
            return;
        }

        // Grace period'u azalt
        if (mountGraceTicks > 0) {
            mountGraceTicks--;
            return; // Grace period bitene kadar ates etme
        }

        // Sag tik basili tutuldugunda surekli ates et (hold-to-fire)
        // Server tarafindaki fireCooldown (10 tick) spam'i onler
        if (mc.options.keyUse.isDown()) {
            ModNetwork.CHANNEL.sendToServer(new Flak38FirePacket());
        }
    }
}
