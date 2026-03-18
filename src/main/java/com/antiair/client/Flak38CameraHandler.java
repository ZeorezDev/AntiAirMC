package com.antiair.client;

import com.antiair.AntiAirMod;
import com.antiair.entity.Flak38Entity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;

/**
 * Flak38'e binildiginde kamerayi namlu yonunde biraz ileriye kaydirir.
 * Oyuncu 1. sahis kamerasinda namlu ucuna dogru bakar.
 * Camera.position field'ina reflection ile erisir.
 */
@Mod.EventBusSubscriber(modid = AntiAirMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class Flak38CameraHandler {

    // Kamera namlu yonunde ne kadar ileri kaysin (blok)
    private static final double FORWARD_OFFSET = 2.0;
    // Kamera ne kadar yukari kaysin (blok)
    private static final double UP_OFFSET = 0.3;

    private static Field cameraPositionField = null;
    private static boolean fieldInitialized = false;

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        // Kamera sistemini tamamen devre dışı bıraktık
        return;
    }

    /**
     * Camera.position field'ini reflection ile set eder.
     * Forge'da Camera.setPosition protected oldugu icin bu yontemi kullaniyoruz.
     */
    private static void setCameraPosition(Camera camera, Vec3 pos) {
        try {
            if (!fieldInitialized) {
                fieldInitialized = true;
                // "position" field'ini bul (obfuscated ismi farkli olabilir)
                for (Field f : Camera.class.getDeclaredFields()) {
                    if (f.getType() == Vec3.class) {
                        f.setAccessible(true);
                        cameraPositionField = f;
                        break;
                    }
                }
            }

            if (cameraPositionField != null) {
                cameraPositionField.set(camera, pos);
            }
        } catch (Exception e) {
            // Sessizce hatala — oyun cokmesin
        }
    }
}
