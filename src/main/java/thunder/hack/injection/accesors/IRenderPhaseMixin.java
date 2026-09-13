package thunder.hack.injection.accesors;

import net.minecraft.client.renderer.RenderStateShard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderStateShard.class)
public interface IRenderPhaseMixin {
    @Accessor("name")
    String getName();
}