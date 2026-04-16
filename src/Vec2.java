/**
 * Immutable 2D vector. All operations return new instances.
 */
public record Vec2(double x, double y) {

    public Vec2 add(Vec2 other) {
        return new Vec2(x + other.x, y + other.y);
    }

    public Vec2 scale(double s) {
        return new Vec2(x * s, y * s);
    }

    public Vec2 withX(double newX) {
        return new Vec2(newX, y);
    }

    public Vec2 withY(double newY) {
        return new Vec2(x, newY);
    }
}
