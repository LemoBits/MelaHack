package thunder.hack.features.hud.impl;

import thunder.hack.gui.font.FontRenderers;
import thunder.hack.features.hud.HudElement;
import thunder.hack.gui.windows.WindowsScreen;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.setting.Setting;
import thunder.hack.utility.render.Render2DEngine;

import java.awt.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class Hotbar extends HudElement {
    public Hotbar() {
        super("Hotbar", 0, 0);
    }

    public static final Setting<Mode> lmode = new Setting<>("LeftHandMode", Mode.Merged);

    public enum Mode {
        Merged, Separately
    }

    public void onRender2D(GuiGraphics context) {
        if (mc.screen instanceof WindowsScreen)
            return;

        Player playerEntity = mc.player;
        if (playerEntity != null) {
            var matrices = context.pose();
            int i = mc.getWindow().getGuiScaledWidth() / 2;

            if (mc.player.getOffhandItem().isEmpty()) {
                Render2DEngine.drawHudBase(matrices, i - 90, mc.getWindow().getGuiScaledHeight() - 25, 180, 20, HudEditor.hudRound.getValue());
            } else if (lmode.getValue() == Mode.Merged) {
                Render2DEngine.drawHudBase(matrices, i - 111, mc.getWindow().getGuiScaledHeight() - 25, 201, 20, HudEditor.hudRound.getValue());

                if (HudEditor.hudStyle.is(HudEditor.HudStyle.Blurry)) {
                    Render2DEngine.drawRect(context.pose(), i - 109 + 18, mc.getWindow().getGuiScaledHeight() - 23, 0.5f, 15, new Color(0x44FFFFFF, true));
                } else {
                    Render2DEngine.verticalGradient(matrices, i - 109 + 18, mc.getWindow().getGuiScaledHeight() - 22 + 1 - 4, i - 108 + 18 - 0.5f, mc.getWindow().getGuiScaledHeight() - 11 + 1 - 4, Render2DEngine.injectAlpha(HudEditor.textColor.getValue().getColorObject(), 0), HudEditor.textColor.getValue().getColorObject());
                    Render2DEngine.verticalGradient(matrices, i - 109 + 18, mc.getWindow().getGuiScaledHeight() - 11 - 4, i - 108 + 18 - 0.5f, mc.getWindow().getGuiScaledHeight() - 5, HudEditor.textColor.getValue().getColorObject(), Render2DEngine.injectAlpha(HudEditor.textColor.getValue().getColorObject(), 0));
                }
            } else {
                Render2DEngine.drawHudBase(matrices, i - 90, mc.getWindow().getGuiScaledHeight() - 25, 180, 20, HudEditor.hudRound.getValue());
                Render2DEngine.drawHudBase(matrices, i - 112.5f, mc.getWindow().getGuiScaledHeight() - 25, 20, 20, HudEditor.hudRound.getValue());
            }

            Color c = HudEditor.hudStyle.is(HudEditor.HudStyle.Blurry) ? new Color(0x7C151515, true) : new Color(0x7C2F2F2F, true);

            Render2DEngine.drawRect(matrices, i - 88 + playerEntity.getInventory().getSelectedSlot() * 19.8f, mc.getWindow().getGuiScaledHeight() - 24, 17, 17, HudEditor.hudRound.getValue(), 0.7f, c, c, c, c);
        }
    }

    // Bake only items
    public static void renderHotBarItems(float tickDelta, GuiGraphics context) {
        if (mc.screen instanceof WindowsScreen)
            return;

        Player playerEntity = mc.player;
        if (playerEntity != null) {

            var matrices = context.pose();
            int i = mc.getWindow().getGuiScaledWidth() / 2;
            int o = mc.getWindow().getGuiScaledHeight() - 16 - 3;

            if (mc.player.getOffhandItem().isEmpty()) {
            } else if (lmode.getValue() == Mode.Merged) {
                renderHotbarItem(context, i - 109, o - 5, playerEntity.getOffhandItem());
            } else {
                renderHotbarItem(context, i - 111, o - 5, playerEntity.getOffhandItem());
            }

            for (int m = 0; m < 9; ++m) {
                int n = i - 90 + m * 20 + 2;
                if (m == mc.player.getInventory().getSelectedSlot())
                    renderHotbarItem(context, n, o - 7, playerEntity.getInventory().getItem(m));
                else renderHotbarItem(context, n, o - 5, playerEntity.getInventory().getItem(m));
            }
        }
    }

    private static void renderHotbarItem(GuiGraphics context, int i, int j, ItemStack itemStack) {
        if (!itemStack.isEmpty()) {
            context.pose().pushMatrix();
            context.pose().translate((float) ((float) (i + 8)), (float) ((float) (j + 12)));
            context.pose().scale(0.9f, 0.9f);
            context.pose().translate((float) ((float) (-(i + 8))), (float) ((float) (-(j + 12))));
            context.renderItem(itemStack, i, j);
            context.renderItemDecorations(mc.font, itemStack, i, j);
            context.pose().popMatrix();
        }
    }

    public static void renderXpBar(int x, Object matrices) {
        int k;
        int l;

        if (mc.player.experienceLevel > 0) {
            String string = "" + mc.player.experienceLevel;
            k = (int) ((mc.getWindow().getGuiScaledWidth() - FontRenderers.sf_bold_mini.getStringWidth(string)) / 2);
            l = mc.getWindow().getGuiScaledHeight() - 31 - 4;
            FontRenderers.sf_bold_mini.drawString(matrices, string, k, l, 8453920);
        }
    }
}
