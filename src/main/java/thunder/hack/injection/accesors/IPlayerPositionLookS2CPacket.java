package thunder.hack.injection.accesors;

import net.minecraft.entity.player.PlayerPosition;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerPositionLookS2CPacket.class)
public interface IPlayerPositionLookS2CPacket {
    @Mutable
    @Accessor("change")
    void setChange(PlayerPosition change);
}
