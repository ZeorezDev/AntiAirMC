package com.antiair.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Immersive Aircraft entity tespit sistemi.
 * Mod yuklu degilse hicbir islem yapmaz — soft dependency.
 *
 * Tespit yontemi: reflection yerine entity type registry namespace kullanir.
 * "immersive_aircraft" veya "warfare_wings" namespace'ine sahip her entity ucak sayilir.
 * Bu yontem sinif adi degisikliklerine karsi saglamdir.
 */
public final class AircraftDetector {

    private static final String IMMERSIVE_AIRCRAFT_MOD_ID = "immersive_aircraft";
    private static final String WARFARE_WINGS_MOD_ID      = "warfare_wings";

    // Cache
    private static Boolean isModLoaded = null;

    private AircraftDetector() {}

    /**
     * Immersive Aircraft veya Warfare Wings modunun yuklu olup olmadigini kontrol eder.
     */
    public static boolean isImmersiveAircraftLoaded() {
        if (isModLoaded == null) {
            isModLoaded = ModList.get().isLoaded(IMMERSIVE_AIRCRAFT_MOD_ID)
                       || ModList.get().isLoaded(WARFARE_WINGS_MOD_ID);
        }
        return isModLoaded;
    }

    /**
     * Entity'nin namespace'ine bakarak IA / Warfare Wings ucagi olup olmadigini belirler.
     * Class.forName() yerine ForgeRegistries kullanir — sinif adi bagimsiz.
     */
    public static boolean isAircraft(Entity entity) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (key == null) return false;
        String ns = key.getNamespace();
        return IMMERSIVE_AIRCRAFT_MOD_ID.equals(ns) || WARFARE_WINGS_MOD_ID.equals(ns);
    }

    /**
     * Belirtilen pozisyon etrafinda, verilen yaricap icinde en yakin ucagi bulur.
     *
     * @param level  Minecraft world
     * @param pos    Arama merkez noktasi
     * @param radius Arama yaricapi (blok)
     * @return En yakin ucak entity veya null
     */
    @Nullable
    public static Entity findNearestAircraft(Level level, Vec3 pos, double radius) {
        if (!isImmersiveAircraftLoaded()) return null;

        AABB searchBox = new AABB(
                pos.x - radius, pos.y - radius, pos.z - radius,
                pos.x + radius, pos.y + radius, pos.z + radius
        );

        List<Entity> entities = level.getEntities((Entity) null, searchBox,
                e -> isAircraft(e) && e.isAlive());

        Entity nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Entity entity : entities) {
            double dist = entity.position().distanceToSqr(pos);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = entity;
            }
        }

        return nearest;
    }
}
