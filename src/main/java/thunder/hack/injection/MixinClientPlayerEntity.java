package thunder.hack.injection;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thunder.hack.ThunderHack;
import thunder.hack.core.Core;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.*;
import thunder.hack.features.modules.Module;

import static thunder.hack.features.modules.Module.fullNullCheck;
import static thunder.hack.features.modules.Module.mc;

@Mixin(value = LocalPlayer.class, priority = 800)
public abstract class MixinClientPlayerEntity extends AbstractClientPlayer {
    @Unique
    boolean pre_sprint_state = false;
    @Unique
    private boolean updateLock = false;
    @Unique
    private Runnable postAction;

    @Shadow
    public abstract float getViewXRot(float tickDelta);
    @Shadow
    protected abstract void sendPosition();

    public MixinClientPlayerEntity(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void tickHook(CallbackInfo info) {
        if(Module.fullNullCheck()) return;
        ThunderHack.EVENT_BUS.post(new PlayerUpdateEvent());
    }

    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"), require = 0)
    private boolean tickMovementHook(LocalPlayer player) {
        if (ModuleManager.noSlow.isEnabled() && ModuleManager.noSlow.canNoSlow())
            return false;
        return player.isUsingItem();
    }

    @Inject(method = "isMovingSlowly", at = @At("HEAD"), cancellable = true)
    public void shouldSlowDownHook(CallbackInfoReturnable<Boolean> cir) {
        if(ModuleManager.noSlow.isEnabled()) {
            if (isVisuallyCrawling()) {
                if (ModuleManager.noSlow.crawl.getValue())
                    cir.setReturnValue(false);
            } else {
                if (ModuleManager.noSlow.sneak.getValue())
                    cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"), cancellable = true)
    public void onMoveHook(MoverType movementType, Vec3 movement, CallbackInfo ci) {
        if(Module.fullNullCheck()) return;
        EventMove event = new EventMove(movement.x, movement.y, movement.z);
        ThunderHack.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            super.move(movementType, new Vec3(event.getX(), event.getY(), event.getZ()));
            ci.cancel();
        }
    }

    @Inject(method = "sendPosition", at = @At("HEAD"), cancellable = true)
    private void sendMovementPacketsHook(CallbackInfo info) {
        if (fullNullCheck()) return;
        EventSync event = new EventSync(getYRot(), getXRot());
        ThunderHack.EVENT_BUS.post(event);
        postAction = event.getPostAction();
        EventSprint e = new EventSprint(isSprinting());
        ThunderHack.EVENT_BUS.post(e);
        ThunderHack.EVENT_BUS.post(new EventAfterRotate());
        if (e.getSprintState() != mc.player.wasSprinting) {
            if (e.getSprintState())
                mc.player.connection.send(new ServerboundPlayerCommandPacket(this, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
            else
                mc.player.connection.send(new ServerboundPlayerCommandPacket(this, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));

            mc.player.wasSprinting = e.getSprintState();
        }
        pre_sprint_state = mc.player.wasSprinting;
        Core.lockSprint = true;

        if (event.isCancelled()) info.cancel();
    }

    @Inject(method = "sendPosition", at = @At("RETURN"), cancellable = true)
    private void sendMovementPacketsPostHook(CallbackInfo info) {
        if (fullNullCheck()) return;
        mc.player.wasSprinting = pre_sprint_state;
        Core.lockSprint = false;
        EventPostSync event = new EventPostSync();
        ThunderHack.EVENT_BUS.post(event);
        if(postAction != null) {
            postAction.run();
            postAction = null;
        }
        if (event.isCancelled())
            info.cancel();
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;sendPosition()V", ordinal = 0, shift = At.Shift.AFTER), cancellable = true)
    private void PostUpdateHook(CallbackInfo info) {
        if(Module.fullNullCheck()) return;
        if (updateLock) {
            return;
        }
        PostPlayerUpdateEvent playerUpdateEvent = new PostPlayerUpdateEvent();
        ThunderHack.EVENT_BUS.post(playerUpdateEvent);
        if (playerUpdateEvent.isCancelled()) {
            info.cancel();
            if (playerUpdateEvent.getIterations() > 0) {
                for (int i = 0; i < playerUpdateEvent.getIterations(); i++) {
                    updateLock = true;
                    tick();
                    updateLock = false;
                    sendPosition();
                }
            }
        }
    }

    @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
    private void onPushOutOfBlocksHook(double x, double d, CallbackInfo info) {
        if (ModuleManager.noPush.isEnabled() && ModuleManager.noPush.blocks.getValue()) {
            info.cancel();
        }
    }

    @Inject(method = "handlePortalTransitionEffect", at = @At("HEAD"), cancellable = true)
    private void updateNauseaHook(CallbackInfo ci) {
        if(ModuleManager.portalInventory.isEnabled())
            ci.cancel();
    }
}
