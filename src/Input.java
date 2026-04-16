/**
 * Immutable snapshot of player input at a single frame.
 */
public record Input(boolean left, boolean right, boolean jump, boolean shoot) {

    public static final Input NONE = new Input(false, false, false, false);
}
