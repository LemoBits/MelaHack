package thunder.hack.injection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thunder.hack.ThunderHack;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.UseTridentEvent;

import static thunder.hack.ThunderHack.mc;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

@Mixin(TridentItem.class)
public abstract class MixinTridentItem {

    @Inject(method = "releaseUsing", at = @At(value = "HEAD"), cancellable = true)
    public void onStoppedUsingHook(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks, CallbackInfoReturnable<Boolean> cir) {
        if (user == mc.player && EnchantmentHelper.getTridentSpinAttackStrength(stack, mc.player) > 0) {
            UseTridentEvent e = new UseTridentEvent();
            ThunderHack.EVENT_BUS.post(e);
            if (e.isCancelled()) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "use", at = @At(value = "HEAD"), cancellable = true)
    public void useHook(Level world, Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack itemStack = user.getItemInHand(hand);
        if (EnchantmentHelper.getTridentSpinAttackStrength(itemStack, user) > 0 && !user.isInWaterOrRain() && ModuleManager.tridentBoost.isEnabled() && ModuleManager.tridentBoost.anyWeather.getValue()) {
            user.startUsingItem(hand);
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }
}
