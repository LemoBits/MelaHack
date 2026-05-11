package thunder.hack.injection;

import net.minecraft.client.particle.*;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.SpriteAtlasTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.render.NoRender;

@Mixin(ParticleManager.class)
public class MixinParticleManager {
    @Shadow
    @Final
    private SpriteAtlasTexture particleAtlasTexture;

    @Inject(at = @At("HEAD"), method = "addParticle(Lnet/minecraft/client/particle/Particle;)V", cancellable = true)
    public void addParticleHook(Particle p, CallbackInfo e) {
        NoRender nR = ModuleManager.noRender;

        if(!nR.isEnabled())
            return;
        
        if (nR.elderGuardian.getValue() && p instanceof ElderGuardianAppearanceParticle)
            e.cancel();

        if (nR.explosions.getValue() && p instanceof ExplosionLargeParticle)
            e.cancel();

        if (nR.campFire.getValue() && p instanceof CampfireSmokeParticle)
            e.cancel();

        if (nR.breakParticles.getValue() && p instanceof BlockDustParticle)
            e.cancel();

        if (nR.fireworks.getValue() && (p instanceof FireworksSparkParticle.FireworkParticle || p instanceof FireworksSparkParticle.Flash))
            e.cancel();
    }

    @Inject(at = @At("HEAD"), method = "renderParticles")
    private void renderParticlesHook(Camera camera, float tickDelta, VertexConsumerProvider.Immediate vertexConsumers, CallbackInfo ci) {
        particleAtlasTexture.setFilter(false, false);
    }
}
