package thunder.hack.features.cmd;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;

public abstract class Command {
    protected static final CommandBuildContext REGISTRY_ACCESS = Commands.createValidationContext(VanillaRegistries.createLookup());
    protected static final Minecraft mc = Minecraft.getInstance();

    protected final List<String> names;
    private final String description;

    public Command(String... names) {
        this.names = Arrays.asList(names);
        this.description = "descriptions.commands." + this.names.get(0);
    }

    public abstract void executeBuild(LiteralArgumentBuilder<SharedSuggestionProvider> builder);

    public static void sendMessage(String message) {
        if (mc.player == null) return;
        mc.player.displayClientMessage(Component.nullToEmpty(thunder.hack.core.manager.client.CommandManager.getClientMessage() + " " + message), false);
    }

    protected static <T> @NotNull RequiredArgumentBuilder<SharedSuggestionProvider, T> arg(final String name, final ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    protected static @NotNull LiteralArgumentBuilder<SharedSuggestionProvider> literal(final String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    public void register(CommandDispatcher<SharedSuggestionProvider> dispatcher) {
        for (String name : names) {
            LiteralArgumentBuilder<SharedSuggestionProvider> builder = LiteralArgumentBuilder.literal(name);
            executeBuild(builder);
            dispatcher.register(builder);
        }
    }

    public String getName() {
        return names.get(0);
    }

    public String getAliases() {
        return String.join(", ", names.stream().filter(n -> !n.equals(names.get(0))).toList());
    }

    public String getDescription() {
        return I18n.get(description);
    }
}
