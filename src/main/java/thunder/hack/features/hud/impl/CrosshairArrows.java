package thunder.hack.features.hud.impl;

import com.google.common.collect.Lists;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.hud.HudElement;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.BooleanSettingGroup;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.math.MathUtility;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.animation.AnimationUtility;

import java.awt.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

import static thunder.hack.features.hud.impl.RadarRewrite.getRotations;

public class CrosshairArrows extends HudElement {
    public CrosshairArrows() {
        super("CrosshairArrows", 0, 0);
    }

    public static Setting<Boolean> glow = new Setting<>("Glow", false);
    private final Setting<Float> width = new Setting<>("Height", 2.28f, 0.1f, 5f);
    private final Setting<BooleanSettingGroup> down = new Setting<>("Down", new BooleanSettingGroup(false));
    private final Setting<Float> downHeight = new Setting<>("DownHeight", 3.63f, 0.1F, 20.0F).addToGroup(down);
    private final Setting<Float> tracerWidth = new Setting<>("Width", 0.44F, 0.0F, 8.0F);
    private final Setting<Integer> xOffset = new Setting<>("TracerRadius", 68, 20, 100);
    private final Setting<Integer> pitchLock = new Setting<>("PitchLock", 42, 0, 90);
    private final Setting<triangleModeEn> triangleMode = new Setting<>("TracerCMode", triangleModeEn.Astolfo);
    private final Setting<ColorSetting> colorf = new Setting<>("Friend", new ColorSetting(new Color(0x00E800)));
    private final Setting<ColorSetting> colors = new Setting<>("Tracer", new ColorSetting(new Color(0xFFFF00)));

    private float smoothYaw = 0;

    public void onRender2D(GuiGraphics context) {
        super.onRender2D(context);
        if (fullNullCheck()) return;

        float middleW = ModuleManager.crosshair.getAnimatedPosX();
        float middleH = ModuleManager.crosshair.getAnimatedPosY();

        int color = 0;

        context.pose().pushMatrix();
        context.pose().translate((float) (middleW), (float) (middleH));
        context.pose().translate((float) (-middleW), (float) (-middleH));

        smoothYaw = AnimationUtility.fast(smoothYaw, mc.player.getYRot(), 13);

        for (Player e : Lists.newArrayList(mc.level.players())) {
            if (e != mc.player){
                context.pose().pushMatrix();

                float yaw = getRotations(e) - smoothYaw;
                context.pose().translate((float) (middleW), (float) (middleH));
                context.pose().rotate((float) Math.toRadians(yaw));
                context.pose().translate((float) (-middleW), (float) (-middleH));

                if (Managers.FRIEND.isFriend(e))
                    color = colorf.getValue().getColor();
                else color = switch (triangleMode.getValue()) {
                    case Custom -> colors.getValue().getColor();
                    case Astolfo -> Render2DEngine.astolfo(false, 1).getRGB();
                };

                Render2DEngine.drawTracerPointer(context.pose(), middleW, middleH - xOffset.getValue(), width.getValue() * 5F,tracerWidth.getValue(), downHeight.getValue(), down.getValue().isEnabled(), glow.getValue(), color);

                context.pose().translate((float) (middleW), (float) (middleH));
                context.pose().rotate((float) Math.toRadians(-yaw));
                context.pose().translate((float) (-middleW), (float) (-middleH));
                context.pose().popMatrix();
            }
        }
        context.pose().popMatrix();
    }

    public enum triangleModeEn {
        Custom, Astolfo
    }
}
