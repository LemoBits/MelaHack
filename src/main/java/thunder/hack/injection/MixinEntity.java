package thunder.hack.injection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import thunder.hack.ThunderHack;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventFixVelocity;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.combat.Aura;
import thunder.hack.features.modules.combat.HitBox;
import thunder.hack.features.modules.render.Shaders;
import thunder.hack.features.modules.render.Trails;
import thunder.hack.utility.interfaces.IEntity;
import thunder.hack.utility.interfaces.IEntityLiving;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static thunder.hack.features.modules.Module.mc;

@Mixin(Entity.class)
public abstract class MixinEntity implements IEntity, IEntityLiving {

    @Shadow
    protected abstract BlockPos getBlockPosBelowThatAffectsMyMovement();

    @Shadow
    public abstract Vec3 trackingPosition();

    @Shadow
    private AABB bb;

    @Override
    public List<Trails.Trail> getTrails() {
        return trails;
    }

    @Override
    public BlockPos thunderHack_Recode$getVelocityBP() {
        return getBlockPosBelowThatAffectsMyMovement();
    }

    @Unique
    public List<Trails.Trail> trails = new ArrayList<>();

    @Unique
    double prevServerX, prevServerY, prevServerZ;

    @Unique
    public List<Aura.Position> positonHistory = new ArrayList<>();

    @Override
    public List<Aura.Position> getPositionHistory() {
        return positonHistory;
    }

    @Inject(method = {"moveOrInterpolateTo"}, at = {@At("HEAD")})
    private void updateTrackedPositionAndAnglesHook(Vec3 pos, float yaw, float pitch, CallbackInfo ci) {
        if (Module.fullNullCheck()) return;
        Vec3 syncedPos = trackingPosition();
        prevServerX = syncedPos.x;
        prevServerY = syncedPos.y;
        prevServerZ = syncedPos.z;
        positonHistory.add(new Aura.Position(syncedPos.x, syncedPos.y, syncedPos.z));
        positonHistory.removeIf(Aura.Position::shouldRemove);
    }

    @Override
    public double getPrevServerX() {
        return prevServerX;
    }

    @Override
    public double getPrevServerY() {
        return prevServerY;
    }

    @Override
    public double getPrevServerZ() {
        return prevServerZ;
    }

    @ModifyArgs(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;push(DDD)V"))
    public void pushAwayFromHook(Args args) {

        //Condition '...' is always 'false' is a lie!!! do not delete
        if ((Object) this == mc.player && ModuleManager.noPush.isEnabled() && ModuleManager.noPush.players.getValue()) {
            args.set(0, 0.);
            args.set(1, 0.);
            args.set(2, 0.);
        }
    }

    @Inject(method = "moveRelative", at = {@At("HEAD")}, cancellable = true)
    public void updateVelocityHook(float speed, Vec3 movementInput, CallbackInfo ci) {
        if(Module.fullNullCheck()) return;
        if ((Object) this == mc.player) {
            ci.cancel();
            EventFixVelocity event = new EventFixVelocity(movementInput, speed, mc.player.getYRot(), movementInputToVelocityC(movementInput, speed, mc.player.getYRot()));
            ThunderHack.EVENT_BUS.post(event);
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().add(event.getVelocity()));
        }
    }

    @Unique
    private static Vec3 movementInputToVelocityC(Vec3 movementInput, float speed, float yaw) {
        double d = movementInput.lengthSqr();
        if (d < 1.0E-7) {
            return Vec3.ZERO;
        }
        Vec3 vec3d = (d > 1.0 ? movementInput.normalize() : movementInput).scale(speed);
        float f = Mth.sin(yaw * ((float) Math.PI / 180));
        float g = Mth.cos(yaw * ((float) Math.PI / 180));
        return new Vec3(vec3d.x * (double) g - vec3d.z * (double) f, vec3d.y, vec3d.z * (double) g + vec3d.x * (double) f);
    }

    @Inject(method = "getBoundingBox", at = {@At("HEAD")}, cancellable = true)
    public final void getBoundingBox(CallbackInfoReturnable<AABB> cir) {
        if (ModuleManager.hitBox.isEnabled() && mc != null && mc.player != null && ((Entity) (Object) this).getId() != mc.player.getId() && (ModuleManager.aura.isDisabled() || HitBox.affectToAura.getValue())) {
            cir.setReturnValue(new AABB(this.bb.minX - HitBox.XZExpand.getValue() / 2f, this.bb.minY - HitBox.YExpand.getValue() / 2f, this.bb.minZ - HitBox.XZExpand.getValue() / 2f, this.bb.maxX + HitBox.XZExpand.getValue() / 2f, this.bb.maxY + HitBox.YExpand.getValue() / 2f, this.bb.maxZ + HitBox.XZExpand.getValue() / 2f));
        }
    }

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    public void isGlowingHook(CallbackInfoReturnable<Boolean> cir) {
        Shaders shaders = ModuleManager.shaders;
        if (shaders.isEnabled()) {
            cir.setReturnValue(shaders.shouldRender((Entity) (Object) this));
        }
    }

    @Inject(method = "isOnFire", at = @At("HEAD"), cancellable = true)
    public void isOnFireHook(CallbackInfoReturnable<Boolean> cir) {
        if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.fireEntity.getValue()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
    public void isInvisibleToHook(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (ModuleManager.serverHelper.isEnabled() && ModuleManager.serverHelper.trueSight.getValue()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isInLava", at = @At("HEAD"), cancellable = true)
    public void isInLavaHook(CallbackInfoReturnable<Boolean> cir) {
        if((ModuleManager.jesus.isEnabled() || ModuleManager.noWaterCollision.isEnabled()) && mc.player != null && ((Entity) (Object) this).getId() == mc.player.getId())
            cir.setReturnValue(false);
    }

    @Inject(method = "isInWater", at = @At("HEAD"), cancellable = true)
    public void isTouchingWaterHook(CallbackInfoReturnable<Boolean> cir) {
        if((ModuleManager.jesus.isEnabled() || ModuleManager.noWaterCollision.isEnabled()) && mc.player != null && ((Entity) (Object) this).getId() == mc.player.getId())
            cir.setReturnValue(false);
    }

    @Inject(method = "setSwimming", at = @At("HEAD"), cancellable = true)
    public void setSwimmingHook(boolean swimming, CallbackInfo ci) {
        if((ModuleManager.jesus.isEnabled() || ModuleManager.noWaterCollision.isEnabled()) && swimming && mc.player != null && ((Entity) (Object) this).getId() == mc.player.getId())
            ci.cancel();
    }

    @ModifyVariable(method = "turn", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private double changeLookDirectionHook0(double value) {
        if(ModuleManager.viewLock.isEnabled() && ModuleManager.viewLock.yaw.getValue())
            return 0d;
        return value;
    }

    @ModifyVariable(method = "turn", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private double changeLookDirectionHook1(double value) {
        if(ModuleManager.viewLock.isEnabled() && ModuleManager.viewLock.pitch.getValue())
            return 0d;
        return value;
    }
}