package thunder.hack.injection;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.core.manager.client.ModuleManager;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public abstract class MixinCamera {
    @Shadow
    protected abstract float getMaxZoom(float desiredCameraDistance);

    @Shadow
    private boolean detached;

    @ModifyArgs(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;move(FFF)V", ordinal = 0))
    private void modifyCameraDistance(Args args) {
        if (ModuleManager.noCameraClip.isEnabled()) {
            args.set(0, -getMaxZoom(ModuleManager.noCameraClip.getDistance()));
        }
    }

    @Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
    private void onClipToSpace(float f, CallbackInfoReturnable<Float> cir) {
        if (ModuleManager.noCameraClip.isEnabled()) {
            cir.setReturnValue(ModuleManager.noCameraClip.getDistance());
        }
    }

    @Inject(method = "update", at = @At("TAIL"))
    private void updateHook(DeltaTracker tickCounter, CallbackInfo ci) {
        if (ModuleManager.freeCam.isEnabled()) {
            this.detached = true;
        }
    }

    @ModifyArgs(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"))
    private void setRotationHook(Args args) {
        if(ModuleManager.freeCam.isEnabled())
            args.setAll(ModuleManager.freeCam.getFakeYaw(), ModuleManager.freeCam.getFakePitch());
    }

    @ModifyArgs(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"))
    private void setPosHook(Args args) {
        if(ModuleManager.freeCam.isEnabled())
            args.setAll(ModuleManager.freeCam.getFakeX(), ModuleManager.freeCam.getFakeY(), ModuleManager.freeCam.getFakeZ());
    }
}