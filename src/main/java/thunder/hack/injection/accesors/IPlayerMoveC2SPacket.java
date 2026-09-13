package thunder.hack.injection.accesors;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundMovePlayerPacket.class)
public interface IPlayerMoveC2SPacket {
    @Mutable
    @Accessor("onGround")
    void setOnGround(boolean onGround);

    @Mutable
    @Accessor("y")
    void setY(double y);
}