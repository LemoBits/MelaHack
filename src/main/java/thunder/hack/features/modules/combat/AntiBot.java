package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import thunder.hack.events.impl.EventSync;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.misc.FakePlayer;
import thunder.hack.features.modules.render.NameTags;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;

public final class AntiBot extends Module {
    public static ArrayList<Player> bots = new ArrayList<>();
    public Setting<Boolean> remove = new Setting<>("Remove", false);
    public Setting<Boolean> onlyAura = new Setting<>("OnlyAura", true);
    private final Setting<Mode> mode = new Setting<>("Mode", Mode.UUIDCheck);
    public Setting<Integer> checkticks = new Setting<>("checkTicks", 3, 0, 10, v -> mode.getValue() == Mode.MotionCheck);
    private final Timer clearTimer = new Timer();
    private int ticks = 0;

    public AntiBot() {
        super("AntiBot", Category.COMBAT);
    }

    @EventHandler
    public void onSync(EventSync e) {
        if (!onlyAura.getValue()) mc.level.players().forEach(this::markAsBot);
        else if (Aura.target instanceof Player ent) this.markAsBot(ent);

        bots.removeIf(Player::isCreative);

        if (remove.getValue())
            bots.forEach(b -> {
                    try {
                        mc.level.removeEntity(b.getId(), Entity.RemovalReason.KILLED);
                    } catch (Exception ignored) {
                    }
            });

        if (clearTimer.passedMs(10000)) {
            bots.clear();
            ticks = 0;
            clearTimer.reset();
        }
    }

    private void markAsBot(Player ent) {
        if (bots.contains(ent))
            return;
        if (ent.isCreative())
            return;

        switch (mode.getValue()) {
            case UUIDCheck -> {
                if (!ent.getUUID().equals(UUID.nameUUIDFromBytes(("OfflinePlayer:" + ent.getName().getString()).getBytes(StandardCharsets.UTF_8))) && ent instanceof RemotePlayer
                        && (FakePlayer.fakePlayer == null || ent.getId() != FakePlayer.fakePlayer.getId())
                        && !ent.getName().getString().contains("-")) {
                    this.addBot(ent);
                }
            }
            case MotionCheck -> {
                double diffX = ent.getX() - ent.xo;
                double diffZ = ent.getZ() - ent.zo;
                
                if ((diffX * diffX) + (diffZ * diffZ) > 0.5D) {
                    if (ticks >= checkticks.getValue())
                        this.addBot(ent);
                    ticks++;
                }
            }
            case ZeroPing -> {
                if (NameTags.getEntityPing(ent) <= 0)
                    this.addBot(ent);
            }
        }
    }

    private void addBot(Player entity) {
        this.sendMessage(entity.getName().getString() + " is a bot!");
        bots.add(entity);
    }

    @Override
    public String getDisplayInfo() {
        return String.valueOf(bots.size());
    }

    public enum Mode {
        UUIDCheck, MotionCheck, ZeroPing
    }
}
