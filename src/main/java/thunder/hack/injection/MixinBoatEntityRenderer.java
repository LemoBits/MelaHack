package thunder.hack.injection;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.AbstractBoatRenderer;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.core.manager.client.ModuleManager;

@Mixin(AbstractBoatRenderer.class)
public class MixinBoatEntityRenderer {

    @Inject(method = "render(Lnet/minecraft/client/renderer/entity/state/BoatRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V", shift = At.Shift.AFTER))
    public void render(BoatRenderState boatEntityRenderState, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int i, CallbackInfo ci) {
        if (ModuleManager.boatFly.isEnabled() && ModuleManager.boatFly.hideBoat.getValue())
            matrixStack.scale(0.3f, 0.3f, 0.3f);
    }
}
