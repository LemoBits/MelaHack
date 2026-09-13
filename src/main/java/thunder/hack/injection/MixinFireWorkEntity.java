package thunder.hack.injection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.combat.Aura;

import static thunder.hack.core.manager.IManager.mc;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(FireworkRocketEntity.class)
public class MixinFireWorkEntity {

    @Shadow
    private LivingEntity attachedToEntity;

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 tickHook(LivingEntity instance) {
        if (ModuleManager.aura.isEnabled() && ModuleManager.aura.rotationMode.not(Aura.Mode.None)
                && ModuleManager.aura.target != null && attachedToEntity == mc.player && ModuleManager.aura.elytraTarget.getValue()) {

          //  float[] nonLimitedRotation = PlayerManager.calcAngle(ModuleManager.aura.target.getEyePos().add(0, 0.5, 0));
            return Managers.PLAYER.getRotationVector(ModuleManager.aura.rotationPitch, ModuleManager.aura.rotationYaw);
        }
        return attachedToEntity.getLookAngle();
    }
}
