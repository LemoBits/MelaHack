package thunder.hack.injection.accesors;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundSetEntityMotionPacket.class)
public interface ISPacketEntityVelocity {
    @Mutable
    @Accessor("movement")
    void setMovement(Vec3 movement);

    private ClientboundSetEntityMotionPacket packet() {
        return (ClientboundSetEntityMotionPacket) (Object) this;
    }

    default void setMotionX(double velocityX) {
        Vec3 movement = packet().getMovement();
        setMovement(new Vec3(velocityX, movement.y, movement.z));
    }

    default void setMotionY(double velocityY) {
        Vec3 movement = packet().getMovement();
        setMovement(new Vec3(movement.x, velocityY, movement.z));
    }

    default void setMotionZ(double velocityZ) {
        Vec3 movement = packet().getMovement();
        setMovement(new Vec3(movement.x, movement.y, velocityZ));
    }
}
