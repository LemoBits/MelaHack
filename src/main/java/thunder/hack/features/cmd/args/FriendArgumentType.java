package thunder.hack.features.cmd.args;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import thunder.hack.core.Managers;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import static thunder.hack.features.modules.client.ClientSettings.isRu;

public class FriendArgumentType implements ArgumentType<String> {
    private static final List<String> EXAMPLES = Managers.FRIEND.getFriends().stream().limit(5).toList();

    public static FriendArgumentType create() {
        return new FriendArgumentType();
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        String friend = reader.readString();
        if (!Managers.FRIEND.isFriend(friend)) throw new DynamicCommandExceptionType(
                name -> Component.literal(isRu() ? "Друга с именем " + name.toString() + " не существует(" : "Friend with name " + name.toString() + " does not exist :(")
        ).create(friend);

        return friend;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(Managers.FRIEND.getFriends(), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
