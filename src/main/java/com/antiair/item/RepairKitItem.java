package com.antiair.item;

import com.antiair.entity.Flak38Entity;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class RepairKitItem extends Item {

    private static final float REPAIR_AMOUNT = 10.0f;

    public RepairKitItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (!(entity instanceof Flak38Entity flak38)) return false;

        if (!player.level().isClientSide()) {
            float currentHealth = flak38.getHealth();
            if (currentHealth < Flak38Entity.MAX_HEALTH) {
                flak38.heal(REPAIR_AMOUNT);
                float newHealth = Math.min(currentHealth + REPAIR_AMOUNT, Flak38Entity.MAX_HEALTH);

                // Vanilla tamir sesi
                player.level().playSound(null, flak38.getX(), flak38.getY(), flak38.getZ(),
                        SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 1.0f, 1.2f);

                if (newHealth >= Flak38Entity.MAX_HEALTH) {
                    player.displayClientMessage(Component.literal("Repair complete! (" + (int) Flak38Entity.MAX_HEALTH + "/" + (int) Flak38Entity.MAX_HEALTH + " HP)"), true);
                } else {
                    player.displayClientMessage(Component.literal("Repaired! (" + (int) newHealth + "/" + (int) Flak38Entity.MAX_HEALTH + " HP)"), true);
                }

                if (!player.isCreative()) {
                    stack.shrink(1);
                }
            } else {
                player.displayClientMessage(Component.literal("Already at full health!"), true);
            }
        }

        return true; // Cancel the normal attack
    }
}
