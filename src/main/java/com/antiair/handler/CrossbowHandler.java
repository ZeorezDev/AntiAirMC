package com.antiair.handler;

import com.antiair.util.AircraftDetector;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Crossbow'dan atilan oklari tespit eder.
 * Yakinlarda ucak varsa oku anti-air muhimmat olarak isaretler.
 */
public class CrossbowHandler {

    // Anti-air okunu tanimlamak icin kullanilan NBT anahtari
    public static final String ANTI_AIR_TAG = "antiair_projectile";

    // Yakinlarda ucak aranacak yaricap (blok)
    private static final double DETECTION_RADIUS = 80.0;

    // Anti-air okunun hiz carpani
    private static final double SPEED_MULTIPLIER = 1.8;

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        // Sadece server tarafinda calis
        if (event.getLevel().isClientSide()) return;

        // Sadece ok entity'leri ile ilgilen
        if (!(event.getEntity() instanceof AbstractArrow arrow)) return;

        // Immersive Aircraft yuklu degilse hicbir sey yapma
        if (!AircraftDetector.isImmersiveAircraftLoaded()) return;

        // Oku atan entity'yi kontrol et — sadece oyuncular
        Entity owner = arrow.getOwner();
        if (!(owner instanceof Player player)) return;

        // Oyuncunun ana elindeki veya ikinci elindeki esya crossbow mu?
        boolean firedFromCrossbow =
                player.getMainHandItem().getItem() instanceof CrossbowItem ||
                player.getOffhandItem().getItem() instanceof CrossbowItem;

        if (!firedFromCrossbow) return;

        // Yakinlarda ucak var mi kontrol et
        Entity nearestAircraft = AircraftDetector.findNearestAircraft(
                event.getLevel(), arrow.position(), DETECTION_RADIUS
        );

        if (nearestAircraft == null) return;

        // Oku anti-air muhimmat olarak isaretler
        CompoundTag persistentData = arrow.getPersistentData();
        persistentData.putBoolean(ANTI_AIR_TAG, true);
        persistentData.putInt("antiair_target", nearestAircraft.getId());

        // Ok hizini artir — ucaga dogru yonlendir
        Vec3 toTarget = nearestAircraft.position().add(0, nearestAircraft.getBbHeight() / 2, 0)
                .subtract(arrow.position()).normalize();
        Vec3 currentVelocity = arrow.getDeltaMovement();
        double speed = currentVelocity.length() * SPEED_MULTIPLIER;

        // Mevcut yon ile hedef yonunun karisimi (%70 mevcut, %30 hedefe dogru)
        Vec3 blended = currentVelocity.normalize().scale(0.7).add(toTarget.scale(0.3)).normalize().scale(speed);
        arrow.setDeltaMovement(blended);

        // Oku guclendir — daha fazla hasar
        arrow.setBaseDamage(arrow.getBaseDamage() * 2.5);
    }
}
