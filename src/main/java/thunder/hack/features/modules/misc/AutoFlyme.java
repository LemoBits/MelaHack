package thunder.hack.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import thunder.hack.ThunderHack;
import thunder.hack.events.impl.EventSync;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;
import thunder.hack.utility.player.MovementUtility;

public class AutoFlyme extends Module {
    public AutoFlyme() {
        super("AutoFlyme", Category.MISC);
    }

    public final Setting<Boolean> instantSpeed = new Setting<>("InstantSpeed", true);
    public final Setting<Boolean> hover = new Setting<>("hover", false);
    public final Setting<Boolean> useTimer = new Setting<>("UseTimer", false);

    public Setting<Float> hoverY = new Setting<>("hoverY", 0.228f, 0.0f, 1.0f, v -> hover.getValue());
    public Setting<Float> speed = new Setting<>("speed", 1.05f, 0.0f, 8f, v -> hover.getValue());

    //фаннигейм перешел на матрикс, и теперь можно летать со скоростью 582 км/ч :skull:
    private final Timer timer = new Timer();

    @Override
    public void onEnable() {
        if (!mc.player.getAbilities().flying) {
            mc.player.connection.sendCommand("flyme");
        }
    }

    @Override
    public void onDisable() {
        ThunderHack.TICK_TIMER = 1.f;
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive e) {
        if (e.getPacket() instanceof ClientboundSystemChatPacket) {
            final ClientboundSystemChatPacket packet = e.getPacket();
            if ((packet.content().getString().contains("Вы атаковали игрока") || packet.content().getString().contains("Возможность летать была удалена")) && timer.passedMs(1000)) {
                mc.player.connection.sendCommand("flyme");
                mc.player.connection.sendCommand("flyme");
                timer.reset();
            }
        }
    }

    @Override
    public void onUpdate() {
        if (useTimer.getValue()) ThunderHack.TICK_TIMER = 1.088f;
        if (!mc.player.getAbilities().flying && timer.passedMs(1000) && !mc.player.onGround() && mc.options.keyJump.isDown()) {
            mc.player.connection.sendCommand("flyme");
            timer.reset();
        }
        if (!mc.options.keyJump.isDown() && hover.getValue() && mc.player.getAbilities().flying && mc.player.getAbilities().flying && !mc.player.onGround() && !mc.level.getBlockCollisions(mc.player, mc.player.getBoundingBox().move(0.0, -hoverY.getValue(), 0.0)).iterator().hasNext()) {
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, -0.05, mc.player.getDeltaMovement().z);
        }
    }

    @EventHandler
    public void onUpdateWalkingPlayer(final EventSync event) {
        if (!instantSpeed.getValue() || !mc.player.getAbilities().flying) return;
        final double[] dir = MovementUtility.isMoving() ? MovementUtility.forward(speed.getValue()) : new double[]{0, 0};
        mc.player.setDeltaMovement(dir[0], mc.player.getDeltaMovement().y, dir[1]);
    }
}
