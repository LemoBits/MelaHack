package thunder.hack.features.modules.render;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.world.level.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.BarrierBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.BooleanSettingGroup;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.setting.impl.ItemSelectSetting;
import thunder.hack.utility.Timer;
import thunder.hack.utility.player.PlayerUtility;
import thunder.hack.utility.render.Render3DEngine;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static thunder.hack.features.modules.client.ClientSettings.isRu;

public class BlockESP extends Module {
    public BlockESP() {
        super("BlockESP", Category.RENDER);
    }

    public final Setting<ItemSelectSetting> selectedBlocks = new Setting<>("SelectedBlocks", new ItemSelectSetting(new ArrayList<>()));
    public static ArrayList<BlockVec> blocks = new ArrayList<>();
    private final Setting<Integer> range = new Setting<>("Range", 100, 1, 128);
    private final Setting<BooleanSettingGroup> limit = new Setting<>("Limit", new BooleanSettingGroup(true));
    private final Setting<Integer> limitCount = new Setting<>("LimitCount", 50, 1, 2048).addToGroup(limit);
    private final Setting<ColorSetting> color = new Setting<>("Color", new ColorSetting(0xFF00FFFF));
    private final Setting<Boolean> illegals = new Setting<>("Illegals", true);
    private final Setting<Boolean> tracers = new Setting<>("Tracers", false);
    private final Setting<Boolean> fill = new Setting<>("Fill", true);
    private final Setting<Boolean> outline = new Setting<>("Outline", true);

    private final ExecutorService searchThread = Executors.newSingleThreadExecutor();
    private final Timer searchTimer = new Timer();
    private long lastFrameTime;
    private boolean canContinue;

    @Override
    public void onEnable() {
        blocks.clear();
        lastFrameTime = System.currentTimeMillis();
        canContinue = true;
    }

    @Override
    public void onUpdate() {
        if (searchTimer.every(1000) && canContinue) {
            CompletableFuture.supplyAsync(this::scan, searchThread).thenAcceptAsync(this::sync, Util.backgroundExecutor());
            canContinue = false;
        }
    }

    private ArrayList<BlockVec> scan() {
        ArrayList<BlockVec> blocks = new ArrayList<>();
        int startX = (int) Math.floor(mc.player.getX() - range.getValue());
        int endX = (int) Math.ceil(mc.player.getX() + range.getValue());
        int startY = mc.level.getMinY() + 1;
        int endY = mc.level.getHeight(Heightmap.Types.WORLD_SURFACE, (int) mc.player.getX(), (int) mc.player.getZ());
        int startZ = (int) Math.floor(mc.player.getZ() - range.getValue());
        int endZ = (int) Math.ceil(mc.player.getZ() + range.getValue());

        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                for (int z = startZ; z <= endZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState bs = mc.level.getBlockState(pos);
                    if (shouldAdd(bs.getBlock(), pos)) {
                        blocks.add(new BlockVec(pos.getX(), pos.getY(), pos.getZ()));
                    }
                }
            }
        }
        return blocks;
    }

    private void sync(ArrayList<BlockVec> b) {
        blocks = b;
        canContinue = true;
    }

    public void onRender3D(PoseStack stack) {
        if (fullNullCheck() || blocks.isEmpty()) return;
        int count = 0;

        if (mc.getFps() < 8 && mc.player.tickCount > 100) {
            disable(isRu() ? "Спасаем твой ПК :)" : "Saving ur pc :)");
            return;
        }

        if (fill.getValue() || outline.getValue()) {
            for (BlockVec vec : Lists.newArrayList(blocks)) {
                if (count > limitCount.getValue() && limit.getValue().isEnabled())
                    continue;

                if (vec.getDistance(mc.player.position()) > range.getPow2Value()) {
                    blocks.remove(vec);
                    continue;
                }

                AABB b = new AABB(vec.x, vec.y, vec.z, vec.x + 1, vec.y + 1, vec.z + 1);

                if (fill.getValue())
                    Render3DEngine.FILLED_QUEUE.add(new Render3DEngine.FillAction(b, color.getValue().getColorObject()));

                if (outline.getValue())
                    Render3DEngine.OUTLINE_QUEUE.add(new Render3DEngine.OutlineAction(b, color.getValue().getColorObject(), 2f));

                if (tracers.getValue()) {
                    Vec3 vec2 = new Vec3(0, 0, 75)
                            .xRot(-(float) Math.toRadians(mc.gameRenderer.getMainCamera().getXRot()))
                            .yRot(-(float) Math.toRadians(mc.gameRenderer.getMainCamera().getYRot()))
                            .add(mc.cameraEntity.getEyePosition());

                    Render3DEngine.drawLineDebug(vec2, vec.getVector(), color.getValue().getColorObject());
                }
                count++;
            }
        }
        lastFrameTime = System.currentTimeMillis();
    }

    private boolean shouldAdd(Block block, BlockPos pos) {
        if (block instanceof AirBlock) return false;
        if (selectedBlocks.getValue().contains(block)) return true;
        if (illegals.getValue()) return isIllegal(block, pos);
        return false;
    }

    private boolean isIllegal(Block block, BlockPos pos) {
        if (block instanceof CommandBlock || block instanceof BarrierBlock) return true;

        if (block == Blocks.BEDROCK) {
            if (!PlayerUtility.isInHell())
                return pos.getY() > 4;
            else
                return pos.getY() > 127 || (pos.getY() < 123 && pos.getY() > 4);
        }
        return false;
    }

    public record BlockVec(double x, double y, double z) {
        public double getDistance(@NotNull Vec3 v) {
            double dx = x - v.x;
            double dy = y - v.y;
            double dz = z - v.z;
            return dx * dx + dy * dy + dz * dz;
        }

        public Vec3 getVector() {
            return new Vec3(x + 0.5f, y + 0.5f, z + 0.5f);
        }
    }
}
