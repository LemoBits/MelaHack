package thunder.hack.core.manager.world;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import thunder.hack.core.manager.IManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class HoleManager implements IManager {
    public static final Vec3i[] VECTOR_PATTERN = {
            new Vec3i(0, 0, 1),
            new Vec3i(0, 0, -1),
            new Vec3i(1, 0, 0),
            new Vec3i(-1, 0, 0)
    };

    public @NotNull List<BlockPos> getHolePoses(@NotNull Vec3 from) {
        List<BlockPos> positions = new ArrayList<>();

        double decimalX = from.x() - Math.floor(from.x());
        double decimalZ = from.z() - Math.floor(from.z());
        int offX = calcOffset(decimalX);
        int offZ = calcOffset(decimalZ);
        positions.add(getPos(from));
        for (int x = 0; x <= Math.abs(offX); ++x) {
            for (int z = 0; z <= Math.abs(offZ); ++z) {
                int properX = x * offX;
                int properZ = z * offZ;
                positions.add(Objects.requireNonNull(getPos(from)).offset(properX, 0, properZ));
            }
        }

        return positions;
    }

    public @NotNull List<BlockPos> getSurroundPoses(@NotNull Vec3 from) {
        final BlockPos fromPos = BlockPos.containing(from);
        final ArrayList<BlockPos> tempOffsets = new ArrayList<>();

        final double decimalX = Math.abs(from.x()) - Math.floor(Math.abs(from.x()));
        final double decimalZ = Math.abs(from.z()) - Math.floor(Math.abs(from.z()));
        final int lengthXPos = calcLength(decimalX, false);
        final int lengthXNeg = calcLength(decimalX, true);
        final int lengthZPos = calcLength(decimalZ, false);
        final int lengthZNeg = calcLength(decimalZ, true);

        for (int x = 1; x < lengthXPos + 1; ++x) {
            tempOffsets.add(addToPlayer(fromPos, x, 0.0, 1 + lengthZPos));
            tempOffsets.add(addToPlayer(fromPos, x, 0.0, -(1 + lengthZNeg)));
        }
        for (int x = 0; x <= lengthXNeg; ++x) {
            tempOffsets.add(addToPlayer(fromPos, -x, 0.0, 1 + lengthZPos));
            tempOffsets.add(addToPlayer(fromPos, -x, 0.0, -(1 + lengthZNeg)));
        }
        for (int z = 1; z < lengthZPos + 1; ++z) {
            tempOffsets.add(addToPlayer(fromPos, 1 + lengthXPos, 0.0, z));
            tempOffsets.add(addToPlayer(fromPos, -(1 + lengthXNeg), 0.0, z));
        }
        for (int z = 0; z <= lengthZNeg; ++z) {
            tempOffsets.add(addToPlayer(fromPos, 1 + lengthXPos, 0.0, -z));
            tempOffsets.add(addToPlayer(fromPos, -(1 + lengthXNeg), 0.0, -z));
        }

        return tempOffsets;
    }

    private @NotNull BlockPos getPos(@NotNull Vec3 from) {
        return BlockPos.containing(from.x(), from.y() - Math.floor(from.y()) > 0.8 ? Math.floor(from.y()) + 1.0 : Math.floor(from.y()), from.z());
    }

    public int calcOffset(double dec) {
        return dec >= 0.7 ? 1 : (dec <= 0.3 ? -1 : 0);
    }

    public int calcLength(double decimal, boolean negative) {
        if (negative) return decimal <= 0.3 ? 1 : 0;
        return decimal >= 0.7 ? 1 : 0;
    }

    public BlockPos addToPlayer(@NotNull BlockPos playerPos, double x, double y, double z) {
        if (playerPos.getX() < 0) x = -x;
        if (playerPos.getY() < 0) y = -y;
        if (playerPos.getZ() < 0) z = -z;
        return playerPos.offset(BlockPos.containing(x, y, z));
    }

    public boolean isHole(BlockPos pos) {
        return isSingleHole(pos)
                || validTwoBlockIndestructible(pos) || validTwoBlockBedrock(pos)
                || validQuadIndestructible(pos) || validQuadBedrock(pos);
    }

    public boolean isSingleHole(BlockPos pos) {
        return validIndestructible(pos) || validBedrock(pos);
    }

    public boolean validIndestructible(@NotNull BlockPos pos) {
        return !validBedrock(pos)
                && (isIndestructible(pos.offset(0, -1, 0)) || isBedrock(pos.offset(0, -1, 0)))
                && (isIndestructible(pos.offset(1, 0, 0)) || isBedrock(pos.offset(1, 0, 0)))
                && (isIndestructible(pos.offset(-1, 0, 0)) || isBedrock(pos.offset(-1, 0, 0)))
                && (isIndestructible(pos.offset(0, 0, 1)) || isBedrock(pos.offset(0, 0, 1)))
                && (isIndestructible(pos.offset(0, 0, -1)) || isBedrock(pos.offset(0, 0, -1)))
                && isReplaceable(pos)
                && isReplaceable(pos.offset(0, 1, 0))
                && isReplaceable(pos.offset(0, 2, 0));
    }

    public boolean validBedrock(@NotNull BlockPos pos) {
        return isBedrock(pos.offset(0, -1, 0))
                && isBedrock(pos.offset(1, 0, 0))
                && isBedrock(pos.offset(-1, 0, 0))
                && isBedrock(pos.offset(0, 0, 1))
                && isBedrock(pos.offset(0, 0, -1))
                && isReplaceable(pos)
                && isReplaceable(pos.offset(0, 1, 0))
                && isReplaceable(pos.offset(0, 2, 0));
    }

    public boolean validTwoBlockBedrock(@NotNull BlockPos pos) {
        if (!isReplaceable(pos)) return false;
        Vec3i addVec = getTwoBlocksDirection(pos);

        // If addVec not found -> hole incorrect
        if (addVec == null)
            return false;

        BlockPos[] checkPoses = new BlockPos[]{pos, pos.offset(addVec)};
        // Check surround poses of checkPoses
        for (BlockPos checkPos : checkPoses) {
            BlockPos downPos = checkPos.below();
            if (!isBedrock(downPos))
                return false;

            for (Vec3i vec : VECTOR_PATTERN) {
                BlockPos reducedPos = checkPos.offset(vec);
                if (!isBedrock(reducedPos) && !reducedPos.equals(pos) && !reducedPos.equals(pos.offset(addVec)))
                    return false;
            }
        }

        return true;
    }

    public boolean validTwoBlockIndestructible(@NotNull BlockPos pos) {
        if (!isReplaceable(pos)) return false;
        Vec3i addVec = getTwoBlocksDirection(pos);

        // If addVec not found -> hole incorrect
        if (addVec == null)
            return false;

        BlockPos[] checkPoses = new BlockPos[]{pos, pos.offset(addVec)};
        // Check surround poses of checkPoses
        boolean wasIndestrictible = false;
        for (BlockPos checkPos : checkPoses) {
            BlockPos downPos = checkPos.below();
            if (isIndestructible(downPos))
                wasIndestrictible = true;
            else if (!isBedrock(downPos))
                return false;

            for (Vec3i vec : VECTOR_PATTERN) {
                BlockPos reducedPos = checkPos.offset(vec);

                if (isIndestructible(reducedPos)) {
                    wasIndestrictible = true;
                    continue;
                }
                if (!isBedrock(reducedPos) && !reducedPos.equals(pos) && !reducedPos.equals(pos.offset(addVec)))
                    return false;
            }
        }

        return wasIndestrictible;
    }

    private @Nullable Vec3i getTwoBlocksDirection(BlockPos pos) {
        // Try to get direction
        for (Vec3i vec : VECTOR_PATTERN) {
            if (isReplaceable(pos.offset(vec)))
                return vec;
        }

        return null;
    }

    public boolean validQuadIndestructible(@NotNull BlockPos pos) {
        List<BlockPos> checkPoses = getQuadDirection(pos);
        // If checkPoses not found -> hole incorrect
        if (checkPoses == null)
            return false;

        boolean wasIndestrictible = false;
        for (BlockPos checkPos : checkPoses) {
            BlockPos downPos = checkPos.below();
            if (isIndestructible(downPos)) {
                wasIndestrictible = true;
            } else if (!isBedrock(downPos)) {
                return false;
            }

            for (Vec3i vec : VECTOR_PATTERN) {
                BlockPos reducedPos = checkPos.offset(vec);

                if (isIndestructible(reducedPos)) {
                    wasIndestrictible = true;
                    continue;
                }
                if (!isBedrock(reducedPos) && !checkPoses.contains(reducedPos)) {
                    return false;
                }
            }
        }

        return wasIndestrictible;
    }

    public boolean validQuadBedrock(@NotNull BlockPos pos) {
        List<BlockPos> checkPoses = getQuadDirection(pos);
        // If checkPoses not found -> hole incorrect
        if (checkPoses == null)
            return false;

        for (BlockPos checkPos : checkPoses) {
            BlockPos downPos = checkPos.below();
            if (!isBedrock(downPos)) {
                return false;
            }

            for (Vec3i vec : VECTOR_PATTERN) {
                BlockPos reducedPos = checkPos.offset(vec);
                if (!isBedrock(reducedPos) && !checkPoses.contains(reducedPos)) {
                    return false;
                }
            }
        }

        return true;
    }

    private @Nullable List<BlockPos> getQuadDirection(@NotNull BlockPos pos) {
        // Try to get direction
        List<BlockPos> dirList = new ArrayList<>();
        dirList.add(pos);

        if (!isReplaceable(pos))
            return null;

        if (isReplaceable(pos.offset(1, 0, 0)) && isReplaceable(pos.offset(0, 0, 1)) && isReplaceable(pos.offset(1, 0, 1))) {
            dirList.add(pos.offset(1, 0, 0));
            dirList.add(pos.offset(0, 0, 1));
            dirList.add(pos.offset(1, 0, 1));
        }
        if (isReplaceable(pos.offset(-1, 0, 0)) && isReplaceable(pos.offset(0, 0, -1)) && isReplaceable(pos.offset(-1, 0, -1))) {
            dirList.add(pos.offset(-1, 0, 0));
            dirList.add(pos.offset(0, 0, -1));
            dirList.add(pos.offset(-1, 0, -1));
        }
        if (isReplaceable(pos.offset(1, 0, 0)) && isReplaceable(pos.offset(0, 0, -1)) && isReplaceable(pos.offset(1, 0, -1))) {
            dirList.add(pos.offset(1, 0, 0));
            dirList.add(pos.offset(0, 0, -1));
            dirList.add(pos.offset(1, 0, -1));
        }
        if (isReplaceable(pos.offset(-1, 0, 0)) && isReplaceable(pos.offset(0, 0, 1)) && isReplaceable(pos.offset(-1, 0, 1))) {
            dirList.add(pos.offset(-1, 0, 0));
            dirList.add(pos.offset(0, 0, 1));
            dirList.add(pos.offset(-1, 0, 1));
        }

        if (dirList.size() != 4)
            return null;

        return dirList;
    }

    private boolean isIndestructible(BlockPos bp) {
        if (mc.level == null) return false;

        return mc.level.getBlockState(bp).getBlock() == Blocks.OBSIDIAN
                || mc.level.getBlockState(bp).getBlock() == Blocks.NETHERITE_BLOCK
                || mc.level.getBlockState(bp).getBlock() == Blocks.CRYING_OBSIDIAN
                || mc.level.getBlockState(bp).getBlock() == Blocks.RESPAWN_ANCHOR;
    }

    private boolean isBedrock(BlockPos bp) {
        if (mc.level == null) return false;

        return mc.level.getBlockState(bp).getBlock() == Blocks.BEDROCK;
    }

    private boolean isReplaceable(BlockPos bp) {
        if (mc.level == null) return false;

        return mc.level.getBlockState(bp).canBeReplaced();
    }
}
