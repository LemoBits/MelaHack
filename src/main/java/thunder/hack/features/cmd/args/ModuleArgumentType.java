package thunder.hack.features.cmd.args;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import thunder.hack.core.Managers;
import thunder.hack.features.modules.Module;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import static thunder.hack.features.modules.client.ClientSettings.isRu;

public class ModuleArgumentType implements ArgumentType<Module> {
    private static final Collection<String> EXAMPLES = Managers.MODULE.modules.stream()
            .map(Module::getName)
            .limit(5)
            .toList();

    public static ModuleArgumentType create() {
        return new ModuleArgumentType();
    }

    @Override
    public Module parse(StringReader reader) throws CommandSyntaxException {
        Module module = Managers.MODULE.get(reader.readString());
        if (module == null) throw new DynamicCommandExceptionType(
                name -> Component.literal(isRu() ? "Модуля " + name.toString() + " не существует(" : "Module " + name.toString() + " does not exist :(")
        ).create(reader.readString());

        return module;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(Managers.MODULE.modules.stream().map(Module::getName), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
