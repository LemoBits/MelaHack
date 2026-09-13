package thunder.hack.injection;

import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.render.Tooltips;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

@Mixin(Item.class)
public class MixinShulkerBoxBlock {
    @Inject(method = "appendHoverText", at = @At("HEAD"), cancellable = true)
    private void onAppendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag options, CallbackInfo ci) {
        if (ModuleManager.tooltips == null) return;
        if (Tooltips.storage.getValue()) ci.cancel();
    }
}
