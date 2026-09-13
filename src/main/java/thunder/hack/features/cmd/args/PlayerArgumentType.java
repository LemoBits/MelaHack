package thunder.hack.features.cmd.args;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import static thunder.hack.core.manager.IManager.mc;
import static thunder.hack.features.modules.client.ClientSettings.isRu;

public class PlayerArgumentType implements ArgumentType<PlayerInfo> {
    private static final Collection<String> EXAMPLES = List.of("pan4ur", "06ED");

    public static PlayerArgumentType create() {
        return new PlayerArgumentType();
    }

    @Override
    public PlayerInfo parse(StringReader reader) throws CommandSyntaxException {
        String name = reader.readString();

        final PlayerInfo player = mc.getConnection().getOnlinePlayers().stream()
                .filter(p -> name.equals(p.getProfile().name()))
                .findFirst()
                .orElse(null);
        if (player == null) {
            throw new DynamicCommandExceptionType(nickname -> Component.literal(isRu() ? "Игрок " + nickname + " не в сети" : "Player " + nickname + " offline")).create(name);
        }
        return player;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(mc.getConnection().getOnlinePlayers().stream().map(p -> p.getProfile().name()), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
