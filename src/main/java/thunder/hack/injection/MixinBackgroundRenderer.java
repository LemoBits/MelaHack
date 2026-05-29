package thunder.hack.injection;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.render.WorldTweaks;

@Mixin(FogRenderer.class)
public class MixinBackgroundRenderer {
    @Unique
    private int thunderHack$viewDistance;

    @Inject(method = "applyFog", at = @At("HEAD"))
    private void captureFogContext(Camera camera, int viewDistance, boolean thickFog, RenderTickCounter tickCounter,
                                   float skyDarkness, ClientWorld world, CallbackInfoReturnable<Vector4f> cir) {
        thunderHack$viewDistance = viewDistance;
    }

    @ModifyArgs(
            method = "applyFog",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/fog/FogRenderer;applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"
            )
    )
    private void modifyFogUpload(Args args) {
        if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.fog.getValue()) {
            float start = thunderHack$viewDistance * 4.0f;
            float end = thunderHack$viewDistance * 4.25f;
            args.set(3, start);
            args.set(4, start);
            args.set(5, end);
            args.set(6, end);
            args.set(7, end);
            args.set(8, end);
        }

        if (ModuleManager.worldTweaks.isEnabled() && WorldTweaks.fogModify.getValue().isEnabled()) {
            args.set(2, new Vector4f(
                    WorldTweaks.fogColor.getValue().getGlRed(),
                    WorldTweaks.fogColor.getValue().getGlGreen(),
                    WorldTweaks.fogColor.getValue().getGlBlue(),
                    ((Vector4f) args.get(2)).w
            ));
            args.set(3, WorldTweaks.fogStart.getValue().floatValue());
            args.set(4, WorldTweaks.fogStart.getValue().floatValue());
            args.set(5, WorldTweaks.fogEnd.getValue().floatValue());
            args.set(6, WorldTweaks.fogEnd.getValue().floatValue());
        }
    }
}
