package com.antiair.handler;

import com.antiair.util.AircraftDetector;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Anti-air oklari icin:
 * 1) Havadayken hafif hedef takip (homing) — sadece ucaklar icin
 * 2) Ucaga carptiginda patlama efekti
 *
 * Flak38 oklari icin:
 * 3) Ucus sirasinda trail parcaciklari (CRIT + SMOKE)
 * 4) Yere veya herhangi bir seye carptiginda patlama
 */
public class ProjectileHandler {

    // Homing sistemi sabitleri
    private static final double HOMING_STRENGTH = 0.08;
    private static final double HOMING_RANGE = 60.0;
    private static final int TICK_INTERVAL = 2;

    // Ucak carpma patlama sabitleri
    private static final float EXPLOSION_DAMAGE = 15.0f;
    private static final int PARTICLE_COUNT = 30;

    // Flak38 roket sabitleri
    private static final double FLAK38_ROCKET_SPEED      = 4.0;   // blocks/tick
    private static final int    FLAK38_ROCKET_LIFETIME   = 80;    // kaç tick sonra patlasın (~4 sn)
    private static final float  FLAK38_EXPLOSION_POWER   = 2.5f;  // gerçek patlama gücü
    private static final double FLAK38_AIRCRAFT_RADIUS   = 10.0;  // IA aircraft hasar yarıçapı
    private static final float  FLAK38_AIRCRAFT_MAX_DMG  = 20.0f; // IA aircraft merkez hasarı

    private int tickCounter = 0;

    /**
     * Server tick'te:
     * - Anti-air oklarina homing uygula
     * - Flak38 oklarina trail parcaciklari spawn et
     */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        tickCounter++;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            processFlak38Projectiles(level);

            // Homing sadece Immersive Aircraft yukluyse calisir
            if (AircraftDetector.isImmersiveAircraftLoaded() && tickCounter % TICK_INTERVAL == 0) {
                processAntiAirArrows(level);
            }
        }
    }

    /**
     * Flak38 roketleri (FireworkRocketEntity) için her tick:
     *  1) Yaş takibi — FLAK38_ROCKET_LIFETIME tick sonunda manuel patlama tetiklenir.
     *     Manuel detonasyon: broadcastEntityEvent(17) → vanilla firework görseli,
     *     AABB sorgusuyla alan hasarı. Vanilla owner-check güvenilmez olduğundan
     *     hasarı kendimiz uyguluyoruz.
     *  2) Hız kontrolü — vanilla ×1.5 birikimini END fazında düzelterek sabit hız.
     *  3) Hafif yerçekimi — balistik yay efekti.
     *  4) Trail parçacıkları (CRIT + SMOKE).
     */
    private void processFlak38Projectiles(ServerLevel level) {
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof FireworkRocketEntity rocket)) continue;
            if (!rocket.isAlive()) continue;

            CompoundTag data = rocket.getPersistentData();
            if (!data.getBoolean("flak38_firework")) continue;

            // --- Yaş takibi & detonasyon ---
            int age = data.getInt("flak38_age");
            age++;
            data.putInt("flak38_age", age);

            if (age >= FLAK38_ROCKET_LIFETIME) {
                detonateFlak38Rocket(rocket, level);
                continue; // trajectory güncellemesine gerek yok
            }

            // --- Hız kontrolü — dümdüz, yerçekimsiz uçuş ---
            Vec3 vel = rocket.getDeltaMovement();
            if (vel.lengthSqr() < 1.0e-8) continue;

            // Vanilla her tick hızı ×1.5 çarpar; biz yönü normalize edip sabit hızda tutuyoruz.
            // Yerçekimi yok: mermi atıldığı yönde dümdüz gider.
            Vec3 corrected = vel.normalize().scale(FLAK38_ROCKET_SPEED);
            rocket.setDeltaMovement(corrected);

            // --- Uçak hitbox çarpışma kontrolü — tek vuruşta imha ---
            if (AircraftDetector.isImmersiveAircraftLoaded()) {
                Vec3 rocketPos = rocket.position();
                AABB rocketBox = rocket.getBoundingBox().inflate(0.5);
                for (Entity target : level.getEntities(rocket, rocketBox,
                        e -> AircraftDetector.isAircraft(e) && e.isAlive())) {
                    destroyAircraftOnImpact(rocket, target, level);
                    break;
                }
                if (!rocket.isAlive()) continue;
            }

            // --- Trail parçacıkları ---
            Vec3 pos = rocket.position();
            level.sendParticles(ParticleTypes.CRIT,  pos.x, pos.y, pos.z, 2, 0.05, 0.05, 0.05, 0.01);
            level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    /**
     * Flak38 roketini havada patlatır — üç katmanlı efekt:
     *
     * 1) broadcastEntityEvent(17)  → vanilla firework görseli (sarı-beyaz Large Ball)
     * 2) level.explode() MOB       → gerçek patlama partikeli + sesi + LivingEntity hasarı
     * 3) Manuel IA aircraft arama  → explosion / generic / magic kaynaklarıyla hasar zinciri
     *    (IA VehicleEntity bazı damage source'larını ignore edebildiğinden çoklu deneme)
     */
    private void detonateFlak38Rocket(FireworkRocketEntity rocket, ServerLevel level) {
        Vec3 pos   = rocket.position();
        Entity owner = rocket.getOwner();

        // ── 1. Vanilla firework patlama görseli ──────────────────────────────────
        level.broadcastEntityEvent(rocket, (byte) 17);
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.FIREWORK_ROCKET_BLAST,       SoundSource.AMBIENT, 2.0f, 1.0f);
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.AMBIENT, 2.0f, 1.0f);
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.FIREWORK_ROCKET_TWINKLE,     SoundSource.AMBIENT, 1.5f, 1.0f);

        rocket.discard();

        // ── 2. Gerçek küçük patlama — LivingEntity + blok hasarı ─────────────────
        level.explode(
                owner,
                pos.x, pos.y, pos.z,
                FLAK38_EXPLOSION_POWER,
                false,
                Level.ExplosionInteraction.MOB
        );

        // ── 3. IA Aircraft doğrudan hasar zinciri ────────────────────────────────
        // Level.explode() IA VehicleEntity'yi zaman zaman atlayabilir.
        // Geniş AABB + çoklu damage source denemeleriyle hasar garantilenir.
        if (!AircraftDetector.isImmersiveAircraftLoaded()) return;

        AABB blastZone = new AABB(
                pos.x - FLAK38_AIRCRAFT_RADIUS, pos.y - FLAK38_AIRCRAFT_RADIUS, pos.z - FLAK38_AIRCRAFT_RADIUS,
                pos.x + FLAK38_AIRCRAFT_RADIUS, pos.y + FLAK38_AIRCRAFT_RADIUS, pos.z + FLAK38_AIRCRAFT_RADIUS
        );

        for (Entity target : level.getEntities((Entity) null, blastZone,
                e -> AircraftDetector.isAircraft(e) && e.isAlive())) {

            double dist = pos.distanceTo(target.position());
            if (dist >= FLAK38_AIRCRAFT_RADIUS) continue;

            float factor = (float)(1.0 - dist / FLAK38_AIRCRAFT_RADIUS);
            float damage = FLAK38_AIRCRAFT_MAX_DMG * factor;

            // Sırayla birden fazla damage source dene — biri mutlaka geçer
            boolean hit = target.hurt(level.damageSources().explosion(null, owner), damage);
            if (!hit)   hit = target.hurt(level.damageSources().generic(),            damage);
            if (!hit)         target.hurt(level.damageSources().magic(),              damage);
        }
    }

    /**
     * Flak38 mermisi uçağa isabet ettiğinde: uçağı tek vuruşta imha eder.
     * Büyük patlama efekti + uçağı öldürür + mermiyi siler.
     */
    private void destroyAircraftOnImpact(FireworkRocketEntity rocket, Entity aircraft, ServerLevel level) {
        Vec3 impactPos = rocket.position();
        Entity owner = rocket.getOwner();

        // Patlama görseli
        level.broadcastEntityEvent(rocket, (byte) 17);

        // Sesler
        level.playSound(null, impactPos.x, impactPos.y, impactPos.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 3.0f, 0.8f);
        level.playSound(null, impactPos.x, impactPos.y, impactPos.z,
                SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.AMBIENT, 2.0f, 1.0f);

        // Parçacıklar — büyük patlama efekti
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, impactPos.x, impactPos.y, impactPos.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.FLAME,     impactPos.x, impactPos.y, impactPos.z, 40, 2.0, 2.0, 2.0, 0.05);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, impactPos.x, impactPos.y, impactPos.z, 30, 2.0, 2.0, 2.0, 0.02);
        level.sendParticles(ParticleTypes.LAVA,      impactPos.x, impactPos.y, impactPos.z, 15, 1.5, 1.5, 1.5, 0.0);

        // Gerçek patlama — blok ve entity hasarı
        level.explode(owner, impactPos.x, impactPos.y, impactPos.z,
                3.0f, false, Level.ExplosionInteraction.MOB);

        // Uçağı doğrudan öldür — çok yüksek hasar, birden fazla kaynak dene
        float killDamage = 9999.0f;
        boolean hit = aircraft.hurt(level.damageSources().explosion(rocket, owner), killDamage);
        if (!hit) hit = aircraft.hurt(level.damageSources().generic(), killDamage);
        if (!hit) hit = aircraft.hurt(level.damageSources().magic(), killDamage);
        if (!hit) {
            // Hiçbir damage source çalışmazsa zorla öldür
            aircraft.kill();
        }

        // Mermiyi sil
        rocket.discard();
    }

    private void processAntiAirArrows(ServerLevel level) {
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof AbstractArrow arrow)) continue;
            if (!arrow.isAlive()) continue;
            if (arrow.onGround()) continue;

            CompoundTag data = arrow.getPersistentData();
            if (!data.getBoolean(CrossbowHandler.ANTI_AIR_TAG)) continue;

            applyHoming(arrow, level, data);
        }
    }

    private void applyHoming(AbstractArrow arrow, Level level, CompoundTag data) {
        Entity target = null;
        if (data.contains("antiair_target")) {
            target = level.getEntity(data.getInt("antiair_target"));
            if (target == null || !target.isAlive() ||
                    target.distanceToSqr(arrow) > HOMING_RANGE * HOMING_RANGE) {
                target = null;
                data.remove("antiair_target");
            }
        }

        if (target == null) {
            target = AircraftDetector.findNearestAircraft(level, arrow.position(), HOMING_RANGE);
            if (target != null) {
                data.putInt("antiair_target", target.getId());
            }
        }

        if (target == null) return;

        Vec3 targetCenter = target.position().add(0, target.getBbHeight() / 2, 0);
        Vec3 toTarget = targetCenter.subtract(arrow.position()).normalize();

        Vec3 currentVelocity = arrow.getDeltaMovement();
        double speed = currentVelocity.length();

        Vec3 adjusted = currentVelocity.normalize()
                .add(toTarget.scale(HOMING_STRENGTH))
                .normalize()
                .scale(speed);

        arrow.setDeltaMovement(adjusted);
    }

    /**
     * Mermi çarpma olayı:
     *  A) Anti-air crossbow oku → uçağa çarparsa büyük patlama + hasar
     *  B) Flak38 FireworkRocket → her türlü çarpışmada vanilla patlama efekti + hasar
     */
    @SubscribeEvent
    public void onProjectileImpact(ProjectileImpactEvent event) {
        Entity projectile = event.getEntity();
        if (projectile.level().isClientSide()) return;

        // ── A) Anti-air crossbow oku ────────────────────────────────────────────
        if (projectile instanceof AbstractArrow arrow) {
            CompoundTag data = arrow.getPersistentData();
            if (!data.getBoolean(CrossbowHandler.ANTI_AIR_TAG)) return;

            Entity hitEntity = event.getRayTraceResult() instanceof EntityHitResult entityHit
                    ? entityHit.getEntity() : null;

            if (hitEntity != null && AircraftDetector.isAircraft(hitEntity)) {
                ServerLevel serverLevel = (ServerLevel) arrow.level();
                Vec3 impactPos = arrow.position();

                serverLevel.sendParticles(ParticleTypes.EXPLOSION, impactPos.x, impactPos.y, impactPos.z, 3, 0.5, 0.5, 0.5, 0.0);
                serverLevel.sendParticles(ParticleTypes.FLAME,     impactPos.x, impactPos.y, impactPos.z, PARTICLE_COUNT, 1.0, 1.0, 1.0, 0.05);
                serverLevel.sendParticles(ParticleTypes.SMOKE,     impactPos.x, impactPos.y, impactPos.z, 20, 0.8, 0.8, 0.8, 0.02);

                serverLevel.playSound(null, impactPos.x, impactPos.y, impactPos.z,
                        SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS,
                        1.5f, 1.0f + serverLevel.random.nextFloat() * 0.3f);

                hitEntity.hurt(serverLevel.damageSources().explosion(arrow, arrow.getOwner()), EXPLOSION_DAMAGE);
                arrow.discard();
            }
            return;
        }

        // ── B) Flak38 FireworkRocket → uçağa çarparsa tek vuruşta imha ────────
        if (projectile instanceof FireworkRocketEntity rocket) {
            CompoundTag data = rocket.getPersistentData();
            if (!data.getBoolean("flak38_firework")) return;

            Entity hitEntity = event.getRayTraceResult() instanceof EntityHitResult entityHit
                    ? entityHit.getEntity() : null;

            if (hitEntity != null && AircraftDetector.isImmersiveAircraftLoaded()
                    && AircraftDetector.isAircraft(hitEntity) && hitEntity.isAlive()) {
                ServerLevel serverLevel = (ServerLevel) rocket.level();
                destroyAircraftOnImpact(rocket, hitEntity, serverLevel);
                event.setCanceled(true);
            }
        }
    }
}
