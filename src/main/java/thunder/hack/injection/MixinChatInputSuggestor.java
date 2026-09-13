package thunder.hack.injection;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.features.modules.Module;

import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.commands.SharedSuggestionProvider;

@Mixin(CommandSuggestions.class)
public abstract class MixinChatInputSuggestor {
    @Final @Shadow EditBox input;
    @Shadow boolean keepSuggestions;
    @Shadow private ParseResults<SharedSuggestionProvider> currentParse;
    @Shadow private CompletableFuture<Suggestions> pendingSuggestions;
    @Shadow private CommandSuggestions.SuggestionsList suggestions;

    @Shadow protected abstract void updateUsageInfo();

    @Inject(method = "updateCommandInfo", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;canRead()Z", remap = false), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    public void refreshHook(CallbackInfo ci, String string, StringReader reader) {
        if(Module.fullNullCheck()) return;
        if (reader.canRead(Managers.COMMAND.getPrefix().length()) && reader.getString().startsWith(Managers.COMMAND.getPrefix(), reader.getCursor())) {
            reader.setCursor(reader.getCursor() + 1);

            if (currentParse == null)
                currentParse = Managers.COMMAND.getDispatcher().parse(reader, Managers.COMMAND.getSource());

            final int cursor = input.getCursorPosition();

            if (cursor >= 1 && (suggestions == null || !keepSuggestions)) {
                pendingSuggestions = Managers.COMMAND.getDispatcher().getCompletionSuggestions(currentParse, cursor);
                pendingSuggestions.thenRun(() -> {
                    if (pendingSuggestions.isDone()) updateUsageInfo();
                });
            }

            ci.cancel();
        }
    }
}
