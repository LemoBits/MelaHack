package thunder.hack.injection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thunder.hack.utility.world.ExplosionUtility;

import static thunder.hack.features.modules.Module.mc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

@Mixin(Level.class)
public abstract class MixinWorld {
    @Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true)
    public void blockStateHook(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        if (ExplosionUtility.terrainIgnore && mc.level != null && !mc.level.isInWorldBounds(pos)) {
            LevelChunk worldChunk = mc.level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);

            BlockState tempState = worldChunk.getBlockState(pos);

            if (tempState.getBlock() == Blocks.OBSIDIAN
                    || tempState.getBlock() == Blocks.BEDROCK
                    || tempState.getBlock() == Blocks.ENDER_CHEST
                    || tempState.getBlock() == Blocks.RESPAWN_ANCHOR
            ) return;
            cir.setReturnValue(Blocks.AIR.defaultBlockState());
        }
    }
}