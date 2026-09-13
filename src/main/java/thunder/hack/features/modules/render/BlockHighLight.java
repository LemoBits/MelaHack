package thunder.hack.features.modules.render;

import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine.FillAction;
import thunder.hack.utility.render.Render3DEngine.FillSideAction;
import thunder.hack.utility.render.Render3DEngine.OutlineAction;
import thunder.hack.utility.render.Render3DEngine.OutlineSideAction;

import static thunder.hack.utility.render.Render3DEngine.*;
import static thunder.hack.utility.render.Render3DEngine.OUTLINE_QUEUE;
import static thunder.hack.utility.render.Render3DEngine.OUTLINE_SIDE_QUEUE;
import static thunder.hack.utility.render.Render3DEngine.FILLED_QUEUE;
import static thunder.hack.utility.render.Render3DEngine.FILLED_SIDE_QUEUE;
import static thunder.hack.utility.render.Render3DEngine.OutlineAction;
import static thunder.hack.utility.render.Render3DEngine.OutlineSideAction;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import static thunder.hack.utility.render.Render3DEngine.FillAction;
import static thunder.hack.utility.render.Render3DEngine.FillSideAction;

public class BlockHighLight extends Module {
    public BlockHighLight() {
        super("BlockHighLight", Category.RENDER);
    }

    private final Setting<Mode> mode = new Setting("Mode", Mode.Outline);
    private final Setting<ColorSetting> color = new Setting<>("Color", new ColorSetting(0xFFFFFFFF));
    private final Setting<Float> lineWidth = new Setting<>("LineWidth", 1F, 0f, 5F);

    private enum Mode {
        Both, BothSide, Fill, FilledSide, Outline, OutlinedSide
    }

    public void onRender3D(PoseStack stack) {
        if (mc.hitResult == null) return;
        if (mc.hitResult.getType() != HitResult.Type.BLOCK) return;
        if (!(mc.hitResult instanceof BlockHitResult bhr)) return;

        switch (mode.getValue()) {
            case Both -> {
                OUTLINE_QUEUE.add(new OutlineAction(new AABB(bhr.getBlockPos()), Render2DEngine.injectAlpha(color.getValue().getColorObject(), 255), lineWidth.getValue()));
                FILLED_QUEUE.add(new FillAction(new AABB(bhr.getBlockPos()), color.getValue().getColorObject()));
            }
            case BothSide -> {
                OUTLINE_SIDE_QUEUE.add(new OutlineSideAction(new AABB(bhr.getBlockPos()), Render2DEngine.injectAlpha(color.getValue().getColorObject(),255), lineWidth.getValue(),bhr.getDirection()));
                FILLED_SIDE_QUEUE.add(new FillSideAction(new AABB(bhr.getBlockPos()),color.getValue().getColorObject(),bhr.getDirection()));
            }
            case Fill -> FILLED_QUEUE.add(new FillAction(new AABB(bhr.getBlockPos()), color.getValue().getColorObject()));
            case FilledSide -> FILLED_SIDE_QUEUE.add(new FillSideAction(new AABB(bhr.getBlockPos()),color.getValue().getColorObject(),bhr.getDirection()));

            case Outline ->  OUTLINE_QUEUE.add(new OutlineAction(new AABB(bhr.getBlockPos()), Render2DEngine.injectAlpha(color.getValue().getColorObject(),255), lineWidth.getValue()));
            case OutlinedSide -> OUTLINE_SIDE_QUEUE.add(new OutlineSideAction(new AABB(bhr.getBlockPos()), Render2DEngine.injectAlpha(color.getValue().getColorObject(),255), lineWidth.getValue(),bhr.getDirection()));
        }
    }
}
