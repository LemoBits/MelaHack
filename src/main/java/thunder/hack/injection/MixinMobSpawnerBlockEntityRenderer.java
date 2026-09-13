package thunder.hack.injection;

import net.minecraft.client.renderer.blockentity.SpawnerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.Module;

@Mixin(SpawnerRenderer.class)
public class MixinMobSpawnerBlockEntityRenderer {

    @Inject(method = "submitEntityInSpawner", at = @At("HEAD"), cancellable = true)
    private static void renderHook(CallbackInfo ci) {
        if (!Module.fullNullCheck() && ModuleManager.noRender.isOn() && ModuleManager.noRender.spawnerEntity.getValue())
            ci.cancel();
    }
}
