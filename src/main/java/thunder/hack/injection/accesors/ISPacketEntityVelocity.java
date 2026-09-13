package thunder.hack.injection.accesors;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundSetEntityMotionPacket.class)
public interface ISPacketEntityVelocity {
    @Mutable
    @Accessor("xa")
    void setMotionX(int velocityX);

    @Mutable
    @Accessor("ya")
    void setMotionY(int velocityY);

    @Mutable
    @Accessor("za")
    void setMotionZ(int velocityZ);
}