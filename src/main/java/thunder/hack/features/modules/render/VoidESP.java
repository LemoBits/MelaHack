package thunder.hack.features.modules.render;

import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.render.Render3DEngine;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

public class VoidESP extends Module {
    public VoidESP() {
        super("VoidESP", Category.RENDER);
    }

    public final Setting<ColorSetting> color = new Setting<>("Color", new ColorSetting(new Color(0xD7FFA600, true)));
    public Setting<Float> range = new Setting<>("Range", 6.0f, 3.0f, 16.0f);

    private List<BlockPos> holes = new ArrayList<>();

    public void onRender3D(PoseStack stack) {
        holes.forEach(h -> Render3DEngine.renderCrosses(new AABB(h), color.getValue().getColorObject(), 2.0f));
    }

    public List<BlockPos> calcHoles() {
        ArrayList<BlockPos> voidHoles = new ArrayList<>();
        for (int x = (int) (mc.player.getX() - range.getValue()); x < mc.player.getX() + range.getValue(); x++)
            for (int z = (int) (mc.player.getZ() - range.getValue()); z < mc.player.getZ() + range.getValue(); z++) {
                BlockPos pos = BlockPos.containing(x, mc.level.getMinY(), z);
                if (mc.level.getBlockState(pos).getBlock() == Blocks.BEDROCK) continue;
                voidHoles.add(pos);
            }
        return voidHoles;
    }

    @Override
    public void onThread() {
        holes = calcHoles();
    }
}
