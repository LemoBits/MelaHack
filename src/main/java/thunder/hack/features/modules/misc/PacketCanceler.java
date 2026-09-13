package thunder.hack.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.jetbrains.annotations.NotNull;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public final class PacketCanceler extends Module {
    private final Setting<Boolean> clickSlot = new Setting<>("ClickSlotC2SPacket", false);
    private final Setting<Boolean> playerMovePosAndOnGround = new Setting<>("PositionAndOnGround", false);
    private final Setting<Boolean> playerMoveOnGroundOnly = new Setting<>("OnGroundOnly", false);
    private final Setting<Boolean> playerMoveLookAndOnGround = new Setting<>("LookAndOnGround", false);
    public PacketCanceler() {
        super("PacketCanceler", Category.MISC);
    }

    @EventHandler
    @SuppressWarnings("unused")
    private void onPacketSend(PacketEvent.@NotNull Send e) {
        if (e.getPacket() instanceof ServerboundContainerClickPacket && clickSlot.getValue()) {
            e.cancel();
        } else if (e.getPacket() instanceof ServerboundMovePlayerPacket.Pos && playerMovePosAndOnGround.getValue()) {
            e.cancel();
        } else if (e.getPacket() instanceof ServerboundMovePlayerPacket.StatusOnly && playerMoveOnGroundOnly.getValue()) {
            e.cancel();
        } else if (e.getPacket() instanceof ServerboundMovePlayerPacket.Rot && playerMoveLookAndOnGround.getValue()) {
            e.cancel();
        }
    }
}