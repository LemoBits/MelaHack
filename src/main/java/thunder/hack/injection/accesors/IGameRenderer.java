package thunder.hack.injection.accesors;

import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface IGameRenderer {
    @Invoker("renderItemInHand")
    void irenderHand(float tickDelta, boolean renderWorld, Matrix4f matrix4f);
}
