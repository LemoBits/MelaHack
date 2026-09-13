package thunder.hack.injection;

import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.render.Fullbright;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightTexture.class)
public class MixinLightmapTextureManager {

    @Inject(method = "calculateDarknessScale", at = @At("HEAD"), cancellable = true)
    private void getDarkness(LivingEntity entity, float tickDelta, float factor, CallbackInfoReturnable<Float> info) {
        if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.darkness.getValue()) info.setReturnValue(0.0f);
    }
    @Inject(method = "getBrightness", at = @At("HEAD"), cancellable = true)
    private static void getBrightnessHook(DimensionType type, int lightLevel, CallbackInfoReturnable<Float> cir) {
        if (ModuleManager.fullbright.isEnabled()) {
            float f = (float)lightLevel / 15.0F;
            float g = f / (4.0F - 3.0F * f);
            cir.setReturnValue(Math.max(Mth.lerp(type.ambientLight(), g, 1.0F), Fullbright.minBright.getValue()));
        }
    }
}
