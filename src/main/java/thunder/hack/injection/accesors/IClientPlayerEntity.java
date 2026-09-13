package thunder.hack.injection.accesors;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LocalPlayer.class)
public interface IClientPlayerEntity {
    @Invoker(value = "sendPosition")
    void iSendMovementPackets();

    @Accessor(value = "jumpRidingScale")
    void setMountJumpStrength(float v);
}
