package thunder.hack.injection.accesors;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientInput.class)
public interface IInput {
    @Accessor("moveVector")
    Vec2 getMovementVector();

    @Accessor("moveVector")
    void setMovementVector(Vec2 movementVector);

    @Accessor("keyPresses")
    Input getPlayerInput();

    @Accessor("keyPresses")
    void setPlayerInput(Input playerInput);
}
