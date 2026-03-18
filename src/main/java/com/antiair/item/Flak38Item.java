package com.antiair.item;

import com.antiair.entity.Flak38Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Flak38 yerlestirme itemi.
 * Sag tikla yere entity spawn eder.
 */
public class Flak38Item extends Item {

    public Flak38Item(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockPos clickedPos = context.getClickedPos();
        Vec3 spawnPos = Vec3.atBottomCenterOf(clickedPos.above());

        Flak38Entity entity = new Flak38Entity(level, spawnPos.x, spawnPos.y, spawnPos.z);

        // Oyuncuya dogru baksın
        if (context.getPlayer() != null) {
            entity.setYRot(context.getPlayer().getYRot() + 180.0f);
        }

        level.addFreshEntity(entity);
        level.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z,
                SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.8f, 0.9f);

        // Itemi tuket (creative degilse)
        if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
            context.getItemInHand().shrink(1);
        }

        return InteractionResult.CONSUME;
    }
}
