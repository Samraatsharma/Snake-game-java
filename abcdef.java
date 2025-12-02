import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import java.io.File;
import javax.sound.sampled.*;

public class abcdef extends JPanel implements ActionListener, KeyListener {

    private final int WIDTH = 800;
    private final int HEIGHT = 600;
    private final int SIZE = 20;
    private final int DELAY = 45; // smoother movement
    private final double SMOOTH_FACTOR = 0.35; // trail smoothing

    private ArrayList<Point> snake = new ArrayList<>();
    private Point food;
    private int direction = KeyEvent.VK_RIGHT;
    private boolean running = false;
    private Timer timer;
    private int score = 0;
    private Random random = new Random();

    // For realistic motion and animation
    private double headX, headY;
    private double targetX, targetY;
    private double angle = 0;
    private boolean showTongue = false;
    private int tongueTimer = 0;

    // Sound effects
    private Clip eatSound, gameOverSound;

    public abcdef() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        loadSounds();
        startGame();
    }

    private void loadSounds() {
        try {
            eatSound = AudioSystem.getClip();
            eatSound.open(AudioSystem.getAudioInputStream(new File("eat.wav")));

            gameOverSound = AudioSystem.getClip();
            gameOverSound.open(AudioSystem.getAudioInputStream(new File("gameover.wav")));
        } catch (Exception e) {
            System.out.println("Sound files not found. Continue without sound.");
        }
    }

    private void playSound(Clip clip) {
        if (clip != null) {
            clip.setFramePosition(0);
            clip.start();
        }
    }

    private void startGame() {
        snake.clear();
        snake.add(new Point(100, 100));
        snake.add(new Point(80, 100));
        snake.add(new Point(60, 100));
        headX = 100;
        headY = 100;
        direction = KeyEvent.VK_RIGHT;
        score = 0;
        running = true;
        spawnFood();
        timer = new Timer(DELAY, this);
        timer.start();
    }

    private void spawnFood() {
        int x = random.nextInt(WIDTH / SIZE) * SIZE;
        int y = random.nextInt(HEIGHT / SIZE) * SIZE;
        food = new Point(x, y);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawBackground(g);

        if (running) {
            drawFood(g);
            drawSnake(g);
            drawScore(g);
        } else {
            drawGameOver(g);
        }
    }

    private void drawBackground(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        GradientPaint gp = new GradientPaint(0, 0, new Color(0, 30, 0), 0, HEIGHT, new Color(0, 60, 0));
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
    }

    private void drawSnake(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        for (int i = 0; i < snake.size(); i++) {
            Point p = snake.get(i);
            float alpha = (float) (1.0 - (i * 0.05));
            alpha = Math.max(0.2f, alpha);
            g2d.setColor(new Color(0, 255, 0, (int) (255 * alpha)));
            g2d.fillRoundRect(p.x, p.y, SIZE, SIZE, 10, 10);
        }

        // Draw the snake head with direction and tongue
        drawHead(g2d);
    }

    private void drawHead(Graphics2D g2d) {
        int headSize = SIZE + 4;
        g2d.setColor(new Color(0, 220, 0));
        g2d.fillOval((int) headX, (int) headY, headSize, headSize);

        // Eyes
        g2d.setColor(Color.WHITE);
        int eyeOffsetX = (int) (Math.cos(angle) * 8);
        int eyeOffsetY = (int) (Math.sin(angle) * 8);
        g2d.fillOval((int) headX + 8 + eyeOffsetX / 3, (int) headY + 5 + eyeOffsetY / 3, 5, 5);
        g2d.fillOval((int) headX + 8 + eyeOffsetX / 3, (int) headY + 10 + eyeOffsetY / 3, 5, 5);

        // Tongue animation
        if (showTongue) {
            g2d.setColor(Color.RED);
            int tongueX = (int) (headX + headSize / 2 + Math.cos(angle) * 15);
            int tongueY = (int) (headY + headSize / 2 + Math.sin(angle) * 15);
            g2d.fillRect(tongueX, tongueY, 8, 3);
        }
    }

    private void drawFood(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        RadialGradientPaint rgp = new RadialGradientPaint(
                new Point(food.x + SIZE / 2, food.y + SIZE / 2),
                SIZE,
                new float[]{0f, 1f},
                new Color[]{Color.YELLOW, Color.RED}
        );
        g2d.setPaint(rgp);
        g2d.fillOval(food.x, food.y, SIZE, SIZE);
    }

    private void drawScore(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 22));
        g.drawString("Score: " + score, 15, 25);
    }

    private void drawGameOver(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        String msg = "Game Over!";
        String restartMsg = "Press SPACE to Restart";
        FontMetrics fm = getFontMetrics(g.getFont());
        g.drawString(msg, (WIDTH - fm.stringWidth(msg)) / 2, HEIGHT / 2 - 30);
        g.setFont(new Font("Arial", Font.PLAIN, 25));
        fm = getFontMetrics(g.getFont());
        g.drawString(restartMsg, (WIDTH - fm.stringWidth(restartMsg)) / 2, HEIGHT / 2 + 20);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (running) {
            move();
            checkCollision();
            updateTongue();
        }
        repaint();
    }

    private void updateTongue() {
        tongueTimer++;
        if (tongueTimer % 40 == 0) showTongue = !showTongue;
    }

    private void move() {
        Point head = snake.get(0);
        targetX = head.x;
        targetY = head.y;

        switch (direction) {
            case KeyEvent.VK_LEFT -> targetX -= SIZE;
            case KeyEvent.VK_RIGHT -> targetX += SIZE;
            case KeyEvent.VK_UP -> targetY -= SIZE;
            case KeyEvent.VK_DOWN -> targetY += SIZE;
        }

        headX += (targetX - headX) * SMOOTH_FACTOR;
        headY += (targetY - headY) * SMOOTH_FACTOR;
        angle = Math.atan2(targetY - headY, targetX - headX);

        snake.add(0, new Point((int) headX, (int) headY));

        if (head.distance(food) < SIZE) {
            playSound(eatSound);
            score += 10;
            spawnFood();
        } else {
            snake.remove(snake.size() - 1);
        }
    }

    private void checkCollision() {
        Point head = snake.get(0);
        if (head.x < 0 || head.x >= WIDTH || head.y < 0 || head.y >= HEIGHT) {
            playSound(gameOverSound);
            running = false;
            timer.stop();
        }
        for (int i = 1; i < snake.size(); i++) {
            if (head.equals(snake.get(i))) {
                playSound(gameOverSound);
                running = false;
                timer.stop();
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if ((key == KeyEvent.VK_LEFT) && (direction != KeyEvent.VK_RIGHT)) direction = KeyEvent.VK_LEFT;
        if ((key == KeyEvent.VK_RIGHT) && (direction != KeyEvent.VK_LEFT)) direction = KeyEvent.VK_RIGHT;
        if ((key == KeyEvent.VK_UP) && (direction != KeyEvent.VK_DOWN)) direction = KeyEvent.VK_UP;
        if ((key == KeyEvent.VK_DOWN) && (direction != KeyEvent.VK_UP)) direction = KeyEvent.VK_DOWN;

        if (!running && key == KeyEvent.VK_SPACE) startGame();
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        JFrame frame = new JFrame("Realistic Snake Game 🐍 by Samraat");
        abcdef game = new abcdef();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
