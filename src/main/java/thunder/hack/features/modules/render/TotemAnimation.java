package thunder.hack.features.modules.render;

import thunder.hack.utility.render.compat.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public class TotemAnimation extends Module {
    public TotemAnimation() {
        super("TotemAnimation", Category.RENDER);
    }

    private final Setting<Mode> mode = new Setting<>("Mode", Mode.FadeOut);
    private final Setting<Integer> speed = new Setting<>("Speed", 40, 1, 100);

    private ItemStack floatingItem = null;
    private int floatingItemTimeLeft;

    public void showFloatingItem(ItemStack floatingItem) {
        this.floatingItem = floatingItem;
        floatingItemTimeLeft = getTime();
    }

    @Override
    public void onUpdate() {
        if (floatingItemTimeLeft > 0) {
            --floatingItemTimeLeft;
            if (floatingItemTimeLeft == 0) {
                floatingItem = null;
            }
        }
    }

    public void renderFloatingItem(GuiGraphicsExtractor context, float tickDelta) {
        if (floatingItem != null && floatingItemTimeLeft > 0 && !mode.is(Mode.Off)) {
            int scaledWidth = mc.getWindow().getGuiScaledWidth();
            int scaledHeight = mc.getWindow().getGuiScaledHeight();

            int elapsedTime = getTime() - floatingItemTimeLeft;
            float animationProgress = ((float) elapsedTime + tickDelta) / (float) getTime();
            float progressSquared = animationProgress * animationProgress;
            float progressCubed = animationProgress * progressSquared;
            float oscillationFactor = 10.25F * progressCubed * progressSquared - 24.95F * progressSquared * progressSquared + 25.5F * progressCubed - 13.8F * progressSquared + 4.0F * animationProgress;
            float oscillationRadians = oscillationFactor * 3.1415927F;
            RenderSystem.enableDepthTest();
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            PoseStack matrixStack = new PoseStack();
            matrixStack.pushPose();
            float adjustedProgress = ((float) elapsedTime + tickDelta);
            float scale = 50.0F + 175.0F * Mth.sin(oscillationRadians);

            switch (mode.getValue()) {
                case FadeOut -> {
                    final float x2 = (float) (Math.sin(((adjustedProgress * 112) / 180f)) * 100);
                    final float y2 = (float) (Math.cos(((adjustedProgress * 112) / 180f)) * 50);
                    matrixStack.translate((float) (scaledWidth / 2) + x2, (float) (scaledHeight / 2) + y2, -50.0F);
                    matrixStack.scale(scale, -scale, scale);
                }

                case Size -> {
                    matrixStack.translate((float) (scaledWidth / 2), (float) (scaledHeight / 2), -50.0F);
                    matrixStack.scale(scale, -scale, scale);
                }

                case Otkisuli -> {
                    matrixStack.translate((float) (scaledWidth / 2), (float) (scaledHeight / 2), -50.0F);
                    matrixStack.mulPose(Axis.XP.rotationDegrees(adjustedProgress * 2));
                    matrixStack.mulPose(Axis.ZP.rotationDegrees(adjustedProgress * 2));
                    matrixStack.scale(200 - adjustedProgress * 1.5f, -200 + adjustedProgress * 1.5f, 200 - adjustedProgress * 1.5f);
                }

                case Insert -> {
                    matrixStack.translate((float) (scaledWidth / 2), (float) (scaledHeight / 2), -50.0F);
                    matrixStack.mulPose(Axis.XP.rotationDegrees(adjustedProgress * 3));
                    matrixStack.scale(200 - adjustedProgress * 1.5f, -200 + adjustedProgress * 1.5f, 200 - adjustedProgress * 1.5f);
                }

                case Fall -> {
                    float downFactor = (float) (Math.pow(adjustedProgress, 3) * 0.2f);
                    matrixStack.translate((float) (scaledWidth / 2), (float) (scaledHeight / 2) + downFactor, -50.0F);
                    matrixStack.mulPose(Axis.ZP.rotationDegrees(adjustedProgress * 5));
                    matrixStack.scale(200 - adjustedProgress * 1.5f, -200 + adjustedProgress * 1.5f, 200 - adjustedProgress * 1.5f);
                }

                case Rocket -> {
                    float downFactor = (float) (Math.pow(adjustedProgress, 3) * 0.2f) - 20;
                    matrixStack.translate((float) (scaledWidth / 2), (float) (scaledHeight / 2) - downFactor, -50.0F);
                    matrixStack.mulPose(Axis.YP.rotationDegrees(adjustedProgress * floatingItemTimeLeft * 2));
                    matrixStack.scale(200 - adjustedProgress * 1.5f, -200 + adjustedProgress * 1.5f, 200 - adjustedProgress * 1.5f);
                }

                case Roll -> {
                    float rightFactor = (float) (Math.pow(adjustedProgress, 2) * 4.5f);
                    matrixStack.translate((float) (scaledWidth / 2) + rightFactor, (float) (scaledHeight / 2), -50.0F);
                    matrixStack.mulPose(Axis.ZP.rotationDegrees(adjustedProgress * 40));
                    matrixStack.scale(200 - adjustedProgress * 1.5f, -200 + adjustedProgress * 1.5f, 200 - adjustedProgress * 1.5f);
                }
            }

            RenderSystem.setShaderColor(1f, 1f, 1f, 1f - animationProgress);
            context.item(floatingItem, scaledWidth / 2 - 8, scaledHeight / 2 - 8);
            matrixStack.popPose();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            RenderSystem.disableDepthTest();
        }
    }

    private int getTime() {
        int invertedSpeed = 101 - speed.getValue();

        if (mode.is(Mode.FadeOut))
            return invertedSpeed / 4;

        if (mode.is(Mode.Insert))
            return invertedSpeed / 2;

        return invertedSpeed;
    }

    private enum Mode {
        FadeOut, Size, Otkisuli, Insert, Fall, Rocket, Roll, Off
    }
}
