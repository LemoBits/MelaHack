package thunder.hack.features.modules.player;

import thunder.hack.utility.BaritoneIntegration;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thunder.hack.ThunderHack;
import thunder.hack.injection.accesors.IMinecraftClient;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public class AutoEat extends Module {
    public AutoEat() {
        super("AutoEat", Category.PLAYER);
    }

    public final Setting<Integer> hunger = new Setting("Hunger", 8, 0, 20);
    public final Setting<Boolean> gapple = new Setting("Gapple", false);
    public final Setting<Boolean> chorus = new Setting("Chorus", false);
    public final Setting<Boolean> rottenFlesh = new Setting("RottenFlesh", false);
    public final Setting<Boolean> spiderEye = new Setting("SpiderEye", false);
    public final Setting<Boolean> pufferfish = new Setting("Pufferfish", false);
    public final Setting<Boolean> swapBack = new Setting<>("SwapBack", true);
    public final Setting<Boolean> pauseBaritone = new Setting<>("PauseBaritone", true, v -> ThunderHack.baritone);

    private boolean eating;
    private int prevSlot;

    @Override
    public void onUpdate() {
        if (mc.player.getFoodData().getFoodLevel() <= hunger.getValue()) {

            boolean found;

            if(!isHandGood(InteractionHand.MAIN_HAND) && !isHandGood(InteractionHand.OFF_HAND)) {
                found = switchToFood();
            } else found = true;

            if (!found) {
                if (eating)
                    stopEating();
                return;
            }

            startEating();
        } else if (eating)
            stopEating();
    }

    public void startEating() {
        eating = true;

        if (mc.gui.screen() != null && !mc.player.isUsingItem())
            ((IMinecraftClient) mc).idoItemUse();
        else {
            if(pauseBaritone.getValue() && ThunderHack.baritone)
                BaritoneIntegration.execute("pause");

            mc.options.keyUse.setDown(true);
        }
    }

    public void stopEating() {
        eating = false;
        mc.options.keyUse.setDown(false);
        if (swapBack.getValue())
            mc.player.getInventory().setSelectedSlot(prevSlot);

        if (pauseBaritone.getValue() && ThunderHack.baritone)
            BaritoneIntegration.execute("resume");
    }

    public boolean switchToFood() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.getComponents().has(DataComponents.FOOD)) {
                if (!gapple.getValue() && (stack.getItem() == Items.GOLDEN_APPLE || stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE))
                    continue;
                if (!chorus.getValue() && (stack.getItem() == Items.CHORUS_FRUIT))
                    continue;
                if (!rottenFlesh.getValue() && (stack.getItem() == Items.ROTTEN_FLESH))
                    continue;
                if (!spiderEye.getValue() && (stack.getItem() == Items.SPIDER_EYE))
                    continue;
                if (!pufferfish.getValue() && (stack.getItem() == Items.PUFFERFISH))
                    continue;
                prevSlot = mc.player.getInventory().getSelectedSlot();
                mc.player.getInventory().setSelectedSlot(i);
                sendPacket(new ServerboundSetCarriedItemPacket(i));
                return true;
            }
        }
        return false;
    }


    private boolean isHandGood(InteractionHand hand) {
        ItemStack stack = hand == InteractionHand.MAIN_HAND ? mc.player.getMainHandItem() : mc.player.getOffhandItem();

        Item item = stack.getItem();
        return stack.getComponents().has(DataComponents.FOOD)
                && (gapple.getValue() || (item != Items.GOLDEN_APPLE && item != Items.ENCHANTED_GOLDEN_APPLE))
                && (chorus.getValue() || item != Items.CHORUS_FRUIT)
                && (rottenFlesh.getValue() || item != Items.ROTTEN_FLESH)
                && (spiderEye.getValue() || item != Items.SPIDER_EYE)
                && (pufferfish.getValue() || item != Items.PUFFERFISH);
    }
}
