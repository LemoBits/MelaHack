package thunder.hack.injection;

import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.player.AutoTool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.minecraft.world.item.enchantment.Enchantments.EFFICIENCY;
import static thunder.hack.core.manager.IManager.mc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

@Mixin({BlockBehaviour.class})
public abstract class MixinAbstractBlock {
    @Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
    public void calcBlockBreakingDeltaHook(BlockState state, Player player, BlockGetter world, BlockPos pos, CallbackInfoReturnable<Float> ci)  {
        if(ModuleManager.autoTool.isEnabled() && AutoTool.silent.getValue()) {
            float f = state.getDestroySpeed(world, pos);
            if (f < 0.0F) {
                ci.setReturnValue(0.0f);
            } else {
                float dig_speed = getDigSpeed(state, player.getInventory().getItem(AutoTool.itemIndex)) / f;
                ci.setReturnValue(player.getInventory().getItem(AutoTool.itemIndex).isCorrectToolForDrops(state) ? dig_speed / 30.0F : dig_speed / 100.0F);
            }
        }
    }

    public float getDigSpeed(BlockState state, ItemStack stack)
    {
        double str = stack.getDestroySpeed(state);
        int effect = EnchantmentHelper.getItemEnchantmentLevel(mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(EFFICIENCY), stack);
        return (float) Math.max(str + (str > 1.0 ? (effect * effect + 1.0) : 0.0), 0.0);
    }
}
