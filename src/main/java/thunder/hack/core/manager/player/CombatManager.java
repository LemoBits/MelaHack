package thunder.hack.core.manager.player;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import thunder.hack.ThunderHack;
import thunder.hack.core.manager.IManager;
import thunder.hack.core.Managers;
import thunder.hack.events.impl.EventPostTick;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.events.impl.TotemPopEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.combat.AntiBot;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CombatManager implements IManager {
    public HashMap<String, Integer> popList = new HashMap<>();

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (Module.fullNullCheck()) return;

        if (event.getPacket() instanceof ClientboundEntityEventPacket pac) {
            if (pac.getEventId() == EntityEvent.PROTECTED_FROM_DEATH) {
                Entity ent = pac.getEntity(mc.level);
                if (!(ent instanceof Player)) return;
                if (popList == null) {
                    popList = new HashMap<>();
                }
                if (popList.get(ent.getName().getString()) == null) {
                    popList.put(ent.getName().getString(), 1);
                } else if (popList.get(ent.getName().getString()) != null) {
                    popList.put(ent.getName().getString(), popList.get(ent.getName().getString()) + 1);
                }
                ThunderHack.EVENT_BUS.post(new TotemPopEvent((Player) ent, popList.get(ent.getName().getString())));
            }
        }
    }

    @EventHandler
    public void onPostTick(EventPostTick event) {
        if (Module.fullNullCheck())
            return;
        for (Player player : mc.level.players()) {
            if (AntiBot.bots.contains(player)) continue;

            if (player.getHealth() <= 0 && popList.containsKey(player.getName().getString()))
                popList.remove(player.getName().getString(), popList.get(player.getName().getString()));
        }
    }

    public int getPops(@NotNull Player entity) {
        if (popList.get(entity.getName().getString()) == null) return 0;
        return popList.get(entity.getName().getString());
    }

    public List<Player> getTargets(float range) {
        return mc.level.players().stream()
                .filter(e -> !e.isDeadOrDying())
                .filter(entityPlayer -> !Managers.FRIEND.isFriend(entityPlayer.getName().getString()))
                .filter(entityPlayer -> entityPlayer != mc.player)
                .filter(entityPlayer -> mc.player.distanceToSqr(entityPlayer) < range * range)
                .sorted(Comparator.comparing(e -> mc.player.distanceToSqr(e)))
                .collect(Collectors.toList());
    }

    public @Nullable Player getTarget(float range, @NotNull TargetBy targetBy) {
        Player target = null;

        switch (targetBy) {
            case FOV -> target = getTargetByFOV(range);
            case Health -> target = getTargetByHealth(range);
            case Distance -> target = getNearestTarget(range);
        }

        return target;
    }

    public @Nullable Player getNearestTarget(float range) {
        return getTargets(range).stream().min(Comparator.comparing(t -> mc.player.distanceTo(t))).orElse(null);
    }

    public Player getTargetByHealth(float range) {
        return getTargets(range).stream().min(Comparator.comparing(t -> (t.getHealth() + t.getAbsorptionAmount()))).orElse(null);
    }

    public Player getTargetByFOV(float range) {
        return getTargets(range).stream().min(Comparator.comparing(this::getFOVAngle)).orElse(null);
    }

    public Player getTargetByFOV(float range, float fov) {
        return getTargets(range).stream()
                .filter(entityPlayer -> getFOVAngle(entityPlayer) < fov)
                .min(Comparator.comparing(this::getFOVAngle)).orElse(null);
    }

    public @Nullable Player getTarget(float range, @NotNull TargetBy targetBy, @NotNull Predicate<Player> predicate) {
        Player target = null;

        switch (targetBy) {
            case FOV -> target = getTargetByFOV(range, predicate);
            case Health -> target = getTargetByHealth(range, predicate);
            case Distance -> target = getNearestTarget(range, predicate);
        }

        return target;
    }

    public @Nullable Player getNearestTarget(float range, Predicate<Player> predicate) {
        return getTargets(range).stream()
                .filter(predicate)
                .min(Comparator.comparing(t -> mc.player.distanceTo(t)))
                .orElse(null);
    }

    public Player getTargetByHealth(float range, Predicate<Player> predicate) {
        return getTargets(range).stream()
                .filter(predicate)
                .min(Comparator.comparing(t -> (t.getHealth() + t.getAbsorptionAmount())))
                .orElse(null);
    }

    public Player getTargetByFOV(float range, Predicate<Player> predicate) {
        return getTargets(range).stream()
                .filter(predicate)
                .min(Comparator.comparing(this::getFOVAngle))
                .orElse(null);
    }

    private float getFOVAngle(@NotNull LivingEntity e) {
        float yaw = (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(e.getZ() - mc.player.getZ(), e.getX() - mc.player.getX())) - 90.0);
        return Math.abs(yaw - Mth.wrapDegrees(mc.player.getYRot()));
    }

    public enum TargetBy {
        Distance,
        FOV,
        Health
    }
}
