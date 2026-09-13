package thunder.hack.injection;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.core.manager.client.ModuleManager;

@Mixin(AbstractSignRenderer.class)
public class MixinSignBlockEntityRenderer {
    @Inject(method = "submitSignText", at = {@At("HEAD")}, cancellable = true)
    public final void renderTextHook(SignRenderState state, PoseStack matrices, SubmitNodeCollector vertexConsumers, SignText signText, CallbackInfo ci) {
        if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.signText.getValue())
            ci.cancel();
    }
}
