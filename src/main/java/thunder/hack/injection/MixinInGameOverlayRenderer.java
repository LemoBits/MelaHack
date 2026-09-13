package thunder.hack.injection;

import thunder.hack.core.manager.client.ModuleManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public class MixinInGameOverlayRenderer {
    @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
    private static void renderFireOverlayHook(PoseStack matrixStack, MultiBufferSource vertexConsumers, CallbackInfo ci) {
        if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.fireOverlay.getValue())
            ci.cancel();
    }

    @Inject(method = "renderWater", at = @At("HEAD"), cancellable = true)
    private static void renderUnderwaterOverlayHook(Minecraft minecraftClient, PoseStack matrixStack, MultiBufferSource vertexConsumers, CallbackInfo ci) {
        if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.waterOverlay.getValue() || ModuleManager.shaders.isEnabled())
            ci.cancel();
    }

    @Inject(method = "renderTex", at = @At("HEAD"), cancellable = true)
    private static void renderInWallOverlayHook(TextureAtlasSprite sprite, PoseStack matrices, MultiBufferSource vertexConsumers, CallbackInfo ci) {
        if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.blockOverlay.getValue() || ModuleManager.shaders.isEnabled())
            ci.cancel();
    }
}
