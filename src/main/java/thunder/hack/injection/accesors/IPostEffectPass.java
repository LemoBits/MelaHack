package thunder.hack.injection.accesors;

import com.mojang.blaze3d.buffers.GpuBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import net.minecraft.client.renderer.PostPass;

@Mixin(PostPass.class)
public interface IPostEffectPass {
    @Accessor("customUniforms")
    Map<String, GpuBuffer> getUniformBuffers();
}
