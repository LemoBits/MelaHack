package thunder.hack.injection;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.AbstractBoatEntityRenderer;
import net.minecraft.client.render.entity.state.BoatEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.core.manager.client.ModuleManager;

@Mixin(AbstractBoatEntityRenderer.class)
public class MixinBoatEntityRenderer {

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/BoatEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V", shift = At.Shift.AFTER))
    public void render(BoatEntityRenderState boatEntityRenderState, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        if (ModuleManager.boatFly.isEnabled() && ModuleManager.boatFly.hideBoat.getValue())
            matrixStack.scale(0.3f, 0.3f, 0.3f);
    }
}
