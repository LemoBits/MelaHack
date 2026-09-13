package thunder.hack.events.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import thunder.hack.events.Event;

public class EventCollision extends Event {
    private BlockState bs;
    private BlockPos bp;

    public EventCollision(BlockState bs, BlockPos bp) {
        this.bs = bs;
        this.bp = bp;
    }

    public BlockState getState() {
        return bs;
    }

    public BlockPos getPos() {
        return bp;
    }

    public void setState(BlockState bs) {
        this.bs = bs;
    }
}
