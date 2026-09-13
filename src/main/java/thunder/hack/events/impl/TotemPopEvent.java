package thunder.hack.events.impl;

import net.minecraft.world.entity.player.Player;
import thunder.hack.events.Event;

public class TotemPopEvent extends Event {
    private final Player entity;
    private int pops;

    public TotemPopEvent(Player entity,int pops) {
        this.entity = entity;
        this.pops = pops;
    }

    public Player getEntity() {
        return this.entity;
    }

    public int getPops() {
        return this.pops;
    }
}