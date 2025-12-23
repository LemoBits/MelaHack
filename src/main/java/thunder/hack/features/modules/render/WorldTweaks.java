package thunder.hack.features.modules.render;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.BooleanSettingGroup;
import thunder.hack.setting.impl.ColorSetting;

import java.awt.*;

public class WorldTweaks extends Module {
    public WorldTweaks() {
        super("WorldTweaks", Category.RENDER);
    }

    public static final Setting<BooleanSettingGroup> fogModify = new Setting<>("FogModify", new BooleanSettingGroup(true));
    public static final Setting<Integer> fogStart = new Setting<>("FogStart", 0, 0, 256).addToGroup(fogModify);
    public static final Setting<Integer> fogEnd = new Setting<>("FogEnd", 64, 10, 256).addToGroup(fogModify);
    public static final Setting<ColorSetting> fogColor = new Setting<>("FogColor", new ColorSetting(new Color(0xA900FF))).addToGroup(fogModify);
    public final Setting<Boolean> ctime = new Setting<>("ChangeTime", false);
    public final Setting<Integer> ctimeVal = new Setting<>("Time", 21, 0, 23);

    long oldTime;
    long oldTimeOfDay;
    boolean oldTickDayTime = true;

    @Override
    public void onEnable() {
        oldTime = mc.world.getTime();
        oldTimeOfDay = mc.world.getTimeOfDay();
    }

    @Override
    public void onDisable() {
        mc.world.setTime(oldTime, oldTimeOfDay, oldTickDayTime);
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.getPacket() instanceof WorldTimeUpdateS2CPacket && ctime.getValue()) {
            WorldTimeUpdateS2CPacket packet = (WorldTimeUpdateS2CPacket) event.getPacket();
            oldTime = packet.time();
            oldTimeOfDay = packet.timeOfDay();
            oldTickDayTime = packet.tickDayTime();
            event.cancel();
        }
    }

    @Override
    public void onUpdate() {
        if (ctime.getValue())
            mc.world.setTime(mc.world.getTime(), ctimeVal.getValue() * 1000L, false);
    }
}
