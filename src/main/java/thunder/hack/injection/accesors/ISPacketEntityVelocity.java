package thunder.hack.injection.accesors;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundSetEntityMotionPacket.class)
public abstract class ISPacketEntityVelocity {
    @Mutable
    @Accessor("movement")
    abstract void setMovement(Vec3 movement);

    private ClientboundSetEntityMotionPacket packet() {
        return (ClientboundSetEntityMotionPacket) (Object) this;
    }

    public void setMotionX(double velocityX) {
        Vec3 movement = packet().movement();
        setMovement(new Vec3(velocityX, movement.y, movement.z));
    }

    public void setMotionY(double velocityY) {
        Vec3 movement = packet().movement();
        setMovement(new Vec3(movement.x, velocityY, movement.z));
    }

    public void setMotionZ(double velocityZ) {
        Vec3 movement = packet().movement();
        setMovement(new Vec3(movement.x, movement.y, velocityZ));
    }
}
