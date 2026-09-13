package thunder.hack.core.manager.player;

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.core.*;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import thunder.hack.core.manager.IManager;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.*;
import thunder.hack.injection.accesors.IClientPlayerEntity;
import thunder.hack.injection.accesors.IEntity;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.combat.Aura;
import thunder.hack.utility.Timer;
import thunder.hack.utility.world.ExplosionUtility;
import thunder.hack.utility.math.MathUtility;

import java.util.ArrayDeque;

import static net.minecraft.util.Mth.clamp;

public class PlayerManager implements IManager {
    public float yaw, pitch, lastYaw, lastPitch, currentPlayerSpeed, averagePlayerSpeed;
    public int ticksElytraFlying, serverSideSlot;
    public final Timer switchTimer = new Timer();

    private final ArrayDeque<Float> speedResult = new ArrayDeque<>(20);

    public float bodyYaw, prevBodyYaw;

    // Мы можем зайти в инвентарь, и сервер этого не узнает, пока мы не начнем кликать
    // Юзать везде!
    // We can go into inventory and the server won't know until we start clicking
    // Use everywhere!

    public boolean inInventory;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSync(EventSync event) {
        if (Module.fullNullCheck()) return;

        yaw = mc.player.getYRot();
        pitch = mc.player.getXRot();
        lastYaw = ((IEntity) mc.player).getLastYaw();
        lastPitch = ((IEntity) mc.player).getLastPitch();
        if (mc.screen == null) inInventory = false;
        if (mc.player.isFallFlying() && mc.player.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
            ticksElytraFlying++;
        } else ticksElytraFlying = 0;
    }

    @EventHandler
    public void onTick(EventTick e) {
        currentPlayerSpeed = (float) Math.hypot(mc.player.getX() - mc.player.xo, mc.player.getZ() - mc.player.zo);

        if (speedResult.size() > 20)
            speedResult.poll();

        speedResult.add(currentPlayerSpeed);

        float average = 0.0f;

        for (Float value : speedResult) average += MathUtility.clamp(value, 0f, 20f);

        averagePlayerSpeed = average / (float) speedResult.size();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void postSync(EventPostSync event) {
        if (mc.player == null) return;

        prevBodyYaw = bodyYaw;
        bodyYaw = getBodyYaw();

        if (!ModuleManager.rotations.clientLook.getValue()) {
            mc.player.setYRot(yaw);
            mc.player.setXRot(pitch);
        }

        ModuleManager.rotations.fixRotation = Float.NaN;
    }

    @EventHandler
    public void onJump(EventPlayerJump e) {
        ModuleManager.rotations.onJump(e);
    }

    @EventHandler
    public void onPlayerMove(EventFixVelocity e) {
        ModuleManager.rotations.onPlayerMove(e);
    }

    @EventHandler
    public void modifyVelocity(EventPlayerTravel e) {
        ModuleManager.rotations.modifyVelocity(e);
    }

    @EventHandler
    public void onKeyInput(EventKeyboardInput e) {
        ModuleManager.rotations.onKeyInput(e);
    }

    @EventHandler
    public void onSyncWithServer(PacketEvent.@NotNull Send event) {
        if (event.getPacket() instanceof ServerboundContainerClickPacket) {
            inInventory = true;
        }
        if (event.getPacket() instanceof ServerboundSetCarriedItemPacket slot) {
            switchTimer.reset();
            serverSideSlot = slot.getSlot();
        }
        if (event.getPacket() instanceof ServerboundContainerClosePacket) {
            inInventory = false;
        }
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.@NotNull Receive event) {
        if (event.getPacket() instanceof ClientboundSetHeldSlotPacket slot) {
            switchTimer.reset();
            serverSideSlot = slot.slot();
        }
    }

    private float getBodyYaw() {
        double x = mc.player.getX() - mc.player.xo;
        double z = mc.player.getZ() - mc.player.zo;
        float offset = bodyYaw;
        if ((x * x + z * z) > 0.0025000002f) offset = (float) (Mth.atan2(z, x) * 57.295776f - 90.0f);
        if (mc.player.attackAnim > 0.0f)
            offset = ((IEntity) Minecraft.getInstance().player).getLastYaw();
        float deltaBodyYaw = clamp(Mth.wrapDegrees((((IEntity) Minecraft.getInstance().player).getLastYaw()) - (bodyYaw + Mth.wrapDegrees(offset - bodyYaw) * 0.3f)), -45.0f, 75.0f);
        return (deltaBodyYaw > 50f ? deltaBodyYaw * 0.2f : 0) + ((IEntity) Minecraft.getInstance().player).getLastYaw() - deltaBodyYaw;
    }

    public boolean checkRtx(float yaw, float pitch, float distance, float wallDistance, Aura.RayTrace rt) {
        if (rt == Aura.RayTrace.OFF)
            return true;

        HitResult result = rayTrace(distance, yaw, pitch);
        Vec3 startPoint = mc.player.position().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0);
        double distancePow2 = Math.pow(distance, 2);

        if (result != null)
            distancePow2 = startPoint.distanceToSqr(result.getLocation());

        Vec3 rotationVector = getRotationVector(pitch, yaw).scale(distance);
        Vec3 endPoint = startPoint.add(rotationVector);

        AABB entityArea = mc.player.getBoundingBox().expandTowards(rotationVector).inflate(1.0, 1.0, 1.0);

        EntityHitResult ehr;

        double maxDistance = Math.max(distancePow2, Math.pow(wallDistance, 2));

        if (rt == Aura.RayTrace.OnlyTarget && Aura.target != null)
            ehr = ProjectileUtil.getEntityHitResult(mc.player, startPoint, endPoint, entityArea, e -> !e.isSpectator() && e.isPickable() && e == Aura.target, maxDistance);
        else
            ehr = ProjectileUtil.getEntityHitResult(mc.player, startPoint, endPoint, entityArea, e -> !e.isSpectator() && e.isPickable(), maxDistance);

        if (ehr != null) {
            boolean allowedWallDistance = startPoint.distanceToSqr(ehr.getLocation()) <= Math.pow(wallDistance, 2);
            boolean wallMissing = result == null;
            boolean wallBehindEntity = startPoint.distanceToSqr(ehr.getLocation()) < distancePow2;
            boolean allowWallHit = wallMissing || allowedWallDistance || wallBehindEntity;

            if (allowWallHit && startPoint.distanceToSqr(ehr.getLocation()) <= Math.pow(distance, 2))
                return ehr.getEntity() == Aura.target || Aura.target == null || rt == Aura.RayTrace.OnlyTarget;
        }

        return false;
    }

    public boolean checkRtx(float yaw, float pitch, float distance, float wallDistance, Entity entity) {
        HitResult result = rayTrace(distance, yaw, pitch);
        Vec3 startPoint = mc.player.position().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0);
        double distancePow2 = Math.pow(distance, 2);

        if (result != null)
            distancePow2 = startPoint.distanceToSqr(result.getLocation());

        Vec3 rotationVector = getRotationVector(pitch, yaw).scale(distance);
        Vec3 endPoint = startPoint.add(rotationVector);

        AABB entityArea = mc.player.getBoundingBox().expandTowards(rotationVector).inflate(1.0, 1.0, 1.0);

        EntityHitResult ehr;

        double maxDistance = Math.max(distancePow2, Math.pow(wallDistance, 2));

        ehr = ProjectileUtil.getEntityHitResult(mc.player, startPoint, endPoint, entityArea, e -> !e.isSpectator() && e.isPickable() && e == entity, maxDistance);

        if (ehr != null) {
            boolean allowedWallDistance = startPoint.distanceToSqr(ehr.getLocation()) <= Math.pow(wallDistance, 2);
            boolean wallMissing = result == null;
            boolean wallBehindEntity = startPoint.distanceToSqr(ehr.getLocation()) < distancePow2;
            boolean allowWallHit = wallMissing || allowedWallDistance || wallBehindEntity;

            if (allowWallHit && startPoint.distanceToSqr(ehr.getLocation()) <= Math.pow(distance, 2))
                return ehr.getEntity() == entity;
        }

        return false;
    }


    public Entity getRtxTarget(float yaw, float pitch, float distance, boolean ignoreWalls) {
        Entity targetedEntity = null;
        HitResult result = ignoreWalls ? null : rayTrace(distance, yaw, pitch);
        Vec3 vec3d = mc.player.position().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0);
        double distancePow2 = Math.pow(distance, 2);
        if (result != null) distancePow2 = result.getLocation().distanceToSqr(vec3d);
        Vec3 vec3d2 = getRotationVector(pitch, yaw);
        Vec3 vec3d3 = vec3d.add(vec3d2.x * distance, vec3d2.y * distance, vec3d2.z * distance);
        AABB box = mc.player.getBoundingBox().expandTowards(vec3d2.scale(distance)).inflate(1.0, 1.0, 1.0);
        EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(mc.player, vec3d, vec3d3, box, (entity) -> !entity.isSpectator() && entity.isPickable(), distancePow2);
        if (entityHitResult != null) {
            Entity entity2 = entityHitResult.getEntity();
            Vec3 vec3d4 = entityHitResult.getLocation();
            double g = vec3d.distanceToSqr(vec3d4);
            if (g < distancePow2 || result == null) {
                if (entity2 instanceof LivingEntity) {
                    targetedEntity = entity2;
                    return targetedEntity;
                }
            }
        }
        return targetedEntity;
    }

    public Vec3 getRtxPoint(float yaw, float pitch, float distance) {
        Vec3 vec3d = mc.player.position().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0);
        double distancePow2 = Math.pow(distance, 2);
        Vec3 vec3d2 = getRotationVector(pitch, yaw);
        Vec3 vec3d3 = vec3d.add(vec3d2.x * distance, vec3d2.y * distance, vec3d2.z * distance);
        AABB box = mc.player.getBoundingBox().expandTowards(vec3d2.scale(distance)).inflate(1.0, 1.0, 1.0);
        EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(mc.player, vec3d, vec3d3, box, (entity) -> !entity.isSpectator() && entity.isPickable(), distancePow2);
        if (entityHitResult != null) {
            Entity entity2 = entityHitResult.getEntity();
            Vec3 vec3d4 = entityHitResult.getLocation();
            if (entity2 instanceof LivingEntity) {
                return vec3d4;
            }
        }
        return null;
    }

    public boolean isLookingAtBox(float yaw, float pitch, BlockPos blockPos) {
        Vec3 vec3d = mc.player.getEyePosition(1f);
        Vec3 vec3d2 = getRotationVector(pitch, yaw);
        Vec3 vec3d3 = vec3d.add(vec3d2.x * 7, vec3d2.y * 7, vec3d2.z * 7);
        BlockHitResult result = ExplosionUtility.rayCastBlock(new ClipContext(vec3d, vec3d3, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player), blockPos);
        return result != null && result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(blockPos);
    }

    public HitResult rayTrace(double dst, float yaw, float pitch) {
        Vec3 vec3d = mc.player.getEyePosition(1f);
        Vec3 vec3d2 = getRotationVector(pitch, yaw);
        Vec3 vec3d3 = vec3d.add(vec3d2.x * dst, vec3d2.y * dst, vec3d2.z * dst);
        return mc.level.clip(new ClipContext(vec3d, vec3d3, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
    }

    public HitResult getRtxTarget(float yaw, float pitch, double x, double y, double z) {
        HitResult result = rayTrace(5, yaw, pitch, x, y, z);
        Vec3 vec3d = new Vec3(x, y, z).add(0, mc.player.getEyeHeight(mc.player.getPose()), 0);
        double distancePow2 = 25;
        if (result != null)
            distancePow2 = result.getLocation().distanceToSqr(vec3d);
        Vec3 vec3d2 = getRotationVector(pitch, yaw);
        Vec3 vec3d3 = vec3d.add(vec3d2.x * 5, vec3d2.y * 5, vec3d2.z * 5);
        AABB box = new AABB(x - .3, y, z - .3, x + .3, y + 1.8, z + .3).expandTowards(vec3d2.scale(5)).inflate(1.0, 1.0, 1.0);
        EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(mc.player, vec3d, vec3d3, box, (entity) -> !entity.isSpectator() && entity.isPickable(), distancePow2);
        if (entityHitResult != null) {
            Entity entity2 = entityHitResult.getEntity();
            Vec3 vec3d4 = entityHitResult.getLocation();
            double g = vec3d.distanceToSqr(vec3d4);
            if (g < distancePow2 || result == null) {
                if (entity2 instanceof LivingEntity) {
                    return entityHitResult;
                }
            }
        }
        return result;
    }

    public boolean isInWeb() {
        AABB pBox = mc.player.getBoundingBox();
        BlockPos pBlockPos = BlockPos.containing(mc.player.position());

        for (int x = pBlockPos.getX() - 2; x <= pBlockPos.getX() + 2; x++) {
            for (int y = pBlockPos.getY() - 1; y <= pBlockPos.getY() + 4; y++) {
                for (int z = pBlockPos.getZ() - 2; z <= pBlockPos.getZ() + 2; z++) {
                    BlockPos bp = new BlockPos(x, y, z);
                    if (pBox.intersects(new AABB(bp)) && mc.level.getBlockState(bp).getBlock() == Blocks.COBWEB)
                        return true;
                }
            }
        }

        return false;
    }

    public HitResult rayTrace(double dst, float yaw, float pitch, double x, double y, double z) {
        Vec3 vec3d = new Vec3(x, y, z);
        Vec3 vec3d2 = getRotationVector(pitch, yaw);
        Vec3 vec3d3 = vec3d.add(vec3d2.x * dst, vec3d2.y * dst, vec3d2.z * dst);
        return mc.level.clip(new ClipContext(vec3d, vec3d3, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
    }

    public static float[] calcAngle(Vec3 to) {
        if (to == null) return null;
        double difX = to.x - mc.player.getEyePosition().x;
        double difY = (to.y - mc.player.getEyePosition().y) * -1.0;
        double difZ = to.z - mc.player.getEyePosition().z;
        double dist = Mth.sqrt((float) (difX * difX + difZ * difZ));
        return new float[]{(float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(difZ, difX)) - 90.0), (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(difY, dist)))};
    }

    public static Vec2 calcAngleVec(Vec3 to) {
        if (to == null) return null;
        double difX = to.x - mc.player.getEyePosition().x;
        double difY = (to.y - mc.player.getEyePosition().y) * -1.0;
        double difZ = to.z - mc.player.getEyePosition().z;
        double dist = Mth.sqrt((float) (difX * difX + difZ * difZ));
        return new Vec2((float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(difZ, difX)) - 90.0), (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(difY, dist))));
    }

    public @NotNull Vec3 getRotationVector(float yaw, float pitch) {
        return new Vec3(Mth.sin(-pitch * 0.017453292F) * Mth.cos(yaw * 0.017453292F), -Mth.sin(yaw * 0.017453292F), Mth.cos(-pitch * 0.017453292F) * Mth.cos(yaw * 0.017453292F));
    }

    public static float[] calcAngle(Vec3 from, Vec3 to) {
        if (to == null) return null;
        double difX = to.x - from.x;
        double difY = (to.y - from.y) * -1.0;
        double difZ = to.z - from.z;
        double dist = Mth.sqrt((float) (difX * difX + difZ * difZ));
        return new float[]{(float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(difZ, difX)) - 90.0), (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(difY, dist)))};
    }
}
