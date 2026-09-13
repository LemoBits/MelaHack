package thunder.hack.features.hud.impl;

import thunder.hack.utility.render.compat.RenderSystem;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventEatFood;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.features.hud.HudElement;
import thunder.hack.setting.Setting;
import thunder.hack.utility.math.MathUtility;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;

import java.awt.*;

public class GapplesHud extends HudElement {
    public GapplesHud() {
        super("GapplesHud", 0, 0);
    }

    private float angle, prevAngle;

    private final Setting<Boolean> crapple = new Setting<>("Crapple", true);

    public void onRender2D(GuiGraphics context) {
        Item targetItem = crapple.getValue() ? Items.GOLDEN_APPLE : Items.ENCHANTED_GOLDEN_APPLE;

        if (getItemCount(targetItem) == 0)
            return;

        float xPos = ModuleManager.crosshair.getAnimatedPosX();
        float yPos = ModuleManager.crosshair.getAnimatedPosY();

        float factor = angle > 0 ? angle / 15f : 0f;
        float factor2 = 1f - mc.player.getTicksUsingItem() / 40f;

        if (mc.player.getUseItem().getItem() != targetItem)
            factor2 = 1f;

        factor2 = MathUtility.clamp(factor2, 0.01f, 1f);

        context.pose().pushMatrix();
        context.pose().translate((float) (xPos), (float) (yPos));
        context.pose().rotate(-((float) Math.toRadians(-Render2DEngine.interpolateFloat(prevAngle, angle, Render3DEngine.getTickDelta()))));
        context.pose().translate((float) (-xPos), (float) (-yPos));

        RenderSystem.setShaderColor(0.3f, 0.3f, 0.3f, 1f);
        context.pose().translate((float) (xPos + 20), (float) (yPos - 9));
        context.renderItem(targetItem.getDefaultInstance(), 0, 0);
        context.pose().translate((float) (-(xPos + 20)), (float) (-(yPos - 9)));
        RenderSystem.setShaderColor(1f, 1f - factor, 1f - factor, 1f);

        context.pose().translate((float) ((xPos + 28)), (float) ((yPos - 1)));
        context.pose().scale(factor2, factor2);
        context.renderItem(targetItem.getDefaultInstance(), -8, -8);
        context.pose().scale(factor2 != 0 ? 1f / factor2 : 1f, factor2 != 0 ? 1f / factor2 : 1f);
        context.pose().translate((float) (-(xPos + 28)), (float) (-(yPos - 1)));

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        if (factor > 0)
            Render2DEngine.drawBlurredShadow(context.pose(), xPos + 22, yPos - 6, 11, 11, 8, Render2DEngine.injectAlpha(new Color(0xFF1500), (int) (255 * factor)));

        FontRenderers.sf_bold_mini.drawCenteredString(context.pose(), getItemCount(targetItem) + "", xPos + 28.5f, yPos + 8, -1);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        context.pose().popMatrix();
    }

    @EventHandler
    public void onEatFood(EventEatFood e) {
        if (e.getFood().getItem() == Items.GOLDEN_APPLE || e.getFood().getItem() == Items.ENCHANTED_GOLDEN_APPLE)
            angle = 15;
    }

    @Override
    public void onUpdate() {
        prevAngle = angle;
        if (angle > 0)
            angle--;
    }

    public int getItemCount(Item item) {
        if (mc.player == null) return 0;
        int n = 0;
        int n2 = 44;
        for (int i = 0; i <= n2; ++i) {
            ItemStack itemStack = mc.player.getInventory().getItem(i);
            if (itemStack.getItem() != item) continue;
            n += itemStack.getCount();
        }
        return n;
    }
}
