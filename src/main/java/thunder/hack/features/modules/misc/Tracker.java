package thunder.hack.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.item.Items;
import thunder.hack.events.impl.EventEntitySpawn;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.math.MathUtility;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;

import static thunder.hack.features.modules.client.ClientSettings.isRu;

//~pasted~ Ported from 3arthh4ack
public class Tracker extends Module {
    public Tracker() {
        super("Tracker", Category.MISC);
    }

    protected final Setting<Boolean> only1v1 = new Setting<>("1v1-Only", true);

    protected final Set<BlockPos> placed = ConcurrentHashMap.newKeySet();
    protected final AtomicInteger awaitingExp = new AtomicInteger();
    protected static final AtomicInteger crystals = new AtomicInteger();
    protected static final AtomicInteger exp = new AtomicInteger();
    protected static Player trackedPlayer;
    protected boolean awaiting;
    protected int crystalStacks;
    protected int expStacks;

    @Override
    public void onEnable() {
        awaiting = false;
        trackedPlayer = null;
        awaitingExp.set(0);
        crystals.set(0);
        exp.set(0);
        crystalStacks = 0;
        expStacks = 0;
    }

    @Override
    public String getDisplayInfo() {
        return trackedPlayer == null ? null : trackedPlayer.getName().getString();
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (event.getPacket() instanceof ClientboundSystemChatPacket pac) {

            String s = pac.content().getString();
            if (!s.contains("<") && (s.contains("has accepted your duel request") || s.contains("Accepted the duel request from"))) {
                sendMessage(isRu() ? "Дуель принята! Обновляю цель..." : "Duel accepted! Resetting target...");
                trackedPlayer = null;
                awaitingExp.set(0);
                crystals.set(0);
                exp.set(0);
                crystalStacks = 0;
                expStacks = 0;
            }
        }
    }

    @EventHandler
    public void onEntitySpawn(EventEntitySpawn e) {
        if (e.getEntity() instanceof EndCrystal) {
            if (!placed.remove(BlockPos.containing(e.getEntity().getX(), e.getEntity().getY() - 1, e.getEntity().getZ()))) {
                crystals.incrementAndGet();
            }
        }
        if (e.getEntity() instanceof ThrownExperienceBottle) {
            if (awaitingExp.get() > 0) {
                if (mc.player.distanceToSqr(e.getEntity()) < 16) awaitingExp.decrementAndGet();
                else exp.incrementAndGet();
            } else exp.incrementAndGet();
        }
    }

    @Override
    public void onUpdate() {
        boolean found = false;
        for (Player player : mc.level.players()) {
            if (player == null || player.equals(mc.player)) continue;

            if (found && only1v1.getValue()) {
                disable(isRu() ? "Ты не в дуели! Отключаю.." : "Disabled, you are not in a 1v1! Disabling...");
                return;
            }
            if (trackedPlayer == null)
                sendMessage(ChatFormatting.LIGHT_PURPLE + (isRu() ? "Следим за " : "Now tracking ") + ChatFormatting.DARK_PURPLE + player.getName().getString() + ChatFormatting.LIGHT_PURPLE + "!");
            trackedPlayer = player;
            found = true;
        }

        if (trackedPlayer == null) return;

        int exp = this.exp.get() / 64;
        if (expStacks != exp) {
            expStacks = exp;
            if (isRu()) sendMessage(ChatFormatting.DARK_PURPLE + trackedPlayer.getName().getString() + ChatFormatting.LIGHT_PURPLE + " использовал " + ChatFormatting.WHITE + exp + ChatFormatting.LIGHT_PURPLE + (exp == 1 ? " стак" : " стаков") + " Пузырьков опыта!");
            else sendMessage(ChatFormatting.DARK_PURPLE + trackedPlayer.getName().getString() + ChatFormatting.LIGHT_PURPLE + " used " + ChatFormatting.WHITE + exp + ChatFormatting.LIGHT_PURPLE + (exp == 1 ? " stack" : " stacks") + " of XP Bottles!");
        }

        int crystals = this.crystals.get() / 64;
        if (crystalStacks != crystals) {
            crystalStacks = crystals;
            if (!isRu()) sendMessage(ChatFormatting.DARK_PURPLE + trackedPlayer.getName().getString() + ChatFormatting.LIGHT_PURPLE + " used " + ChatFormatting.WHITE + crystals + ChatFormatting.LIGHT_PURPLE + (crystals == 1 ? " stack" : " stacks") + " of Crystals!");
            else sendMessage(ChatFormatting.DARK_PURPLE + trackedPlayer.getName().getString() + ChatFormatting.LIGHT_PURPLE + " использовал " + ChatFormatting.WHITE + crystals + ChatFormatting.LIGHT_PURPLE + (crystals == 1 ? " стак" : " стаков") + " Кристаллов!");
        }
    }

    public void sendTrack() {
        if (trackedPlayer != null) {
            int c = crystals.get();
            int e = exp.get();
            StringBuilder builder;

            if (isRu()) builder = new StringBuilder().append(trackedPlayer.getName().getString()).append(ChatFormatting.LIGHT_PURPLE).append(" использовал ").append(ChatFormatting.WHITE).append(c).append(ChatFormatting.LIGHT_PURPLE).append(" (").append(ChatFormatting.WHITE);
            else builder = new StringBuilder().append(trackedPlayer.getName().getString()).append(ChatFormatting.LIGHT_PURPLE).append(" has used ").append(ChatFormatting.WHITE).append(c).append(ChatFormatting.LIGHT_PURPLE).append(" (").append(ChatFormatting.WHITE);

            if (c % 64 == 0) builder.append(c / 64);
            else builder.append(MathUtility.round(c / 64.0, 1));

            if (isRu()) builder.append(ChatFormatting.LIGHT_PURPLE).append(") кристаллов и ").append(ChatFormatting.WHITE).append(e).append(ChatFormatting.LIGHT_PURPLE).append(" (").append(ChatFormatting.WHITE);
            else builder.append(ChatFormatting.LIGHT_PURPLE).append(") crystals and ").append(ChatFormatting.WHITE).append(e).append(ChatFormatting.LIGHT_PURPLE).append(" (").append(ChatFormatting.WHITE);

            if (e % 64 == 0) builder.append(e / 64);
            else builder.append(MathUtility.round(e / 64.0, 1));

            if (isRu()) builder.append(ChatFormatting.LIGHT_PURPLE).append(") пузырьков опыта.");
            else builder.append(ChatFormatting.LIGHT_PURPLE).append(") bottles of experience.");

            sendMessage(builder.toString());
        }
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (event.getPacket() instanceof ServerboundUseItemPacket) {
            if (mc.player.getMainHandItem().getItem() == Items.EXPERIENCE_BOTTLE || mc.player.getOffhandItem().getItem() == Items.EXPERIENCE_BOTTLE) {
                awaitingExp.incrementAndGet();
            }
        }
        if (event.getPacket() instanceof ServerboundUseItemOnPacket pac) {
            if (mc.player.getMainHandItem().getItem() == Items.END_CRYSTAL || mc.player.getOffhandItem().getItem() == Items.END_CRYSTAL) {
                placed.add(pac.getHitResult().getBlockPos());
            }
        }
    }
}
