package thunder.hack.utility;

/** Small immutable pair used where Minecraft's removed Tuple type was previously used. */
public record Tuple<A, B>(A a, B b) {
    public A getA() { return a; }
    public B getB() { return b; }
}
