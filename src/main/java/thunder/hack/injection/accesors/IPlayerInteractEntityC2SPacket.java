package thunder.hack.injection.accesors;

import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.InteractTypeHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerInteractEntityC2SPacket.class)
public interface IPlayerInteractEntityC2SPacket {
    @Accessor("entityId")
    int getEntityId();

    @Accessor("type")
    InteractTypeHandler getType();
}
