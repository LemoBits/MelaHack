package thunder.hack.injection;

import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thunder.hack.ThunderHack;
@Mixin(RenderTickCounter.Dynamic.class)
public class MixinDynamic {
    @Shadow
    private float dynamicDeltaTicks;
    @Shadow
    private float tickProgress;
    @Shadow private long lastTimeMillis;
    @Final
    @Shadow private float tickTime;

    @Inject(method = "Lnet/minecraft/client/render/RenderTickCounter$Dynamic;beginRenderTick(J)I", at = @At("HEAD"), cancellable = true)
    private void beginRenderTickHook(long timeMillis, CallbackInfoReturnable<Integer> cir) {
        if(ThunderHack.TICK_TIMER == 1)
            return;

        this.dynamicDeltaTicks = ((timeMillis - this.lastTimeMillis) / this.tickTime) * ThunderHack.TICK_TIMER;
        this.lastTimeMillis = timeMillis;
        this.tickProgress += this.dynamicDeltaTicks;
        int i = (int) this.tickProgress;
        this.tickProgress -= i;
        cir.setReturnValue(i);
    }
}
