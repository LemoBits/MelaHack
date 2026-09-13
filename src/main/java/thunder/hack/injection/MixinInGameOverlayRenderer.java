package thunder.hack.injection;

import thunder.hack.core.manager.client.ModuleManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public class MixinInGameOverlayRenderer {
    @Inject(method = "submitFire", at = @At("HEAD"), cancellable = true)
    private static void renderFireOverlayHook(PoseStack matrixStack, SubmitNodeCollector vertexConsumers, TextureAtlasSprite sprite, CallbackInfo ci) {
        if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.fireOverlay.getValue())
            ci.cancel();
    }

    @Inject(method = "submitWater", at = @At("HEAD"), cancellable = true)
    private static void renderUnderwaterOverlayHook(Minecraft minecraftClient, PoseStack matrixStack, SubmitNodeCollector vertexConsumers, CallbackInfo ci) {
        if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.waterOverlay.getValue() || ModuleManager.shaders.isEnabled())
            ci.cancel();
    }

    @Inject(method = "submitBlockSprite", at = @At("HEAD"), cancellable = true)
    private static void renderInWallOverlayHook(TextureAtlasSprite sprite, PoseStack matrices, SubmitNodeCollector vertexConsumers, int color, CallbackInfo ci) {
        if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.blockOverlay.getValue() || ModuleManager.shaders.isEnabled())
            ci.cancel();
    }
}
