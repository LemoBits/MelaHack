package thunder.hack.features.modules.combat;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
import thunder.hack.utility.player.PlayerUtility;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static thunder.hack.features.modules.client.ClientSettings.isRu;

public class TNTAura extends Module {
    public TNTAura() {
        super("TNTAura", Category.COMBAT);
    }

    private final Setting<Float> range = new Setting<>("Range", 5f, 2f, 7f);
    private final Setting<Integer> blocksPerTick = new Setting<>("Block/Tick", 8, 1, 12);
    private final Setting<Integer> placeDelay = new Setting<>("Delay/Place", 3, 0, 10);
    private final Setting<InteractionUtility.PlaceMode> placeMode = new Setting<>("PlaceMode", InteractionUtility.PlaceMode.Normal);
    private final Setting<InteractionUtility.Rotate> rotate = new Setting<>("Rotate", InteractionUtility.Rotate.None);
    private final Setting<SettingGroup> renderCategory = new Setting<>("Render", new SettingGroup(false, 0));
    private final Setting<RenderMode> renderMode = new Setting<>("RenderMode", RenderMode.Fade).addToGroup(renderCategory);
    private final Setting<ColorSetting> renderFillColor = new Setting<>("Fill", new ColorSetting(HudEditor.getColor(0))).addToGroup(renderCategory);
    private final Setting<ColorSetting> renderLineColor = new Setting<>("Line", new ColorSetting(HudEditor.getColor(0))).addToGroup(renderCategory);
    private final Setting<Integer> renderLineWidth = new Setting<>("LineWidth", 2, 1, 5).addToGroup(renderCategory);

    private final Map<BlockPos, Long> renderPoses = new ConcurrentHashMap<>();
    public static Timer inactivityTimer = new Timer();

    private int delay;

    public void onRender3D(PoseStack stack) {
        renderPoses.forEach((pos, time) -> {
            if (System.currentTimeMillis() - time > 500) {
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

    @EventHandler
    public void onTick(EventTick e) {
        if (getTntSlot() == -1) {
            disable(isRu() ? "Нет динамита!" : "No TNT!");
            return;
        }
        if (getFlintSlot() == -1) {
            disable(isRu() ? "Нет зажигалки!" : "No flint and steel!");
            return;
        }
        if (getObbySlot() == -1) {
            disable(isRu() ? "Нет обсидиана!" : "No obsidian!");
            return;
        }

        Player targetedPlayer = Managers.COMBAT.getNearestTarget(range.getValue());

        List<BlockPos> blocks = getBlocks(targetedPlayer);
        if (!blocks.isEmpty()) {
            if (delay > 0) {
                delay--;
                return;
            }

            InventoryUtility.saveSlot();
            int placed = 0;
            while (placed < blocksPerTick.getValue()) {
                BlockPos targetBlock = getSequentialPos(targetedPlayer);
                if (targetBlock == null)
                    break;
                if (InteractionUtility.placeBlock(targetBlock, rotate.getValue(), InteractionUtility.Interact.Vanilla, placeMode.getValue(), getObbySlot(), false, false)) {
                    placed++;
                    delay = placeDelay.getValue();
                    inactivityTimer.reset();
                    renderPoses.put(targetBlock, System.currentTimeMillis());
                } else break;
            }
            InventoryUtility.returnSlot();
        }

        if (targetedPlayer != null) {
            BlockPos headBlock = BlockPos.containing(targetedPlayer.position()).above(2);
            InventoryUtility.saveSlot();
            InteractionUtility.placeBlock(headBlock, rotate.getValue(), InteractionUtility.Interact.Vanilla, placeMode.getValue(), getTntSlot(), false, false);
            BlockHitResult igniteResult = getIgniteResult(headBlock);
            InventoryUtility.switchTo(getFlintSlot());
            if (mc.level.getBlockState(headBlock).getBlock() instanceof TntBlock && igniteResult != null) {
                sendSequencedPacket(id -> new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, igniteResult, id));
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
            renderPoses.put(headBlock, System.currentTimeMillis());
            InventoryUtility.returnSlot();
        }
    }

    private BlockPos getSequentialPos(Player pl) {
        List<BlockPos> list = getBlocks(pl);
        if (list.isEmpty()) return null;
        for (BlockPos bp : getBlocks(pl)) {
            if (InteractionUtility.canPlaceBlock(bp, InteractionUtility.Interact.Vanilla, false) && mc.level.isEmptyBlock(bp)) {
                return bp;
            }
        }
        return null;
    }

    private List<BlockPos> getBlocks(Player pl) {
        if (pl == null) return new ArrayList<>();
        List<BlockPos> blocks = new ArrayList<>();
        for (BlockPos bp : getAffectedBlocks(pl)) {
            for (Direction dir : Direction.values()) {
                if (dir == Direction.UP || dir == Direction.DOWN) continue;
                blocks.add(bp.relative(dir));
                blocks.add(bp.relative(dir).above());

                if (!new AABB(bp.relative(dir).above(1)).intersects(pl.getBoundingBox()))
                    blocks.add(bp.relative(dir).above(2));

                blocks.add(bp.relative(dir).below());
            }

            blocks.add(bp.below());
            blocks.add(bp.above(3));

            if (!InteractionUtility.canPlaceBlock(bp.above(3), InteractionUtility.Interact.Vanilla, false)) {
                Direction dir = mc.player.getDirection();
                if (dir != null) {
                    blocks.add(bp.above(3).relative(dir, 1));
                }
            }
        }

        return blocks.stream().sorted(Comparator.comparing(b -> mc.player.distanceToSqr(b.getCenter()) * -1)).toList();
    }

    private int getObbySlot() {
        if (mc.player.getMainHandItem().getItem() == Items.OBSIDIAN)
            return mc.player.getInventory().getSelectedSlot();

        int slot = -1;

        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).getItem() == Items.OBSIDIAN) {
                slot = i;
                break;
            }
        }
        return slot;
    }

    private int getTntSlot() {
        if (mc.player.getMainHandItem().getItem() == Items.TNT)
            return mc.player.getInventory().getSelectedSlot();

        int slot = -1;

        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).getItem() == Items.TNT) {
                slot = i;
                break;
            }
        }
        return slot;
    }

    private int getFlintSlot() {
        if (mc.player.getMainHandItem().getItem() == Items.FLINT_AND_STEEL)
            return mc.player.getInventory().getSelectedSlot();

        int slot = -1;

        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).getItem() == Items.FLINT_AND_STEEL) {
                slot = i;
                break;
            }
        }
        return slot;
    }

    private @Nullable BlockHitResult getIgniteResult(BlockPos bp) {
        if (mc.player == null || mc.level == null) return null;

        if (PlayerUtility.squaredDistanceFromEyes(bp.getCenter()) > range.getPow2Value())
            return null;

        return new BlockHitResult(bp.getCenter().add(0, -0.5, 0), Direction.DOWN, bp, false);
    }

    private List<BlockPos> getAffectedBlocks(Player pl) {
        List<BlockPos> tempPos = new ArrayList<>();
        List<BlockPos> finalPos = new ArrayList<>();
        List<AABB> boxes = new ArrayList<>();

        for (Player player : mc.level.players()) {
            if (player.distanceToSqr(pl) < 9 && player != pl)
                boxes.add(player.getBoundingBox());
        }

        boxes.add(pl.getBoundingBox());

        BlockPos center = getPlayerPos(pl);

        tempPos.add(center);
        tempPos.add(center.north());
        tempPos.add(center.north().east());
        tempPos.add(center.west());
        tempPos.add(center.west().north());
        tempPos.add(center.south());
        tempPos.add(center.south().west());
        tempPos.add(center.east());
        tempPos.add(center.east().south());

        for (BlockPos bp : tempPos)
            if (new AABB(bp).intersects(pl.getBoundingBox()))
                finalPos.add(bp);

        for (BlockPos bp : Lists.newArrayList(finalPos)) {
            for (AABB box : boxes) {
                if (new AABB(bp).intersects(box))
                    finalPos.add(BlockPos.containing(box.getCenter()));
            }
        }

        return finalPos;
    }

    private BlockPos getPlayerPos(@NotNull Player pl) {
        return BlockPos.containing(pl.getX(), pl.getY() - Math.floor(pl.getY()) > 0.8 ? Math.floor(pl.getY()) + 1.0 : Math.floor(pl.getY()), pl.getZ());
    }

    private enum RenderMode {
        Fade, Decrease
    }
}
