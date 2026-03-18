package com.antiair.client;

import com.antiair.AntiAirMod;
import com.antiair.entity.Flak38Entity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;

@OnlyIn(Dist.CLIENT)
public class Flak38Renderer extends EntityRenderer<Flak38Entity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AntiAirMod.MOD_ID, "textures/model/flak38_texture.png");

    private static final ResourceLocation MODEL_LOCATION =
            new ResourceLocation(AntiAirMod.MOD_ID, "block/flak38");

    public Flak38Renderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.2f;
    }

    @Override
    public void render(Flak38Entity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {

        poseStack.pushPose();

        // Binen oyuncunun bakis yonune gore don
        float renderYaw = entityYaw;
        Entity passenger = entity.getFirstPassenger();
        if (passenger instanceof LivingEntity living) {
            // Oyuncunun yaw'ini kullan — entity ile birlikte donecek
            renderYaw = living.getViewYRot(partialTicks);
        }

        // Entity rotasyonu (Y ekseninde) — vanilla Minecraft standardinda
        // Boat, Minecart vb. hepsi 180 - yaw formulunu kullanir
        poseStack.mulPose(Axis.YP.rotationDegrees(270.0f - renderYaw));

        // OBJ modeli olcegi — insan boyutunda (eskisi 0.15 idi, simdi buyutuyoruz)
        poseStack.scale(0.35f, 0.35f, 0.35f);

        // Modeli yere hizala
        poseStack.translate(0.0, 0.15, 0.0);

        // BakedModel'i model manager'dan al
        BakedModel model = Minecraft.getInstance().getModelManager()
                .getModel(MODEL_LOCATION);

        if (model != null && model != Minecraft.getInstance().getModelManager().getMissingModel()) {
            VertexConsumer consumer = bufferSource.getBuffer(net.minecraft.client.renderer.RenderType.entityCutout(TEXTURE));

            Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                    poseStack.last(),
                    consumer,
                    null,
                    model,
                    1.0f, 1.0f, 1.0f,
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    ModelData.EMPTY,
                    net.minecraft.client.renderer.RenderType.entityCutout(TEXTURE)
            );
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(Flak38Entity entity) {
        return TEXTURE;
    }
}
