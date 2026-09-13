package thunder.hack.injection;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thunder.hack.gui.font.FontRenderers;

@Mixin(GuiGraphics.class)
public class MixinDrawContext {

    @Shadow
    @Final
    private Matrix3x2fStack pose;

 //   @Inject(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)I", at = @At("HEAD"), cancellable = true)
    public void drawTextHook(Font textRenderer, FormattedCharSequence text, int x, int y, int color, boolean shadow, CallbackInfoReturnable<Integer> cir) {
        MutableComponent text1 = Component.empty();
        text.accept((i, style, codePoint) -> {
            text1.append(Component.literal(new String(Character.toChars(codePoint))).setStyle(style));
            return true;
        });

        FontRenderers.sf_medium.drawString(pose, text1.getString(), x,y, color);
        cir.setReturnValue((int) FontRenderers.sf_medium.getStringWidth(text.toString()));
    }
}
