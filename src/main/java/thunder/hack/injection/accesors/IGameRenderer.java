package thunder.hack.injection.accesors;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface IGameRenderer {
    @Invoker("renderItemInHand")
    void irenderHand(CameraRenderState cameraState, float tickDelta, Matrix4fc matrix4f);
}
