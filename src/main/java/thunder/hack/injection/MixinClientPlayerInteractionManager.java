package thunder.hack.injection;

import net.minecraft.world.level.block.*;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.phys.BlockHitResult;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thunder.hack.ThunderHack;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventAttackBlock;
import thunder.hack.events.impl.EventBreakBlock;
import thunder.hack.events.impl.EventClickSlot;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.player.NoInteract;
import thunder.hack.features.modules.player.SpeedMine;

import static thunder.hack.features.modules.Module.mc;

@Mixin(MultiPlayerGameMode.class)
public class MixinClientPlayerInteractionManager {

    @Shadow
    private int destroyDelay;

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void interactBlock(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        Block bs = mc.level.getBlockState(hitResult.getBlockPos()).getBlock();
        if (ModuleManager.noInteract.isEnabled() && (
                bs == Blocks.CHEST ||
                        bs == Blocks.TRAPPED_CHEST ||
                        bs == Blocks.FURNACE ||
                        bs == Blocks.ANVIL ||
                        bs == Blocks.CRAFTING_TABLE ||
                        bs == Blocks.HOPPER ||
                        bs == Blocks.JUKEBOX ||
                        bs == Blocks.NOTE_BLOCK ||
                        bs == Blocks.ENDER_CHEST ||
                        bs == Blocks.DISPENSER ||
                        bs == Blocks.DROPPER ||
                        bs == Blocks.BELL ||
                        bs instanceof ShulkerBoxBlock ||
                        bs instanceof FenceBlock ||
                        bs instanceof FenceGateBlock ||
                        bs instanceof DoorBlock ||
                        bs instanceof TrapDoorBlock)
                && (ModuleManager.aura.isEnabled() || !NoInteract.onlyAura.getValue())) {
            cir.setReturnValue(InteractionResult.PASS);
        }

        if(mc.player != null && ModuleManager.antiBallPlace.isEnabled()
                && ((mc.player.getOffhandItem().getItem() == Items.PLAYER_HEAD && hand == InteractionHand.OFF_HAND) || (mc.player.getMainHandItem().getItem() == Items.PLAYER_HEAD && hand == InteractionHand.MAIN_HAND)))
            cir.setReturnValue(InteractionResult.PASS);
    }

    @Redirect(method = "continueDestroyBlock", at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;destroyDelay:I", opcode = Opcodes.GETFIELD, ordinal = 0))
    public int updateBlockBreakingProgressHook(MultiPlayerGameMode clientPlayerInteractionManager) {
        return ModuleManager.speedMine.isEnabled() ? 0 : this.destroyDelay;
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    public void updateBlockBreakingProgress(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (ModuleManager.speedMine.isEnabled() && ModuleManager.speedMine.mode.getValue() == SpeedMine.Mode.Packet) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void attackBlockHook(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if(Module.fullNullCheck()) return;
        EventAttackBlock event = new EventAttackBlock(pos, direction);
        ThunderHack.EVENT_BUS.post(event);
        if (event.isCancelled())
            cir.setReturnValue(false);
    }

    /*
    @Inject(method = "getReachDistance", at = @At("HEAD"), cancellable = true)
    private void getReachDistanceHook(CallbackInfoReturnable<Float> cir) {
        if (ModuleManager.reach.isEnabled()) {
            cir.setReturnValue(Reach.range.getValue());
        }
    }

    @Inject(method = "hasExtendedReach", at = @At("HEAD"), cancellable = true)
    private void hasExtendedReachHook(CallbackInfoReturnable<Boolean> cir) {
        if (ModuleManager.reach.isEnabled()) {
            cir.setReturnValue(true);
        }
    }
     */

    @Inject(method = "destroyBlock", at = @At("RETURN"), cancellable = true)
    public void breakBlockHook(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if(Module.fullNullCheck()) return;
        EventBreakBlock event = new EventBreakBlock(pos);
        ThunderHack.EVENT_BUS.post(event);
        if (event.isCancelled())
            cir.setReturnValue(false);
    }

    @Inject(method = "handleInventoryMouseClick", at = @At("HEAD"), cancellable = true)
    public void clickSlotHook(int syncId, int slotId, int button, ClickType actionType, Player player, CallbackInfo ci) {
        if(Module.fullNullCheck()) return;
        EventClickSlot event = new EventClickSlot(actionType, slotId, button, syncId);
        ThunderHack.EVENT_BUS.post(event);
        if (event.isCancelled())
            ci.cancel();
    }
}
