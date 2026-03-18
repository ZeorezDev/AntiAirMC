package com.antiair.registry;

import com.antiair.AntiAirMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Modun özel ses olaylarını kaydeder.
 * Ses dosyaları: assets/antiairsystem/sounds/*.ogg
 * Ses tanımları: assets/antiairsystem/sounds.json
 */
public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, AntiAirMod.MOD_ID);

    /** Flak 38 ateş sesi — her atışta çalar */
    public static final RegistryObject<SoundEvent> FLAK38_SHOOT =
            register("flak38_shoot");

    /** Flak 38 ateş durma sesi — mermi bitince çalar */
    public static final RegistryObject<SoundEvent> FLAK38_STOP =
            register("flak38_stop");

    /** Mermi şarjörü takılırken çalar */
    public static final RegistryObject<SoundEvent> AMMO_RELOAD_START =
            register("ammo_reload_start");

    /** Mermi şarjörü kilitlenince çalar */
    public static final RegistryObject<SoundEvent> AMMO_RELOAD_END =
            register("ammo_reload_end");

    // -------------------------------------------------------------------------

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () ->
                SoundEvent.createVariableRangeEvent(
                        new ResourceLocation(AntiAirMod.MOD_ID, name)));
    }
}
