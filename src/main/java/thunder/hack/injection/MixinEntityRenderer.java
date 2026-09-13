package thunder.hack.injection;

import thunder.hack.core.manager.client.ModuleManager;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer<T extends Entity> {
    @Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true)
    private void hasLabelHook(T entity, double squaredDistanceToCamera, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof ArmorStand && ModuleManager.noRender.isEnabled() && ModuleManager.noRender.noArmorStands.getValue()) {
            cir.setReturnValue(false);
            return;
        }

        if (entity instanceof Player && ModuleManager.nameTags.isEnabled()) {
            cir.setReturnValue(false);
        }
    }
}
