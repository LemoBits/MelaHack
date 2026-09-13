package thunder.hack.injection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thunder.hack.ThunderHack;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventTravel;
import thunder.hack.events.impl.EventEatFood;
import thunder.hack.events.impl.EventPlayerJump;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.render.Animations;

import static thunder.hack.features.modules.Module.mc;
import static thunder.hack.features.modules.movement.WaterSpeed.Mode.CancelResurface;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.Vec3;

@Mixin(LivingEntity.class)
public abstract class MixinEntityLiving {

    @Shadow
    public abstract InteractionHand getUsedItemHand();

    @Shadow
    public abstract ItemStack getItemInHand(InteractionHand hand);

    @Inject(method = "getCurrentSwingDuration", at = {@At("HEAD")}, cancellable = true)
    private void getArmSwingAnimationEnd(final CallbackInfoReturnable<Integer> info) {
        if (!ModuleManager.noRender.noSwing.getValue() && ModuleManager.animations.shouldChangeAnimationDuration() && Animations.slowAnimation.getValue())
            info.setReturnValue(Animations.slowAnimationVal.getValue());
    }

    @Unique
    private boolean prevFlying = false;

    @Unique
    private InteractionHand lastConsumeHand;

    @Inject(method = "isFallFlying", at = @At("TAIL"), cancellable = true)
    public void isGlidingHook(CallbackInfoReturnable<Boolean> cir) {
        if (ModuleManager.elytraRecast.isEnabled()) {
            boolean elytra = cir.getReturnValue();
            if (prevFlying && !cir.getReturnValue()) {
                cir.setReturnValue(ModuleManager.elytraRecast.castElytra());
            }
            prevFlying = elytra;
        }
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    public void travelHook(Vec3 movementInput, CallbackInfo ci) {
        if (Module.fullNullCheck()) return;
        if ((LivingEntity) (Object) this != mc.player) return;
        final EventTravel event = new EventTravel(mc.player.getDeltaMovement(), true);
        ThunderHack.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            mc.player.move(MoverType.SELF, event.getmVec());
            ci.cancel();
        }
    }

    @Inject(method = "travel", at = @At("RETURN"), cancellable = true)
    public void travelPostHook(Vec3 movementInput, CallbackInfo ci) {
        if (Module.fullNullCheck()) return;
        if ((LivingEntity) (Object) this != mc.player) return;
        final EventTravel event = new EventTravel(movementInput, false);
        ThunderHack.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            mc.player.move(MoverType.SELF, mc.player.getDeltaMovement());
            ci.cancel();
        }
    }

    @Inject(method = "jumpFromGround", at = @At("HEAD"))
    private void onJumpPre(CallbackInfo ci) {
        if ((LivingEntity) (Object) this == mc.player) {
            ThunderHack.EVENT_BUS.post(new EventPlayerJump(true));
        }
    }

    @Inject(method = "jumpFromGround", at = @At("RETURN"))
    private void onJumpPost(CallbackInfo ci) {
        if ((LivingEntity) (Object) this == mc.player) {
            ThunderHack.EVENT_BUS.post(new EventPlayerJump(false));
        }
    }

    @Inject(method = "completeUsingItem", at = @At("HEAD"))
    private void onConsumeItemStart(CallbackInfo ci) {
        if ((LivingEntity) (Object) this == mc.player) {
            lastConsumeHand = this.getUsedItemHand();
        }
    }

    @Inject(method = "completeUsingItem", at = @At("RETURN"))
    private void onConsumeItemEnd(CallbackInfo ci) {
        if ((LivingEntity) (Object) this == mc.player && lastConsumeHand != null) {
            ItemStack stack = this.getItemInHand(lastConsumeHand);
            ThunderHack.EVENT_BUS.post(new EventEatFood(stack));
            lastConsumeHand = null;
        }
    }

    @ModifyVariable(method = "setSprinting", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private boolean setSprintingHook(boolean sprinting) {
        if (mc.player != null && mc.level != null && ModuleManager.waterSpeed.isEnabled() && ModuleManager.waterSpeed.mode.is(CancelResurface)) {
            if (mc.player.isInWater() || mc.level.getBlockState(BlockPos.containing(mc.player.position().add(0, -0.5, 0))).getBlock() instanceof LiquidBlock)
                return true;
        }
        return sprinting;
    }

    @Inject(method = "getCurrentSwingDuration", at = @At("HEAD"), cancellable = true)
    private void onGetHandSwingDuration(CallbackInfoReturnable<Integer> cir) {
        if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.noSwing.getValue()) {
            cir.setReturnValue(0);
            cir.cancel();
        }
    }
}
