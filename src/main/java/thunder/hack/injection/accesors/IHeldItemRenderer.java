package thunder.hack.injection.accesors;

import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemInHandRenderer.class)
public interface IHeldItemRenderer {
    @Accessor(value="mainHandHeight")
    void setEquippedProgressMainHand(float var1);

    @Accessor(value="offHandHeight")
    void setEquippedProgressOffHand(float var1);

    @Accessor(value="mainHandHeight")
    float getEquippedProgressMainHand();

    @Accessor(value="offHandHeight")
    float getEquippedProgressOffHand();

    @Accessor(value="mainHandItem")
    void setItemStackMainHand(ItemStack var1);

    @Accessor(value="offHandItem")
    void setItemStackOffHand(ItemStack var1);
}