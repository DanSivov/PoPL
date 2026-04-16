import java.util.ArrayList;
import java.util.List;

/**
 * Immutable snapshot of the entire game world at one instant.
 */
public record GameState(List<Fighter> fighters, List<Platform> platforms,
                         List<Projectile> projectiles, List<Particle> particles,
                         int winnerId, double time) {

    /** Create the initial world. */
    public static GameState initial() {
        var fighters = List.of(Fighter.player1(), Fighter.player2());

        var platforms = List.of(
                Platform.ground(0, 550, 800, 50),
                Platform.block(300, 460, 180, 20)
        );

        return new GameState(fighters, platforms, List.of(), List.of(), -1, 0);
    }

    public boolean isGameOver() {
        return winnerId >= 0;
    }

    public GameState withFighters(List<Fighter> f) {
        return new GameState(f, platforms, projectiles, particles, winnerId, time);
    }

    public GameState withFighter(int index, Fighter f) {
        var updated = new ArrayList<>(fighters);
        updated.set(index, f);
        return new GameState(List.copyOf(updated), platforms, projectiles, particles, winnerId, time);
    }

    public GameState withProjectiles(List<Projectile> p) {
        return new GameState(fighters, platforms, p, particles, winnerId, time);
    }

    public GameState withParticles(List<Particle> p) {
        return new GameState(fighters, platforms, projectiles, p, winnerId, time);
    }

    public GameState withWinnerId(int id) {
        return new GameState(fighters, platforms, projectiles, particles, id, time);
    }

    public GameState withTime(double t) {
        return new GameState(fighters, platforms, projectiles, particles, winnerId, t);
    }
}
