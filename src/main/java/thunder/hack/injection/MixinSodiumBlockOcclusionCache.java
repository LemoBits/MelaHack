package thunder.hack.injection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.render.XRay;

@SuppressWarnings("UnresolvedMixinReference")
@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache", remap = false)
public class MixinSodiumBlockOcclusionCache {
    @Inject(method = "shouldDrawSide", at = @At("RETURN"), cancellable = true)
    void shouldDrawSideHook(BlockState state, BlockGetter view, BlockPos pos, Direction facing, CallbackInfoReturnable<Boolean> cir) {
        if (ModuleManager.xray.isEnabled() && ModuleManager.xray.wallHack.getValue())
            cir.setReturnValue(XRay.isCheckableOre(state.getBlock()));
        if(ModuleManager.autoAnchor.isEnabled() && state.getBlock() instanceof FireBlock)
            cir.setReturnValue(false);
    }
}