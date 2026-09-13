package thunder.hack.features.modules.render;

import thunder.hack.injection.accesors.IWorldRenderer;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.math.MathUtility;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

public class BreakHighLight extends Module {
    public BreakHighLight() {
        super("BreakHighLight", Category.RENDER);
    }

    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Shrink);

    private final Setting<ColorSetting> color = new Setting<>("Color", new ColorSetting(new Color(0x90FD0000, true)));
    private final Setting<ColorSetting> color2 = new Setting<>("Color2", new ColorSetting(new Color(0xFFFD0000, true)));
    private final Setting<ColorSetting> ocolor = new Setting<>("OutlineColor", new ColorSetting(new Color(0x903FFD00, true)));
    private final Setting<ColorSetting> ocolor2 = new Setting<>("OutlineColor2", new ColorSetting(new Color(0xFF2EFD00, true)));
    private final Setting<ColorSetting> textColor = new Setting<>("TextColor", new ColorSetting(0xFFFFFFFF));

    private final Setting<Float> lineWidth = new Setting<>("LineWidth", 2F, 0f, 5F);
    private final Setting<Boolean> otherPlayer = new Setting<>("OtherPlayer", true);

    private float prevProgress;

    public void onRender3D(PoseStack stack) {
        if (mc.gameMode.isDestroying() && mc.hitResult != null && mc.hitResult instanceof BlockHitResult bhr && !mc.level.isEmptyBlock(bhr.getBlockPos())) {
            AABB shrunkMineBox = new AABB(bhr.getBlockPos().getX(), bhr.getBlockPos().getY(), bhr.getBlockPos().getZ(), bhr.getBlockPos().getX(), bhr.getBlockPos().getY(), bhr.getBlockPos().getZ());

            float noom; //ам ням ебался

            switch (mode.getValue()) {
                case Grow -> noom = Render2DEngine.interpolateFloat(prevProgress, MathUtility.clamp(mc.gameMode.destroyProgress, 0f, 1f), Render3DEngine.getTickDelta());
                case Shrink -> noom = 1f - Render2DEngine.interpolateFloat(prevProgress, mc.gameMode.destroyProgress, Render3DEngine.getTickDelta());
                default -> noom = 1;
            }

            Render3DEngine.FILLED_QUEUE.add(new Render3DEngine.FillAction(
                    shrunkMineBox.contract(noom, noom, noom).move(0.5 + noom * 0.5, 0.5 + noom * 0.5, 0.5 + noom * 0.5),
                    Render2DEngine.interpolateColorC(color.getValue().getColorObject(),color2.getValue().getColorObject(),noom)
            ));
            Render3DEngine.OUTLINE_QUEUE.add(new Render3DEngine.OutlineAction(
                    shrunkMineBox.contract(noom, noom, noom).move(0.5 + noom * 0.5, 0.5 + noom * 0.5, 0.5 + noom * 0.5),
                    Render2DEngine.interpolateColorC(ocolor.getValue().getColorObject(),ocolor2.getValue().getColorObject(),noom),
                    lineWidth.getValue()
            ));

            switch (mode.getValue()) {
                case Grow -> prevProgress = noom;
                case Shrink -> prevProgress = 1 - noom;
                default -> prevProgress = 1f;
            }
        }
        ((IWorldRenderer) mc.levelRenderer).getBlockBreakingInfos().forEach(((integer, destroyBlockProgress) -> {
            Entity object = mc.level.getEntity(integer);
            if (object != null && otherPlayer.getValue() && !object.getName().equals(mc.player.getName())) {
                BlockPos pos = destroyBlockProgress.getPos();
                Render3DEngine.drawTextIn3D(String.valueOf(object.getName().getString()),pos.getCenter(),0,0.1,0,textColor.getValue().getColorObject());
                AABB shrunkMineBox = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());

                float noom;
                switch (mode.getValue()) {
                    case Grow -> noom = MathUtility.clamp((destroyBlockProgress.getProgress() / 10f), 0f, 1f);
                    case Shrink -> noom = 1f - (destroyBlockProgress.getProgress() / 10f);
                    default -> noom = 1;
                }

                Render3DEngine.FILLED_QUEUE.add(new Render3DEngine.FillAction(
                        shrunkMineBox.contract(noom, noom, noom).move(0.5 + noom * 0.5, 0.5 + noom * 0.5, 0.5 + noom * 0.5),
                        Render2DEngine.interpolateColorC(color.getValue().getColorObject(),color2.getValue().getColorObject(),noom)
                ));

                Render3DEngine.OUTLINE_QUEUE.add(new Render3DEngine.OutlineAction(
                        shrunkMineBox.contract(noom, noom, noom).move(0.5 + noom * 0.5, 0.5 + noom * 0.5, 0.5 + noom * 0.5),
                        Render2DEngine.interpolateColorC(ocolor.getValue().getColorObject(),ocolor2.getValue().getColorObject(),noom),
                        lineWidth.getValue()
                ));
            }
        }));
    }

    private enum Mode {
        Grow, Shrink, Static
    }
}