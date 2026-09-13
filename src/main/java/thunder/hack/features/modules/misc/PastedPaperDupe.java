package thunder.hack.features.modules.misc;

import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;
import thunder.hack.features.modules.Module;
import thunder.hack.events.impl.EventPostTick;

import java.util.List;
import java.util.Optional;

public class PastedPaperDupe extends Module {

    public PastedPaperDupe() {
        super("PastedPaperDupe", Category.MISC);
    }

    @EventHandler
    private void onTick(EventPostTick event) {
        if(!(mc.player.getMainHandItem().getItem()  == Items.WRITABLE_BOOK)) {
            disable("Please hold a writable book!");
            return;
        }
        for (int i = 9; i < 44; i++) {
            if (36 + mc.player.getInventory().getSelectedSlot() == i) continue;
            mc.player.connection.send(new ServerboundContainerClickPacket(
                    mc.player.containerMenu.containerId,
                    mc.player.containerMenu.getStateId(),
                    (short) i,
                    (byte) 1,
                    ClickType.THROW,
                    Int2ObjectMaps.emptyMap(),
                    HashedStack.EMPTY
            ));
        }
        mc.player.connection.send(new ServerboundEditBookPacket(
                mc.player.getInventory().getSelectedSlot(), List.of(""), Optional.of("The quick brown fox jumps over the lazy dog"
        )));
        toggle();
    }
}
