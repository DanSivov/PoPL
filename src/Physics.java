import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Pure functions that advance the game state.
 * No side effects — every method takes state in and returns new state out.
 */
public final class Physics {

    private Physics() {}

    private static final double GRAVITY = 1400;
    private static final double JUMP_VEL = -550;
    private static final double MOVE_SPEED = 320;
    private static final double FRICTION = 0.85;
    private static final int CONFETTI_PER_FRAME = 5;
    private static final Random RNG = new Random();

    // ---- top-level pipeline ----

    public static GameState update(GameState state, List<Input> inputs, double dt) {
        if (state.isGameOver()) {
            // Only animate confetti when game is over
            return updateParticles(spawnConfetti(state), dt).withTime(state.time() + dt);
        }

        var s = applyAllInputs(state, inputs);
        s = applyAllPhysics(s, dt);
        s = fireProjectiles(s, inputs);
        s = updateProjectiles(s, dt);
        s = resolveProjectileHits(s);
        s = checkDeaths(s);
        s = tickCooldowns(s);
        s = tickInvulnerability(s);
        s = resolveAllFighterCollisions(s);
        // Spawn confetti on the frame the game ends
        if (s.isGameOver()) {
            s = spawnConfetti(s);
        }
        return s.withTime(state.time() + dt);
    }

    // ---- input handling (pure) ----

    private static GameState applyAllInputs(GameState state, List<Input> inputs) {
        List<Fighter> updated = IntStream.range(0, state.fighters().size())
                .mapToObj(i -> {
                    Fighter f = state.fighters().get(i);
                    if (f.eliminated()) return f;
                    return applyInput(f, inputs.get(i));
                })
                .toList();
        return state.withFighters(updated);
    }

    private static Fighter applyInput(Fighter f, Input input) {
        double vx = f.vel().x();

        if (input.left()) {
            vx = -MOVE_SPEED;
        } else if (input.right()) {
            vx = MOVE_SPEED;
        } else {
            vx *= FRICTION;
            if (Math.abs(vx) < 1) vx = 0;
        }

        double vy = f.vel().y();
        if (input.jump() && f.grounded()) {
            vy = JUMP_VEL;
        }

        boolean facing = input.left() ? false
                        : input.right() ? true
                        : f.facingRight();

        return f.withVel(new Vec2(vx, vy)).withFacing(facing);
    }

    // ---- physics (pure) ----

    private static GameState applyAllPhysics(GameState state, double dt) {
        List<Fighter> updated = state.fighters().stream()
                .map(f -> f.eliminated() ? f : applyPhysics(f, dt))
                .toList();
        return state.withFighters(updated);
    }

    private static Fighter applyPhysics(Fighter f, double dt) {
        double newVy = f.vel().y() + GRAVITY * dt;
        var newVel = f.vel().withY(newVy);
        var newPos = f.pos().add(newVel.scale(dt));
        return f.withPos(newPos).withVel(newVel).withGrounded(false);
    }

    // ---- projectile firing (pure) ----

    private static GameState fireProjectiles(GameState state, List<Input> inputs) {
        var fighters = new ArrayList<>(state.fighters());
        var projectiles = new ArrayList<>(state.projectiles());

        for (int i = 0; i < fighters.size(); i++) {
            Fighter f = fighters.get(i);
            Input input = inputs.get(i);

            if (!f.eliminated() && input.shoot() && f.shootCooldown() <= 0) {
                projectiles.add(Projectile.fire(f));
                fighters.set(i, f.withShootCooldown(Fighter.SHOOT_COOLDOWN_FRAMES));
            }
        }

        return state.withFighters(List.copyOf(fighters))
                     .withProjectiles(List.copyOf(projectiles));
    }

    // ---- projectile movement (pure) ----

    private static GameState updateProjectiles(GameState state, double dt) {
        List<Projectile> updated = state.projectiles().stream()
                .map(p -> p.move(dt))
                .filter(p -> !p.isOffScreen())
                .toList();
        return state.withProjectiles(updated);
    }

    // ---- projectile hit detection (pure) ----

    private static GameState resolveProjectileHits(GameState state) {
        var fighters = new ArrayList<>(state.fighters());
        var surviving = new ArrayList<>(state.projectiles());

        for (int pi = surviving.size() - 1; pi >= 0; pi--) {
            Projectile proj = surviving.get(pi);

            for (int fi = 0; fi < fighters.size(); fi++) {
                Fighter f = fighters.get(fi);
                if (f.id() == proj.ownerId()) continue;
                if (f.eliminated() || f.isInvulnerable()) continue;

                if (projectileHitsFighter(proj, f)) {
                    fighters.set(fi, f.withHealth(Math.max(0, f.health() - Projectile.DAMAGE)));
                    surviving.remove(pi);
                    break;
                }
            }
        }

        return state.withFighters(List.copyOf(fighters))
                     .withProjectiles(List.copyOf(surviving));
    }

    private static boolean projectileHitsFighter(Projectile p, Fighter f) {
        return p.pos().x() < f.pos().x() + Fighter.WIDTH
            && p.pos().x() + Projectile.SIZE > f.pos().x()
            && p.pos().y() < f.pos().y() + Fighter.HEIGHT
            && p.pos().y() + Projectile.SIZE > f.pos().y();
    }

    // ---- death and respawn (pure) ----

    private static GameState checkDeaths(GameState state) {
        List<Fighter> updated = state.fighters().stream()
                .map(f -> {
                    if (f.eliminated() || f.health() > 0) return f;
                    // Health hit 0
                    if (f.lives() > 1) {
                        return f.respawn(); // lose a life, respawn with full HP
                    } else {
                        return f.withLives(0).withEliminated(true);
                    }
                })
                .toList();

        // Check win condition
        long alive = updated.stream().filter(f -> !f.eliminated()).count();
        int winnerId = state.winnerId();
        if (alive == 1 && winnerId < 0) {
            winnerId = updated.stream().filter(f -> !f.eliminated()).findFirst().get().id();
        }

        return state.withFighters(updated).withWinnerId(winnerId);
    }

    // ---- cooldown and invulnerability ticking (pure) ----

    private static GameState tickCooldowns(GameState state) {
        List<Fighter> updated = state.fighters().stream()
                .map(f -> f.shootCooldown() > 0 ? f.withShootCooldown(f.shootCooldown() - 1) : f)
                .toList();
        return state.withFighters(updated);
    }

    private static GameState tickInvulnerability(GameState state) {
        List<Fighter> updated = state.fighters().stream()
                .map(f -> f.invulnFrames() > 0 ? f.withInvulnFrames(f.invulnFrames() - 1) : f)
                .toList();
        return state.withFighters(updated);
    }

    // ---- collision resolution (pure) ----

    private static GameState resolveAllFighterCollisions(GameState state) {
        List<Fighter> updated = state.fighters().stream()
                .map(f -> f.eliminated() ? f : resolveCollisions(f, state.platforms()))
                .toList();
        return state.withFighters(updated);
    }

    private static Fighter resolveCollisions(Fighter f, List<Platform> platforms) {
        Fighter resolved = platforms.stream()
                .reduce(f, Physics::resolveOne, (a, b) -> b);
        return clampToScreen(resolved);
    }

    private static Fighter resolveOne(Fighter f, Platform plat) {
        double px = f.pos().x(), py = f.pos().y();
        double pw = Fighter.WIDTH, ph = Fighter.HEIGHT;

        double bx = plat.pos().x(), by = plat.pos().y();
        double bw = plat.width(), bh = plat.height();

        boolean overlaps = px < bx + bw && px + pw > bx
                        && py < by + bh && py + ph > by;
        if (!overlaps) return f;

        double overlapLeft   = (px + pw) - bx;
        double overlapRight  = (bx + bw) - px;
        double overlapTop    = (py + ph) - by;
        double overlapBottom = (by + bh) - py;

        double minOverlap = Math.min(Math.min(overlapLeft, overlapRight),
                                     Math.min(overlapTop, overlapBottom));

        if (minOverlap == overlapTop && f.vel().y() >= 0) {
            return f.withPos(f.pos().withY(by - ph))
                    .withVel(f.vel().withY(0))
                    .withGrounded(true);
        } else if (minOverlap == overlapBottom && f.vel().y() < 0) {
            return f.withPos(f.pos().withY(by + bh))
                    .withVel(f.vel().withY(0));
        } else if (minOverlap == overlapLeft) {
            return f.withPos(f.pos().withX(bx - pw))
                    .withVel(f.vel().withX(0));
        } else if (minOverlap == overlapRight) {
            return f.withPos(f.pos().withX(bx + bw))
                    .withVel(f.vel().withX(0));
        }
        return f;
    }

    private static Fighter clampToScreen(Fighter f) {
        double x = Math.max(0, Math.min(f.pos().x(), 800 - Fighter.WIDTH));
        double y = Math.min(f.pos().y(), 600 - Fighter.HEIGHT);
        boolean grounded = f.grounded() || y != f.pos().y();
        return f.withPos(new Vec2(x, y)).withGrounded(grounded);
    }

    // ---- confetti particles (pure) ----

    private static GameState spawnConfetti(GameState state) {
        var particles = new ArrayList<>(state.particles());
        for (int i = 0; i < CONFETTI_PER_FRAME; i++) {
            particles.add(Particle.randomConfetti(RNG, 800));
        }
        return state.withParticles(List.copyOf(particles));
    }

    private static GameState updateParticles(GameState state, double dt) {
        List<Particle> updated = state.particles().stream()
                .map(p -> p.update(dt))
                .filter(p -> !p.isDead() && p.pos().y() < 650)
                .toList();
        return state.withParticles(updated);
    }
}
