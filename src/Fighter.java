import java.awt.*;

/**
 * Immutable fighter state — a colored square with position, velocity, health, and lives.
 */
public record Fighter(int id, Vec2 pos, Vec2 vel, boolean grounded, boolean facingRight,
                       Color color, double health, int lives, int shootCooldown,
                       int invulnFrames, boolean eliminated) {

    public static final double WIDTH = 40;
    public static final double HEIGHT = 40;
    public static final double MAX_HEALTH = 100;
    public static final int SHOOT_COOLDOWN_FRAMES = 30;
    public static final int RESPAWN_INVULN_FRAMES = 120; // 2s at 60fps

    public static Fighter player1() {
        return new Fighter(0, new Vec2(200, 300), new Vec2(0, 0),
                false, true, new Color(0x3B82F6), MAX_HEALTH, 3, 0, 0, false);
    }

    public static Fighter player2() {
        return new Fighter(1, new Vec2(550, 300), new Vec2(0, 0),
                false, false, new Color(0xEF4444), MAX_HEALTH, 3, 0, 0, false);
    }

    /** Respawn at starting position with full health and invulnerability. */
    public Fighter respawn() {
        Vec2 spawnPos = id == 0 ? new Vec2(200, 300) : new Vec2(550, 300);
        boolean spawnFacing = id == 0;
        return new Fighter(id, spawnPos, new Vec2(0, 0), false, spawnFacing,
                color, MAX_HEALTH, lives - 1, 0, RESPAWN_INVULN_FRAMES, false);
    }

    public boolean isInvulnerable() {
        return invulnFrames > 0;
    }

    public Fighter withPos(Vec2 newPos) {
        return new Fighter(id, newPos, vel, grounded, facingRight, color, health, lives, shootCooldown, invulnFrames, eliminated);
    }

    public Fighter withVel(Vec2 newVel) {
        return new Fighter(id, pos, newVel, grounded, facingRight, color, health, lives, shootCooldown, invulnFrames, eliminated);
    }

    public Fighter withGrounded(boolean g) {
        return new Fighter(id, pos, vel, g, facingRight, color, health, lives, shootCooldown, invulnFrames, eliminated);
    }

    public Fighter withFacing(boolean right) {
        return new Fighter(id, pos, vel, grounded, right, color, health, lives, shootCooldown, invulnFrames, eliminated);
    }

    public Fighter withHealth(double h) {
        return new Fighter(id, pos, vel, grounded, facingRight, color, h, lives, shootCooldown, invulnFrames, eliminated);
    }

    public Fighter withLives(int l) {
        return new Fighter(id, pos, vel, grounded, facingRight, color, health, l, shootCooldown, invulnFrames, eliminated);
    }

    public Fighter withShootCooldown(int cd) {
        return new Fighter(id, pos, vel, grounded, facingRight, color, health, lives, cd, invulnFrames, eliminated);
    }

    public Fighter withInvulnFrames(int frames) {
        return new Fighter(id, pos, vel, grounded, facingRight, color, health, lives, shootCooldown, frames, eliminated);
    }

    public Fighter withEliminated(boolean e) {
        return new Fighter(id, pos, vel, grounded, facingRight, color, health, lives, shootCooldown, invulnFrames, e);
    }
}
