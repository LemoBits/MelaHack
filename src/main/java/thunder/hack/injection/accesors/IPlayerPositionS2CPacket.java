package thunder.hack.injection.accesors;

import net.minecraft.entity.EntityPosition;
import net.minecraft.network.packet.s2c.play.PlayerPositionS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerPositionS2CPacket.class)
public interface IPlayerPositionS2CPacket {
    @Mutable
    @Accessor("change")
    void setChange(EntityPosition change);
}
