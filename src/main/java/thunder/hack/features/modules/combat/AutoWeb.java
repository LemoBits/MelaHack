package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import thunder.hack.core.Managers;
import thunder.hack.events.impl.EventTick;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.setting.impl.SettingGroup;
import thunder.hack.utility.Timer;
import thunder.hack.utility.player.InteractionUtility;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static thunder.hack.utility.player.InteractionUtility.squaredDistanceFromEyes;

import com.mojang.blaze3d.vertex.PoseStack;

public final class AutoWeb extends Module {
    private final Setting<Integer> range = new Setting<>("Range", 5, 1, 7);
    private final Setting<Integer> placeWallRange = new Setting<>("WallRange", 5, 1, 7);
    private final Setting<PlaceTiming> placeTiming = new Setting<>("PlaceTiming", PlaceTiming.Default);
    private final Setting<Integer> blocksPerTick = new Setting<>("Block/Tick", 8, 1, 12, v -> placeTiming.getValue() == PlaceTiming.Default);
    private final Setting<Integer> placeDelay = new Setting<>("Delay/Place", 3, 0, 10);
    private final Setting<InteractionUtility.Interact> interact = new Setting<>("Interact", InteractionUtility.Interact.Strict);
    private final Setting<InteractionUtility.PlaceMode> placeMode = new Setting<>("PlaceMode", InteractionUtility.PlaceMode.Normal);
    private final Setting<InteractionUtility.Rotate> rotate = new Setting<>("Rotate", InteractionUtility.Rotate.None);
    private final Setting<SettingGroup> selection = new Setting<>("Selection", new SettingGroup(false, 0));
    private final Setting<Boolean> head = new Setting<>("Head", true).addToGroup(selection);
    private final Setting<Boolean> leggs = new Setting<>("Leggs", true).addToGroup(selection);
    private final Setting<Boolean> surround = new Setting<>("Surround", true).addToGroup(selection);
    private final Setting<Boolean> upperSurround = new Setting<>("UpperSurround", false).addToGroup(selection);
    private final Setting<SettingGroup> renderCategory = new Setting<>("Render", new SettingGroup(false, 0));
    private final Setting<RenderMode> renderMode = new Setting<>("Render Mode", RenderMode.Fade).addToGroup(renderCategory);
    private final Setting<ColorSetting> renderFillColor = new Setting<>("Render Fill Color", new ColorSetting(HudEditor.getColor(0))).addToGroup(renderCategory);
    private final Setting<ColorSetting> renderLineColor = new Setting<>("Render Line Color", new ColorSetting(HudEditor.getColor(0))).addToGroup(renderCategory);
    private final Setting<Integer> renderLineWidth = new Setting<>("Render Line Width", 2, 1, 5).addToGroup(renderCategory);
    private final Setting<Integer> effectDurationMs = new Setting<>("Effect Duration (MS)", 500, 0, 10000).addToGroup(renderCategory);

    private final ArrayList<BlockPos> sequentialBlocks = new ArrayList<>();
    public static Timer inactivityTimer = new Timer();

    private final Map<BlockPos, Long> renderPoses = new ConcurrentHashMap<>();

    private int delay = 0;

    public AutoWeb() {
        super("AutoWeb", Category.COMBAT);
    }

    public void onRender3D(PoseStack stack) {
        renderPoses.forEach((pos, time) -> {
            if (System.currentTimeMillis() - time > effectDurationMs.getValue()) {
                renderPoses.remove(pos);
            } else {
                switch (renderMode.getValue()) {
                    case Fade -> {
                        Render3DEngine.FILLED_QUEUE.add(new Render3DEngine.FillAction(new AABB(pos), Render2DEngine.injectAlpha(renderFillColor.getValue().getColorObject(), (int) (100f * (1f - ((System.currentTimeMillis() - time) / 500f))))));
                        Render3DEngine.OUTLINE_QUEUE.add(new Render3DEngine.OutlineAction(new AABB(pos), Render2DEngine.injectAlpha(renderLineColor.getValue().getColorObject(), (int) (100f * (1f - ((System.currentTimeMillis() - time) / 500f)))), renderLineWidth.getValue()));
                    }
                    case Decrease -> {
                        float scale = 1 - (float) (System.currentTimeMillis() - time) / 500;
                        AABB box = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
                        AABB scaledBox = box.contract(scale, scale, scale).move(0.5 + scale * 0.5, 0.5 + scale * 0.5, 0.5 + scale * 0.5);

                        Render3DEngine.FILLED_QUEUE.add(new Render3DEngine.FillAction(scaledBox, Render2DEngine.injectAlpha(renderFillColor.getValue().getColorObject(), (int) (100f * (1f - ((System.currentTimeMillis() - time) / 500f))))));
                        Render3DEngine.OUTLINE_QUEUE.add(new Render3DEngine.OutlineAction(scaledBox, renderLineColor.getValue().getColorObject(), renderLineWidth.getValue()));
                    }
                }
            }
        });
    }


    @Override
    public void onEnable() {
        sequentialBlocks.clear();
        renderPoses.clear();
    }

    @EventHandler
    public void onTick(EventTick e) {
        BlockPos targetBlock1 = getSequentialPos();
        if (targetBlock1 == null) return;

        if (delay > 0) {
            delay--;
            return;
        }

        InventoryUtility.saveSlot();
        if (placeTiming.getValue() == PlaceTiming.Default) {
            int placed = 0;
            while (placed < blocksPerTick.getValue()) {
                BlockPos targetBlock = getSequentialPos();
                if (targetBlock == null)
                    break;

                if (InteractionUtility.placeBlock(targetBlock, rotate.getValue(), interact.getValue(), placeMode.getValue(), getSlot(), false, true)) {
                    placed++;
                    renderPoses.put(targetBlock, System.currentTimeMillis());
                    delay = placeDelay.getValue();
                    inactivityTimer.reset();
                } else break;
            }
        } else if (placeTiming.getValue() == PlaceTiming.Vanilla) {
            BlockPos targetBlock = getSequentialPos();
            if (targetBlock == null) return;

            if (InteractionUtility.placeBlock(targetBlock, rotate.getValue(), interact.getValue(), placeMode.getValue(), getSlot(), false, true)) {
                sequentialBlocks.add(targetBlock);
                renderPoses.put(targetBlock, System.currentTimeMillis());
                delay = placeDelay.getValue();
                inactivityTimer.reset();
            }
        }
        InventoryUtility.returnSlot();
    }

    private BlockPos getSequentialPos() {
        Player target = Managers.COMBAT.getNearestTarget(range.getValue());
        if (target != null) {

            BlockPos targetBp = BlockPos.containing(target.position());

            ArrayList<BlockPos> positions = new ArrayList<>();
            if (leggs.getValue())
                positions.add(targetBp);

            if (head.getValue())
                positions.add(targetBp.above());

            if (surround.getValue()) {
                positions.add(targetBp.east());
                positions.add(targetBp.west());
                positions.add(targetBp.south());
                positions.add(targetBp.north());
            }

            if (upperSurround.getValue()) {
                positions.add(targetBp.east().above());
                positions.add(targetBp.west().above());
                positions.add(targetBp.south().above());
                positions.add(targetBp.north().above());
            }

            for (BlockPos bp : positions) {
                BlockHitResult wallCheck = mc.level.clip(new ClipContext(InteractionUtility.getEyesPos(mc.player), net.minecraft.world.phys.Vec3.atBottomCenterOf(bp).relative(Direction.UP, 0.5f), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
                if (wallCheck != null && wallCheck.getType() == HitResult.Type.BLOCK && wallCheck.getBlockPos() != bp)
                    if (squaredDistanceFromEyes(net.minecraft.world.phys.Vec3.atBottomCenterOf(bp)) > placeWallRange.getPow2Value()) continue;
                if (InteractionUtility.canPlaceBlock(bp, interact.getValue(), true) && mc.level.getBlockState(bp).canBeReplaced()) {
                    return bp;
                }
            }
        }

        return null;
    }


    private int getSlot() {
        List<Block> canUseBlocks = new ArrayList<>();
        canUseBlocks.add(Blocks.COBWEB);
        int slot = -1;
        final ItemStack mainhandStack = mc.player.getMainHandItem();
        if (mainhandStack != ItemStack.EMPTY && mainhandStack.getItem() instanceof BlockItem) {
            final Block blockFromMainhandItem = ((BlockItem) mainhandStack.getItem()).getBlock();
            if (canUseBlocks.contains(blockFromMainhandItem)) {
                slot = mc.player.getInventory().getSelectedSlot();
            }
        }
        if (slot == -1) {
            for (int i = 0; i < 9; i++) {
                final ItemStack stack = mc.player.getInventory().getItem(i);
                if (stack != ItemStack.EMPTY && stack.getItem() instanceof BlockItem) {
                    final Block blockFromItem = ((BlockItem) stack.getItem()).getBlock();
                    if (canUseBlocks.contains(blockFromItem)) {
                        slot = i;
                        break;
                    }
                }
            }
        }
        return slot;
    }

    private enum PlaceTiming {
        Default, Vanilla
    }

    private enum RenderMode {
        Fade, Decrease
    }
}