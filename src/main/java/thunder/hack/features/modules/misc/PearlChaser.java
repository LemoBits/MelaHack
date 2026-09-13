package thunder.hack.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventEntitySpawn;
import thunder.hack.events.impl.EventPostSync;
import thunder.hack.events.impl.EventSync;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.combat.Aura;
import thunder.hack.features.modules.combat.AutoCrystal;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.BooleanSettingGroup;
import thunder.hack.utility.Timer;
import thunder.hack.utility.math.MathUtility;
import thunder.hack.utility.player.MovementUtility;

import java.util.Comparator;
import java.util.HashMap;

import static thunder.hack.features.modules.client.ClientSettings.isRu;

public class PearlChaser extends Module {
    public PearlChaser() { //todo better targeting?..
        super("PearlChaser", Category.MISC);
    }

    private final Setting<BooleanSettingGroup> stopMotion = new Setting<>("StopMotion", new BooleanSettingGroup(false));
    private final Setting<Boolean> legitStop = new Setting<>("LegitStop", false).addToGroup(stopMotion);
    private final Setting<Boolean> pauseAura = new Setting<>("PauseAura", false);
    private final Setting<Boolean> onlyOnGround = new Setting<>("OnlyOnGround", false);
    private final Setting<Boolean> noMove = new Setting<>("NoMove", false);
    private final Setting<Boolean> onlyTarget = new Setting<>("OnlyTarget", false);

    private Runnable postSyncAction;
    private final Timer delayTimer = new Timer();
    private BlockPos targetBlock;
    private int lastPearlId;
    private int lastOurPearlId;
    private HashMap<Player, Long> targets = new HashMap<>();

    @EventHandler
    public void onEntitySpawn(EventEntitySpawn e) {
        if (e.getEntity() instanceof ThrownEnderpearl)
            mc.level.players().stream()
                    .min(Comparator.comparingDouble((p) -> p.distanceToSqr(e.getEntity().position())))
                    .ifPresent((player) -> {
                        if (player.equals(mc.player))
                            lastOurPearlId = e.getEntity().getId();
                    });

    }

    @EventHandler(priority = EventPriority.LOW)
    public void onSync(EventSync event) {
        if (onlyTarget.getValue()) {
            if (Aura.target != null && ModuleManager.aura.isEnabled() && Aura.target instanceof Player pl && !targets.containsKey(pl))
                targets.put(pl, System.currentTimeMillis());

            if (AutoCrystal.target != null && ModuleManager.autoCrystal.isEnabled() && AutoCrystal.target instanceof Player pl && !targets.containsKey(pl))
                targets.put(pl, System.currentTimeMillis());

            new HashMap<>(targets).forEach((k, v) -> {
                if (System.currentTimeMillis() - v > 10000)
                    targets.remove(k);
            });
        }

        // Анти селфкилл
        if (mc.player.getHealth() < 5)
            return;

        // Антиспам
        if (!delayTimer.passedMs(1000))
            return;

        for (Entity ent : mc.level.entitiesForRendering()) {
            if (!(ent instanceof ThrownEnderpearl)) continue;
            if (ent.getId() == lastPearlId || ent.getId() == lastOurPearlId) continue;
            mc.level.players().stream()
                    .filter(e -> targets.containsKey(e) || !onlyTarget.getValue())
                    .min(Comparator.comparingDouble((p) -> p.distanceToSqr(ent.position())))
                    .ifPresent((player) -> {
                        if (!player.equals(mc.player)) {
                            targetBlock = calcTrajectory(ent);
                            lastPearlId = ent.getId();
                        }
                    });
        }

        // Анти NPE
        if (targetBlock == null)
            return;

        // Нет смысла кидать если кидают в нас
        if (mc.player.distanceToSqr(targetBlock.getCenter()) < 49)
            return;

        float rotationPitch = (float) (-Math.toDegrees(calcTrajectory(targetBlock)));
        float rotationYaw = (float) Math.toDegrees(Math.atan2(targetBlock.getZ() + 0.5f - mc.player.getZ(), targetBlock.getX() + 0.5f - mc.player.getX())) - 90.0f;
        BlockPos tracedBP = checkTrajectory(rotationYaw, rotationPitch);

        if (tracedBP == null || targetBlock.distToCenterSqr(tracedBP.getCenter()) > 36)
            return;

        if(pauseAura.getValue() && ModuleManager.aura.isEnabled())
            ModuleManager.aura.pause();

        if(onlyOnGround.getValue() && !mc.player.onGround())
            return;

        if(noMove.getValue() && MovementUtility.isMoving())
            return;

        if(stopMotion.getValue().isEnabled()) {
            if(!legitStop.getValue())
                mc.player.setDeltaMovement(0,0,0);
            mc.options.keyUp.setDown(false);
            mc.options.keyDown.setDown(false);
            mc.options.keyLeft.setDown(false);
            mc.options.keyRight.setDown(false);
            thunder.hack.utility.player.MovementUtility.clearMovementInput();
            return;
        }

        sendMessage(isRu() ?
                ("Догоняем перл! Позиция X:" + tracedBP.getX() + " Y:" + tracedBP.getY() + " Z:" + tracedBP.getZ() + " Углы Y:" + rotationYaw + " P:" + rotationPitch) :
                ("Chasing pearl on X:" + tracedBP.getX() + " Y:" + tracedBP.getY() + " Z:" + tracedBP.getZ() + " Angle Y:" + rotationYaw + " P:" + rotationPitch));

        mc.player.setYRot(rotationYaw);
        mc.player.setXRot(MathUtility.clamp(rotationPitch, -89, 89));

        float yaw = mc.player.getYRot();
        float pitch = mc.player.getXRot();

        postSyncAction = () -> {
            int epSlot = findEPSlot();
            int originalSlot = mc.player.getInventory().getSelectedSlot();
            if (epSlot != -1) {
                mc.player.getInventory().setSelectedSlot(epSlot);
                sendPacket(new ServerboundSetCarriedItemPacket(epSlot));
                sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, yaw, pitch));
                sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                mc.player.getInventory().setSelectedSlot(originalSlot);
                sendPacket(new ServerboundSetCarriedItemPacket(originalSlot));
            }
        };

        targetBlock = null;
        delayTimer.reset();
    }

    @EventHandler
    public void onPostSync(EventPostSync event) {
        if (postSyncAction != null) {
            postSyncAction.run();
            postSyncAction = null;
        }
    }

    private int findEPSlot() {
        int epSlot = -1;
        if (mc.player.getMainHandItem().getItem() == Items.ENDER_PEARL)
            epSlot = mc.player.getInventory().getSelectedSlot();
        if (epSlot == -1)
            for (int l = 0; l < 9; ++l)
                if (mc.player.getInventory().getItem(l).getItem() == Items.ENDER_PEARL) {
                    epSlot = l;
                    break;
                }
        return epSlot;
    }

    private float calcTrajectory(@NotNull BlockPos bp) {
        double a = Math.hypot(bp.getX() + 0.5f - mc.player.getX(), bp.getZ() + 0.5f - mc.player.getZ());
        double y = 6.125 * ((bp.getY() + 1f) - (mc.player.getY() + (double) mc.player.getEyeHeight(mc.player.getPose())));
        y = 0.05000000074505806 * ((0.05000000074505806 * (a * a)) + y);
        y = Math.sqrt(9.37890625 - y);
        double d = 3.0625 - y;
        y = Math.atan2(d * d + y, 0.05000000074505806 * a);
        d = Math.atan2(d, 0.05000000074505806 * a);
        return (float) Math.min(y, d);
    }

    private BlockPos calcTrajectory(Entity e) {
        return traceTrajectory(e.getX(), e.getY(), e.getZ(), e.getDeltaMovement().x, e.getDeltaMovement().y, e.getDeltaMovement().z);
    }

    private BlockPos checkTrajectory(float yaw, float pitch) {
        if (Float.isNaN(pitch))
            return null;
        float yawRad = yaw / 180.0f * 3.1415927f;
        float pitchRad = pitch / 180.0f * 3.1415927f;
        double x = mc.player.getX() - Mth.cos(yawRad) * 0.16f;
        double y = mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()) - 0.1000000014901161;
        double z = mc.player.getZ() - Mth.sin(yawRad) * 0.16f;
        double motionX = -Mth.sin(yawRad) * Mth.cos(pitchRad) * 0.4f;
        double motionY = -Mth.sin(pitchRad) * 0.4f;
        double motionZ = Mth.cos(yawRad) * Mth.cos(pitchRad) * 0.4f;
        final float distance = Mth.sqrt((float) (motionX * motionX + motionY * motionY + motionZ * motionZ));
        motionX /= distance;
        motionY /= distance;
        motionZ /= distance;
        motionX *= 1.5f;
        motionY *= 1.5f;
        motionZ *= 1.5f;
        if (!mc.player.onGround()) motionY += mc.player.getDeltaMovement().y();
        return traceTrajectory(x, y, z, motionX, motionY, motionZ);
    }

    private BlockPos traceTrajectory(double x, double y, double z, double mx, double my, double mz) {
        Vec3 lastPos;
        for (int i = 0; i < 300; i++) {
            lastPos = new Vec3(x, y, z);
            x += mx;
            y += my;
            z += mz;
            mx *= 0.99;
            my *= 0.99;
            mz *= 0.99;
            my -= 0.03f;
            Vec3 pos = new Vec3(x, y, z);
            BlockHitResult bhr = mc.level.clip(new ClipContext(lastPos, pos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
            if (bhr != null && bhr.getType() == HitResult.Type.BLOCK) return bhr.getBlockPos();

            for (Entity ent : mc.level.entitiesForRendering()) {
                if (ent instanceof Arrow || ent == mc.player || ent instanceof ThrownEnderpearl) continue;
                if (ent.getBoundingBox().intersects(new AABB(x - 0.3, y - 0.3, z - 0.3, x + 0.3, y + 0.3, z + 0.2)))
                    return null;
            }

            if (y <= -65) break;
        }
        return null;
    }
}
