package thunder.hack.injection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thunder.hack.core.manager.client.ModuleManager;

import static net.minecraft.world.level.material.FlowingFluid.FALLING;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

@Mixin(FlowingFluid.class)
public abstract class MixinFlowableFluid {

    @Shadow
    protected abstract boolean isSolidFace(BlockGetter world, BlockPos pos, Direction direction);

    @Inject(method = "getFlow", at = @At(value = "HEAD"), cancellable = true)
    private void getVelocityHook(BlockGetter world, BlockPos pos, FluidState state, CallbackInfoReturnable<Vec3> cir) {
        if (ModuleManager.noPush.isEnabled() && ModuleManager.noPush.water.getValue()) {
            double d = 0.0;
            double e = 0.0;
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
            Vec3 vec3d = new Vec3(d, 0.0, e);
            if (state.getValue(FALLING)) {
                for (Direction direction2 : Direction.Plane.HORIZONTAL) {
                    mutable.setWithOffset(pos, direction2);
                    if (!this.isSolidFace(world, mutable, direction2) && !this.isSolidFace(world, mutable.above(), direction2))
                        continue;
                    vec3d = vec3d.normalize().add(0.0, -6.0, 0.0);
                    break;
                }
            }
            cir.setReturnValue(vec3d.normalize());
        }
    }
}