package thunder.hack.injection;

import net.minecraft.client.Camera;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.render.NoRender;

@Mixin(ParticleEngine.class)
public class MixinParticleManager {
    @Shadow
    @Final
    private TextureAtlas textureAtlas;

    @Inject(at = @At("HEAD"), method = "add(Lnet/minecraft/client/particle/Particle;)V", cancellable = true)
    public void addParticleHook(Particle p, CallbackInfo e) {
        NoRender nR = ModuleManager.noRender;

        if(!nR.isEnabled())
            return;
        
        if (nR.elderGuardian.getValue() && p instanceof MobAppearanceParticle)
            e.cancel();

        if (nR.explosions.getValue() && p instanceof HugeExplosionParticle)
            e.cancel();

        if (nR.campFire.getValue() && p instanceof CampfireSmokeParticle)
            e.cancel();

        if (nR.breakParticles.getValue() && p instanceof TerrainParticle)
            e.cancel();

        if (nR.fireworks.getValue() && (p instanceof FireworkParticles.Starter || p instanceof FireworkParticles.OverlayParticle))
            e.cancel();
    }

    @Inject(at = @At("HEAD"), method = "render")
    private void renderParticlesHook(Camera camera, float tickDelta, MultiBufferSource.BufferSource vertexConsumers, CallbackInfo ci) {
        textureAtlas.setFilter(false, false);
    }
}
