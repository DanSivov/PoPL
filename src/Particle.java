import java.awt.*;
import java.util.Random;

/**
 * Immutable confetti particle for the victory screen.
 */
public record Particle(Vec2 pos, Vec2 vel, Color color, double life) {

    private static final Color[] COLORS = {
            new Color(0xFF6B6B), new Color(0xFFE66D), new Color(0x4ECDC4),
            new Color(0xA78BFA), new Color(0xF472B6), new Color(0x60A5FA),
            new Color(0x34D399), new Color(0xFBBF24)
    };

    public static Particle randomConfetti(Random rng, int screenWidth) {
        double x = rng.nextDouble() * screenWidth;
        double vx = (rng.nextDouble() - 0.5) * 200;
        double vy = rng.nextDouble() * 100 + 50;
        Color color = COLORS[rng.nextInt(COLORS.length)];
        double life = 2.0 + rng.nextDouble() * 2.0;

        return new Particle(new Vec2(x, -10), new Vec2(vx, vy), color, life);
    }

    public Particle update(double dt) {
        var newVel = vel.withY(vel.y() + 200 * dt); // gentle gravity
        var newPos = pos.add(newVel.scale(dt));
        return new Particle(newPos, newVel, color, life - dt);
    }

    public boolean isDead() {
        return life <= 0;
    }
}
