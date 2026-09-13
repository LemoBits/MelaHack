package thunder.hack.injection;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.core.manager.client.ShaderManager;

import static thunder.hack.features.modules.Module.mc;

@Mixin(LevelRenderer.class)
public abstract class MixinWorldRenderer {

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PostChain;addToFrame(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;IILnet/minecraft/client/renderer/PostChain$TargetBundle;)V", ordinal = 0))
    private void replaceShaderHook(PostChain instance, FrameGraphBuilder frameGraphBuilder, int width, int height, PostChain.TargetBundle framebufferSet) {
        ShaderManager.Shader shaders = ModuleManager.shaders.mode.getValue();
        if (ModuleManager.shaders.isEnabled() && mc.level != null) {
            if (Managers.SHADER.fullNullCheck()) return;
            Managers.SHADER.setupShader(shaders, Managers.SHADER.getShaderOutline(shaders));
        } else {
            instance.addToFrame(frameGraphBuilder, width, height, framebufferSet);
        }
    }

    @Inject(method = "addWeatherPass", at = @At("HEAD"), cancellable = true)
    private void renderWeatherHook(FrameGraphBuilder frameGraphBuilder, GpuBufferSlice fog, CallbackInfo ci) {
        if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.noWeather.getValue()) {
            ci.cancel();
        }
    }
}
