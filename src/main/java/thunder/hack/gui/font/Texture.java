package thunder.hack.gui.font;

import net.minecraft.resources.Identifier;

public class Texture {
    final Identifier id;

    public Texture(String path) {
        id = Identifier.fromNamespaceAndPath("thunderhack", validatePath(path));
    }

    public Texture(Identifier i) {
        id = Identifier.fromNamespaceAndPath(i.getNamespace(), i.getPath());
    }

    String validatePath(String path) {
        if (Identifier.isValidPath(path)) {
            return path;
        }
        StringBuilder ret = new StringBuilder();
        for (char c : path.toLowerCase().toCharArray()) {
            if (Identifier.validPathChar(c)) {
                ret.append(c);
            }
        }
        return ret.toString();
    }

    public Identifier getId() {
        return id;
    }
}