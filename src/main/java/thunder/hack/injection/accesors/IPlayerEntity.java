package thunder.hack.injection.accesors;

import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerEntity.class)
public interface IPlayerEntity {
    @Accessor("lastStrideDistance")
    float getLastStrideDistance();

    @Accessor("lastStrideDistance")
    void setLastStrideDistance(float strideDistance);
}
