package thunder.hack.features.cmd.args;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.MacroManager;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import static thunder.hack.features.modules.client.ClientSettings.isRu;

public class MacroArgumentType implements ArgumentType<MacroManager.Macro> {
    private static final Collection<String> EXAMPLES = Managers.MACRO.getMacros().stream().map(MacroManager.Macro::getName).limit(5).toList();

    public static MacroArgumentType create() {
        return new MacroArgumentType();
    }

    @Override
    public MacroManager.Macro parse(StringReader reader) throws CommandSyntaxException {
        MacroManager.Macro macro = Managers.MACRO.getMacroByName(reader.readString());

        if (macro == null) throw new DynamicCommandExceptionType(
                name -> Component.literal(isRu() ? "Макроса " + name.toString() + " не существует(" : "Macro with name " + name.toString() + " does not exists(")
        ).create(reader.readString());

        return macro;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(Managers.MACRO.getMacros().stream().map(MacroManager.Macro::getName), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
