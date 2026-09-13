package thunder.hack.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import thunder.hack.events.impl.EventScreen;
import thunder.hack.injection.accesors.ISignEditScreen;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.SearchInvResult;

import java.text.SimpleDateFormat;
import java.util.Date;

import static thunder.hack.features.modules.client.ClientSettings.isRu;

public class AutoSign extends Module {
    public AutoSign() {
        super("AutoSign", Category.MISC);
    }

    private final Setting<String> line1 = new Setting<>("Line1", "<player>");
    private final Setting<String> line2 = new Setting<>("Line2", "was here");
    private final Setting<String> line3 = new Setting<>("Line3", "<------------->");
    private final Setting<String> line4 = new Setting<>("Line4", "<date>");
    private final Setting<String> dateFormat = new Setting<>("DateFormat", "dd/MM/yyyy",
            v -> line1.getValue().contains("<date>") || line2.getValue().contains("<date>") || line3.getValue().contains("<date>") || line4.getValue().contains("<date>"));
    private final Setting<Boolean> glow = new Setting<>("Glowing", false);

    @EventHandler
    public void onScreen(EventScreen e) {
        if (e.getScreen() instanceof SignEditScreen ses) {
            e.cancel();
            sendPacketSilent(new ServerboundSignUpdatePacket(((ISignEditScreen) ses).getBlockEntity().getBlockPos(), ((ISignEditScreen) ses).isFront(), format(line1.getValue()), format(line2.getValue()), format(line3.getValue()), format(line4.getValue())));

            if (glow.getValue()) {
                SearchInvResult result = InventoryUtility.findItemInHotBar(Items.GLOW_INK_SAC);
                boolean offhand = mc.player.getOffhandItem().getItem() == Items.GLOW_INK_SAC;
                if (result.found() || offhand) {
                    InventoryUtility.saveSlot();
                    result.switchTo();
                    mc.gameMode.useItemOn(mc.player, offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND,
                            new BlockHitResult(((ISignEditScreen) ses).getBlockEntity().getBlockPos().getCenter().add(0, 0.5, 0), Direction.UP, ((ISignEditScreen) ses).getBlockEntity().getBlockPos(), false));
                    sendPacket(new ServerboundSwingPacket(offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND));
                    InventoryUtility.returnSlot();
                }
            }
        }
    }

    public String format(String s) {
        String format = "dd/MM/yyyy";

        try {
            format = new SimpleDateFormat(dateFormat.getValue()).format(new Date());
        } catch (Exception e) {
            sendMessage(ChatFormatting.RED + (isRu() ? "У тебя не правильный формат даты!" : "Your date format is wrong!"));
        }

        return s.replace("<player>", mc.getUser().getName()).replace("<date>", format);
    }
}
