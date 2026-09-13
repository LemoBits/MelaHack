package thunder.hack.injection.accesors;

import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.entity.PositionMoveRotation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundPlayerPositionPacket.class)
public interface IPlayerPositionLookS2CPacket {
    @Mutable
    @Accessor("change")
    void setChange(PositionMoveRotation change);
}
