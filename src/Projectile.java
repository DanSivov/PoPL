import java.awt.*;

/**
 * Immutable projectile — a small square fired by a fighter.
 */
public record Projectile(int ownerId, Vec2 pos, Vec2 vel, Color color) {

    public static final double SIZE = 10;
    public static final double SPEED = 500;
    public static final double DAMAGE = 10;

    /** Fire a projectile from a fighter in their facing direction. */
    public static Projectile fire(Fighter owner) {
        double dir = owner.facingRight() ? 1 : -1;
        double startX = owner.facingRight()
                ? owner.pos().x() + Fighter.WIDTH
                : owner.pos().x() - SIZE;
        double startY = owner.pos().y() + Fighter.HEIGHT / 2 - SIZE / 2;

        return new Projectile(
                owner.id(),
                new Vec2(startX, startY),
                new Vec2(dir * SPEED, 0),
                owner.color().brighter()
        );
    }

    public Projectile withPos(Vec2 newPos) {
        return new Projectile(ownerId, newPos, vel, color);
    }

    /** Move the projectile by its velocity over dt seconds. */
    public Projectile move(double dt) {
        return withPos(pos.add(vel.scale(dt)));
    }

    /** True if the projectile is outside the screen bounds. */
    public boolean isOffScreen() {
        return pos.x() + SIZE < 0 || pos.x() > 800 || pos.y() + SIZE < 0 || pos.y() > 600;
    }
}
