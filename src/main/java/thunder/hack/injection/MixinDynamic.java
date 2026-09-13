package thunder.hack.injection;

import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thunder.hack.ThunderHack;
@Mixin(DeltaTracker.Timer.class)
public class MixinDynamic {
    @Shadow
    private float deltaTicks;
    @Shadow
    private float deltaTickResidual;
    @Shadow private long lastMs;
    @Final
    @Shadow private float msPerTick;

    @Inject(method = "advanceGameTime(J)I", at = @At("HEAD"), cancellable = true)
    private void beginRenderTickHook(long timeMillis, CallbackInfoReturnable<Integer> cir) {
        if(ThunderHack.TICK_TIMER == 1)
            return;

        this.deltaTicks = ((timeMillis - this.lastMs) / this.msPerTick) * ThunderHack.TICK_TIMER;
        this.lastMs = timeMillis;
        this.deltaTickResidual += this.deltaTicks;
        int i = (int) this.deltaTickResidual;
        this.deltaTickResidual -= i;
        cir.setReturnValue(i);
    }
}
