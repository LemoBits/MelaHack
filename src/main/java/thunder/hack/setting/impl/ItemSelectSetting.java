package thunder.hack.setting.impl;

import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ItemSelectSetting {
    private List<String> itemsById;

    public ItemSelectSetting(List<String> itemsById) {
        this.itemsById = itemsById;
    }

    public List<String> getItemsById() {
        return itemsById;
    }

    public void add(String s) {
        itemsById.add(s);
    }

    public void remove(String s) {
        itemsById.remove(s);
    }

    public boolean contains(String s) {
        return itemsById.contains(s);
    }

    public void add(Block b) {
        add(b.getDescriptionId().replace("block.minecraft.", ""));
    }

    public void add(Item i) {
        add(i.getDescriptionId().replace("item.minecraft.", ""));
    }

    public void remove(Block b) {
        remove(b.getDescriptionId().replace("block.minecraft.", ""));
    }

    public void remove(Item i) {
        remove(i.getDescriptionId().replace("item.minecraft.", ""));
    }

    public boolean contains(Block b) {
        return contains(b.getDescriptionId().replace("block.minecraft.", ""));
    }

    public boolean contains(Item i) {
        return contains(i.getDescriptionId().replace("item.minecraft.", ""));
    }

    public void clear() {
        itemsById.clear();
    }
}
