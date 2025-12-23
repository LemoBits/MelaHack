package thunder.hack.injection;

import net.minecraft.entity.decoration.ArmorStandEntity;
import thunder.hack.core.manager.client.ModuleManager;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer<T extends Entity> {
    @Inject(method = "hasLabel", at = @At("HEAD"), cancellable = true)
    private void hasLabelHook(T entity, double squaredDistanceToCamera, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof ArmorStandEntity && ModuleManager.noRender.isEnabled() && ModuleManager.noRender.noArmorStands.getValue()) {
            cir.setReturnValue(false);
            return;
        }

        if (entity instanceof PlayerEntity && ModuleManager.nameTags.isEnabled()) {
            cir.setReturnValue(false);
        }
    }
}
