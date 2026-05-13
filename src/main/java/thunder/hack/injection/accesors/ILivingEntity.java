package thunder.hack.injection.accesors;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface ILivingEntity {
    @Accessor("lastAttackedTicks")
    int getLastAttackedTicks();

    @Accessor("lastYaw")
    float getLastYaw();

    @Accessor("lastYaw")
    void setLastYaw(float yaw);

    @Accessor("lastPitch")
    float getLastPitch();

    @Accessor("lastPitch")
    void setLastPitch(float pitch);

    @Accessor("lastBodyYaw")
    float getLastBodyYaw();

    @Accessor("lastBodyYaw")
    void setLastBodyYaw(float bodyYaw);

    @Accessor("lastHeadYaw")
    float getLastHeadYaw();

    @Accessor("lastHeadYaw")
    void setLastHeadYaw(float headYaw);

    @Accessor("jumpingCooldown")
    int getLastJumpCooldown();

    @Accessor("jumpingCooldown")
    void setLastJumpCooldown(int val);
}
