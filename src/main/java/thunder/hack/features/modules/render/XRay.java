package thunder.hack.features.modules.render;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import thunder.hack.events.impl.EventMove;
import thunder.hack.events.impl.EventSetting;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;
import thunder.hack.utility.math.FrameRateCounter;
import thunder.hack.utility.math.MathUtility;
import thunder.hack.utility.player.MovementUtility;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import static thunder.hack.features.modules.client.ClientSettings.isRu;

import com.mojang.blaze3d.vertex.PoseStack;

public class XRay extends Module {
    public XRay() {
        super("XRay", Category.MISC);
    }

    private final Setting<Plugin> plugin = new Setting<>("Plugin", Plugin.New);
    public final Setting<Boolean> wallHack = new Setting<>("WallHack", false);
    private final Setting<Boolean> brutForce = new Setting<>("OreDeobf", false);
    private final Setting<Boolean> fast = new Setting<>("Fast", false, v -> brutForce.getValue());
    private final Setting<Integer> delay = new Setting<>("Delay", 25, 1, 100, v -> brutForce.getValue());
    private final Setting<Integer> radius = new Setting<>("Radius", 5, 1, 64, v -> brutForce.getValue());
    private final Setting<Integer> up = new Setting<>("Up", 5, 1, 32, v -> brutForce.getValue());
    private final Setting<Integer> down = new Setting<>("Down", 5, 1, 32, v -> brutForce.getValue());
    private static final Setting<Boolean> netherite = new Setting<>("Netherite", false);
    private static final Setting<Boolean> diamond = new Setting<>("Diamond ", false);
    private static final Setting<Boolean> gold = new Setting<>("Gold", false);
    private static final Setting<Boolean> iron = new Setting<>("Iron", false);
    private static final Setting<Boolean> emerald = new Setting<>("Emerald", false);
    private static final Setting<Boolean> redstone = new Setting<>("Redstone", false);
    private static final Setting<Boolean> lapis = new Setting<>("Lapis", false);
    private static final Setting<Boolean> coal = new Setting<>("Coal", false);
    private static final Setting<Boolean> quartz = new Setting<>("Quartz", false);
    private static final Setting<Boolean> water = new Setting<>("Water", false);
    private static final Setting<Boolean> lava = new Setting<>("Lava", false);

    private final Timer delayTimer = new Timer();
    private final ArrayList<BlockPos> ores = new ArrayList<>();
    private final ArrayList<BlockPos> toCheck = new ArrayList<>();
    private final ArrayList<BlockMemory> checked = new ArrayList<>();
    private BlockPos displayBlock;
    private int done, all;
    private AABB area = new AABB(BlockPos.ZERO);

    @Override
    public void onEnable() {
        ores.clear();
        toCheck.clear();
        checked.clear();
        toCheck.addAll(getBlocks());
        all = toCheck.size();
        done = 0;
        mc.smartCull = false;
        mc.levelRenderer.allChanged();
        area = getArea();
    }

    @Override
    public void onDisable() {
        mc.levelRenderer.allChanged();
        mc.smartCull = true;
    }

    @EventHandler
    public void onReceivePacket(PacketEvent.Receive e) {
        if (e.getPacket() instanceof ClientboundBlockUpdatePacket pac) {
            debug(((ClientboundBlockUpdatePacket) e.getPacket()).getBlockState().getBlock().getName().getString() + " " + pac.getPos().toString());
            if (isCheckableOre(pac.getBlockState().getBlock()) && !ores.contains(pac.getPos())) ores.add(pac.getPos());
        }
    }

    @EventHandler
    public void onSettingChange(EventSetting e) {
        if (e.getSetting() == wallHack) {
            mc.levelRenderer.allChanged();
        }
    }

    @EventHandler
    public void onMove(EventMove e) {
        if (brutForce.getValue()) {
            if (all != done) {
                e.setZ(0);
                e.setX(0);
                e.cancel();
                if (mc.player.tickCount % 8 == 0 && MovementUtility.isMoving())
                    sendMessage(isRu() ? "Не двигайся пока идет деобфускация!" : "Don't move while deobf!");
            } else {
                AABB newArea = getArea();
                if (!newArea.intersects(area)) {
                    area = newArea;
                    toCheck.clear();
                    toCheck.addAll(getBlocks());
                    checked.clear();
                    all = toCheck.size();
                    done = 0;
                }
            }
        }
    }

    @NotNull
    private AABB getArea() {
        int radius_ = plugin.is(Plugin.New) ? Math.min(4, radius.getValue()) : radius.getValue();
        int down_ = plugin.is(Plugin.New) ? Math.min(3, down.getValue()) : down.getValue();
        int up_ = plugin.is(Plugin.New) ? Math.min(4, up.getValue()) : up.getValue();
        return new AABB(mc.player.getX() - radius_, mc.player.getY() - down_, mc.player.getZ() - radius_, mc.player.getX() + radius_, mc.player.getY() + up_, mc.player.getZ() + radius_);
    }

    @Override
    public void onUpdate() {
        if (plugin.is(Plugin.New))
            checked.forEach(blockMemory -> {
                if (blockMemory.isDelayed() && !ores.contains(blockMemory.bp))
                    ores.add(blockMemory.bp);
            });
    }

    public void onRender3D(PoseStack stack) { //FABOS IDI NAHUI
        for (BlockPos pos : ores) {
            Block block = mc.level.getBlockState(pos).getBlock();
            if ((block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) && diamond.getValue())
                draw(pos, 0, 255, 255);
            if ((block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE) && gold.getValue())
                draw(pos, 255, 215, 0);
            if (block == Blocks.NETHER_GOLD_ORE && gold.getValue()) draw(pos, 255, 215, 0);
            if ((block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) && iron.getValue())
                draw(pos, 213, 213, 213);
            if ((block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) && emerald.getValue())
                draw(pos, 0, 255, 77);
            if ((block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) && redstone.getValue())
                draw(pos, 255, 0, 0);
            if (block == Blocks.COAL_ORE && coal.getValue()) draw(pos, 0, 0, 0);
            if ((block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) && lapis.getValue())
                draw(pos, 38, 97, 156);
            if (block == Blocks.ANCIENT_DEBRIS && netherite.getValue()) draw(pos, 255, 255, 255);
            if (block == Blocks.NETHER_QUARTZ_ORE && quartz.getValue()) draw(pos, 170, 170, 170);
        }

        if (displayBlock != null && (done != all)) draw(displayBlock, 255, 0, 60);

        if (brutForce.getValue())
            Render3DEngine.OUTLINE_QUEUE.add(new Render3DEngine.OutlineAction(area, HudEditor.getColor(1), 2));

        if (toCheck.isEmpty() || !brutForce.getValue()) return;

        if (mc.isLocalServer()) {
            disable(isRu() ? "Братан, ты в синглплеере" : "Bro, you're in singleplayer");
            return;
        }

        if (mc.player.getMainHandItem().is(ItemTags.PICKAXES)) {
            if (mc.player.tickCount % 8 == 0) disable(isRu() ? "Убери кирку из руки!" : "Remove pickaxe from ur hand!");
            return;
        }

        if (delayTimer.every(delay.getValue())) {
            BlockPos pos = toCheck.remove(toCheck.size() - 1 <= 1 ? 0 : ThreadLocalRandom.current().nextInt(0, toCheck.size() - 1));
            mc.gameMode.startDestroyBlock(displayBlock = pos, mc.player.getDirection());
            mc.gameMode.stopDestroyBlock();
            sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            checked.add(new BlockMemory(pos));
            ++done;
        }
    }

    private void draw(BlockPos pos, int r, int g, int b) {
        Render3DEngine.FILLED_QUEUE.add(new Render3DEngine.FillAction(new AABB(pos), new Color(r, g, b, 100)));
        Render3DEngine.OUTLINE_QUEUE.add(new Render3DEngine.OutlineAction(new AABB(pos), new Color(r, g, b, 200), 2));
    }

    public void onRender2D(GuiGraphicsExtractor context) {
        if (brutForce.getValue()) {

            float posX = mc.getWindow().getGuiScaledWidth() / 2f - 68;
            float posY = mc.getWindow().getGuiScaledHeight() / 2f + 68;

            Render2DEngine.drawGradientBlurredShadow(context.pose(), posX + 2, posY + 2, 133, 44, 14, HudEditor.getColor(270), HudEditor.getColor(0), HudEditor.getColor(180), HudEditor.getColor(90));
            Render2DEngine.renderRoundedGradientRect(context.pose(), HudEditor.getColor(270), HudEditor.getColor(0), HudEditor.getColor(180), HudEditor.getColor(90), posX, posY, 137, 47.5f, 9);
            Render2DEngine.drawRound(context.pose(), posX + 0.5f, posY + 0.5f, 136f, 46, 9, Render2DEngine.injectAlpha(Color.BLACK, 220));

            Render2DEngine.drawGradientRound(context.pose(), posX + 4, posY + 32, 129, 11, 4f, HudEditor.getColor(0).darker().darker(), HudEditor.getColor(0).darker().darker().darker().darker(), HudEditor.getColor(0).darker().darker().darker().darker(), HudEditor.getColor(0).darker().darker().darker().darker());

            Render2DEngine.renderRoundedGradientRect(context.pose(), HudEditor.getColor(270), HudEditor.getColor(0), HudEditor.getColor(0), HudEditor.getColor(270), posX + 4, posY + 32, (int) Mth.clamp((129 * ((float) done / Math.max((float) all, 1))), 8, 129), 11, 4f);

            FontRenderers.sf_bold.drawCenteredString(context.pose(), (int) ((float) done / (float) all * 100) + "%", posX + 68, posY + 35f, -1);
            FontRenderers.sf_bold.drawCenteredString(context.pose(), "XRay", posX + 68, posY + 7, -1);
            double time = 0;
            try {
                time = MathUtility.round((all - done) * ((1000. / FrameRateCounter.INSTANCE.getFps() + delay.getValue()) / 1000f), 1);
            } catch (NumberFormatException ignored) {
            }
            FontRenderers.sf_bold.drawCenteredString(context.pose(), done + " / " + all + (isRu() ? " Осталось: " : " Estimated time: ") + time + "s", posX + 68, posY + 18, -1);

        }
    }

    public static boolean isCheckableOre(Block block) {
        if (diamond.getValue() && (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE)) return true;
        if (gold.getValue() && (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE || block == Blocks.NETHER_GOLD_ORE))
            return true;
        if (iron.getValue() && (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE)) return true;
        if (emerald.getValue() && (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE)) return true;
        if (redstone.getValue() && (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE))
            return true;
        if (coal.getValue() && (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE)) return true;
        if (netherite.getValue() && block == Blocks.ANCIENT_DEBRIS) return true;
        if (water.getValue() && block == Blocks.WATER) return true;
        if (lava.getValue() && block == Blocks.LAVA) return true;
        if (quartz.getValue() && block == Blocks.NETHER_QUARTZ_ORE) return true;
        if (lapis.getValue() && (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE)) return true;
        return lapis.getValue() && (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE);
    }

    private ArrayList<BlockPos> getBlocks() {
        int radius_ = plugin.is(Plugin.New) ? Math.min(4, radius.getValue()) : radius.getValue();
        int down_ = plugin.is(Plugin.New) ? Math.min(3, down.getValue()) : down.getValue();
        int up_ = plugin.is(Plugin.New) ? Math.min(4, up.getValue()) : up.getValue();

        ArrayList<BlockPos> positions = new ArrayList<>();
        for (int x = (int) (mc.player.getX() - radius_); x < mc.player.getX() + radius_; x++)
            for (int y = (int) (mc.player.getY() - down_); y < mc.player.getY() + up_; y++)
                for (int z = (int) (mc.player.getZ() - radius_); z < mc.player.getZ() + radius_; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (mc.level.isEmptyBlock(pos) || (fast.getValue() && plugin.is(Plugin.Old) && (x % 2 == 0 || y % 2 == 0 || z % 2 == 0)))
                        continue;
                    positions.add(pos);
                }
        return positions;
    }

    public class BlockMemory {
        private final BlockPos bp;
        private long time = 0;

        public BlockMemory(BlockPos bp) {
            this.bp = bp;
        }

        private boolean isDelayed() {
            return this.time++ > 10;
        }
    }

    private enum Plugin {
        Old, New;
    }
}
