package thunder.hack.injection.accesors;

import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BeaconBlockEntity.class)
public interface IBeaconBlockEntity {
    @Accessor(value = "levels")
    int getLevel();
}
