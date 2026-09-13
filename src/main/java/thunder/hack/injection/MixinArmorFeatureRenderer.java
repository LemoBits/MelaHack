package thunder.hack.injection;

import thunder.hack.core.manager.client.ModuleManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public class MixinArmorFeatureRenderer {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(PoseStack matrices, MultiBufferSource vertexConsumers, int light, HumanoidRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.armor.getValue()) {
            ci.cancel();
        }
    }
}
