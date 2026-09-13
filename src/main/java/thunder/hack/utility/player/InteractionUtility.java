package thunder.hack.utility.player;

import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.core.*;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import thunder.hack.injection.accesors.IClientWorldMixin;
import thunder.hack.utility.world.ExplosionUtility;

import java.util.*;

import static thunder.hack.features.modules.Module.mc;

public final class InteractionUtility {
    private static final List<Block> SHIFT_BLOCKS = new ArrayList<>();
    static {
        SHIFT_BLOCKS.addAll(Arrays.asList(
                Blocks.ENDER_CHEST, Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.CRAFTING_TABLE,
                Blocks.BIRCH_TRAPDOOR, Blocks.BAMBOO_TRAPDOOR, Blocks.DARK_OAK_TRAPDOOR, Blocks.CHERRY_TRAPDOOR,
                Blocks.ANVIL, Blocks.BREWING_STAND, Blocks.HOPPER, Blocks.DROPPER, Blocks.DISPENSER,
                Blocks.ACACIA_TRAPDOOR, Blocks.ENCHANTING_TABLE, Blocks.SHULKER_BOX));
        SHIFT_BLOCKS.addAll(Blocks.DYED_SHULKER_BOX.asList());
    }

    public static Map<BlockPos, Long> awaiting = new HashMap<>();

    public static boolean canSee(Vec3 vec) {
        return canSee(vec, vec);
    }

    public static boolean canSee(Entity entity) {
        Vec3 entityEyes = getEyesPos(entity);
        Vec3 entityPos = entity.position();
        return canSee(entityEyes, entityPos);
    }

    public static boolean canSee(Vec3 entityEyes, Vec3 entityPos) {
        if (mc.player == null || mc.level == null) return false;

        Vec3 playerEyes = getEyesPos(mc.player);
        if (ExplosionUtility.raycast(playerEyes, entityEyes, false) == HitResult.Type.MISS)
            return true;

        if (playerEyes.y() > entityPos.y())
            return ExplosionUtility.raycast(playerEyes, entityEyes, false) == HitResult.Type.MISS;
        return false;
    }

    public static Vec3 getEyesPos(@NotNull Entity entity) {
        return entity.position().add(0, entity.getEyeHeight(entity.getPose()), 0);
    }

    public static float @NotNull [] calculateAngle(Vec3 to) {
        return calculateAngle(getEyesPos(mc.player), to);
    }

    public static float @NotNull [] calculateAngle(@NotNull Vec3 from, @NotNull Vec3 to) {
        double difX = to.x - from.x;
        double difY = (to.y - from.y) * -1.0;
        double difZ = to.z - from.z;
        double dist = Mth.sqrt((float) (difX * difX + difZ * difZ));

        float yD = (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(difZ, difX)) - 90.0);
        float pD = (float) Mth.clamp(Mth.wrapDegrees(Math.toDegrees(Math.atan2(difY, dist))), -90f, 90f);

        return new float[]{yD, pD};
    }

    public static boolean placeBlock(BlockPos bp, Rotate rotate, Interact interact, PlaceMode mode, int slot, boolean returnSlot, boolean ignoreEntities) {
        int prevItem = mc.player.getInventory().getSelectedSlot();
        if (slot != -1) InventoryUtility.switchTo(slot);
        else return false;

        boolean result = placeBlock(bp, rotate, interact, mode, ignoreEntities);

        if (returnSlot) InventoryUtility.switchTo(prevItem);
        return result;
    }

    public static boolean placeBlock(BlockPos bp, Rotate rotate, Interact interact, PlaceMode mode, @NotNull SearchInvResult invResult, boolean returnSlot, boolean ignoreEntities) {
        int prevItem = mc.player.getInventory().getSelectedSlot();
        invResult.switchTo();
        boolean result = placeBlock(bp, rotate, interact, mode, ignoreEntities);
        if (returnSlot) InventoryUtility.switchTo(prevItem);

        return result;
    }

    public static boolean placeBlock(BlockPos bp, Rotate rotate, Interact interact, PlaceMode mode, boolean ignoreEntities) {
        BlockHitResult result = getPlaceResult(bp, interact, ignoreEntities);
        if (result == null || mc.level == null || mc.gameMode == null || mc.player == null) return false;

        boolean sprint = mc.player.isSprinting();
        boolean sneak = needSneak(mc.level.getBlockState(result.getBlockPos()).getBlock()) && !mc.player.isShiftKeyDown();

        if (sprint)
            mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
        if (sneak)
            mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundPlayerInputPacket(new net.minecraft.world.entity.player.Input(false, false, false, false, false, true, false)));

        float[] angle = calculateAngle(result.getLocation());

        switch (rotate) {
            case None -> {

            }
            case Default -> mc.player.connection.send(new ServerboundMovePlayerPacket.Rot(angle[0], angle[1], mc.player.onGround(), mc.player.horizontalCollision));
            case Grim -> mc.player.connection.send(new ServerboundMovePlayerPacket.PosRot(mc.player.getX(), mc.player.getY(), mc.player.getZ(), angle[0], angle[1], mc.player.onGround(), mc.player.horizontalCollision));
        }

        if (mode == PlaceMode.Normal)
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, result);

        if (mode == PlaceMode.Packet)
            sendSequencedPacket(id -> new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, result, id));

        awaiting.put(bp, System.currentTimeMillis());

        if (rotate == Rotate.Grim)
            mc.player.connection.send(new ServerboundMovePlayerPacket.PosRot(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot(), mc.player.onGround(), mc.player.horizontalCollision));

        if (sneak)
            mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundPlayerInputPacket(new net.minecraft.world.entity.player.Input(false, false, false, false, false, false, false)));

        if (sprint)
            mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));

        mc.player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
        return true;
    }

    public static boolean canPlaceBlock(@NotNull BlockPos bp, Interact interact, boolean ignoreEntities) {
        if (awaiting.containsKey(bp)) return false;
        return getPlaceResult(bp, interact, ignoreEntities) != null;
    }

    public static float @Nullable [] getPlaceAngle(@NotNull BlockPos bp, Interact interact, boolean ignoreEntities) {
        BlockHitResult result = getPlaceResult(bp, interact, ignoreEntities);
        if (result != null) return calculateAngle(result.getLocation());
        return null;
    }

    public static void sendSequencedPacket(PredictiveAction packetCreator) {
        if (mc.getConnection() == null || mc.level == null) return;
        try (BlockStatePredictionHandler pendingUpdateManager = ((IClientWorldMixin) mc.level).getPendingUpdateManager().startPredicting();) {
            int i = pendingUpdateManager.currentSequence();
            mc.getConnection().send(packetCreator.predict(i));
        }
    }

    @Nullable
    public static BlockHitResult getPlaceResult(@NotNull BlockPos bp, Interact interact, boolean ignoreEntities) {
        if (!ignoreEntities)
            for (Entity entity : new ArrayList<>(mc.level.getEntitiesOfClass(Entity.class, new AABB(bp))))
                if (!(entity instanceof ItemEntity) && !(entity instanceof ExperienceOrb))
                    return null;

        if (!mc.level.getBlockState(bp).canBeReplaced())
            return null;

        if (interact == Interact.AirPlace)
            return ExplosionUtility.rayCastBlock(new ClipContext(InteractionUtility.getEyesPos(mc.player), net.minecraft.world.phys.Vec3.atBottomCenterOf(bp), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player), bp);

        ArrayList<BlockPosWithFacing> supports = getSupportBlocks(bp);
        for (BlockPosWithFacing support : supports) {
            if (interact != Interact.Vanilla) {
                @NotNull List<Direction> dirs = getStrictDirections(bp);
                if (dirs.isEmpty())
                    return null;

                if (!dirs.contains(support.facing))
                    continue;
            }
            BlockHitResult result = null;
            if (interact == Interact.Legit) {
                Vec3 p = getVisibleDirectionPoint(support.facing, support.position, 0, 6); //TODO Implement Range
                if (p != null)
                    return new BlockHitResult(p, support.facing, support.position, false);
            } else {
                Vec3 directionVec = new Vec3(support.position.getX() + 0.5 + support.facing.getUnitVec3i().getX() * 0.5, support.position.getY() + 0.5 + support.facing.getUnitVec3i().getY() * 0.5, support.position.getZ() + 0.5 + support.facing.getUnitVec3i().getZ() * 0.5);
                result = new BlockHitResult(directionVec, support.facing, support.position, false);
            }
            return result;
        }
        return null;
    }


    public static @NotNull ArrayList<BlockPosWithFacing> getSupportBlocks(@NotNull BlockPos bp) {
        ArrayList<BlockPosWithFacing> list = new ArrayList<>();

        if (mc.level.getBlockState(bp.offset(0, -1, 0)).isRedstoneConductor(mc.level, bp.offset(0, -1, 0)) || awaiting.containsKey(bp.offset(0, -1, 0)))
            list.add(new BlockPosWithFacing(bp.offset(0, -1, 0), Direction.UP));

        if (mc.level.getBlockState(bp.offset(0, 1, 0)).isRedstoneConductor(mc.level, bp.offset(0, 1, 0)) || awaiting.containsKey(bp.offset(0, 1, 0)))
            list.add(new BlockPosWithFacing(bp.offset(0, 1, 0), Direction.DOWN));

        if (mc.level.getBlockState(bp.offset(-1, 0, 0)).isRedstoneConductor(mc.level, bp.offset(-1, 0, 0)) || awaiting.containsKey(bp.offset(-1, 0, 0)))
            list.add(new BlockPosWithFacing(bp.offset(-1, 0, 0), Direction.EAST));

        if (mc.level.getBlockState(bp.offset(1, 0, 0)).isRedstoneConductor(mc.level, bp.offset(1, 0, 0)) || awaiting.containsKey(bp.offset(1, 0, 0)))
            list.add(new BlockPosWithFacing(bp.offset(1, 0, 0), Direction.WEST));

        if (mc.level.getBlockState(bp.offset(0, 0, 1)).isRedstoneConductor(mc.level, bp.offset(0, 0, 1)) || awaiting.containsKey(bp.offset(0, 0, 1)))
            list.add(new BlockPosWithFacing(bp.offset(0, 0, 1), Direction.NORTH));

        if (mc.level.getBlockState(bp.offset(0, 0, -1)).isRedstoneConductor(mc.level, bp.offset(0, 0, -1)) || awaiting.containsKey(bp.offset(0, 0, -1)))
            list.add(new BlockPosWithFacing(bp.offset(0, 0, -1), Direction.SOUTH));

        return list;
    }

    public static @Nullable BlockPosWithFacing checkNearBlocks(@NotNull BlockPos blockPos) {
        if (mc.level.getBlockState(blockPos.offset(0, -1, 0)).isRedstoneConductor(mc.level, blockPos.offset(0, -1, 0)))
            return new BlockPosWithFacing(blockPos.offset(0, -1, 0), Direction.UP);

        else if (mc.level.getBlockState(blockPos.offset(-1, 0, 0)).isRedstoneConductor(mc.level, blockPos.offset(-1, 0, 0)))
            return new BlockPosWithFacing(blockPos.offset(-1, 0, 0), Direction.EAST);

        else if (mc.level.getBlockState(blockPos.offset(1, 0, 0)).isRedstoneConductor(mc.level, blockPos.offset(1, 0, 0)))
            return new BlockPosWithFacing(blockPos.offset(1, 0, 0), Direction.WEST);

        else if (mc.level.getBlockState(blockPos.offset(0, 0, 1)).isRedstoneConductor(mc.level, blockPos.offset(0, 0, 1)))
            return new BlockPosWithFacing(blockPos.offset(0, 0, 1), Direction.NORTH);

        else if (mc.level.getBlockState(blockPos.offset(0, 0, -1)).isRedstoneConductor(mc.level, blockPos.offset(0, 0, -1)))
            return new BlockPosWithFacing(blockPos.offset(0, 0, -1), Direction.SOUTH);
        return null;
    }

    public static float squaredDistanceFromEyes(@NotNull Vec3 vec) {
        double d0 = vec.x - mc.player.getX();
        double d1 = vec.z - mc.player.getZ();
        double d2 = vec.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        return (float) (d0 * d0 + d1 * d1 + d2 * d2);
    }

    public static float squaredDistanceFromEyes2d(@NotNull Vec3 vec) {
        double d0 = vec.x - mc.player.getX();
        double d1 = vec.z - mc.player.getZ();
        return (float) (d0 * d0 + d1 * d1);
    }

    public static @NotNull List<Direction> getStrictDirections(@NotNull BlockPos bp) {
        List<Direction> visibleSides = new ArrayList<>();
        Vec3 positionVector = net.minecraft.world.phys.Vec3.atBottomCenterOf(bp);

        double westDelta = getEyesPos(mc.player).x - (positionVector.add(0.5, 0, 0).x);
        double eastDelta = getEyesPos(mc.player).x - (positionVector.add(-0.5, 0, 0).x);
        double northDelta = getEyesPos(mc.player).z - (positionVector.add(0, 0, 0.5).z);
        double southDelta = getEyesPos(mc.player).z - (positionVector.add(0, 0, -0.5).z);
        double upDelta = getEyesPos(mc.player).y - (positionVector.add(0, 0.5, 0).y);
        double downDelta = getEyesPos(mc.player).y - (positionVector.add(0, -0.5, 0).y);

        if (westDelta > 0 && isSolid(bp.west()))
            visibleSides.add(Direction.EAST);
        if (westDelta < 0 && isSolid(bp.east()))
            visibleSides.add(Direction.WEST);
        if (eastDelta < 0 && isSolid(bp.east()))
            visibleSides.add(Direction.WEST);
        if (eastDelta > 0 && isSolid(bp.west()))
            visibleSides.add(Direction.EAST);

        if (northDelta > 0 && isSolid(bp.north()))
            visibleSides.add(Direction.SOUTH);
        if (northDelta < 0 && isSolid(bp.south()))
            visibleSides.add(Direction.NORTH);
        if (southDelta < 0 && isSolid(bp.south()))
            visibleSides.add(Direction.NORTH);
        if (southDelta > 0 && isSolid(bp.north()))
            visibleSides.add(Direction.SOUTH);

        if (upDelta > 0 && isSolid(bp.below()))
            visibleSides.add(Direction.UP);
        if (upDelta < 0 && isSolid(bp.above()))
            visibleSides.add(Direction.DOWN);
        if (downDelta < 0 && isSolid(bp.above()))
            visibleSides.add(Direction.DOWN);
        if (downDelta > 0 && isSolid(bp.below()))
            visibleSides.add(Direction.UP);

        return visibleSides;
    }

    public static boolean isSolid(BlockPos bp) {
        return mc.level.getBlockState(bp).isRedstoneConductor(mc.level, bp) || awaiting.containsKey(bp);
    }

    public static @NotNull List<Direction> getStrictBlockDirections(@NotNull BlockPos bp) {
        List<Direction> visibleSides = new ArrayList<>();
        Vec3 pV = net.minecraft.world.phys.Vec3.atBottomCenterOf(bp);

        double westDelta = getEyesPos(mc.player).x - (pV.add(0.5, 0, 0).x);
        double eastDelta = getEyesPos(mc.player).x - (pV.add(-0.5, 0, 0).x);
        double northDelta = getEyesPos(mc.player).z - (pV.add(0, 0, 0.5).z);
        double southDelta = getEyesPos(mc.player).z - (pV.add(0, 0, -0.5).z);
        double upDelta = getEyesPos(mc.player).y - (pV.add(0, 0.5, 0).y);
        double downDelta = getEyesPos(mc.player).y - (pV.add(0, -0.5, 0).y);

        if (westDelta > 0 && mc.level.getBlockState(bp.east()).canBeReplaced())
            visibleSides.add(Direction.EAST);

        if (eastDelta < 0 && mc.level.getBlockState(bp.west()).canBeReplaced())
            visibleSides.add(Direction.WEST);

        if (northDelta > 0 && mc.level.getBlockState(bp.south()).canBeReplaced())
            visibleSides.add(Direction.SOUTH);

        if (southDelta < 0 && mc.level.getBlockState(bp.north()).canBeReplaced())
            visibleSides.add(Direction.NORTH);

        if (upDelta > 0 && mc.level.getBlockState(bp.above()).canBeReplaced())
            visibleSides.add(Direction.UP);

        if (downDelta < 0 && mc.level.getBlockState(bp.below()).canBeReplaced())
            visibleSides.add(Direction.DOWN);

        return visibleSides;
    }

    public static @Nullable BreakData getBreakData(BlockPos bp, Interact interact) {
        if (interact == Interact.Vanilla) return new BreakData(Direction.UP, net.minecraft.world.phys.Vec3.atBottomCenterOf(bp).add(0, 0.5, 0));
        if (interact == Interact.Strict) {
            float bestDistance = 999f;
            Direction bestDirection = Direction.UP;
            Vec3 bestVector = null;

            for (Direction dir : Direction.values()) {
                Vec3 directionVec = new Vec3(bp.getX() + 0.5 + dir.getUnitVec3i().getX() * 0.5, bp.getY() + 0.5 + dir.getUnitVec3i().getY() * 0.5, bp.getZ() + 0.5 + dir.getUnitVec3i().getZ() * 0.5);
                float distance = squaredDistanceFromEyes(directionVec);
                if (bestDistance > distance) {
                    bestDirection = dir;
                    bestVector = directionVec;
                    bestDistance = distance;
                }
            }

            if (bestVector == null) return null;
            return new BreakData(bestDirection, bestVector);
        }

        if (interact == Interact.Legit) {
            float bestDistance = 999f;
            BreakData bestData = null;
            for (float x = 0f; x <= 1f; x += 0.2f) {
                for (float y = 0f; y <= 1; y += 0.2f) {
                    for (float z = 0f; z <= 1; z += 0.2f) {
                        Vec3 point = new Vec3(bp.getX() + x, bp.getY() + y, bp.getZ() + z);
                        BlockHitResult wallCheck = mc.level.clip(new ClipContext(InteractionUtility.getEyesPos(mc.player), point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
                        if (wallCheck != null && wallCheck.getType() == HitResult.Type.BLOCK && !wallCheck.getBlockPos().equals(bp))
                            continue;
                        BlockHitResult result = ExplosionUtility.rayCastBlock(new ClipContext(InteractionUtility.getEyesPos(mc.player), point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player), bp);

                        if (squaredDistanceFromEyes(point) < bestDistance)
                            if (result != null && result.getType() == HitResult.Type.BLOCK)
                                bestData = new BreakData(result.getDirection(), result.getLocation());
                    }
                }
            }
            if (bestData == null) return null;
            if (bestData.vector == null || bestData.dir == null) return null;
            return bestData;
        }
        return null;
    }

    public static @Nullable Vec3 getVisibleDirectionPoint(@NotNull Direction dir, @NotNull BlockPos bp, float wallRange, float range) {
        AABB brutBox = getDirectionBox(dir);

        // EAST, WEST
        if (brutBox.maxX - brutBox.minX == 0)
            for (double y = brutBox.minY; y < brutBox.maxY; y += 0.1f)
                for (double z = brutBox.minZ; z < brutBox.maxZ; z += 0.1f) {
                    Vec3 point = new Vec3(bp.getX() + brutBox.minX, bp.getY() + y, bp.getZ() + z);

                    if (shouldSkipPoint(point, bp, dir, wallRange, range))
                        continue;

                    return point;
                }


        // DOWN, UP
        if (brutBox.maxY - brutBox.minY == 0)
            for (double x = brutBox.minX; x < brutBox.maxX; x += 0.1f)
                for (double z = brutBox.minZ; z < brutBox.maxZ; z += 0.1f) {
                    Vec3 point = new Vec3(bp.getX() + x, bp.getY() + brutBox.minY, bp.getZ() + z);

                    if (shouldSkipPoint(point, bp, dir, wallRange, range))
                        continue;

                    return point;
                }


        // NORTH, SOUTH
        if (brutBox.maxZ - brutBox.minZ == 0)
            for (double x = brutBox.minX; x < brutBox.maxX; x += 0.1f)
                for (double y = brutBox.minY; y < brutBox.maxY; y += 0.1f) {
                    Vec3 point = new Vec3(bp.getX() + x, bp.getY() + y, bp.getZ() + brutBox.minZ);

                    if (shouldSkipPoint(point, bp, dir, wallRange, range))
                        continue;

                    return point;
                }


        return null;
    }

    private static @NotNull AABB getDirectionBox(Direction dir) {
        return switch (dir) {
            case UP -> new AABB(.15f, 1f, .15f, .85f, 1f, .85f);
            case DOWN -> new AABB(.15f, 0f, .15f, .85f, 0f, .85f);

            case EAST -> new AABB(1f, .15f, .15f, 1f, .85f, .85f);
            case WEST -> new AABB(0f, .15f, .15f, 0f, .85f, .85f);

            case NORTH -> new AABB(.15f, .15f, 0f, .85f, .85f, 0f);
            case SOUTH -> new AABB(.15f, .15f, 1f, .85f, .85f, 1f);
        };
    }

    private static boolean shouldSkipPoint(Vec3 point, BlockPos bp, Direction dir, float wallRange, float range) {
        ClipContext context = new ClipContext(InteractionUtility.getEyesPos(mc.player), point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player);
        BlockHitResult result = mc.level.clip(context);

        float dst = InteractionUtility.squaredDistanceFromEyes(point);

        if (result != null
                && result.getType() == HitResult.Type.BLOCK
                && !result.getBlockPos().equals(bp)
                && dst > wallRange * wallRange)
            return true;

        return dst > range * range;
    }

    public static boolean needSneak(Block in) {
        return SHIFT_BLOCKS.contains(in);
    }

    public static void lookAt(BlockPos bp) {
        if (bp != null) {
            float[] angle = calculateAngle(net.minecraft.world.phys.Vec3.atBottomCenterOf(bp));
            mc.player.setYRot(angle[0]);
            mc.player.setXRot(angle[1]);
        }
    }

    public static boolean isVecInFOV(Vec3 pos, Integer fov) {
        double deltaX = pos.x() - mc.player.getX();
        double deltaZ = pos.z() - mc.player.getZ();
        float yawDelta = Mth.wrapDegrees((float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0) - Mth.wrapDegrees(mc.player.getYRot()));
        return Math.abs(yawDelta) <= fov;
    }

    public record BlockPosWithFacing(BlockPos position, Direction facing) {
    }

    public record BreakData(Direction dir, Vec3 vector) {
    }

    public enum PlaceMode {
        Packet,
        Normal
    }

    public enum Rotate {
        None,
        Default,
        Grim
    }

    public enum Interact {
        Vanilla,
        Strict,
        Legit,
        AirPlace
    }
}
