package thunder.hack.gui.clickui.impl;

import thunder.hack.core.Managers;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.gui.clickui.AbstractElement;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.setting.Setting;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.TextureStorage;

import java.awt.*;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;

import static thunder.hack.utility.render.animation.AnimationUtility.fast;

public class ModeElement extends AbstractElement {
    public Setting setting2;
    private boolean open;
    private double wheight;
    private String prevMode;

    private float animation, animation2;

    public ModeElement(Setting setting) {
        super(setting);
        this.setting2 = setting;
        prevMode = setting.currentEnumName();
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        animation = fast(animation, open ? 0 : 1, 15f);
        animation2 = fast(animation2, 1f, 10f);

        float tx = x + width - 11;
        float ty = y + 7.5f;

        var matrixStack = context.pose();

        float thetaRotation = -180f * animation;
        matrixStack.pushMatrix();

        matrixStack.translate(tx, ty);
        matrixStack.rotate((float) Math.toRadians(thetaRotation));
        matrixStack.translate(-tx, -ty);

        matrixStack.translate((x + width - 14), y + 4.5f);
        context.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TextureStorage.guiArrow, 0, 0, 0, 0, 6, 6, 6, 6);
        matrixStack.translate(-(x + width - 14), -y - 4.5f);

        matrixStack.popMatrix();

        if (setting.group != null)
            Render2DEngine.drawRect(context.pose(), x + 4, y, 1f, 17, HudEditor.getColor(1));


        FontRenderers.sf_medium_mini.drawString(matrixStack, setting2.getName(), (setting.group != null ? 2f : 0f) + (x + 6), (y + wheight / 2 - (6 / 2f)) + 3, new Color(-1).getRGB());

        if (animation2 < 0.99 && !Objects.equals(setting2.currentEnumName(), prevMode)) {
            FontRenderers.sf_medium_mini.drawString(matrixStack, prevMode, (int) (x + width - 18 - FontRenderers.sf_medium_mini.getStringWidth(prevMode)), 3 + (y + wheight / 2 - 3f) - animation2 * 5, Render2DEngine.applyOpacity(new Color(-1), animation2));
            FontRenderers.sf_medium_mini.drawString(matrixStack, setting2.currentEnumName(), (x + width - 18 - FontRenderers.sf_medium_mini.getStringWidth(setting2.currentEnumName())), 3 + (y + wheight / 2 - 3f) - animation2 * 5 + 5, Render2DEngine.applyOpacity(new Color(-1), 1f - animation2));
        } else
            FontRenderers.sf_medium_mini.drawString(matrixStack, setting2.currentEnumName(), (x + width - 18 - FontRenderers.sf_medium_mini.getStringWidth(setting.currentEnumName())), 3 + (y + wheight / 2 - 3f), new Color(-1).getRGB());

        if (open) {
            Color color = HudEditor.getColor(0);
            double offsetY = 0;
            for (int i = 0; i <= setting2.getModes().length - 1; i++) {
                FontRenderers.sf_medium_mini.drawString(matrixStack, setting2.getModes()[i], x + width / 2f - (FontRenderers.sf_medium_mini.getStringWidth(setting2.getModes()[i]) / 2f), (y + wheight + 2 + offsetY), setting2.currentEnumName().equalsIgnoreCase(setting2.getModes()[i]) ? color.getRGB() : new Color(-1).getRGB());
                offsetY += 12;
            }
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        if (Render2DEngine.isHovered(mouseX, mouseY, x, y, width, wheight)) {
            if (button == 0) {
                prevMode = setting2.currentEnumName();
                animation2 = 0;
                setting2.increaseEnum();
                Managers.SOUND.playBoolean();
            } else {
                open = !open;
                if (open) {
                    Managers.SOUND.playSwipeIn();
                } else {
                    Managers.SOUND.playSwipeOut();
                }
            }
        }

        if (open) {
            double offsetY = 0;
            for (int i = 0; i <= setting2.getModes().length - 1; i++) {
                if (Render2DEngine.isHovered(mouseX, mouseY, x, y + wheight + offsetY, width, 12) && button == 0) {
                    prevMode = setting2.currentEnumName();
                    animation2 = 0;
                    setting2.setEnumByNumber(i);
                    Managers.SOUND.playBoolean();
                }
                offsetY += 12;
            }
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    public void setWHeight(double height) {
        this.wheight = height;
    }

    public boolean isOpen() {
        return open;
    }
}
