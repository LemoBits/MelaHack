package thunder.hack.injection.accesors;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiPlayerGameMode.class)
public interface IInteractionManager {
    @Accessor(value = "destroyProgress")
    float getCurBlockDamageMP();

    @Accessor(value = "destroyProgress")
    void setCurBlockDamageMP(float a);

    @Invoker(value = "ensureHasSentCarriedItem")
    void syncSlot();
}