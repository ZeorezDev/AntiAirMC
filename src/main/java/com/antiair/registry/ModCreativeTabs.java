package com.antiair.registry;

import com.antiair.AntiAirMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AntiAirMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> ANTI_AIR_TAB = CREATIVE_TABS.register("anti_air_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + AntiAirMod.MOD_ID + ".anti_air_tab"))
                    .icon(() -> new ItemStack(ModItems.FLAK38.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.FLAK38.get());
                        output.accept(ModItems.AMMO_20MM.get());
                        output.accept(ModItems.REPAIR_KIT.get());
                    })
                    .build());
}
