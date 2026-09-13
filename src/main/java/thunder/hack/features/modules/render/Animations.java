package thunder.hack.features.modules.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import thunder.hack.ThunderHack;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventHeldItemRenderer;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.injection.accesors.IHeldItemRenderer;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.combat.Aura;
import thunder.hack.setting.Setting;

public class Animations extends Module {
    public Animations() {
        super("Animations", Category.RENDER);
    }

    private final Setting<Boolean> onlyaura = new Setting<>("OnlyAura", false);
    public Setting<Boolean> oldAnimationsM = new Setting<>("DisableSwapMain", true);
    public Setting<Boolean> oldAnimationsOff = new Setting<>("DisableSwapOff", true);
    private final Setting<Mode> mode = new Setting<Mode>("Mode", Mode.Default);
    public static Setting<Boolean> slowAnimation = new Setting<>("SlowAnimation", true);
    public static Setting<Integer> slowAnimationVal = new Setting<>("SlowValue", 12, 1, 50);

    public boolean flip;

    private enum Mode {
        Normal, Default, One, Two, Three, Four, Five, Six, Seven, Eight, Nine, Ten, Eleven, Twelve, Thirteen, Fourteen
    }

    public boolean shouldAnimate() {
        return isEnabled()
                && (!onlyaura.getValue() || ModuleManager.aura.isEnabled() && Aura.target != null)
                && mode.getValue() != Mode.Normal;
    }

    public boolean shouldChangeAnimationDuration() {
        return isEnabled()
                && (!onlyaura.getValue() || ModuleManager.aura.isEnabled() && Aura.target != null);
    }

    @Override
    public void onUpdate() {
        if (fullNullCheck()) return;
        if (oldAnimationsM.getValue() && ((IHeldItemRenderer) mc.getEntityRenderDispatcher().getItemInHandRenderer()).getEquippedProgressMainHand() <= 1f) {
            ((IHeldItemRenderer) mc.getEntityRenderDispatcher().getItemInHandRenderer()).setEquippedProgressMainHand(1f);
            ((IHeldItemRenderer) mc.getEntityRenderDispatcher().getItemInHandRenderer()).setItemStackMainHand(mc.player.getMainHandItem());
        }

        if (oldAnimationsOff.getValue() && ((IHeldItemRenderer) mc.getEntityRenderDispatcher().getItemInHandRenderer()).getEquippedProgressOffHand() <= 1f) {
            ((IHeldItemRenderer) mc.getEntityRenderDispatcher().getItemInHandRenderer()).setEquippedProgressOffHand(1f);
            ((IHeldItemRenderer) mc.getEntityRenderDispatcher().getItemInHandRenderer()).setItemStackOffHand(mc.player.getOffhandItem());
        }
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send e) {
        if (e.getPacket() instanceof ServerboundSwingPacket)
            flip = !flip;
    }

    private void renderSwordAnimation(PoseStack matrices, float f, float swingProgress, float equipProgress, HumanoidArm arm) {
        if (arm == HumanoidArm.LEFT && (mode.getValue() == Mode.Eleven || mode.getValue() == Mode.Ten || mode.getValue() == Mode.Nine || mode.getValue() == Mode.Three || mode.getValue() == Mode.Thirteen || mode.getValue() == Mode.Fourteen)) {
            applyEquipOffset(matrices, arm, equipProgress);
            matrices.translate(-ModuleManager.viewModel.positionMainX.getValue(), ModuleManager.viewModel.positionMainY.getValue(), ModuleManager.viewModel.positionMainZ.getValue());
            applySwingOffset(matrices, arm, swingProgress);
            matrices.translate(ModuleManager.viewModel.positionMainX.getValue(), -ModuleManager.viewModel.positionMainY.getValue(), -ModuleManager.viewModel.positionMainZ.getValue());
            return;
        }


        switch (mode.getValue()) {
            case Default -> {
                applyEquipOffset(matrices, arm, equipProgress);
                translateToViewModelOff(matrices);
                applySwingOffset(matrices, arm, swingProgress);
                translateBacklOff(matrices);
            }
            case One -> {
                float n = -0.4F * Mth.sin(Mth.sqrt(swingProgress) * 3.1415927F);
                applyEquipOffset(matrices, arm, n);
                int i = arm == HumanoidArm.RIGHT ? 1 : -1;
                translateToViewModel(matrices);
                float f1 = Mth.sin(swingProgress * swingProgress * 3.1415927F);
                matrices.mulPose(Axis.YP.rotationDegrees((float) i * (45.0F + f1 * -20.0F)));
                float g = Mth.sin(Mth.sqrt(swingProgress) * 3.1415927F);
                matrices.mulPose(Axis.ZP.rotationDegrees((float) i * g * -20.0F));
                matrices.mulPose(Axis.XP.rotationDegrees(g * 0.0F));
                matrices.mulPose(Axis.YP.rotationDegrees((float) i * -45.0F));
                translateBack(matrices);
            }
            case Two ->
                    applyEquipOffset(matrices, arm, 0.2F * Mth.sin(Mth.sqrt(swingProgress) * 6.2831855F));
            case Three -> {
                float n = -0.4F * Mth.sin(Mth.sqrt(swingProgress) * 3.1415927F);
                float g = Mth.sin(Mth.sqrt(swingProgress) * 3.1415927F);
                applyEquipOffset(matrices, arm, n);
                int i = arm == HumanoidArm.RIGHT ? 1 : -1;
                translateToViewModel(matrices);
                matrices.mulPose(Axis.YP.rotationDegrees((float) i * (45.0F + f * -20.0F)));
                matrices.mulPose(Axis.ZP.rotationDegrees((float) i * g * -70.0F));
                matrices.mulPose(Axis.XP.rotationDegrees(-70f));
                matrices.mulPose(Axis.YP.rotationDegrees((float) i * -45.0F));
                translateBack(matrices);
            }
            case Four -> {
                applyEquipOffset(matrices, arm, 0);
                translateToViewModel(matrices);
                matrices.mulPose(Axis.XP.rotationDegrees(swingProgress > 0 ? -Mth.sin(swingProgress * 13f) * 37f : 0));
                translateBack(matrices);
            }
            case Five -> {
                applyEquipOffset(matrices, arm, 0);
                int i = arm == HumanoidArm.RIGHT ? 1 : -1;
                float g = Mth.sin(Mth.sqrt(swingProgress) * 3.1415927F);
                translateToViewModel(matrices);
                matrices.mulPose(Axis.ZP.rotationDegrees((float) i * g * -20.0F));
                translateBack(matrices);
            }
            case Six -> {
                applyEquipOffset(matrices, arm, equipProgress);
                translateToViewModel(matrices);
                matrices.mulPose(Axis.XP.rotationDegrees(swingProgress * (flip ? 360.0F : -360)));
                translateBack(matrices);
            }
            case Eight -> {
                applyEquipOffset(matrices, arm, equipProgress);
                translateToViewModel(matrices);
                matrices.mulPose(Axis.XP.rotationDegrees(swingProgress * -360));
                translateBack(matrices);
            }
            case Seven -> {
                applyEquipOffset(matrices, arm, equipProgress);
                float a = -Mth.sin(swingProgress * 3f) / 2f + 1f;
                matrices.scale(a, a, a);
            }
            case Nine -> {
                float g = Mth.sin(Mth.sqrt(swingProgress) * 3.1415927F);
                applyEquipOffset(matrices, arm, 0);
                translateToViewModel(matrices);
                matrices.mulPose(Axis.XP.rotationDegrees(50f));
                matrices.mulPose(Axis.YP.rotationDegrees(-30f * (1f - g) - 30f));
                matrices.mulPose(Axis.ZP.rotationDegrees(110f));
                translateBack(matrices);
            }
            case Ten -> {
                float g = Mth.sin(Mth.sqrt(swingProgress) * 3.1415927F);
                matrices.translate(0, 0, 0);
                applyEquipOffset(matrices, arm, 0);
                translateToViewModel(matrices);
                matrices.mulPose(Axis.XP.rotationDegrees(50f));
                matrices.mulPose(Axis.YP.rotationDegrees(-60f * g - 50));
                matrices.mulPose(Axis.ZP.rotationDegrees(110f));
                translateBack(matrices);
            }
            case Eleven -> {
                float g = Mth.sin(Mth.sqrt(swingProgress) * 3.1415927F);
                applyEquipOffset(matrices, arm, 0);
                translateToViewModel(matrices);
                matrices.mulPose(Axis.XP.rotationDegrees(50f));
                matrices.mulPose(Axis.YP.rotationDegrees(-60f));
                matrices.mulPose(Axis.ZP.rotationDegrees(110f + 20f * g));
                translateBack(matrices);
            }
            case Twelve -> {
                float g = Mth.sin(Mth.sqrt(swingProgress) * 3.1415927F);
                applyEquipOffset(matrices, arm, 0);
                matrices.translate(0, 0, -g / 4f);
                translateToViewModel(matrices);
                matrices.mulPose(Axis.XP.rotationDegrees(-120f));
                translateBack(matrices);
            }
            case Thirteen -> {
                float g = Mth.sin(Mth.sqrt(swingProgress) * 3.1415927F);
                applyEquipOffset(matrices, arm, 0);
                translateToViewModel(matrices);
                matrices.mulPose(Axis.XP.rotationDegrees(-Mth.sin(swingProgress * 3f) * 60f));
                matrices.mulPose(Axis.ZP.rotationDegrees(-60f * g));
                translateBack(matrices);
            }
            case Fourteen -> {
                if (swingProgress > 0) {
                    float g = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
                    matrices.translate(0.56F, equipProgress * -0.2f - 0.5F, -0.7F);

                    translateToViewModel(matrices);
                    matrices.mulPose(Axis.YP.rotationDegrees(45));
                    matrices.mulPose(Axis.XP.rotationDegrees(g * -85.0F));

                    if (ModuleManager.viewModel.isEnabled())
                        matrices.translate(-0.1F * ModuleManager.viewModel.scaleMain.getValue(), 0.28F * ModuleManager.viewModel.scaleMain.getValue(), 0.2F * ModuleManager.viewModel.scaleMain.getValue());
                    else
                        matrices.translate(-0.1F, 0.28F, 0.2F);

                    matrices.mulPose(Axis.XP.rotationDegrees(-85.0F));
                    translateBack(matrices);
                } else {
                    float n = -0.4f * Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
                    float m = 0.2f * Mth.sin(Mth.sqrt(swingProgress) * ((float) Math.PI * 2));
                    float f1 = -0.2f * Mth.sin(swingProgress * (float) Math.PI);
                    matrices.translate(n, m, f1);
                    applyEquipOffset(matrices, arm, equipProgress);
                    applySwingOffset(matrices, arm, swingProgress);
                }
            }
        }
    }


    public void renderFirstPersonItemCustom(AbstractClientPlayer player, float tickDelta, float pitch, InteractionHand hand, float swingProgress, ItemStack item, float equipProgress, PoseStack matrices, MultiBufferSource vertexConsumers, int light) {
        if (!player.isScoping()) {
            boolean bl = hand == InteractionHand.MAIN_HAND;
            HumanoidArm arm = bl ? player.getMainArm() : player.getMainArm().getOpposite();
            matrices.pushPose();

            boolean bl2;
            float f = 0;
            float g;
            float h;
            float j;
            if (item.is(Items.CROSSBOW)) {
                bl2 = CrossbowItem.isCharged(item);
                boolean bl3 = arm == HumanoidArm.RIGHT;
                int i = bl3 ? 1 : -1;
                if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand) {
                    applyEquipOffset(matrices, arm, equipProgress);
                    matrices.translate((float) i * -0.4785682F, -0.094387F, 0.05731531F);
                    matrices.mulPose(Axis.XP.rotationDegrees(-11.935F));
                    matrices.mulPose(Axis.YP.rotationDegrees((float) i * 65.3F));
                    matrices.mulPose(Axis.ZP.rotationDegrees((float) i * -9.785F));
                    f = (float) item.getUseDuration(mc.player) - ((float) mc.player.getUseItemRemainingTicks() - tickDelta + 1.0F);
                    g = f / (float) CrossbowItem.getChargeDuration(item, mc.player);
                    if (g > 1.0F) {
                        g = 1.0F;
                    }

                    if (g > 0.1F) {
                        h = Mth.sin((f - 0.1F) * 1.3F);
                        j = g - 0.1F;
                        float k = h * j;
                        matrices.translate(k * 0.0F, k * 0.004F, k * 0.0F);
                    }

                    matrices.translate(g * 0.0F, g * 0.0F, g * 0.04F);
                    matrices.scale(1.0F, 1.0F, 1.0F + g * 0.2F);
                    matrices.mulPose(Axis.YN.rotationDegrees((float) i * 45.0F));
                } else {
                    f = -0.4F * Mth.sin(Mth.sqrt(swingProgress) * 3.1415927F);
                    g = 0.2F * Mth.sin(Mth.sqrt(swingProgress) * 6.2831855F);
                    h = -0.2F * Mth.sin(swingProgress * 3.1415927F);
                    matrices.translate((float) i * f, g, h);
                    applyEquipOffset(matrices, arm, equipProgress);
                    applySwingOffset(matrices, arm, swingProgress);
                    if (bl2 && swingProgress < 0.001F && bl) {
                        matrices.translate((float) i * -0.641864F, 0.0F, 0.0F);
                        matrices.mulPose(Axis.YP.rotationDegrees((float) i * 10.0F));
                    }
                }

                EventHeldItemRenderer event = new EventHeldItemRenderer(hand, item, equipProgress, matrices);
                ThunderHack.EVENT_BUS.post(event);
                renderItem(player, item, bl3 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, !bl3, matrices, vertexConsumers, light);
            } else {
                bl2 = arm == HumanoidArm.RIGHT;
                int l;
                float m = 0;
                if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand) {
                    l = bl2 ? 1 : -1;
                    switch (item.getUseAnimation()) {
                        case NONE, BLOCK -> applyEquipOffset(matrices, arm, equipProgress);
                        case EAT, DRINK -> {
                            applyEatOrDrinkTransformationCustom(matrices, tickDelta, arm, item);
                            applyEquipOffset(matrices, arm, equipProgress);
                        }
                        case BOW -> {
                            applyEquipOffset(matrices, arm, equipProgress);
                            matrices.translate((float) l * -0.2785682F, 0.18344387F, 0.15731531F);
                            matrices.mulPose(Axis.XP.rotationDegrees(-13.935F));
                            matrices.mulPose(Axis.YP.rotationDegrees((float) l * 35.3F));
                            matrices.mulPose(Axis.ZP.rotationDegrees((float) l * -9.785F));
                            m = (float) item.getUseDuration(mc.player) - ((float) mc.player.getUseItemRemainingTicks() - tickDelta + 1.0F);
                            f = m / 20.0F;
                            f = (f * f + f * 2.0F) / 3.0F;
                            if (f > 1.0F) {
                                f = 1.0F;
                            }
                            if (f > 0.1F) {
                                g = Mth.sin((m - 0.1F) * 1.3F);
                                h = f - 0.1F;
                                j = g * h;
                                matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                            }
                            matrices.translate(f * 0.0F, f * 0.0F, f * 0.04F);
                            matrices.scale(1.0F, 1.0F, 1.0F + f * 0.2F);
                            matrices.mulPose(Axis.YN.rotationDegrees((float) l * 45.0F));
                        }
                        case SPEAR -> {
                            applyEquipOffset(matrices, arm, equipProgress);
                            matrices.translate((float) l * -0.5F, 0.7F, 0.1F);
                            matrices.mulPose(Axis.XP.rotationDegrees(-55.0F));
                            matrices.mulPose(Axis.YP.rotationDegrees((float) l * 35.3F));
                            matrices.mulPose(Axis.ZP.rotationDegrees((float) l * -9.785F));
                            m = (float) item.getUseDuration(mc.player) - ((float) mc.player.getUseItemRemainingTicks() - tickDelta + 1.0F);
                            f = m / 10.0F;
                            if (f > 1.0F) {
                                f = 1.0F;
                            }
                            if (f > 0.1F) {
                                g = Mth.sin((m - 0.1F) * 1.3F);
                                h = f - 0.1F;
                                j = g * h;
                                matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                            }
                            matrices.translate(0.0F, 0.0F, f * 0.2F);
                            matrices.scale(1.0F, 1.0F, 1.0F + f * 0.2F);
                            matrices.mulPose(Axis.YN.rotationDegrees((float) l * 45.0F));
                        }
                        case BRUSH -> applyBrushTransformation(matrices, tickDelta, arm, item, equipProgress);
                    }
                } else if (player.isAutoSpinAttack()) {
                    applyEquipOffset(matrices, arm, equipProgress);
                    l = bl2 ? 1 : -1;
                    matrices.translate((float) l * -0.4F, 0.8F, 0.3F);
                    matrices.mulPose(Axis.YP.rotationDegrees((float) l * 65.0F));
                    matrices.mulPose(Axis.ZP.rotationDegrees((float) l * -85.0F));
                } else {
                    renderSwordAnimation(matrices, f, swingProgress, equipProgress, arm);
                }
                EventHeldItemRenderer event = new EventHeldItemRenderer(hand, item, equipProgress, matrices);
                ThunderHack.EVENT_BUS.post(event);
                renderItem(player, item, bl2 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, !bl2, matrices, vertexConsumers, light);
            }
            matrices.popPose();
        }
    }

    private void applyBrushTransformation(PoseStack matrices, float tickDelta, HumanoidArm arm, @NotNull ItemStack stack, float equipProgress) {
        applyEquipOffset(matrices, arm, equipProgress);
        float f = (float) mc.player.getUseItemRemainingTicks() - tickDelta + 1.0F;
        float g = 1.0F - f / (float) stack.getUseDuration(mc.player);
        float m = -15.0F + 75.0F * Mth.cos(g * 45.0F * 3.1415927F);

        if (arm != HumanoidArm.RIGHT) {
            matrices.translate(0.1, 0.83, 0.35);
            matrices.mulPose(Axis.XP.rotationDegrees(-80.0F));
            matrices.mulPose(Axis.YP.rotationDegrees(-90.0F));
            matrices.mulPose(Axis.XP.rotationDegrees(m));
            matrices.translate(-0.3, 0.22, 0.35);
        } else {
            matrices.translate(-0.25, 0.22, 0.35);
            matrices.mulPose(Axis.XP.rotationDegrees(-80.0F));
            matrices.mulPose(Axis.YP.rotationDegrees(90.0F));
            matrices.mulPose(Axis.ZP.rotationDegrees(0.0F));
            matrices.mulPose(Axis.XP.rotationDegrees(m));
        }
    }

    private void applyEquipOffset(@NotNull PoseStack matrices, HumanoidArm arm, float equipProgress) {
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        matrices.translate((float) i * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
    }

    private void applySwingOffset(@NotNull PoseStack matrices, HumanoidArm arm, float swingProgress) {
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        float f = Mth.sin(swingProgress * swingProgress * 3.1415927F);
        matrices.mulPose(Axis.YP.rotationDegrees((float) i * (45.0F + f * -20.0F)));
        float g = Mth.sin(Mth.sqrt(swingProgress) * 3.1415927F);
        matrices.mulPose(Axis.ZP.rotationDegrees((float) i * g * -20.0F));
        matrices.mulPose(Axis.XP.rotationDegrees(g * -80.0F));
        matrices.mulPose(Axis.YP.rotationDegrees((float) i * -45.0F));
    }

    public void renderItem(LivingEntity entity, ItemStack stack, ItemDisplayContext renderMode, boolean leftHanded, PoseStack matrices, MultiBufferSource vertexConsumers, int light) {
        if (stack.isEmpty()) {
            return;
        }
        mc.getItemRenderer().renderStatic(entity, stack, renderMode, matrices, vertexConsumers, entity.level(), light, OverlayTexture.NO_OVERLAY, entity.getId() + renderMode.ordinal());
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

    private void translateToViewModel(PoseStack matrices) {
        if (ModuleManager.viewModel.isEnabled())
            matrices.translate(ModuleManager.viewModel.positionMainX.getValue(), ModuleManager.viewModel.positionMainY.getValue(), ModuleManager.viewModel.positionMainZ.getValue());
    }

    private void translateToViewModelOff(PoseStack matrices) {
        if (ModuleManager.viewModel.isEnabled())
            matrices.translate(-ModuleManager.viewModel.positionMainX.getValue(), ModuleManager.viewModel.positionMainY.getValue(), ModuleManager.viewModel.positionMainZ.getValue());
    }

    private void translateBack(PoseStack matrices) {
        if (ModuleManager.viewModel.isEnabled())
            matrices.translate(-ModuleManager.viewModel.positionMainX.getValue(), -ModuleManager.viewModel.positionMainY.getValue(), -ModuleManager.viewModel.positionMainZ.getValue());
    }

    private void translateBacklOff(PoseStack matrices) {
        if (ModuleManager.viewModel.isEnabled())
            matrices.translate(ModuleManager.viewModel.positionMainX.getValue(), -ModuleManager.viewModel.positionMainY.getValue(), -ModuleManager.viewModel.positionMainZ.getValue());
    }
}
