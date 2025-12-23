package thunder.hack.injection;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.render.entity.model.EndCrystalEntityModel;
import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.core.manager.client.ModuleManager;

@Mixin(EndCrystalEntityRenderer.class)
public class MixinEndCrystalEntityRenderer {


    @Shadow
    @Final
    private EndCrystalEntityModel model;

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = {@At("HEAD")}, cancellable = true)
    public void render(EndCrystalEntityRenderState state, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        if(ModuleManager.chams.isEnabled() && ModuleManager.chams.crystals.getValue()) {
            ci.cancel();
            ModuleManager.chams.renderCrystal(state, matrixStack, vertexConsumerProvider, i, model);
        }
    }
}
