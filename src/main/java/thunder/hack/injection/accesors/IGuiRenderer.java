package thunder.hack.injection.accesors;

import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(GuiRenderer.class)
public interface IGuiRenderer {
    @Accessor("firstDrawIndexAfterBlur")
    int thunderhack$getFirstDrawIndexAfterBlur();

    @Accessor("draws")
    List<?> thunderhack$getDraws();
}
