package thunder.hack.events.impl;

import net.minecraft.world.phys.Vec3;
import thunder.hack.events.Event;

public class EventFixVelocity extends Event {
    Vec3 movementInput;
    float speed;
    float yaw;
    Vec3 velocity;

    public EventFixVelocity(Vec3 movementInput, float speed, float yaw, Vec3 velocity) {
        this.movementInput = movementInput;
        this.speed = speed;
        this.yaw = yaw;
        this.velocity = velocity;
    }

    public Vec3 getMovementInput() {
        return this.movementInput;
    }

    public float getSpeed() {
        return this.speed;
    }

    public Vec3 getVelocity() {
        return this.velocity;
    }

    public void setVelocity(Vec3 velocity) {
        this.velocity = velocity;
    }
}
