package com.antiair;

import com.antiair.handler.CrossbowHandler;
import com.antiair.handler.ProjectileHandler;
import com.antiair.network.ModNetwork;
import com.antiair.registry.ModEntities;
import com.antiair.registry.ModItems;
import com.antiair.registry.ModCreativeTabs;
import com.antiair.registry.ModSounds;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(AntiAirMod.MOD_ID)
public class AntiAirMod {

    public static final String MOD_ID = "antiairsystem";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AntiAirMod() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // DeferredRegister'lari mod event bus'a kaydet
        ModEntities.ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.CREATIVE_TABS.register(modBus);
        ModSounds.SOUNDS.register(modBus);

        // Common setup icin network kaydı
        modBus.addListener(this::commonSetup);

        // Gameplay event handler'lari Forge event bus'a kaydet
        MinecraftForge.EVENT_BUS.register(new CrossbowHandler());
        MinecraftForge.EVENT_BUS.register(new ProjectileHandler());

        LOGGER.info("Anti-Air System initialized");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        ModNetwork.register();
    }
}
