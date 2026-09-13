package thunder.hack.injection;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.core.manager.client.ModuleManager;

@Mixin(SweetBerryBushBlock.class)
public class MixinSweetBerryBushBlock {
    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    public void onEntityCollisionHook(BlockState state, Level world, BlockPos pos, Entity entity, InsideBlockEffectApplier handler, boolean isClient, CallbackInfo ci) {
        if(ModuleManager.noSlow.isEnabled() && ModuleManager.noSlow.sweetBerryBush.getValue())
            ci.cancel();
    }
}
