package com.antiair.entity;

import com.antiair.registry.ModEntities;
import com.antiair.registry.ModItems;
import com.antiair.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Flak 38 ucaksavar entity'si.
 * Binilebilir, ates edilebilir, sabit turret.
 * Yere yercekimi ile oturur, oyuncu donerken model de doner.
 */
public class Flak38Entity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_AMMO_COUNT =
            SynchedEntityData.defineId(Flak38Entity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Float> DATA_HEALTH =
            SynchedEntityData.defineId(Flak38Entity.class, EntityDataSerializers.FLOAT);

    public static final float MAX_HEALTH = 40.0f;
    private static final int FIRE_COOLDOWN_TICKS = 10;
    /** Roket hızı — ProjectileHandler her tick bu değeri korur (blocks/tick) */
    private static final double ROCKET_SPEED = 4.0;
    private static final int MAX_AMMO = 20;

    // Patlama sabitleri
    private static final float GROUND_EXPLOSION_POWER = 2.5f;   // Ok yere dusunce patlama gucu
    private static final float DEATH_EXPLOSION_POWER = 3.5f;    // Entity olunce patlama gucu
    private static final int DEATH_FLAME_COUNT = 12;             // Olumde sacilan alev sayisi

    private int fireCooldown = 0;
    private boolean onGround = false;

    public Flak38Entity(EntityType<?> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    public Flak38Entity(Level level, double x, double y, double z) {
        this(ModEntities.FLAK38.get(), level);
        this.setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_HEALTH, MAX_HEALTH);
        this.entityData.define(DATA_AMMO_COUNT, 0);
    }

    public boolean isLoaded() {
        return this.getAmmoCount() > 0;
    }

    public int getAmmoCount() {
        return this.entityData.get(DATA_AMMO_COUNT);
    }

    public void setAmmoCount(int count) {
        this.entityData.set(DATA_AMMO_COUNT, Math.max(0, Math.min(count, MAX_AMMO)));
    }

    // --- Tick ---

    @Override
    public void tick() {
        super.tick();

        // Cooldown azalt
        if (fireCooldown > 0) fireCooldown--;

        // Binen oyuncunun bakis yonune gore entity'nin yaw'ini guncelle
        Entity passenger = this.getFirstPassenger();
        if (passenger instanceof LivingEntity living) {
            this.setYRot(living.getYRot());
            this.yRotO = this.getYRot();
        }

        // Yercekimi — yere oturana kadar
        if (!onGround) {
            Vec3 motion = this.getDeltaMovement();
            motion = motion.add(0, -0.04, 0); // Yercekimi
            this.setDeltaMovement(motion);
            this.move(MoverType.SELF, this.getDeltaMovement());

            // Yere degdi mi?
            if (this.verticalCollision) {
                onGround = true;
                this.setDeltaMovement(Vec3.ZERO);
            }
        } else {
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    // --- Etkilesim ---

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Mermi yükleme (20'lik şarjör)
        if (stack.getItem() == ModItems.AMMO_20MM.get()) {
            if (this.getAmmoCount() == 0) {
                this.setAmmoCount(MAX_AMMO);
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                if (this.level().isClientSide()) {
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("Magazine Loaded (" + MAX_AMMO + "/" + MAX_AMMO + ")"), true);
                }
                // Şarjör takma başlangıç sesi
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        ModSounds.AMMO_RELOAD_START.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
                // Şarjör kilit sesi (biraz üst üste başlar — gerçekçi yükleme hissi)
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        ModSounds.AMMO_RELOAD_END.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }

        if (this.level().isClientSide()) return InteractionResult.SUCCESS;
        
        // ... (diğer etkileşimler: sneak, binme)
        // Sneak + sag tik = geri al (yolcu yoksa)
        if (player.isShiftKeyDown()) {
            if (!this.isVehicle()) {
                // Item olarak dusur
                this.spawnAtLocation(new ItemStack(ModItems.FLAK38.get()));
                this.playSound(SoundEvents.ITEM_PICKUP, 1.0f, 1.0f);
                this.discard();
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }

        // Zaten binen var mi?
        if (this.isVehicle()) {
            return InteractionResult.PASS;
        }

        // Bin
        player.startRiding(this);
        return InteractionResult.CONSUME;
    }

    /**
     * Binen oyuncu sag tik yaptiginda ok atesler.
     * Ates edilen ok "flak38_arrow" olarak isaretlenir.
     */
    public void fireArrow(Player rider) {
        if (this.level().isClientSide()) return;

        if (!this.isLoaded()) {
            rider.displayClientMessage(net.minecraft.network.chat.Component.literal("Out of ammo — reload!"), true);
            return;
        }

        if (fireCooldown > 0) return;

        fireCooldown = FIRE_COOLDOWN_TICKS;

        // Namlu ucundan ateşle
        float yawRad = (float) Math.toRadians(this.getYRot());
        double barrelLength = 2.0;
        double spawnX = this.getX() - Math.sin(yawRad) * barrelLength;
        double spawnY = this.getY() + 1.2;
        double spawnZ = this.getZ() + Math.cos(yawRad) * barrelLength;

        // Firework ItemStack — Flight=10 → ~100 tick (~5 sn) uçuş süresi
        // Explosion şarjleri: vanilla görsel patlama efekti + alan hasarı için gerekli
        ItemStack fireworkStack = new ItemStack(Items.FIREWORK_ROCKET);
        net.minecraft.nbt.CompoundTag fw = fireworkStack.getOrCreateTagElement("Fireworks");
        fw.putByte("Flight", (byte) 10);

        // 3 adet Large Ball şarjı — flak patlamasına uygun sarı-beyaz renk paleti
        net.minecraft.nbt.ListTag explosionList = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < 3; i++) {
            net.minecraft.nbt.CompoundTag exp = new net.minecraft.nbt.CompoundTag();
            exp.putByte("Type", (byte) 1);  // 1 = Large Ball
            exp.put("Colors", new net.minecraft.nbt.IntArrayTag(
                    new int[]{16776960, 16777215, 16744448})); // sarı, beyaz, turuncu
            exp.putByte("Flicker", (byte) 0);
            exp.putByte("Trail",   (byte) 0);
            explosionList.add(exp);
        }
        fw.put("Explosions", explosionList);

        // Pozisyona yerleştir, xPower/yPower/zPower=0 (vanilla hız birikimini engeller)
        FireworkRocketEntity rocket = new FireworkRocketEntity(
                this.level(), spawnX, spawnY, spawnZ, fireworkStack
        );

        // Bakış yönünde sabit başlangıç hızı — ProjectileHandler her tick ROCKET_SPEED'de tutar
        Vec3 look = rider.getLookAngle();
        rocket.setDeltaMovement(look.x * ROCKET_SPEED, look.y * ROCKET_SPEED, look.z * ROCKET_SPEED);
        rocket.setOwner(rider);

        // Flak38 roketi olarak işaretle — ProjectileHandler bunu tanır
        rocket.getPersistentData().putBoolean("flak38_firework", true);

        this.level().addFreshEntity(rocket);

        // Ateş sesi — özel 20mm_shoot.ogg
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSounds.FLAK38_SHOOT.get(), SoundSource.PLAYERS, 1.5f, 1.0f);

        // Mermi azalt
        int remaining = this.getAmmoCount() - 1;
        this.setAmmoCount(remaining);

        if (remaining > 0) {
            rider.displayClientMessage(net.minecraft.network.chat.Component.literal("Ammo: " + remaining + "/" + MAX_AMMO), true);
        } else {
            // Mermi bitti — ateş durma sesi
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.FLAK38_STOP.get(), SoundSource.PLAYERS, 1.2f, 1.0f);
            rider.displayClientMessage(net.minecraft.network.chat.Component.literal("Out of ammo — reload!"), true);
        }
    }

    // --- Yolcu Yonetimi ---

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction callback) {
        if (this.hasPassenger(passenger)) {
            // Oturma koltuğu için koordinatları düzelt
            // Model rotasyonuna göre ofset uygulanması gerekebilir
            float yawRad = (float) Math.toRadians(this.getYRot());
            double offsetX = Math.sin(yawRad) * -0.5; // Koltuk biraz geride
            double offsetZ = Math.cos(yawRad) * 0.5;

            callback.accept(passenger,
                    this.getX() + offsetX,
                    this.getY() + 0.5,  // Oturma yüksekliği
                    this.getZ() + offsetZ
            );
        }
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        Entity passenger = this.getFirstPassenger();
        return passenger instanceof LivingEntity living ? living : null;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    // --- Hasar ---

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) return false;
        if (this.level().isClientSide()) return true;

        float health = this.entityData.get(DATA_HEALTH);
        health -= amount;
        this.entityData.set(DATA_HEALTH, health);

        if (health <= 0) {
            // Olum patlamasi ve alev sacilmasi
            spawnDeathExplosion();
            // Item dusurme yok — patladi
            this.discard();
        } else {
            this.playSound(SoundEvents.IRON_GOLEM_DAMAGE, 1.0f, 1.0f);
        }

        return true;
    }

    /**
     * Entity olunce buyuk patlama olusturur ve etrafa alevler sacar.
     */
    private void spawnDeathExplosion() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        Vec3 pos = this.position();

        // Gercek patlama — bloklara hasar verir, entitylere hasar verir
        this.level().explode(
                this, pos.x, pos.y + 0.5, pos.z,
                DEATH_EXPLOSION_POWER,
                true,  // Ates cikarir
                Level.ExplosionInteraction.TNT
        );

        // Ekstra alev parcaciklari — gorsel efekt
        serverLevel.sendParticles(
                ParticleTypes.FLAME,
                pos.x, pos.y + 0.5, pos.z,
                60, 2.0, 1.5, 2.0, 0.08
        );
        serverLevel.sendParticles(
                ParticleTypes.LARGE_SMOKE,
                pos.x, pos.y + 1.0, pos.z,
                40, 1.5, 2.0, 1.5, 0.05
        );
        serverLevel.sendParticles(
                ParticleTypes.LAVA,
                pos.x, pos.y + 0.5, pos.z,
                20, 1.0, 1.0, 1.0, 0.0
        );

        // Etrafa ates bloklari yerlestir (alev sacilmasi)
        for (int i = 0; i < DEATH_FLAME_COUNT; i++) {
            double offsetX = (this.level().random.nextDouble() - 0.5) * 6.0;
            double offsetZ = (this.level().random.nextDouble() - 0.5) * 6.0;
            double offsetY = this.level().random.nextDouble() * 2.0;

            BlockPos firePos = BlockPos.containing(pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ);
            BlockState stateAt = this.level().getBlockState(firePos);
            BlockState stateBelow = this.level().getBlockState(firePos.below());

            if (stateAt.isAir() && stateBelow.isSolidRender(this.level(), firePos.below())) {
                this.level().setBlockAndUpdate(firePos, Blocks.FIRE.defaultBlockState());
            }
        }
    }

    // --- Kayit/Yukleme ---

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Health")) {
            this.entityData.set(DATA_HEALTH, tag.getFloat("Health"));
        }
        if (tag.contains("AmmoCount")) {
            this.setAmmoCount(tag.getInt("AmmoCount"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Health", this.entityData.get(DATA_HEALTH));
        tag.putInt("AmmoCount", this.getAmmoCount());
    }

    // --- Ozellikler ---

    @Override
    public boolean canCollideWith(Entity other) {
        return other != this;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    public float getHealth() {
        return this.entityData.get(DATA_HEALTH);
    }

    public void heal(float amount) {
        float newHealth = Math.min(this.entityData.get(DATA_HEALTH) + amount, MAX_HEALTH);
        this.entityData.set(DATA_HEALTH, newHealth);
    }
}
