package thunder.hack.injection.accesors;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundSystemChatPacket.class)
public interface IGameMessageS2CPacket {
    @Mutable
    @Accessor("content")
    void setContent(Component val);
}
