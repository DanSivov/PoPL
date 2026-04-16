import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The impure shell: captures input, runs the pure game loop, and renders.
 * All mutation is confined to this class — the game logic in Physics is pure.
 */
public class GamePanel extends JPanel implements Runnable {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final double TARGET_FPS = 60.0;
    private static final double NS_PER_FRAME = 1_000_000_000.0 / TARGET_FPS;

    private final Set<Integer> keysDown = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private volatile GameState state;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                keysDown.add(e.getKeyCode());
            }

            @Override
            public void keyReleased(KeyEvent e) {
                keysDown.remove(e.getKeyCode());
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (state.isGameOver()) {
                    Rectangle btn = Renderer.getPlayAgainBounds(WIDTH, HEIGHT);
                    if (btn.contains(e.getPoint())) {
                        state = GameState.initial();
                    }
                }
            }
        });

        state = GameState.initial();
    }

    /** Capture input for both players as immutable snapshots. */
    private List<Input> captureInputs() {
        // P1: WASD + Space, shoot = G
        Input p1 = new Input(
                keysDown.contains(KeyEvent.VK_A),
                keysDown.contains(KeyEvent.VK_D),
                keysDown.contains(KeyEvent.VK_W) || keysDown.contains(KeyEvent.VK_SPACE),
                keysDown.contains(KeyEvent.VK_G)
        );

        // P2: Arrow keys + Enter, shoot = Numpad 0
        Input p2 = new Input(
                keysDown.contains(KeyEvent.VK_LEFT),
                keysDown.contains(KeyEvent.VK_RIGHT),
                keysDown.contains(KeyEvent.VK_UP) || keysDown.contains(KeyEvent.VK_ENTER),
                keysDown.contains(KeyEvent.VK_NUMPAD0)
        );

        return List.of(p1, p2);
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();

        while (true) {
            long now = System.nanoTime();
            double dt = (now - lastTime) / 1_000_000_000.0;
            lastTime = now;
            dt = Math.min(dt, 0.05);

            List<Input> inputs = captureInputs();
            state = Physics.update(state, inputs, dt);

            repaint();

            long elapsed = System.nanoTime() - now;
            long sleepNs = (long) (NS_PER_FRAME - elapsed);
            if (sleepNs > 0) {
                try {
                    Thread.sleep(sleepNs / 1_000_000, (int) (sleepNs % 1_000_000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        var g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Renderer.render(g2, state, getWidth(), getHeight());
    }
}
