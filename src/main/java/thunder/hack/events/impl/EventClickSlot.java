package thunder.hack.events.impl;

import net.minecraft.world.inventory.ContainerInput;
import thunder.hack.events.Event;

public class EventClickSlot extends Event {
    private final ContainerInput slotActionType;
    private final int slot, button, id;

    public EventClickSlot(ContainerInput slotActionType, int slot, int button, int id) {
        this.slot = slot;
        this.button = button;
        this.id = id;
        this.slotActionType = slotActionType;
    }

    public ContainerInput getSlotActionType() {
        return slotActionType;
    }

    public int getSlot() {
        return slot;
    }

    public int getButton() {
        return button;
    }

    public int getId() {
        return id;
    }
}
