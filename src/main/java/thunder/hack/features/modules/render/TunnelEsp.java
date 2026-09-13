package thunder.hack.features.modules.render;

import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.Timer;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public class TunnelEsp extends Module {
    public TunnelEsp() {
        super("TunnelEsp", Category.RENDER);
    }

    private final Setting<ColorSetting> color = new Setting<>("Color", new ColorSetting(new Color(0xAE8A8AF6, true)));
    public Setting<Boolean> box = new Setting<>("Box", true);
    public Setting<Boolean> outline = new Setting<>("Outline", true);
    List<AABB> renderBoxes = new ArrayList<>();
    private Timer delayTimer = new Timer();

    public void onRender3D(PoseStack stack) {
        try {
            for (AABB box_ : renderBoxes) {
                // рандомные генерации
                if (box_.getZsize() < 5 && box_.getXsize() < 5)
                    continue;

                if (box.getValue()) Render3DEngine.FILLED_QUEUE.add(new Render3DEngine.FillAction(box_, color.getValue().getColorObject()));
                if (outline.getValue())
                    Render3DEngine.OUTLINE_QUEUE.add(new Render3DEngine.OutlineAction(box_, Render2DEngine.injectAlpha(color.getValue().getColorObject(), 255), 2));
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onEnable() {
        renderBoxes.clear();
    }

    @Override
    public void onThread() {
        if (delayTimer.passedMs(2000)) {
            for (int x = (int) (mc.player.getX() - 128); x < mc.player.getX() + 128; ++x) {
                for (int z = (int) (mc.player.getZ() - 128); z < mc.player.getZ() + 128; ++z) {
                    for (int y = 0; y < 121; ++y) {
                        BlockPos bp = new BlockPos(x, y, z);

                        if (one_two(bp) && !alreadyIn(new AABB(bp.getX(), bp.getY(), bp.getZ(), bp.getX() + 1, bp.getY() + 2, bp.getZ() + 1))) {
                            AABB renderBox = new AABB(bp.getX(), bp.getY(), bp.getZ(), bp.getX() + 1, bp.getY() + 2, bp.getZ() + 1);
                            renderBoxes.add(getFullBox(renderBox, x, y, z, 1));
                        }

                        if (one_one(bp) && !alreadyIn(new AABB(bp))) {
                            AABB renderBox = new AABB(bp);
                            renderBoxes.add(getFullBox(renderBox, x, y, z, 0));
                        }

                        /*
                        if (one_three(bp) && !alreadyIn(new Box(bp.getX(), bp.getY(), bp.getZ(), bp.getX() + 1, bp.getY() + 3, bp.getZ() + 1))) {
                            Box renderBox = new Box(bp.getX(), bp.getY(), bp.getZ(), bp.getX() + 1, bp.getY() + 3, bp.getZ() + 1);
                            renderBoxes.add(getFullBox(renderBox, x, y, z, 2));
                        }
                         */
                    }
                }
            }
            delayTimer.reset();
        }
    }

    private AABB getFullBox(AABB raw, int x, int y, int z, int mode) {
        BlockPos checkBp1 = new BlockPos(x, y, z + 1);

        Function<BlockPos, Boolean> check = getCheckByMode(mode);

        while (check.apply(checkBp1)) {
            raw = raw.setMaxZ(raw.maxZ + 1);
            checkBp1 = checkBp1.south();
        }

        BlockPos checkBp2 = new BlockPos(x + 1, y, z);
        while (check.apply(checkBp2)) {
            raw = raw.setMaxX(raw.maxX + 1);
            checkBp2 = checkBp2.east();
        }

        BlockPos checkBp3 = new BlockPos(x, y, z - 1);
        while (check.apply(checkBp3)) {
            raw = raw.setMinZ(raw.minZ - 1);
            checkBp3 = checkBp3.north();
        }

        BlockPos checkBp4 = new BlockPos(x - 1, y, z);
        while (check.apply(checkBp4)) {
            raw = raw.setMinX(raw.minX - 1);
            checkBp4 = checkBp4.west();
        }

        return raw;
    }

    private Function<BlockPos, Boolean> getCheckByMode(int mode) {
        return switch (mode) {
            case 1 -> TunnelEsp::one_two;
            case 2 -> TunnelEsp::one_three;
            default -> TunnelEsp::one_one;
        };
    }

    private boolean alreadyIn(AABB box) {
        for (AABB box2 : renderBoxes) {
            if (box.intersects(box2))
                return true;
        }
        return false;
    }

    //1 x 2 check
    private static boolean one_three(BlockPos pos) {
        if (!isAir(pos) || !isAir(pos.above()) || !isAir(pos.above().above())) return false;
        if (isAir(pos.below()) || isAir(pos.above().above().above())) return false;

        if (isAir(pos.above().north()) && isAir(pos.above().south()))
            return !isAir(pos.above().east()) && !isAir(pos.above().west());

        if (isAir(pos.above().east()) && isAir(pos.above().west()))
            return !isAir(pos.above().north()) && !isAir(pos.above().south());

        return false;
    }

    //1 x 2 check
    private static boolean one_two(BlockPos pos) {
        if (!isAir(pos) || !isAir(pos.above())) return false;
        if (isAir(pos.below()) || isAir(pos.above().above())) return false;

        if (isAir(pos.north()) && isAir(pos.south()) && isAir(pos.above().north()) && isAir(pos.above().south()))
            return !isAir(pos.east()) && !isAir(pos.west()) && !isAir(pos.above().east()) && !isAir(pos.above().west());

        if (isAir(pos.east()) && isAir(pos.west()) && isAir(pos.above().east()) && isAir(pos.above().west()))
            return !isAir(pos.north()) && !isAir(pos.south()) && !isAir(pos.above().north()) && !isAir(pos.above().south());

        return false;
    }

    //1 x 1 check
    private static boolean one_one(BlockPos pos) {
        if (!isAir(pos)) return false;
        if (isAir(pos.below()) || isAir(pos.above())) return false;

        if (isAir(pos.north()) && isAir(pos.south()))
            return !isAir(pos.east()) && !isAir(pos.west()) && !isAir(pos.above().east()) && !isAir(pos.above().west());

        if (isAir(pos.east()) && isAir(pos.west()))
            return !isAir(pos.north()) && !isAir(pos.south());

        return false;
    }


    private static boolean isAir(BlockPos bp) {
        return mc.level.isEmptyBlock(bp);
    }
}
