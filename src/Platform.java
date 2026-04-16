import java.awt.*;

/**
 * Immutable rectangular platform.
 */
public record Platform(Vec2 pos, double width, double height, Color color) {

    public static Platform ground(double x, double y, double w, double h) {
        return new Platform(new Vec2(x, y), w, h, new Color(0x4B5563)); // gray
    }

    public static Platform block(double x, double y, double w, double h) {
        return new Platform(new Vec2(x, y), w, h, new Color(0x059669)); // green
    }
}
