package thunder.hack.injection;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EndCrystalModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.core.manager.client.ModuleManager;

@Mixin(EndCrystalRenderer.class)
public class MixinEndCrystalEntityRenderer {


    @Shadow
    @Final
    private EndCrystalModel model;

    @Inject(method = "render(Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = {@At("HEAD")}, cancellable = true)
    public void render(EndCrystalRenderState state, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int i, CallbackInfo ci) {
        if(ModuleManager.chams.isEnabled() && ModuleManager.chams.crystals.getValue()) {
            ci.cancel();
            ModuleManager.chams.renderCrystal(state, matrixStack, vertexConsumerProvider, i, model);
        }
    }
}
