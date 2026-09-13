package thunder.hack.utility.render;

import com.mojang.blaze3d.platform.Window;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.Minecraft;

public interface WindowResizeCallback {
    Event<WindowResizeCallback> EVENT = EventFactory.createArrayBacked(WindowResizeCallback.class, callbacks -> (client, window) -> {
        for (var callback : callbacks) {
            callback.onResized(client, window);
        }
    });

    void onResized(Minecraft client, Window window);
}