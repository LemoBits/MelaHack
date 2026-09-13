package thunder.hack.injection.accesors;

import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundUseItemOnPacket.class)
public interface IPlayerInteractBlockC2SPacket {
    @Mutable
    @Accessor("hand")
    void setHand(InteractionHand hand);
}
