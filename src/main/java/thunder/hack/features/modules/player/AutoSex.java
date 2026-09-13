package thunder.hack.features.modules.player;

import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.Religion;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;
import thunder.hack.utility.math.MathUtility;

import java.util.Arrays;
import java.util.List;
import net.minecraft.world.entity.player.Player;

import static thunder.hack.features.modules.client.ClientSettings.isRu;

public class AutoSex extends Module {
    private final Setting<Integer> targetRange = new Setting<>("Target Range", 5, 1, 10);
    private final Setting<SexMode> mode = new Setting<>("Sex Mode", SexMode.Active);
    private final Setting<Integer> msgDelay = new Setting<>("Message Delay", 1, 0, 50);

    private enum SexMode {
        Active,
        Passive
    }

    private static final String[] PASSIVE_MESSAGES = {
            "It's so Biiiiiiig",
            "Be careful daddy <3",
            "Oh, I feel it inside me!"
    };
    private static final String[] ACTIVE_MESSAGES = {
            "Oh, I'm cumming!",
            "Oh, ur pussy is so nice!",
            "Yeah, yeah",
            "I feel u!",
            "Oh, im inside u"
    };

    private Player target;
    private final Timer messageTimer = new Timer();
    private final Timer sneakTimer = new Timer();

    public AutoSex() {
        super("AutoSex", Category.PLAYER);
    }

    @Override
    public void onEnable() {
        if (ModuleManager.religion.isOn() && ModuleManager.religion.ReligionSetting.is(Religion.YourReligion.Christianity)) {
            ModuleManager.religion.sendMessage(isRu() ? "Не пролюбодействуй!" : "Do not do adulty!");
            disable();
        }
    }

    @Override
    public void onUpdate() {
        if (fullNullCheck()) return;
        if (target == null) {
            target = Managers.COMBAT.getNearestTarget(targetRange.getValue());
            return;
        }
        if (target.position().distanceToSqr(mc.player.position()) >= targetRange.getPow2Value()) {
            target = null;
            return;
        }

        switch (mode.getValue()) {
            case Active -> {
                if (sneakTimer.passedMs((long) MathUtility.random(200, 1200))) {
                    mc.options.keyShift.setDown(!mc.options.keyShift.isDown());
                    sneakTimer.reset();
                }
            }
            case Passive -> {
                if (!mc.options.keyShift.isDown())
                    mc.options.keyShift.setDown(true);
            }
        }

        if (messageTimer.passedMs(msgDelay.getValue() * 1000) && mc.getConnection() != null) {
            List<String> messages = Arrays.stream(mode.getValue() == SexMode.Active ? ACTIVE_MESSAGES : PASSIVE_MESSAGES).toList();
            mc.getConnection().sendCommand("msg " + target.getName().getString() + " " + messages.get((int) (Math.random() * messages.size())));
            messageTimer.reset();
        }
    }
}
