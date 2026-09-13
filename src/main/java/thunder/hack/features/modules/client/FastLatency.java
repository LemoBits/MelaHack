package thunder.hack.features.modules.client;

import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import org.jetbrains.annotations.NotNull;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;
import thunder.hack.utility.math.MathUtility;

public final class FastLatency extends Module {
    private final Setting<Integer> delay = new Setting<>("Delay", 80, 0, 1000);

    private final Timer timer = new Timer();
    private final Timer limitTimer = new Timer();
    private long ping;
    public int resolvedPing;

    public FastLatency() {
        super("FastLatency", Category.CLIENT);
    }

    @Override
    public void onRender3D(PoseStack stack) {
        if (timer.passedMs(5000) && limitTimer.every(delay.getValue())) {
            sendPacket(new ServerboundCommandSuggestionPacket(1337, "w "));
            ping = System.currentTimeMillis();
            timer.reset();
        }
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.@NotNull Receive e) {
        if (e.getPacket() instanceof ClientboundCommandSuggestionsPacket c && c.id() == 1337) {
            resolvedPing = (int) MathUtility.clamp(System.currentTimeMillis() - ping, 0, 1000);
            timer.setMs(5000);
        }
    }
}
