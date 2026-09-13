package thunder.hack.injection.accesors;

import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket.Action;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundInteractPacket.class)
public interface IPlayerInteractEntityC2SPacket {
    @Accessor("entityId")
    int getEntityId();

    @Accessor("action")
    Action getType();
}
