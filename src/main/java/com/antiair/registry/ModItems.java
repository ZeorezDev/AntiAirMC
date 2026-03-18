package com.antiair.registry;

import com.antiair.AntiAirMod;
import com.antiair.item.Flak38Item;
import com.antiair.item.RepairKitItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, AntiAirMod.MOD_ID);

    public static final RegistryObject<Item> AMMO_20MM = ITEMS.register("ammo_20mm",
            () -> new Item(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> FLAK38 = ITEMS.register("flak38",
            () -> new Flak38Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> REPAIR_KIT = ITEMS.register("repair_kit",
            () -> new RepairKitItem(new Item.Properties().stacksTo(16)));
}
