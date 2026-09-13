package thunder.hack.injection;

import thunder.hack.core.manager.player.FriendManager;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.misc.NameProtect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import static thunder.hack.features.modules.Module.mc;

import net.minecraft.util.StringDecomposer;

@Mixin(value = {StringDecomposer.class})
public class MixinTextVisitFactory {
    @ModifyArg(at = @At(value = "INVOKE", target = "Lnet/minecraft/util/StringDecomposer;iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z", ordinal = 0), method = {"iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z" }, index = 0)
    private static String adjustText(String text) {
        return protect(text);
    }

    private static String protect(String string) {
        if (!ModuleManager.nameProtect.isEnabled() || mc.player == null)
            return string;
        String me = mc.getUser().getName();
        if (string.contains(me) || (FriendManager.friends.stream().anyMatch(i -> i.contains(string)) && NameProtect.hideFriends.getValue()))
            return string.replace(me, NameProtect.getCustomName());

        return string;
    }
}
