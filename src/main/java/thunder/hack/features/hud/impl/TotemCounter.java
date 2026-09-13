package thunder.hack.features.hud.impl;

import thunder.hack.utility.render.compat.RenderSystem;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.TotemPopEvent;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.features.hud.HudElement;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;

import java.awt.*;

public class TotemCounter extends HudElement {
    public TotemCounter() {
        super("TotemCounter", 0, 0);
    }

    private float angle, prevAngle;

    public void onRender2D(GuiGraphicsExtractor context) {
        if (getItemCount(Items.TOTEM_OF_UNDYING) == 0)
            return;

        float xPos = ModuleManager.crosshair.getAnimatedPosX();
        float yPos = ModuleManager.crosshair.getAnimatedPosY();

        float factor = Math.abs(angle < 0 ? angle / 15f : 0f);

        context.pose().pushMatrix();
        context.pose().translate((float) (xPos), (float) (yPos));
        context.pose().rotate(-((float) Math.toRadians(-Render2DEngine.interpolateFloat(prevAngle, angle, Render3DEngine.getTickDelta()))));
        context.pose().translate((float) (-xPos), (float) (-yPos));

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        context.pose().translate((float) (xPos - 36), (float) (yPos - 9));
        context.item(Items.TOTEM_OF_UNDYING.getDefaultInstance(), 0, 0);
        context.pose().translate((float) (-(xPos - 36)), (float) (-(yPos - 9)));
        RenderSystem.setShaderColor(1f, 1f - factor, 1f - factor, 1f);

        if (factor > 0)
            Render2DEngine.drawBlurredShadow(context.pose(), xPos - 34, yPos - 6, 11, 11, 8, Render2DEngine.injectAlpha(new Color(0xFF0000), (int) (255 * factor)));

        FontRenderers.sf_bold_mini.drawCenteredString(context.pose(), getItemCount(Items.TOTEM_OF_UNDYING) + "",xPos - 28, yPos + 8, -1);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        context.pose().popMatrix();
    }

    @EventHandler
    public void onTotemPop(TotemPopEvent e) {
        if (e.getEntity() == mc.player)
            angle = -15;
    }

    @Override
    public void onUpdate() {
        prevAngle = angle;
        if (angle < 0)
            angle++;
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
