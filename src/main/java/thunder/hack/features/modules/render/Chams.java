package thunder.hack.features.modules.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.mojang.blaze3d.platform.GlStateManager;
import thunder.hack.utility.render.compat.RenderSystem;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.model.EndCrystalModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.render.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.events.impl.EventHeldItemRenderer;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.TextureStorage;

import java.awt.*;

public class Chams extends Module {
    public Chams() {
        super("Chams", Category.RENDER);
    }

    public final Setting<Boolean> handItems = new Setting<>("HandItems", false);
    private final Setting<ColorSetting> handItemsColor = new Setting<>("HandItemsColor", new ColorSetting(new Color(0x9317DE5D, true)), v -> handItems.getValue());

    public final Setting<Boolean> crystals = new Setting<>("Crystals", false);
    private final Setting<ColorSetting> crystalColor = new Setting<>("CrystalColor", new ColorSetting(new Color(0x932DD8E8, true)), v -> crystals.getValue());
    private final Setting<Boolean> staticCrystal = new Setting<>("StaticCrystal", true, v -> crystals.getValue());
    private final Setting<CMode> crystalMode = new Setting<>("CrystalMode", CMode.One, v -> crystals.getValue());

    public final Setting<Boolean> players = new Setting<>("Players", false);
    private final Setting<ColorSetting> playerColor = new Setting<>("PlayerColor", new ColorSetting(new Color(0x932DD8E8, true)), v -> players.getValue());
    private final Setting<ColorSetting> friendColor = new Setting<>("FriendColor", new ColorSetting(new Color(0x932DE830, true)), v -> players.getValue());
    private final Setting<Boolean> playerTexture = new Setting<>("PlayerTexture", true, v -> players.getValue());
    private final Setting<Boolean> simple = new Setting<>("Simple", false, v -> players.getValue());

    private final Setting<Boolean> alternativeBlending = new Setting<>("AlternativeBlending", true);

    private enum CMode {
        One, Two, Three
    }

    private final ResourceLocation crystalTexture = ResourceLocation.parse("textures/entity/end_crystal/end_crystal.png");
    public void renderCrystal(EndCrystalRenderState state, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int light, EndCrystalModel model) {
        RenderSystem.enableBlend();
        if (alternativeBlending.getValue())
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        else RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();

        RenderSystem.setShaderColor(crystalColor.getValue().getGlRed(), crystalColor.getValue().getGlGreen(), crystalColor.getValue().getGlBlue(), crystalColor.getValue().getGlAlpha());
        float originalAge = state.ageInTicks;
        if (staticCrystal.getValue()) {
            state.ageInTicks = 0.0f;
        }

        ResourceLocation texture = crystalMode.getValue() == CMode.Two ? TextureStorage.crystalTexture2 : crystalTexture;
        RenderType layer = RenderType.entityCutoutNoCull(texture);

        matrixStack.pushPose();
        matrixStack.scale(2.0f, 2.0f, 2.0f);
        matrixStack.translate(0.0f, -0.5f, 0.0f);
        model.setupAnim(state);
        model.renderToBuffer(matrixStack, vertexConsumerProvider.getBuffer(layer), light, OverlayTexture.NO_OVERLAY);
        matrixStack.popPose();

        state.ageInTicks = originalAge;
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
    }

    public void renderPlayer(Player pe, float f, float g, PoseStack matrixStack, int i, EntityModel model, CallbackInfo ci, Runnable post) {
        RenderSystem.enableBlend();
        if (alternativeBlending.getValue())
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        else RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.disableDepthTest();
        BufferBuilder buffer;

        if (!simple.getValue()) {
            RenderSystem.setShaderTexture(0, ((AbstractClientPlayer) pe).getSkin().texture());
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
            buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        } else {
            RenderSystem.setShader(ShaderProgramKeys.POSITION);
            buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        }

        float n;
        Direction direction;
        Entity entity;
        matrixStack.pushPose();

        if (Managers.FRIEND.isFriend(pe)) {
            RenderSystem.setShaderColor(friendColor.getValue().getGlRed(), friendColor.getValue().getGlGreen(), friendColor.getValue().getGlBlue(), friendColor.getValue().getGlAlpha());
        } else {
            RenderSystem.setShaderColor(playerColor.getValue().getGlRed(), playerColor.getValue().getGlGreen(), playerColor.getValue().getGlBlue(), playerColor.getValue().getGlAlpha());
        }

        float h = Mth.rotLerp(g, ((thunder.hack.injection.accesors.ILivingEntity) pe).getLastBodyYaw(), pe.yBodyRot);
        float j = Mth.rotLerp(g, ((thunder.hack.injection.accesors.ILivingEntity) pe).getLastHeadYaw(), pe.yHeadRot);
        float k = j - h;
        if (pe.isPassenger() && (entity = pe.getVehicle()) instanceof LivingEntity) {
            LivingEntity livingEntity2 = (LivingEntity) entity;
            h = Mth.rotLerp(g, ((thunder.hack.injection.accesors.ILivingEntity) livingEntity2).getLastBodyYaw(), livingEntity2.yBodyRot);
            k = j - h;
            float l = Mth.wrapDegrees(k);
            if (l < -85.0f) {
                l = -85.0f;
            }
            if (l >= 85.0f) {
                l = 85.0f;
            }
            h = j - l;
            if (l * l > 2500.0f) {
                h += l * 0.2f;
            }
            k = j - h;
        }
        float m = Mth.lerp(g, ((thunder.hack.injection.accesors.IEntity) pe).getLastPitch(), pe.getXRot());
        if (LivingEntityRenderer.isEntityUpsideDown(pe)) {
            m *= -1.0f;
            k *= -1.0f;
        }
        if (pe.hasPose(Pose.SLEEPING) && (direction = pe.getBedOrientation()) != null) {
            n = pe.getEyeHeight(Pose.STANDING) - 0.1f;
            matrixStack.translate((float) (-direction.getStepX()) * n, 0.0f, (float) (-direction.getStepZ()) * n);
        }
        float l = pe.tickCount + g;

        setupTransforms1(pe, matrixStack, l, h, g);
        matrixStack.scale(-1.0f, -1.0f, 1.0f);

        matrixStack.scale(0.9375f, 0.9375f, 0.9375f);
        matrixStack.translate(0.0f, -1.501f, 0.0f);

        n = 0.0f;
        float o = 0.0f;
        if (!pe.isPassenger() && pe.isAlive()) {
            n = pe.walkAnimation.speed();
            o = pe.walkAnimation.position(g);
            if (pe.isBaby())
                o *= 3.0f;

            if (n > 1.0f)
                n = 1.0f;
        }
        LivingEntityRenderState renderState = null;
        EntityRenderer<? super Player, ? extends EntityRenderState> renderer = mc.getEntityRenderDispatcher().getRenderer(pe);
        EntityRenderState state = renderer.createRenderState(pe, g);
        if (state instanceof LivingEntityRenderState livingState) {
            renderState = livingState;
        }
        if (renderState != null) {
            @SuppressWarnings("unchecked")
            EntityModel<LivingEntityRenderState> typedModel = (EntityModel<LivingEntityRenderState>) model;
            typedModel.setupAnim(renderState);
            int p = LivingEntityRenderer.getOverlayCoords(renderState, 0);
            typedModel.renderToBuffer(matrixStack, buffer, i, p);
        }
        Render2DEngine.endBuilding(buffer);
        RenderSystem.disableBlend();
        RenderSystem.disableCull();
        matrixStack.popPose();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableDepthTest();
        if (!playerTexture.getValue()) {
            ci.cancel();
            post.run();
        }
    }

    public void setupTransforms1(Player abstractClientPlayerEntity, PoseStack matrixStack, float f, float g, float h) {
        float j = abstractClientPlayerEntity.getSwimAmount(h);
        float k = abstractClientPlayerEntity.getViewXRot(h);
        float l;
        float m;
        if (abstractClientPlayerEntity.isFallFlying()) {
            setupTransforms(abstractClientPlayerEntity, matrixStack, f, g, h);
            l = abstractClientPlayerEntity.tickCount + h;
            m = Mth.clamp(l * l / 100.0F, 0.0F, 1.0F);
            if (!abstractClientPlayerEntity.isAutoSpinAttack()) {
                matrixStack.mulPose(Axis.XP.rotationDegrees(m * (-90.0F - k)));
            }

            Vec3 vec3d = abstractClientPlayerEntity.getViewVector(h);
            Vec3 vec3d2 = abstractClientPlayerEntity.getDeltaMovement();
            double d = vec3d2.horizontalDistanceSqr();
            double e = vec3d.horizontalDistanceSqr();
            if (d > 0.0 && e > 0.0) {
                double n = (vec3d2.x * vec3d.x + vec3d2.z * vec3d.z) / Math.sqrt(d * e);
                double o = vec3d2.x * vec3d.z - vec3d2.z * vec3d.x;
                matrixStack.mulPose(Axis.YP.rotation((float) (Math.signum(o) * Math.acos(n))));
            }
        } else if (j > 0.0F) {
            setupTransforms(abstractClientPlayerEntity, matrixStack, f, g, h);
            l = abstractClientPlayerEntity.isInWater() ? -90.0F - k : -90.0F;
            m = Mth.lerp(j, 0.0F, l);
            matrixStack.mulPose(Axis.XP.rotationDegrees(m));
            if (abstractClientPlayerEntity.isVisuallySwimming()) {
                matrixStack.translate(0.0F, -1.0F, 0.3F);
            }
        } else {
            setupTransforms(abstractClientPlayerEntity, matrixStack, f, g, h);
        }
    }

    private void setupTransforms(Player entity, PoseStack matrices, float animationProgress, float bodyYaw, float tickDelta) {
        if (!entity.hasPose(Pose.SLEEPING)) {
            matrices.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        }

        if (entity.deathTime > 0) {
            float f = ((float) entity.deathTime + tickDelta - 1.0F) / 20.0F * 1.6F;
            f = Mth.sqrt(f);
            if (f > 1.0F) {
                f = 1.0F;
            }

            matrices.mulPose(Axis.ZP.rotationDegrees(f * 90.0F));
        } else if (entity.isAutoSpinAttack()) {
            matrices.mulPose(Axis.XP.rotationDegrees(-90.0F - entity.getXRot()));
            matrices.mulPose(Axis.YP.rotationDegrees(((float) entity.tickCount + tickDelta) * -75.0F));
        } else if (entity.hasPose(Pose.SLEEPING)) {
            Direction direction = entity.getBedOrientation();
            float g = direction != null ? getYaw(direction) : bodyYaw;
            matrices.mulPose(Axis.YP.rotationDegrees(g));
            matrices.mulPose(Axis.ZP.rotationDegrees(90.0F));
            matrices.mulPose(Axis.YP.rotationDegrees(270.0F));
        }
    }

    private static float getYaw(Direction direction) {
        return switch (direction) {
            case NORTH -> 270.0f;
            case SOUTH -> 90.0f;
            case EAST -> 180.0f;
            default -> 0.0f;
        };
    }

    @EventHandler
    public void onRenderHands(EventHeldItemRenderer e) {
        if (handItems.getValue())
            RenderSystem.setShaderColor(handItemsColor.getValue().getRed() / 255f, handItemsColor.getValue().getGreen() / 255f, handItemsColor.getValue().getBlue() / 255f, handItemsColor.getValue().getAlpha() / 255f);
    }
}
