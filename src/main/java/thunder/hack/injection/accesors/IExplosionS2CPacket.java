package thunder.hack.injection.accesors;

import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.util.math.Vec3d;
import java.util.Optional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ExplosionS2CPacket.class)
public interface IExplosionS2CPacket {
    @Mutable
    @Accessor("playerKnockback")
    void setPlayerKnockback(Optional<Vec3d> playerKnockback);

    @Accessor("playerKnockback")
    Optional<Vec3d> getPlayerKnockback();
}
