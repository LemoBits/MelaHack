package thunder.hack.injection;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.core.manager.client.ModuleManager;

@Mixin(SlimeBlock.class)
public class MixinSlimeBlock {
    @Inject(method = "stepOn", at = @At("HEAD"), cancellable = true)
    public void onSteppedOnHook(Level world, BlockPos pos, BlockState state, Entity entity, CallbackInfo ci) {
        if(ModuleManager.noSlow.isEnabled() && ModuleManager.noSlow.slime.getValue())
            ci.cancel();
    }
}
