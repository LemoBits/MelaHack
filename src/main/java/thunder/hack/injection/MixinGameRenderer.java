package thunder.hack.injection;

import thunder.hack.utility.render.compat.RenderSystem;
import thunder.hack.core.Managers;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.*;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thunder.hack.ThunderHack;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.ClientSettings;
import thunder.hack.features.modules.player.NoEntityTrace;
import thunder.hack.utility.math.FrameRateCounter;
import thunder.hack.utility.render.BlockAnimationUtility;
import thunder.hack.utility.render.BufferRenderer;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;

import static thunder.hack.features.modules.Module.mc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {

    @Shadow
    public abstract void tick();

    @Inject(method = "render", at = @At("TAIL"))
    void postHudRenderHook(DeltaTracker tickCounter, boolean tick, CallbackInfo ci) {
        // In game MelaHack's HUD belongs on top of the vanilla one, so whatever is still queued is
        // replayed here. Screen content was already replayed by MixinGuiRenderer between the
        // vanilla GUI layers.
        BufferRenderer.flushQueue();
        BufferRenderer.endFrame();
        FrameRateCounter.INSTANCE.recordFrame();
        Render2DEngine.BLUR_PROGRAM.invalidateCapture();
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lnet/minecraft/client/renderer/state/level/CameraRenderState;FLorg/joml/Matrix4fc;)V"), method = "renderLevel")
    void render3dHook(DeltaTracker tickCounter, CallbackInfo ci) {
        if (Module.fullNullCheck()) return;

        Camera camera = mc.gameRenderer.mainCamera();
        PoseStack matrixStack = new PoseStack();
        RenderSystem.getModelViewStack().pushMatrix().mul(matrixStack.last().pose());
        matrixStack.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
        matrixStack.mulPose(Axis.YP.rotationDegrees(camera.yRot() + 180.0f));

        // World space geometry is projected with the level projection and already carries the view
        // rotation, so the model view matrix stays at identity.
        Render3DEngine.lastProjMat.set(mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.projectionMatrix);
        Render3DEngine.lastModMat.identity();
        Render3DEngine.lastWorldSpaceMatrix.set(matrixStack.last().pose());

        RenderSystem.beginWorldDrawing();
        try {
            Managers.MODULE.onRender3D(matrixStack);
            BlockAnimationUtility.onRender(matrixStack);
            Render3DEngine.onRender3D(matrixStack); // <- не двигать
        } finally {
            RenderSystem.endWorldDrawing();
        }

        RenderSystem.getModelViewStack().popMatrix();
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lnet/minecraft/client/renderer/state/level/CameraRenderState;FLorg/joml/Matrix4fc;)V", shift = At.Shift.AFTER))
    public void postRender3dHook(DeltaTracker tickCounter, CallbackInfo ci) {
        if (Module.fullNullCheck()) return;
        Managers.SHADER.renderShaders();
    }

    @Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(FFF)F"))
    private float renderWorldHook(float delta, float first, float second) {
        if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.nausea.getValue()) return 0;
        return Mth.lerp(delta, first, second);
    }

    @Inject(method = "preloadUiShader", at = @At(value = "RETURN"))
    private void loadSatinPrograms(ResourceProvider factory, CallbackInfo ci) {
        // Reload happens via ResourceManagerHelper to ensure mod assets are available.
    }

    @Unique
    private HitResult ensureTargetInRangeCustom(HitResult hitResult, Vec3 cameraPos, double interactionRange) {
        Vec3 vec3d = hitResult.getLocation();
        if (!vec3d.closerThan(cameraPos, interactionRange)) {
            Vec3 vec3d2 = hitResult.getLocation();
            Direction direction = Direction.getApproximateNearest(vec3d2.x - cameraPos.x, vec3d2.y - cameraPos.y, vec3d2.z - cameraPos.z);
            return BlockHitResult.miss(vec3d2, direction, BlockPos.containing(vec3d2));
        } else {
            return hitResult;
        }
    }

    @Inject(method = "displayItemActivation", at = @At("HEAD"), cancellable = true)
    private void showFloatingItemHook(ItemStack floatingItem, CallbackInfo info) {
        if (ModuleManager.totemAnimation.isEnabled()) {
            ModuleManager.totemAnimation.showFloatingItem(floatingItem);
            info.cancel();
        }
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void tiltViewWhenHurtHook(CameraRenderState cameraState, PoseStack matrices, CallbackInfo ci) {
        if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.hurtCam.getValue())
            ci.cancel();
    }
}
