import java.awt.*;

/**
 * Pure-ish rendering: reads a GameState and draws it.
 * The only "side effect" is writing to the Graphics2D context,
 * which is isolated to the paint cycle.
 */
public final class Renderer {

    private Renderer() {}

    public static void render(Graphics2D g, GameState state, int width, int height) {
        // Background
        g.setColor(new Color(0x1E293B));
        g.fillRect(0, 0, width, height);

        // Platforms
        state.platforms().forEach(plat -> drawPlatform(g, plat));

        // Fighters (skip eliminated)
        state.fighters().stream()
                .filter(f -> !f.eliminated())
                .forEach(fighter -> drawFighter(g, fighter, state.time()));

        // Projectiles
        state.projectiles().forEach(proj -> drawProjectile(g, proj));

        // HUD
        drawHud(g, state);

        // Confetti particles
        state.particles().forEach(p -> drawParticle(g, p));

        // Victory overlay
        if (state.isGameOver()) {
            drawVictoryScreen(g, state, width, height);
        }
    }

    private static void drawPlatform(Graphics2D g, Platform plat) {
        g.setColor(plat.color());
        g.fillRect(
                (int) plat.pos().x(), (int) plat.pos().y(),
                (int) plat.width(), (int) plat.height()
        );
        g.setColor(plat.color().brighter());
        g.drawRect(
                (int) plat.pos().x(), (int) plat.pos().y(),
                (int) plat.width(), (int) plat.height()
        );
    }

    private static void drawFighter(Graphics2D g, Fighter fighter, double time) {
        // Flash during invulnerability — skip drawing every other frame
        if (fighter.isInvulnerable() && ((int) (time * 15)) % 2 == 0) {
            return;
        }

        int x = (int) fighter.pos().x();
        int y = (int) fighter.pos().y();
        int w = (int) Fighter.WIDTH;
        int h = (int) Fighter.HEIGHT;

        // Body
        g.setColor(fighter.color());
        g.fillRect(x, y, w, h);

        // Eyes
        g.setColor(Color.WHITE);
        int eyeY = y + 10;
        if (fighter.facingRight()) {
            g.fillRect(x + 22, eyeY, 8, 8);
            g.fillRect(x + 32, eyeY, 6, 6);
        } else {
            g.fillRect(x + 10, eyeY, 8, 8);
            g.fillRect(x + 2, eyeY, 6, 6);
        }
    }

    private static void drawProjectile(Graphics2D g, Projectile proj) {
        g.setColor(proj.color());
        g.fillRect((int) proj.pos().x(), (int) proj.pos().y(),
                   (int) Projectile.SIZE, (int) Projectile.SIZE);
    }

    private static void drawHud(Graphics2D g, GameState state) {
        drawHealthBars(g, state);

        g.setColor(new Color(0xCBD5E1));
        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g.drawString("P1: WASD+Space, G=Shoot", 10, 58);
        g.drawString("P2: Arrows+Enter, Num0=Shoot", 800 - 230, 58);
    }

    private static void drawHealthBars(Graphics2D g, GameState state) {
        int barWidth = 200;
        int barHeight = 20;
        int margin = 10;
        int topY = 10;

        for (Fighter f : state.fighters()) {
            int barX = f.id() == 0 ? margin : 800 - margin - barWidth;

            // Background (dark)
            g.setColor(new Color(0x374151));
            g.fillRect(barX, topY, barWidth, barHeight);

            // Health fill
            double ratio = Math.max(0, f.health() / Fighter.MAX_HEALTH);
            int fillWidth = (int) (barWidth * ratio);
            g.setColor(f.color());
            g.fillRect(barX, topY, fillWidth, barHeight);

            // Border
            g.setColor(Color.WHITE);
            g.drawRect(barX, topY, barWidth, barHeight);

            // Label
            g.setFont(new Font("Monospaced", Font.BOLD, 14));
            String label = "P" + (f.id() + 1) + " " + (int) f.health() + "/" + (int) Fighter.MAX_HEALTH;
            g.setColor(Color.WHITE);
            g.drawString(label, barX + 5, topY + 15);

            // Lives icons (small squares below health bar)
            int lifeSize = 12;
            int lifeY = topY + barHeight + 4;
            g.setColor(f.color());
            for (int i = 0; i < f.lives(); i++) {
                g.fillRect(barX + i * (lifeSize + 4), lifeY, lifeSize, lifeSize);
            }
            g.setColor(Color.WHITE);
            for (int i = 0; i < f.lives(); i++) {
                g.drawRect(barX + i * (lifeSize + 4), lifeY, lifeSize, lifeSize);
            }
        }
    }

    private static void drawParticle(Graphics2D g, Particle p) {
        g.setColor(p.color());
        g.fillRect((int) p.pos().x(), (int) p.pos().y(), 8, 8);
    }

    private static void drawVictoryScreen(Graphics2D g, GameState state, int width, int height) {
        // Semi-transparent overlay
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, width, height);

        // Winner text
        Fighter winner = state.fighters().stream()
                .filter(f -> f.id() == state.winnerId())
                .findFirst().orElse(null);
        if (winner == null) return;

        String winText = "PLAYER " + (winner.id() + 1) + " WINS!";
        g.setFont(new Font("SansSerif", Font.BOLD, 48));
        FontMetrics fm = g.getFontMetrics();
        int textX = (width - fm.stringWidth(winText)) / 2;
        int textY = height / 2 - 40;

        // Text shadow
        g.setColor(Color.BLACK);
        g.drawString(winText, textX + 2, textY + 2);
        // Text in winner's color
        g.setColor(winner.color());
        g.drawString(winText, textX, textY);

        // Play Again button
        int btnW = 200, btnH = 50;
        int btnX = (width - btnW) / 2;
        int btnY = height / 2 + 30;

        g.setColor(new Color(0x059669));
        g.fillRoundRect(btnX, btnY, btnW, btnH, 12, 12);
        g.setColor(Color.WHITE);
        g.drawRoundRect(btnX, btnY, btnW, btnH, 12, 12);

        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        fm = g.getFontMetrics();
        String btnText = "Play Again";
        int btx = btnX + (btnW - fm.stringWidth(btnText)) / 2;
        int bty = btnY + (btnH + fm.getAscent() - fm.getDescent()) / 2;
        g.drawString(btnText, btx, bty);
    }

    /** Returns the Play Again button bounds for click detection. */
    public static Rectangle getPlayAgainBounds(int width, int height) {
        int btnW = 200, btnH = 50;
        int btnX = (width - btnW) / 2;
        int btnY = height / 2 + 30;
        return new Rectangle(btnX, btnY, btnW, btnH);
    }
}
