package com.antiair.registry;

import com.antiair.AntiAirMod;
import com.antiair.entity.Flak38Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AntiAirMod.MOD_ID);

    public static final RegistryObject<EntityType<Flak38Entity>> FLAK38 = ENTITIES.register("flak38",
            () -> EntityType.Builder.<Flak38Entity>of(Flak38Entity::new, MobCategory.MISC)
                    .sized(0.8f, 1.0f)  // Hitbox'ı biraz daha küçültüyoruz (genişlik 0.8, yükseklik 1.0)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build("flak38"));
}
