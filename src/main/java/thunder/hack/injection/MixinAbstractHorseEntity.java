package thunder.hack.injection;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractHorse.class)
public abstract class MixinAbstractHorseEntity extends Animal {
    protected MixinAbstractHorseEntity(EntityType<? extends Animal> entityType, Level world) {
        super(entityType, world);
    }

    // isSaddled() removed in 1.21.5 - needs porting to new saddle check
}
