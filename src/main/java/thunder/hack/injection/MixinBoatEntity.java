package thunder.hack.injection;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.core.manager.client.ModuleManager;

@Mixin(AbstractBoat.class)
public class MixinBoatEntity {

    @Unique
    private float prevYaw, prevHeadYaw;

    @Inject(method = "positionRider", at = @At("HEAD"))
    protected void updatePassengerPositionHookPre(Entity passenger, Entity.MoveFunction positionUpdater, CallbackInfo ci) {
        if(ModuleManager.boatFly.isEnabled()) {
            prevYaw = passenger.getYRot();
            prevHeadYaw = passenger.getYHeadRot();
        }
    }

    @Inject(method = "positionRider", at = @At("RETURN"))
    protected void updatePassengerPositionHookPost(Entity passenger, Entity.MoveFunction positionUpdater, CallbackInfo ci) {
        if(ModuleManager.boatFly.isEnabled()) {
            passenger.setYRot(prevYaw);
            passenger.setYHeadRot(prevHeadYaw);
        }
    }
}
