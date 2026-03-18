package com.antiair.client;

import com.antiair.AntiAirMod;
import com.antiair.registry.ModEntities;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = AntiAirMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        EntityRenderers.register(ModEntities.FLAK38.get(), Flak38Renderer::new);
        AntiAirMod.LOGGER.info("Anti-Air System client setup complete");
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(new ResourceLocation(AntiAirMod.MOD_ID, "block/flak38"));
        AntiAirMod.LOGGER.info("Registered Flak38 OBJ model as additional model");
    }
}
