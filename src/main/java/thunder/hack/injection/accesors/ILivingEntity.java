package thunder.hack.injection.accesors;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface ILivingEntity {
    @Accessor("attackStrengthTicker")
    int getLastAttackedTicks();

    @Accessor("yBodyRotO")
    float getLastBodyYaw();

    @Accessor("yBodyRotO")
    void setLastBodyYaw(float bodyYaw);

    @Accessor("yHeadRotO")
    float getLastHeadYaw();

    @Accessor("yHeadRotO")
    void setLastHeadYaw(float headYaw);

    @Accessor("noJumpDelay")
    int getLastJumpCooldown();

    @Accessor("noJumpDelay")
    void setLastJumpCooldown(int val);
}
