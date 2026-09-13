package thunder.hack.gui.thundergui.components;

import thunder.hack.gui.font.FontRenderers;
import thunder.hack.gui.thundergui.ThunderGui;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.SettingGroup;
import thunder.hack.utility.render.Render2DEngine;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.*;

public class ParentComponent extends SettingElement {
    public ParentComponent(Setting setting) {
        super(setting);
        SettingGroup settingGroup = (SettingGroup) setting.getValue();
        settingGroup.setExtended(true);
    }

    @Override
    public void render(PoseStack stack, int mouseX, int mouseY, float partialTicks) {
        super.render(stack, mouseX, mouseY, partialTicks);
        if ((getY() > ThunderGui.getInstance().main_posY + ThunderGui.getInstance().height) || getY() < ThunderGui.getInstance().main_posY) {
            return;
        }
        FontRenderers.modules.drawCenteredString(stack, getSetting().getName(), getX() + width / 2f, getY() + 2, new Color(0xB0FFFFFF, true).getRGB());
        Render2DEngine.draw2DGradientRect(stack, getX() + 10, getY() + 6, (getX() + width / 2f) - 20, getY() + 7, new Color(0x0FFFFFF, true), new Color(0x0FFFFFF, true), new Color(0xB0FFFFFF, true), new Color(0xB0FFFFFF, true));
        Render2DEngine.draw2DGradientRect(stack, getX() + width / 2f + 20f, getY() + 6, getX() + width - 10, getY() + 7, new Color(0xB0FFFFFF, true), new Color(0xB0FFFFFF, true), new Color(0x0FFFFFF, true), new Color(0x0FFFFFF, true));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        if ((getY() > ThunderGui.getInstance().main_posY + ThunderGui.getInstance().height) || getY() < ThunderGui.getInstance().main_posY) {
            return;
        }
        if (hovered) {
            SettingGroup settingGroup = (SettingGroup) setting.getValue();
            settingGroup.setExtended(!settingGroup.isExtended());
        }
    }
}