package thunder.hack.features.modules.misc;

import thunder.hack.features.cmd.impl.KitCommand;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

import java.util.ArrayList;
import java.util.HashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;

import static thunder.hack.features.modules.client.ClientSettings.isRu;

public class AutoGear extends Module {
    public AutoGear() {
        super("AutoGear", Category.MISC);
    }

    public Setting<Integer> actionDelay = new Setting<>("ActionDelay", 50, 0, 20);
    public Setting<Integer> clicksPerAction = new Setting<>("Click/Action", 1, 1, 108);

    private HashMap<Integer, String> expectedInv = new HashMap<>();
    private int delay = 0;

    @Override
    public void onEnable() {
        setup();
    }

    public void setup() {
        String selectedKit = KitCommand.getSelectedKit();

        if (selectedKit.isEmpty()) {
            disable(isRu() ? "Не выбран кит! Воспользуйся командой kit" : "No kit is selected! Use the kit command");
            return;
        }

        sendMessage(isRu() ? "Выбран кит => " + ChatFormatting.AQUA + selectedKit : "Selected kit -> " + ChatFormatting.AQUA + selectedKit);

        String kitItems = KitCommand.getKitItems(selectedKit);

        if (kitItems.isEmpty() || kitItems.split(" ").length != 36) {
            disable(isRu() ? "Произошла ошибка в конфигурации кита! Создай кит снова" : "There was an error in the kit configuration! Create the kit again");
            return;
        }

        String[] items = kitItems.split(" ");
        expectedInv = new HashMap<>();

        for (int i = 0; i < 36; i++)
            if (!items[i].equals("block.minecraft.air"))
                expectedInv.put(i, items[i]);
    }

    @Override
    public void onUpdate() {
        if (delay > 0) {
            delay--;
            return;
        }

        if (expectedInv.isEmpty()) {
            setup();
            return;
        }

        int actions = 0;

        AbstractContainerMenu handler = mc.player.containerMenu;

        if (handler.slots.size() != 63 && handler.slots.size() != 90)
            return;

        ArrayList<Integer> clickSequence = buildClickSequence(handler);
        for (int s : clickSequence) {
            clickSlot(s);
            actions++;
            if (actions >= clicksPerAction.getValue())
                break;
        }
        delay = actionDelay.getValue();
    }

    private int searchInContainer(String name, boolean lower, AbstractContainerMenu handler) {
        ItemStack cursorStack = handler.getCarried();

        if ((cursorStack.getItem() instanceof PotionItem ?
                cursorStack.getItem().getDescriptionId() + cursorStack.getItem().components().get(DataComponents.POTION_CONTENTS).getColor()
                : cursorStack.getItem().getDescriptionId()).equals(name))
            return -2;

        for (int i = 0; i < (lower ? 26 : 53); i++) {
            ItemStack stack = handler.getSlot(i).getItem();
            if ((stack.getItem() instanceof PotionItem ?
                    stack.getItem().getDescriptionId() + stack.getItem().components().get(DataComponents.POTION_CONTENTS).getColor()
                    : stack.getItem().getDescriptionId()).equals(name))
                return i;
        }
        return -1;
    }

    private ArrayList<Integer> buildClickSequence(AbstractContainerMenu handler) {
        ArrayList<Integer> clicks = new ArrayList<>();
        for (int s : expectedInv.keySet()) {
            int lower = s < 9 ? s + 54 : s + 18;
            int upper = s < 9 ? s + 81 : s + 45;

            ItemStack itemInslot = handler.slots.get((handler.slots.size() == 63 ? lower : upper)).getItem();

            if((itemInslot.getItem() instanceof PotionItem ?
                    itemInslot.getItem().getDescriptionId() + itemInslot.getItem().components().get(DataComponents.POTION_CONTENTS).getColor()
                    : itemInslot.getItem().getDescriptionId()).equals(expectedInv.get(s)))
                continue;

            int slot = searchInContainer(expectedInv.get(s), handler.slots.size() == 63, handler);

            if (slot == -2) {
                clicks.add(handler.slots.size() == 63 ? lower : upper);
            } else if (slot != -1) {
                clicks.add(slot);
                clicks.add(handler.slots.size() == 63 ? lower : upper);
                clicks.add(slot);
            }
        }
        return clicks;
    }
}
