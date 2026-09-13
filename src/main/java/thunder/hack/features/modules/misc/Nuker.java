package thunder.hack.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventAttackBlock;
import thunder.hack.events.impl.EventSetBlockState;
import thunder.hack.events.impl.EventSync;
import thunder.hack.events.impl.PlayerUpdateEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.features.modules.player.SpeedMine;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.setting.impl.ItemSelectSetting;
import thunder.hack.utility.Timer;
import thunder.hack.utility.world.ExplosionUtility;
import thunder.hack.utility.player.InteractionUtility;
import thunder.hack.utility.player.PlayerUtility;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;
import thunder.hack.utility.render.Render3DEngine.FillAction;
import thunder.hack.utility.render.Render3DEngine.OutlineAction;
import java.awt.*;
import java.util.ArrayList;

import static net.minecraft.world.level.block.Blocks.*;
import static thunder.hack.features.modules.client.ClientSettings.isRu;
import static thunder.hack.utility.render.Render3DEngine.FILLED_QUEUE;
import static thunder.hack.utility.render.Render3DEngine.FillAction;
import static thunder.hack.utility.render.Render3DEngine.OUTLINE_QUEUE;
import static thunder.hack.utility.render.Render3DEngine.OutlineAction;

import com.mojang.blaze3d.vertex.PoseStack;

public class Nuker extends Module {
    public Nuker() {
        super("Nuker", Category.MISC);
    }

    public final Setting<ItemSelectSetting> selectedBlocks = new Setting<>("SelectedBlocks", new ItemSelectSetting(new ArrayList<>()));
    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Default);
    private final Setting<Integer> delay = new Setting<>("Delay", 25, 0, 1000);
    private final Setting<BlockSelection> blocks = new Setting<>("Blocks", BlockSelection.Select);
    private final Setting<Boolean> ignoreWalls = new Setting<>("IgnoreWalls", false);
    private final Setting<Boolean> flatten = new Setting<>("Flatten", false);
    private final Setting<Boolean> creative = new Setting<>("Creative", false);
    private final Setting<Boolean> avoidLava = new Setting<>("AvoidLava", false);
    private final Setting<Float> range = new Setting<>("Range", 4.2f, 1.5f, 25f);
    private final Setting<ColorMode> colorMode = new Setting<>("ColorMode", ColorMode.Sync);
    public final Setting<ColorSetting> color = new Setting<>("Color", new ColorSetting(0x2250b4b4), v -> colorMode.getValue() == ColorMode.Custom);

    private Block targetBlockType;
    private BlockData blockData;
    private Timer breakTimer = new Timer();

    private NukerThread nukerThread = new NukerThread();
    private float rotationYaw, rotationPitch;

    @Override
    public void onEnable() {
        nukerThread = new NukerThread();
        nukerThread.setName("ThunderHack-NukerThread");
        nukerThread.setDaemon(true);
        nukerThread.start();
    }

    @Override
    public void onDisable() {
        nukerThread.interrupt();
    }

    @Override
    public void onUpdate() {
        if (!nukerThread.isAlive()) {
            nukerThread = new NukerThread();
            nukerThread.setName("ThunderHack-NukerThread");
            nukerThread.setDaemon(true);
            nukerThread.start();
        }
    }

    @EventHandler
    public void onBlockInteract(EventAttackBlock e) {
        if (mc.level.isEmptyBlock(e.getBlockPos())) return;
        if (blocks.getValue().equals(BlockSelection.Select) && targetBlockType != mc.level.getBlockState(e.getBlockPos()).getBlock()) {
            targetBlockType = mc.level.getBlockState(e.getBlockPos()).getBlock();
            sendMessage(isRu() ? "Выбран блок: " + ChatFormatting.AQUA + targetBlockType.getName().getString() : "Selected block: " + ChatFormatting.AQUA + targetBlockType.getName().getString());
        }
    }

    @EventHandler
    public void onBlockDestruct(EventSetBlockState e) {
        if (blockData != null && e.getPos() == blockData.bp && e.getState().isAir()) {
            blockData = null;
            new Thread(() -> {
                if ((targetBlockType != null || blocks.getValue().equals(BlockSelection.All)) && !mc.options.keyAttack.isDown() && blockData == null) {
                    blockData = getNukerBlockPos();
                }
            }).start();
        }
    }

    @EventHandler
    public void onSync(EventSync e) {
        if(rotationYaw != -999) {
            mc.player.setYRot(rotationYaw);
            mc.player.setXRot(rotationPitch);
            rotationYaw = -999;
        }
    }


    @EventHandler
    public void onPlayerUpdate(PlayerUpdateEvent e) {
        if (blockData != null) {
            if ((mc.level.getBlockState(blockData.bp).getBlock() != targetBlockType && blocks.getValue().equals(BlockSelection.Select))
                    || PlayerUtility.squaredDistanceFromEyes(net.minecraft.world.phys.Vec3.atBottomCenterOf(blockData.bp)) > range.getPow2Value()
                    || mc.level.isEmptyBlock(blockData.bp))
                blockData = null;
        }

        if (blockData == null || mc.options.keyAttack.isDown()) return;

        float[] angle = InteractionUtility.calculateAngle(blockData.vec3d);
        rotationYaw = (angle[0]);
        rotationPitch = (angle[1]);
        ModuleManager.rotations.fixRotation = rotationYaw;

        if (mode.getValue() == Mode.Default) {
            breakBlock();
        }

        if (mode.getValue() == Mode.FastAF) {
            int intRange = (int) (Math.floor(range.getValue()) + 1);
            Iterable<BlockPos> blocks_ = BlockPos.withinManhattan(new BlockPos(BlockPos.containing(mc.player.position()).above()), intRange, intRange, intRange);

            for (BlockPos b : blocks_) {
                if (flatten.getValue() && b.getY() < mc.player.getY())
                    continue;

                if (avoidLava.getValue() && checkLava(b))
                    continue;

                BlockState state = mc.level.getBlockState(b);

                if (PlayerUtility.squaredDistanceFromEyes(net.minecraft.world.phys.Vec3.atBottomCenterOf(b)) <= range.getPow2Value()) {
                    if (isAllowed(state.getBlock())) {
                        try {
                            sendSequencedPacket(id -> new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, b, Direction.UP, id));
                            mc.gameMode.destroyBlock(b);
                            mc.player.swing(InteractionHand.MAIN_HAND);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }
    }

    public synchronized void breakBlock() {
        if (blockData == null || mc.options.keyAttack.isDown()) return;
        if (ModuleManager.speedMine.isEnabled() && ModuleManager.speedMine.mode.getValue() == SpeedMine.Mode.Packet) {
            if (!ModuleManager.speedMine.alreadyActing(blockData.bp)) {
                mc.gameMode.startDestroyBlock(blockData.bp, blockData.dir);
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
        } else {
            BlockPos cache = blockData.bp;
            mc.gameMode.continueDestroyBlock(blockData.bp, blockData.dir);
            mc.player.swing(InteractionHand.MAIN_HAND);
            if (creative.getValue())
                mc.gameMode.destroyBlock(cache);
        }
    }

    public void onRender3D(PoseStack stack) {
        BlockPos renderBp = null;

        if (blockData != null && blockData.bp != null)
            renderBp = blockData.bp;

        if (renderBp != null) {
            Color color1 = colorMode.getValue() == ColorMode.Sync ? HudEditor.getColor(1) : color.getValue().getColorObject();
            OUTLINE_QUEUE.add(new OutlineAction(new AABB(blockData.bp), color1, 2));
            FILLED_QUEUE.add(new FillAction(new AABB(blockData.bp), Render2DEngine.injectAlpha(color1, 100)));
        }

        if (mode.getValue() == Mode.Fast && breakTimer.passedMs(delay.getValue())) {
            breakBlock();
            breakTimer.reset();
        }
    }

    public BlockData getNukerBlockPos() {
        int intRange = (int) (Math.floor(range.getValue()) + 1);
        Iterable<BlockPos> blocks_ = BlockPos.withinManhattan(new BlockPos(BlockPos.containing(mc.player.position()).above()), intRange, intRange, intRange);

        for (BlockPos b : blocks_) {
            BlockState state = mc.level.getBlockState(b);
            if (flatten.getValue() && b.getY() < mc.player.getY())
                continue;
            if (PlayerUtility.squaredDistanceFromEyes(net.minecraft.world.phys.Vec3.atBottomCenterOf(b)) <= range.getPow2Value()) {
                if (avoidLava.getValue() && checkLava(b))
                    continue;
                if (isAllowed(state.getBlock())) {
                    if (ignoreWalls.getValue()) {
                        BlockHitResult result = ExplosionUtility.rayCastBlock(new ClipContext(InteractionUtility.getEyesPos(mc.player), net.minecraft.world.phys.Vec3.atBottomCenterOf(b), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player), b);
                        if(result != null)
                            return new BlockData(b, result.getLocation(), result.getDirection());
                    } else {
                        for (float x1 = 0f; x1 <= 1f; x1 += 0.2f) {
                            for (float y1 = 0f; y1 <= 1; y1 += 0.2f) {
                                for (float z1 = 0f; z1 <= 1; z1 += 0.2f) {
                                    Vec3 p = new Vec3(b.getX() + x1, b.getY() + y1, b.getZ() + z1);
                                    BlockHitResult bhr = mc.level.clip(new ClipContext(InteractionUtility.getEyesPos(mc.player), p, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
                                    if (bhr != null && bhr.getType() == HitResult.Type.BLOCK && bhr.getBlockPos().equals(b))
                                        return new BlockData(b, p, bhr.getDirection());
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean checkLava(BlockPos base) {
        for (Direction dir : Direction.values())
            if (mc.level.getBlockState(base.relative(dir)).getBlock() == Blocks.LAVA)
                return true;
        return false;
    }

    public class NukerThread extends Thread {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (!Module.fullNullCheck()) {
                        while (Managers.ASYNC.ticking.get()) {
                        }

                        if ((targetBlockType != null || !blocks.getValue().equals(BlockSelection.Select)) && !mc.options.keyAttack.isDown() && blockData == null) {
                            blockData = getNukerBlockPos();
                        }
                    } else {
                        Thread.yield();
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    private boolean isAllowed(Block block) {
        boolean allowed = selectedBlocks.getValue().getItemsById().contains(block.getDescriptionId().replace("block.minecraft.", ""));
        return switch (blocks.getValue()) {
            case All -> block != BEDROCK && block != AIR && block != CAVE_AIR && !(block instanceof LiquidBlock) ;
            case Select -> block == targetBlockType;
            case WhiteList -> allowed;
            default -> !allowed && block != BEDROCK && block != AIR && block != CAVE_AIR && !(block instanceof LiquidBlock) ;
        };
    }

    private enum Mode {
        Default, Fast, FastAF
    }

    private enum ColorMode {
        Custom, Sync
    }

    private enum BlockSelection {
        Select, All, BlackList, WhiteList
    }

    public record BlockData(BlockPos bp, Vec3 vec3d, Direction dir) {
    }
}
