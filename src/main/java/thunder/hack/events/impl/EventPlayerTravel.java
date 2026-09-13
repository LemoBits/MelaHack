package thunder.hack.events.impl;

import net.minecraft.world.phys.Vec3;
import thunder.hack.events.Event;

public class EventPlayerTravel extends Event {
    private Vec3 mVec;
    private boolean pre;

    public EventPlayerTravel(Vec3 mVec, boolean pre) {
        this.mVec = mVec;
        this.pre = pre;
    }

    public Vec3 getmVec() {
        return mVec;
    }

    public boolean isPre() {
        return pre;
    }
}