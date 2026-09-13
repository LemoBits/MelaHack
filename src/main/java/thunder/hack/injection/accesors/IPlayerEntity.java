package thunder.hack.injection.accesors;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Player.class)
public interface IPlayerEntity {
    @Accessor("oBob")
    float getLastStrideDistance();

    @Accessor("oBob")
    void setLastStrideDistance(float strideDistance);
}
