package thunder.hack.injection;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import thunder.hack.ThunderHack;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventCollision;

@Mixin(value = BlockCollisions.class, priority = 800)
public abstract class MixinBlockCollisionSpliterator {
    // я надеюсь это никто не будет редиректить
    // I hope no one will redirect this
    @Redirect(method = "computeNext", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/BlockGetter;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState computeNextHook(BlockGetter instance, BlockPos blockPos) {
        if(!ModuleManager.antiWeb.isEnabled() && !ModuleManager.phase.isEnabled() && !ModuleManager.jesus.isEnabled())
            return instance.getBlockState(blockPos);
        EventCollision event = new EventCollision(instance.getBlockState(blockPos), blockPos);
        ThunderHack.EVENT_BUS.post(event);
        return event.getState();
    }
}

