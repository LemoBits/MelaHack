package thunder.hack.injection;

import thunder.hack.utility.render.compat.RenderSystem;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventHeldItemRenderer;
import thunder.hack.features.modules.Module;

import static thunder.hack.features.modules.Module.mc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;

@Mixin(ItemInHandRenderer.class)
public abstract class MixinHeldItemRenderer {

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"))
    private void onRenderItem(AbstractClientPlayer player, float tickDelta, float pitch, InteractionHand hand, float swingProgress, ItemStack item, float equipProgress, PoseStack matrices, SubmitNodeCollector vertexConsumers, int light, CallbackInfo ci) {
        if(Module.fullNullCheck()) return;
        EventHeldItemRenderer event = new EventHeldItemRenderer(hand, item, equipProgress, matrices);
        ThunderHack.EVENT_BUS.post(event);
    }

    @Inject(method = "renderArmWithItem", at = @At(value = "RETURN"))
    private void onRenderItemPost(AbstractClientPlayer player, float tickDelta, float pitch, InteractionHand hand, float swingProgress, ItemStack item, float equipProgress, PoseStack matrices, SubmitNodeCollector vertexConsumers, int light, CallbackInfo ci) {
        if (ModuleManager.chams.isEnabled() && ModuleManager.chams.handItems.getValue())
            RenderSystem.setShaderColor(1f,1f,1f,1f);
    }

    @Inject(method = "renderArmWithItem", at = @At(value = "HEAD"), cancellable = true)
    private void onRenderItemHook(AbstractClientPlayer player, float tickDelta, float pitch, InteractionHand hand, float swingProgress, ItemStack item, float equipProgress, PoseStack matrices, SubmitNodeCollector vertexConsumers, int light, CallbackInfo ci) {
        if (Managers.MODULE != null && ModuleManager.animations.shouldAnimate() && !(item.isEmpty()) && !(item.getItem() instanceof MapItem)) {
            ci.cancel();
            ModuleManager.animations.renderFirstPersonItemCustom(player, tickDelta, pitch, hand, swingProgress, item, equipProgress, matrices, vertexConsumers, light);
        }
    }


    private void applyEatOrDrinkTransformationCustom(PoseStack matrices, float tickDelta, HumanoidArm arm, @NotNull ItemStack stack) {
        float f = (float) mc.player.getUseItemRemainingTicks() - tickDelta + 1.0F;
        float g = f / (float) stack.getUseDuration(mc.player);
        float h;
        if (g < 0.8F) {
            h = Mth.abs(Mth.cos(f / 4.0F * 3.1415927F) * 0.005F);
            matrices.translate(0.0F, h, 0.0F);
        }
        h = 1.0F - (float) Math.pow(g, 27.0);
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;

        matrices.translate(h * 0.6F * (float) i * ModuleManager.viewModel.eatX.getValue(), h * -0.5F * ModuleManager.viewModel.eatY.getValue(), h * 0.0F);
        matrices.mulPose(Axis.YP.rotationDegrees((float) i * h * 90.0F));
        matrices.mulPose(Axis.XP.rotationDegrees(h * 10.0F));
        matrices.mulPose(Axis.ZP.rotationDegrees((float) i * h * 30.0F));
    }

    @Inject(method = "applyEatTransform", at = @At(value = "HEAD"), cancellable = true)
    private void applyEatOrDrinkTransformationHook(PoseStack matrices, float tickDelta, HumanoidArm arm, ItemStack stack, Player player, CallbackInfo ci) {
        if (ModuleManager.animations.isEnabled()) {
            applyEatOrDrinkTransformationCustom(matrices, tickDelta, arm, stack);
            ci.cancel();
        }
    }
    @ModifyArgs(method = "renderHandsWithItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"))
    private void renderItem(Args args) {
        if (ModuleManager.noRender.noSwing.getValue()) {
            args.set(4, 0.0F);
        }
    }

}
