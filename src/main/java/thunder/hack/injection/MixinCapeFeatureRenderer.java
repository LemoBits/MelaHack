package thunder.hack.injection;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(CapeLayer.class)
public class MixinCapeFeatureRenderer {
    @ModifyVariable(method = "render", at = @At("STORE"), ordinal = 6)
    private float renderHook(float n, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int i, AbstractClientPlayer abstractClientPlayerEntity, float f, float g, float h, float j, float k, float l) {
        return Mth.lerp(h, ((thunder.hack.injection.accesors.ILivingEntity) abstractClientPlayerEntity).getLastBodyYaw(), abstractClientPlayerEntity.yBodyRot);
    }
}
