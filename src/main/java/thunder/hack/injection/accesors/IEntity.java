package thunder.hack.injection.accesors;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface IEntity {
    @Mutable
    @Accessor("position")
    void thunderHack$setPosition(Vec3 pos);

    @Mutable
    @Accessor("blockPosition")
    void thunderHack$setBlockPosition(BlockPos blockPos);

    @Accessor("yRotO")
    float getLastYaw();

    @Accessor("yRotO")
    void setLastYaw(float yaw);

    @Accessor("xRotO")
    float getLastPitch();

    @Accessor("xRotO")
    void setLastPitch(float pitch);
}
