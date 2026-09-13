package thunder.hack.utility;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import thunder.hack.ThunderHack;

/** Optional, mapping-independent bridge to Baritone. */
public final class BaritoneIntegration {
    private BaritoneIntegration() {
    }

    public static boolean execute(String command) {
        if (!ThunderHack.baritone) return false;
        try {
            Class<?> api = Class.forName("baritone.api.BaritoneAPI");
            Object provider = api.getMethod("getProvider").invoke(null);
            Object baritone = provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);
            Object commandManager = baritone.getClass().getMethod("getCommandManager").invoke(baritone);
            Method execute = commandManager.getClass().getMethod("execute", String.class);
            execute.invoke(commandManager, command);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            ThunderHack.LOGGER.warn("Unable to invoke optional Baritone command '{}'", command, exception);
            return false;
        }
    }

    public static boolean setBooleanSetting(String name, boolean value) {
        if (!ThunderHack.baritone) return false;
        try {
            Class<?> api = Class.forName("baritone.api.BaritoneAPI");
            Object settings = api.getMethod("getSettings").invoke(null);
            Field settingField = settings.getClass().getField(name);
            Object setting = settingField.get(settings);
            Field valueField = setting.getClass().getField("value");
            valueField.set(setting, value);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            ThunderHack.LOGGER.warn("Unable to update optional Baritone setting '{}'", name, exception);
            return false;
        }
    }
}
