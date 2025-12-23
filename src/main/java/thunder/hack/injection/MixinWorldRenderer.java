package thunder.hack.injection;

import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.core.manager.client.ShaderManager;

import static thunder.hack.features.modules.Module.mc;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer {

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;setupTerrain(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/Frustum;ZZ)V"), index = 3)
    private boolean renderSetupTerrainModifyArg(boolean spectator) {
        return ModuleManager.freeCam.isEnabled() || spectator;
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/PostEffectProcessor;render(Lnet/minecraft/client/render/FrameGraphBuilder;IILnet/minecraft/client/gl/PostEffectProcessor$FramebufferSet;)V", ordinal = 0))
    private void replaceShaderHook(PostEffectProcessor instance, FrameGraphBuilder frameGraphBuilder, int width, int height, PostEffectProcessor.FramebufferSet framebufferSet) {
        ShaderManager.Shader shaders = ModuleManager.shaders.mode.getValue();
        if (ModuleManager.shaders.isEnabled() && mc.world != null) {
            if (Managers.SHADER.fullNullCheck()) return;
            Managers.SHADER.setupShader(shaders, Managers.SHADER.getShaderOutline(shaders));
        } else {
            instance.render(frameGraphBuilder, width, height, framebufferSet);
        }
    }

    @Inject(method = "renderWeather", at = @At("HEAD"), cancellable = true)
    private void renderWeatherHook(FrameGraphBuilder frameGraphBuilder, LightmapTextureManager manager, Vec3d cameraPos, float tickDelta, Fog fog, CallbackInfo ci) {
        if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.noWeather.getValue()) {
            ci.cancel();
        }
    }
}
