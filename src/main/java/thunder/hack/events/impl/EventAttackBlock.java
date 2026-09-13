package thunder.hack.events.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import thunder.hack.events.Event;

public class EventAttackBlock extends Event {
    private BlockPos blockPos;
    private Direction enumFacing;

    public EventAttackBlock(BlockPos blockPos, Direction enumFacing) {
        this.blockPos = blockPos;
        this.enumFacing = enumFacing;
    }

    public BlockPos getBlockPos() {
        return this.blockPos;
    }

    public void setBlockPos(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public Direction getEnumFacing() {
        return this.enumFacing;
    }

    public void setEnumFacing(Direction enumFacing) {
        this.enumFacing = enumFacing;
    }
}
