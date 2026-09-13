package thunder.hack.injection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.movement.AntiWeb;
import thunder.hack.utility.player.InteractionUtility;

import static thunder.hack.core.manager.IManager.mc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(WebBlock.class)
public class MixinCobwebBlock {
    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    public void onEntityCollisionHook(BlockState state, Level world, BlockPos pos, Entity entity, InsideBlockEffectApplier handler, CallbackInfo ci) {
        if (ModuleManager.antiWeb.isEnabled() && AntiWeb.mode.getValue() == AntiWeb.Mode.Ignore && entity == mc.player) {
            ci.cancel();
            if (AntiWeb.grim.getValue())
                InteractionUtility.sendSequencedPacket(id -> new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP, id));
        }
    }
}