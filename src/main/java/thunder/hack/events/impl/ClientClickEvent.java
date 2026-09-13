package thunder.hack.events.impl;

import net.minecraft.network.chat.ClickEvent;

/*When using a clickable text client, you should create this object instead of the usual ClickEvent.
If not, a vulnerability could occur as mentioned in this GitHub issue: https://github.com/MeteorDevelopment/meteor-client/pull/4399.*/
public record ClientClickEvent(Action action, String value) implements ClickEvent {
    @Override
    public Action action() {
        return action;
    }

    public String getValue() {
        return value;
    }
}
