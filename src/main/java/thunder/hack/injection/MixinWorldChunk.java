package thunder.hack.injection;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thunder.hack.ThunderHack;
import thunder.hack.events.impl.EventSetBlockState;

@Mixin({LevelChunk.class})
public class MixinWorldChunk {

    @Shadow @Final Level level;

    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void setBlockStateHook(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> cir) {
        if (level.isClientSide) {
            ThunderHack.EVENT_BUS.post(new EventSetBlockState(pos, cir.getReturnValue(), state));
        }
    }
}
