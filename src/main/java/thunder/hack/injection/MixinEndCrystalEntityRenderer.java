package thunder.hack.injection;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.object.crystal.EndCrystalModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
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

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = {@At("HEAD")}, cancellable = true)
    public void render(EndCrystalRenderState state, PoseStack matrixStack, SubmitNodeCollector vertexConsumerProvider, CameraRenderState cameraState, CallbackInfo ci) {
        if(ModuleManager.chams.isEnabled() && ModuleManager.chams.crystals.getValue()) {
            ci.cancel();
            ModuleManager.chams.renderCrystal(state, matrixStack, vertexConsumerProvider, state.lightCoords, model);
        }
    }
}
