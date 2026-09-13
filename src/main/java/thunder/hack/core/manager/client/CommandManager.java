package thunder.hack.core.manager.client;

import com.mojang.brigadier.CommandDispatcher;
import org.jetbrains.annotations.NotNull;
import thunder.hack.features.cmd.Command;
import thunder.hack.features.cmd.impl.*;
import thunder.hack.core.manager.IManager;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.SharedSuggestionProvider;

public class CommandManager implements IManager {
    private String prefix = "@";

    private final CommandDispatcher<SharedSuggestionProvider> dispatcher = new CommandDispatcher<>();
    private final SharedSuggestionProvider source = new ClientSuggestionProvider(null, Minecraft.getInstance(), false);
    private final List<Command> commands = new ArrayList<>();

    public CommandManager() {
        add(new RpcCommand());
        add(new KitCommand());
        add(new GpsCommand());
        add(new CfgCommand());
        add(new RctCommand());
        add(new BindCommand());
        add(new DrawCommand());
        add(new HelpCommand());
        add(new NukerCommand());
        add(new EClipCommand());
        add(new HClipCommand());
        add(new LoginCommand());
        add(new MacroCommand());
        add(new StaffCommand());
        add(new VClipCommand());
        add(new AddonsCommand());
        add(new GetNbtCommand());
        add(new FriendCommand());
        add(new ModuleCommand());
        add(new PrefixCommand());
        add(new TrackerCommand());
        add(new GamemodeCommand());
        add(new DropAllCommand());
        add(new TreasureCommand());
        add(new WayPointCommand());
        add(new TabParseCommand());
        add(new BlockESPCommand());
        add(new BenchMarkCommand());
        add(new HorseSpeedCommand());
        add(new OpenFolderCommand());
        add(new ResetBindsCommand());
        add(new InvCleanerCommand());
        add(new GotoWaypointCommand());
        add(new ChestStealerCommand());
        add(new GarbageCleanerCommand());
    }

    private void add(@NotNull Command command) {
        command.register(dispatcher);
        commands.add(command);
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public Command get(Class<? extends Command> commandClass) {
        for (Command command : commands)
            if (command.getClass().equals(commandClass)) return command;

        return null;
    }

    public static @NotNull String getClientMessage() {
        return ChatFormatting.WHITE + "⌊" + ChatFormatting.GOLD + "⚡" + ChatFormatting.WHITE + "⌉" + ChatFormatting.RESET;
    }

    public List<Command> getCommands() {
        return commands;
    }

    public SharedSuggestionProvider getSource() {
        return source;
    }

    public CommandDispatcher<SharedSuggestionProvider> getDispatcher() {
        return dispatcher;
    }

    public void registerCommand(Command command) {
        if (command == null) return;

        command.register(dispatcher);
        this.commands.add(command);
    }
}
