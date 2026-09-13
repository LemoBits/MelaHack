package thunder.hack.injection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thunder.hack.core.manager.client.ModuleManager;
import static thunder.hack.ThunderHack.mc;

import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;


@Mixin(KeyMapping.class)
public abstract class MixinKeyBinding {
    @Shadow public abstract boolean same(KeyMapping other);

    @Shadow public abstract boolean isDown();

    @Inject(method = "isDown",at = @At("HEAD"),cancellable = true)
    private void pressHook(CallbackInfoReturnable<Boolean> cir){
        if(     this.same(mc.options.keyShift)
                && mc.player != null
                && mc.level != null
                && ModuleManager.safeWalk.isEnabled()
                && mc.player.onGround() && mc.level.getBlockState(new BlockPos((int) Math.floor(mc.player.position().x), (int) Math.floor(mc.player.position().y) - 1, (int) Math.floor(mc.player.position().z))).isAir()
                && !ModuleManager.scaffold.isEnabled()){
            cir.setReturnValue(true);
        }
    }
}
