package thunder.hack.injection.accesors;


import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.server.level.BlockDestructionProgress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface IWorldRenderer {
    @Accessor("destroyingBlocks")
    Int2ObjectMap<BlockDestructionProgress> getBlockBreakingInfos();
}