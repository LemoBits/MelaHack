package thunder.hack.injection;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.render.WorldTweaks;

@Mixin(FogRenderer.class)
public class MixinBackgroundRenderer {
    @Inject(method = "setupFog", at = @At("RETURN"), cancellable = true)
    private void modifyFog(Camera camera, int viewDistance, DeltaTracker tickCounter,
                           float skyDarkness, ClientLevel world, CallbackInfoReturnable<FogData> cir) {
        FogData fog = cir.getReturnValue();

        if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.fog.getValue()) {
            float start = viewDistance * 4.0f;
            float end = viewDistance * 4.25f;
            fog.environmentalStart = start;
            fog.renderDistanceStart = start;
            fog.environmentalEnd = end;
            fog.renderDistanceEnd = end;
            fog.skyEnd = end;
            fog.cloudEnd = end;
        }

        if (ModuleManager.worldTweaks.isEnabled() && WorldTweaks.fogModify.getValue().isEnabled()) {
            fog.color = new Vector4f(
                    WorldTweaks.fogColor.getValue().getGlRed(),
                    WorldTweaks.fogColor.getValue().getGlGreen(),
                    WorldTweaks.fogColor.getValue().getGlBlue(),
                    fog.color.w
            );
            fog.environmentalStart = WorldTweaks.fogStart.getValue().floatValue();
            fog.renderDistanceStart = WorldTweaks.fogStart.getValue().floatValue();
            fog.environmentalEnd = WorldTweaks.fogEnd.getValue().floatValue();
            fog.renderDistanceEnd = WorldTweaks.fogEnd.getValue().floatValue();
        }
    }
}
