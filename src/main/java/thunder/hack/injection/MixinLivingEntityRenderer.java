package thunder.hack.injection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.injection.accesors.IClientPlayerEntity;
import thunder.hack.injection.accesors.IEntity;
import thunder.hack.injection.accesors.ILivingEntity;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.ClientSettings;
import thunder.hack.utility.math.MathUtility;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;

import static thunder.hack.features.modules.Module.mc;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer {
    private LivingEntity lastEntity;

    private float originalHeadYaw, originalPrevHeadYaw, originalPrevHeadPitch, originalHeadPitch;

    @Shadow
    protected EntityModel<LivingEntityRenderState> model;

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void onUpdateRenderState(LivingEntity entity, LivingEntityRenderState state, float tickDelta, CallbackInfo ci) {
        lastEntity = entity;
    }

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    public void onRenderPre(LivingEntityRenderState state, PoseStack matrixStack, SubmitNodeCollector vertexConsumerProvider, CameraRenderState cameraState, CallbackInfo ci) {
        if (Module.fullNullCheck()) return;
        if (lastEntity == null) return;

        LivingEntity livingEntity = lastEntity;
        if (mc.player != null && livingEntity == mc.player && mc.player.getControlledVehicle() == null && ClientSettings.renderRotations.getValue() && !ThunderHack.isFuturePresent()) {
            originalHeadYaw = livingEntity.yHeadRot;
            originalPrevHeadYaw = ((ILivingEntity) livingEntity).getLastHeadYaw();
            originalPrevHeadPitch = ((IEntity) livingEntity).getLastPitch();
            originalHeadPitch = livingEntity.getXRot();

            livingEntity.setXRot(((IEntity) Minecraft.getInstance().player).getLastPitch());
            ((IEntity) livingEntity).setLastPitch(Managers.PLAYER.lastPitch);
            livingEntity.yHeadRot = ((IEntity) Minecraft.getInstance().player).getLastYaw();
            livingEntity.yBodyRot = Render2DEngine.interpolateFloat(Managers.PLAYER.prevBodyYaw, Managers.PLAYER.bodyYaw, Render3DEngine.getTickDelta());
            ((ILivingEntity) livingEntity).setLastHeadYaw(Managers.PLAYER.lastYaw);
            ((ILivingEntity) livingEntity).setLastBodyYaw(Render2DEngine.interpolateFloat(Managers.PLAYER.prevBodyYaw, Managers.PLAYER.bodyYaw, Render3DEngine.getTickDelta()));
        }

        if (livingEntity != mc.player && ModuleManager.freeCam.isEnabled() && ModuleManager.freeCam.track.getValue() && ModuleManager.freeCam.trackEntity != null && ModuleManager.freeCam.trackEntity == livingEntity) {
            ci.cancel();
            return;
        }

        if (livingEntity instanceof Player pe && ModuleManager.chams.isEnabled() && ModuleManager.chams.players.getValue()) {
            ModuleManager.chams.renderPlayer(pe, state.bodyRot, Render3DEngine.getTickDelta(), matrixStack, state.lightCoords, model, ci, () -> postRender(livingEntity));

            if (!pe.isSpectator()) {
                float g = Render3DEngine.getTickDelta();
                float n;
                Direction direction;
                Entity entity;
                matrixStack.pushPose();
                float h = Mth.rotLerp(g, ((ILivingEntity) pe).getLastBodyYaw(), pe.yBodyRot);
                float j = Mth.rotLerp(g, ((ILivingEntity) pe).getLastHeadYaw(), pe.yHeadRot);
                float k = j - h;
                if (pe.isPassenger() && (entity = pe.getVehicle()) instanceof LivingEntity) {
                    LivingEntity livingEntity2 = (LivingEntity) entity;
                    h = Mth.rotLerp(g, ((ILivingEntity) livingEntity2).getLastBodyYaw(), livingEntity2.yBodyRot);
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
                float m = Mth.lerp(g, ((IEntity) pe).getLastPitch(), pe.getXRot());
                if (("Dinnerbone".equals(pe.getName().getString()) || "Grumm".equals(pe.getName().getString()))) {
                    m *= -1.0f;
                    k *= -1.0f;
                }
                if (pe.hasPose(Pose.SLEEPING) && (direction = pe.getBedOrientation()) != null) {
                    n = pe.getEyeHeight(Pose.STANDING) - 0.1f;
                    matrixStack.translate((float) (-direction.getStepX()) * n, 0.0f, (float) (-direction.getStepZ()) * n);
                }
                float l = pe.tickCount + g;
                ModuleManager.chams.setupTransforms1(pe, matrixStack, l, h, g);
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
                matrixStack.popPose();
            }
        }
    }

    @Unique
    public void postRender(LivingEntity livingEntity) {
        if (Module.fullNullCheck()) return;
        if (mc.player != null && livingEntity == mc.player && mc.player.getControlledVehicle() == null && ClientSettings.renderRotations.getValue() && !ThunderHack.isFuturePresent()) {
            ((IEntity) livingEntity).setLastPitch(originalPrevHeadPitch);
            livingEntity.setXRot(originalHeadPitch);
            livingEntity.yHeadRot = originalHeadYaw;
            ((ILivingEntity) livingEntity).setLastHeadYaw(originalPrevHeadYaw);
        }
    }

    @Inject(method = "submit", at = @At("TAIL"))
    public void onRenderPost(LivingEntityRenderState state, PoseStack matrixStack, SubmitNodeCollector vertexConsumerProvider, CameraRenderState cameraState, CallbackInfo ci) {
        if (Module.fullNullCheck()) return;
        if (lastEntity != null) {
            postRender(lastEntity);
        }
    }

}
