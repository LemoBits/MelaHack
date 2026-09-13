package thunder.hack.features.modules.player;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import thunder.hack.features.modules.Module;
import thunder.hack.injection.accesors.IMinecraftClient;
import thunder.hack.setting.Setting;

public class FastUse extends Module {
    public FastUse() {
        super("FastUse", Category.PLAYER);
    }

    private final Setting<Integer> delay = new Setting<>("Delay", 0, 0, 5);
    public Setting<Boolean> blocks = new Setting<>("Blocks", false);
    public Setting<Boolean> crystals = new Setting<>("Crystals", false);
    public Setting<Boolean> xp = new Setting<>("XP", false);
    public Setting<Boolean> all = new Setting<>("All", true);

    @Override
    public void onUpdate() {
        assert mc.player != null;
        if (check(mc.player.getMainHandItem().getItem()) && ((IMinecraftClient) mc).getUseCooldown() > delay.getValue())
            ((IMinecraftClient) mc).setUseCooldown(delay.getValue());
    }

    public boolean check(Item item) {
        return (item instanceof BlockItem && blocks.getValue())
                || (item == Items.END_CRYSTAL && crystals.getValue())
                || (item == Items.EXPERIENCE_BOTTLE && xp.getValue())
                || (all.getValue());
    }
}
