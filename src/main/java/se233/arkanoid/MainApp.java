package se233.arkanoid;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se233.arkanoid.effect.Particle;
import se233.arkanoid.exception.GameException;
import se233.arkanoid.model.Ball;
import se233.arkanoid.model.Boss;
import se233.arkanoid.model.Brick;
import se233.arkanoid.model.BrickEvent;
import se233.arkanoid.model.Bullet;
import se233.arkanoid.model.GameWorld;
import se233.arkanoid.model.Laser;
import se233.arkanoid.model.Paddle;
import se233.arkanoid.model.PowerUp;
import se233.arkanoid.progress.Progress;
import se233.arkanoid.sprite.Animation;
import se233.arkanoid.sprite.BossSprites;
import se233.arkanoid.sprite.SpriteAtlas;
import se233.arkanoid.sprite.SpriteAtlas.Frame;
import se233.arkanoid.sprite.SpriteEffect;
import se233.arkanoid.stage.StageFactory;
import se233.arkanoid.upgrade.Upgrade;

import java.nio.file.Path;
import java.util.*;

public class MainApp extends Application {
    private static final Logger log = LogManager.getLogger(MainApp.class);
    private static final double SQUASH_TIME = 0.12;

    private enum Screen { MENU, SELECT, GAME }

    private final Set<KeyCode> keys = new HashSet<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<SpriteEffect> effects = new ArrayList<>();
    private final Map<Brick, Double> flashes = new HashMap<>();
    private final Random rng = new Random();
    private final Progress progress = new Progress(Path.of("progress.properties"));

    // animation ชุดที่ 1: paddle เคลื่อนที่ / โหมดเลเซอร์
    private final Animation paddleMove = new Animation(SpriteAtlas.PADDLE, 0.07, true);
    private final Animation paddleLaser = new Animation(SpriteAtlas.PADDLE_LASER, 0.15, true);
    // animation ชุดที่ 2: บอลเด้ง (บอลบีบแบน + ประกายระเบิดเล็ก) ใช้ ballSquash และ effects

    private SpriteAtlas atlas;          // null = ใช้สี่เหลี่ยมแทน (fallback)
    private BossSprites bossSprites;    // null = วาดสี่เหลี่ยมแทน
    private boolean showDebug = false;
    private Screen screen = Screen.MENU;
    private GameWorld world;
    private int selectedStage = 1;
    private int lastCleared = 0;
    private int lastLives;
    private int lastCombo = 0;
    private int paddleDir = 0;
    private double ballSquash = 0;
    private double comboPulse = 0;
    private double redFlash = 0;
    private String errorMessage = null;
    private long errorUntilNanos = 0;

    @Override
    public void start(Stage stage) {
        try {
            atlas = new SpriteAtlas("/sprites/sheet.png");
            log.info("Sprite sheet loaded");
        } catch (GameException ex) {
            handleError(ex);
        }
        try {
            bossSprites = new BossSprites("/sprites/boss.png");
            log.info("Boss sprite loaded");
        } catch (GameException ex) {
            handleError(ex);
        }

        Canvas canvas = new Canvas(GameWorld.WIDTH, GameWorld.HEIGHT);
        GraphicsContext g = canvas.getGraphicsContext2D();

        Scene scene = new Scene(new StackPane(canvas));
        scene.setOnKeyPressed(e -> {
            try {
                handleKey(e.getCode());
            } catch (GameException ex) {
                handleError(ex);
            }
        });
        scene.setOnKeyReleased(e -> keys.remove(e.getCode()));

        new AnimationTimer() {
            private long last = 0;
            @Override
            public void handle(long now) {
                if (last == 0) { last = now; return; }
                double dt = Math.min((now - last) / 1_000_000_000.0, 0.05);
                last = now;

                if (screen == Screen.GAME) {
                    int dir = 0;
                    if (keys.contains(KeyCode.LEFT) || keys.contains(KeyCode.A)) dir -= 1;
                    if (keys.contains(KeyCode.RIGHT) || keys.contains(KeyCode.D)) dir += 1;
                    paddleDir = dir;
                    try {
                        world.update(dt, dir);
                        if (keys.contains(KeyCode.SPACE) && world.isLaunched()) world.fire();
                    } catch (GameException ex) {
                        handleError(ex);
                    }
                    processEvents();
                    updateEffects(dt);
                }
                render(g, now);
            }
        }.start();

        stage.setTitle("Roguelike Arkanoid");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
        log.info("Application started");
    }

    private void startGame(int startStage) {
        world = new GameWorld(startStage);
        particles.clear();
        effects.clear();
        flashes.clear();
        lastLives = world.getLives();
        lastCombo = 0;
        lastCleared = 0;
        comboPulse = 0;
        redFlash = 0;
        ballSquash = 0;
        screen = Screen.GAME;
    }

    private void handleKey(KeyCode k) {
        keys.add(k);
        if (k == KeyCode.F2) { showDebug = !showDebug; return; }

        if (screen == Screen.MENU) {
            if (k == KeyCode.ENTER || k == KeyCode.SPACE) {
                selectedStage = Math.min(selectedStage, progress.getUnlocked());
                screen = Screen.SELECT;
            }
            return;
        }
        if (screen == Screen.SELECT) {
            if ((k == KeyCode.LEFT || k == KeyCode.A) && selectedStage > 1) selectedStage--;
            if ((k == KeyCode.RIGHT || k == KeyCode.D) && selectedStage < progress.getUnlocked()) selectedStage++;
            if (k == KeyCode.ENTER || k == KeyCode.SPACE) startGame(selectedStage);
            if (k == KeyCode.ESCAPE) screen = Screen.MENU;
            return;
        }

        GameWorld.State s = world.getState();
        if (s == GameWorld.State.PLAYING && k == KeyCode.SPACE) {
            if (!world.isLaunched()) world.launch(); else world.fire();
        }
        if (s == GameWorld.State.UPGRADE_SELECT) {
            if (k == KeyCode.DIGIT1) world.chooseUpgrade(0);
            if (k == KeyCode.DIGIT2) world.chooseUpgrade(1);
            if (k == KeyCode.DIGIT3) world.chooseUpgrade(2);
        }
        if (s == GameWorld.State.GAME_OVER || s == GameWorld.State.WON) {
            if (k == KeyCode.R) {
                log.info("Player restarted the game");
                startGame(selectedStage);
            } else if (k == KeyCode.M) {
                log.info("Player returned to menu");
                screen = Screen.MENU;
            }
        }
    }

    /** จุดเดียวที่จัดการ GameException ทั้งหมดของเกม */
    private void handleError(GameException ex) {
        log.error("Game error: {}", ex.getMessage(), ex);
        errorMessage = ex.getMessage();
        errorUntilNanos = System.nanoTime() + 3_000_000_000L;
    }

    // ---------- effects ----------
    private Color brickColor(Brick b) {
        return b.getPoints() > 10 ? Color.ORANGERED : Color.DEEPSKYBLUE;
    }

    private void processEvents() {
        for (BrickEvent ev : world.drainEvents()) {
            Brick b = ev.brick();
            if (ev.destroyed()) {
                flashes.remove(b);
                spawnBurst(b);
                if (atlas != null) {
                    effects.add(new SpriteEffect(new Animation(SpriteAtlas.EXPLOSION_BIG, 0.06, false),
                            b.getX() + b.getWidth() / 2, b.getY() + b.getHeight() / 2, 56));
                }
            } else {
                flashes.put(b, 0.15);
            }
        }
        for (double[] p : world.drainBounces()) {
            ballSquash = SQUASH_TIME;
            if (atlas != null) {
                effects.add(new SpriteEffect(new Animation(SpriteAtlas.EXPLOSION_SMALL, 0.05, false),
                        p[0], p[1], 22));
            }
        }
        if (world.getCombo() > lastCombo) comboPulse = 0.25;
        lastCombo = world.getCombo();
        if (world.getLives() < lastLives) redFlash = 0.4;
        lastLives = world.getLives();

        if (world.getHighestCleared() > lastCleared) {
            lastCleared = world.getHighestCleared();
            progress.unlock(lastCleared + 1);
        }
    }

    private void spawnBurst(Brick b) {
        double cx = b.getX() + b.getWidth() / 2;
        double cy = b.getY() + b.getHeight() / 2;
        int n = (b instanceof Boss) ? 40 : 12;
        for (int i = 0; i < n; i++) {
            double angle = rng.nextDouble() * Math.PI * 2;
            double speed = 60 + rng.nextDouble() * 200;
            particles.add(new Particle(cx, cy,
                    Math.cos(angle) * speed, Math.sin(angle) * speed - 80,
                    0.5 + rng.nextDouble() * 0.4, 3 + rng.nextDouble() * 4, brickColor(b)));
        }
    }

    private void updateEffects(double dt) {
        particles.forEach(p -> p.update(dt));
        particles.removeIf(Particle::isDead);
        effects.forEach(e -> e.update(dt));
        effects.removeIf(SpriteEffect::isDone);
        flashes.replaceAll((b, t) -> t - dt);
        flashes.values().removeIf(t -> t <= 0);
        if (redFlash > 0) redFlash -= dt;
        if (ballSquash > 0) ballSquash -= dt;
        if (comboPulse > 0) comboPulse -= dt;

        if (paddleDir != 0) paddleMove.update(dt); else paddleMove.reset();
        paddleLaser.update(dt);
    }

    // ---------- rendering ----------
    private void centerAt(GraphicsContext g, String text, double cx, double y) {
        g.setTextAlign(TextAlignment.CENTER);
        g.fillText(text, cx, y);
        g.setTextAlign(TextAlignment.LEFT);
    }

    private void center(GraphicsContext g, String text, double y) {
        centerAt(g, text, GameWorld.WIDTH / 2, y);
    }

    private void render(GraphicsContext g, long now) {
        g.setFill(Color.web("#111827"));
        g.fillRect(0, 0, GameWorld.WIDTH, GameWorld.HEIGHT);

        if (screen == Screen.MENU) {
            renderMenu(g);
        } else if (screen == Screen.SELECT) {
            renderSelect(g);
        } else {
            renderGame(g, now);
        }

        if (errorMessage != null && now < errorUntilNanos) {
            g.setFont(Font.font(16));
            g.setFill(Color.TOMATO);
            g.fillText("Error: " + errorMessage, 20, GameWorld.HEIGHT - 10);
        }
        if (showDebug) renderDebug(g);
    }

    private void renderSelect(GraphicsContext g) {
        g.setFill(Color.GOLD);
        g.setFont(Font.font("System", FontWeight.BOLD, 40));
        center(g, "SELECT STAGE", 120);

        int max = StageFactory.MAX_STAGE;
        for (int i = 1; i <= max; i++) {
            double bx = 70 + (i - 1) * 170, by = 200;
            boolean open = i <= progress.getUnlocked();
            g.setFill(open ? Color.web("#1f2937") : Color.web("#111111"));
            g.fillRoundRect(bx, by, 150, 170, 16, 16);
            g.setStroke(i == selectedStage ? Color.GOLD : Color.GRAY);
            g.setLineWidth(i == selectedStage ? 4 : 1);
            g.strokeRoundRect(bx, by, 150, 170, 16, 16);
            g.setLineWidth(1);

            g.setFill(open ? Color.WHITE : Color.DIMGRAY);
            g.setFont(Font.font("System", FontWeight.BOLD, 48));
            centerAt(g, String.valueOf(i), bx + 75, by + 80);
            g.setFont(Font.font(16));
            centerAt(g, StageFactory.isBossStage(i) ? "BOSS" : "Stage " + i, bx + 75, by + 115);
            if (!open) centerAt(g, "LOCKED", bx + 75, by + 145);
        }

        g.setFill(Color.LIGHTGRAY);
        g.setFont(Font.font(18));
        center(g, "LEFT / RIGHT  choose     ENTER  start     ESC  back", 430);
    }

    private void renderMenu(GraphicsContext g) {
        double cx = GameWorld.WIDTH / 2;
        if (atlas != null) {
            atlas.draw(g, SpriteAtlas.COVER, 0, 0, 400, 600);   // 320x480 x 1.25
            cx = 600;
        }
        g.setFill(Color.GOLD);
        g.setFont(Font.font("System", FontWeight.BOLD, 46));
        centerAt(g, "ROGUELIKE", cx, 150);
        centerAt(g, "ARKANOID", cx, 205);

        g.setFill(Color.WHITE);
        g.setFont(Font.font(24));
        centerAt(g, "Press ENTER to start", cx, 290);

        g.setFill(Color.LIGHTGRAY);
        g.setFont(Font.font(15));
        centerAt(g, "LEFT / RIGHT or A / D  -  move paddle", cx, 360);
        centerAt(g, "SPACE  -  launch the ball / fire laser", cx, 385);
        centerAt(g, "Catch the L capsule for 10s of dual lasers", cx, 410);
        centerAt(g, "Chain brick hits for COMBO score bonus", cx, 435);
        centerAt(g, "1 / 2 / 3  -  choose an upgrade", cx, 460);
        centerAt(g, "Stage 4: defeat the BOSS - dodge its shots!", cx, 485);

        g.setFill(Color.GRAY);
        g.setFont(Font.font(11));
        centerAt(g, "Sprites: Wii Furu Furu Park (Arkanoid), Arkanoid (Arcade) DOH", cx, 560);
        centerAt(g, "via The Spriters Resource - for educational use", cx, 576);
    }

    private Frame brickFrame(Brick b) {
        if (b.getHp() > 1) return SpriteAtlas.BRICK_SILVER;   // อิฐแข็งเป็นสีเงิน โดนตีแล้วเปลี่ยนเป็นสี
        int row = (int) Math.max(0, (b.getY() - 60) / 30);
        return SpriteAtlas.BRICK_COLORS[row % SpriteAtlas.BRICK_COLORS.length];
    }

    private void renderGame(GraphicsContext g, long now) {
        if (atlas != null) {
            atlas.draw(g, SpriteAtlas.BACKGROUND, 0, 0, GameWorld.WIDTH, GameWorld.HEIGHT);
            g.setFill(Color.rgb(0, 0, 0, 0.55));
            g.fillRect(0, 0, GameWorld.WIDTH, GameWorld.HEIGHT);
        }

        for (Brick b : world.getBricks()) {
            if (b.isDestroyed()) continue;
            if (b instanceof Boss boss) { drawBoss(g, boss); continue; }
            if (atlas != null) {
                atlas.draw(g, brickFrame(b), b.getX(), b.getY(), b.getWidth(), b.getHeight());
                if (flashes.containsKey(b)) {
                    g.setFill(Color.rgb(255, 255, 255, 0.7));
                    g.fillRect(b.getX(), b.getY(), b.getWidth(), b.getHeight());
                }
            } else {
                g.setFill(flashes.containsKey(b) ? Color.WHITE : brickColor(b));
                g.fillRect(b.getX(), b.getY(), b.getWidth(), b.getHeight());
            }
        }

        particles.forEach(p -> p.draw(g));
        if (atlas != null) effects.forEach(e -> e.draw(atlas, g));

        // power-ups
        for (PowerUp p : world.getPowerUps()) {
            if (atlas != null) {
                Frame f = SpriteAtlas.CAPSULE_L[(int) (now / 80_000_000L) % SpriteAtlas.CAPSULE_L.length];
                atlas.draw(g, f, p.getX() - 6, p.getY() - 3, 40, 20);
            } else {
                g.setFill(Color.LIMEGREEN);
                g.fillRoundRect(p.getX(), p.getY(), p.getWidth(), p.getHeight(), 10, 10);
            }
        }

        g.setFill(Color.CYAN);
        for (Laser l : world.getLasers()) {
            g.fillRect(l.getX(), l.getY(), l.getWidth(), l.getHeight());
        }

        // กระสุนบอส
        g.setFill(Color.RED);
        for (Bullet s : world.getBossShots()) {
            g.fillOval(s.getX(), s.getY(), s.getWidth(), s.getHeight());
        }

        // paddle
        Paddle p = world.getPaddle();
        if (atlas != null) {
            Frame f = world.isLaserActive() ? paddleLaser.current()
                    : (paddleDir != 0 ? paddleMove.current() : SpriteAtlas.PADDLE[0]);
            atlas.draw(g, f, p.getX(), p.getY() - 4, p.getWidth(), 22);
        } else {
            g.setFill(world.isLaserActive() ? Color.CYAN : Color.WHITE);
            g.fillRect(p.getX(), p.getY(), p.getWidth(), p.getHeight());
        }

        // balls (บอลหลัก + บอลเสริม)
        drawBall(g, world.getBall());
        for (Ball eb : world.getExtraBalls()) drawBall(g, eb);

        g.setFill(Color.WHITE);
        g.setFont(Font.font(18));
        g.fillText("Score: " + world.getScore(), 20, 30);
        center(g, "Stage: " + world.getStage() + "/" + StageFactory.MAX_STAGE
                + (StageFactory.isBossStage(world.getStage()) ? "  BOSS" : ""), 30);
        g.fillText("Lives: " + world.getLives(), GameWorld.WIDTH - 100, 30);

        if (world.isLaserActive()) {
            g.setFill(Color.CYAN);
            g.fillText(String.format("LASER %.1fs", world.getLaserSecondsLeft()), 20, 56);
        }

        if (world.getPierceLeft() > 0) {
            g.setFill(Color.MAGENTA);
            g.fillText("PIERCE x" + world.getPierceLeft(), 20, 82);
        }

        // combo
        if (world.getCombo() >= 2) {
            double scale = 1 + Math.max(0, comboPulse) * 2;
            double mult = GameWorld.comboMultiplier(world.getCombo());
            g.setFill(mult >= 3 ? Color.GOLD : mult >= 2 ? Color.ORANGE
                                               : mult >= 1.5 ? Color.YELLOW : Color.WHITE);
            g.setFont(Font.font("System", FontWeight.BOLD, 30 * scale));
            centerAt(g, "COMBO x" + world.getCombo(), GameWorld.WIDTH / 2, 90);
            if (mult > 1) {
                g.setFont(Font.font(16));
                centerAt(g, String.format("score x%.1f", mult), GameWorld.WIDTH / 2, 112);
            }
        }

        if (redFlash > 0) {
            g.setFill(Color.rgb(255, 0, 0, Math.min(0.35, redFlash)));
            g.fillRect(0, 0, GameWorld.WIDTH, GameWorld.HEIGHT);
        }

        GameWorld.State st = world.getState();
        g.setFont(Font.font(32));
        if (st == GameWorld.State.PLAYING && !world.isLaunched()) {
            g.setFill(Color.WHITE);
            center(g, "Press SPACE to launch", 400);
        } else if (st == GameWorld.State.GAME_OVER) {
            overlay(g);
            g.setFill(Color.WHITE);
            center(g, "GAME OVER", 300);
            g.setFont(Font.font(20));
            center(g, "Score " + world.getScore() + "  |  Best combo x" + world.getBestCombo(), 340);
            center(g, "R = restart   M = menu", 370);
        } else if (st == GameWorld.State.WON) {
            overlay(g);
            g.setFill(Color.GOLD);
            center(g, "YOU WIN!  Score " + world.getScore(), 300);
            g.setFont(Font.font(20));
            center(g, "Best combo x" + world.getBestCombo(), 340);
            center(g, "R = play again   M = menu", 370);
        } else if (st == GameWorld.State.UPGRADE_SELECT) {
            overlay(g);
            g.setFill(Color.GOLD);
            center(g, "STAGE CLEAR! Choose an upgrade", 160);
            List<Upgrade> ch = world.getChoices();
            for (int i = 0; i < ch.size(); i++) {
                double bx = 60 + i * 235, by = 220;
                g.setFill(Color.web("#1f2937"));
                g.fillRoundRect(bx, by, 215, 220, 16, 16);
                g.setStroke(Color.GOLD);
                g.strokeRoundRect(bx, by, 215, 220, 16, 16);
                g.setFill(Color.WHITE);
                g.setFont(Font.font(22));
                g.fillText("[" + (i + 1) + "] " + ch.get(i).getName(), bx + 14, by + 50);
                g.setFont(Font.font(16));
                g.fillText(ch.get(i).getDescription(), bx + 14, by + 100);
            }
        }
    }

    private void drawBoss(GraphicsContext g, Boss boss) {
        double ratio = (double) boss.getHp() / boss.getMaxHp();
        double cx = boss.getX() + boss.getWidth() / 2;

        if (bossSprites != null) {
            int state = flashes.containsKey(boss) ? 1 : (ratio < 0.3 ? 2 : 0);
            int col = (int) (System.nanoTime() / 200_000_000L) % 4;   // ปากขยับ
            double dh = 100, dw = dh * 64 / 90;
            bossSprites.draw(g, state, col, cx - dw / 2,
                    boss.getY() + boss.getHeight() / 2 - dh / 2, dw, dh);
        } else {
            g.setFill(flashes.containsKey(boss) ? Color.WHITE : Color.web("#7f1d1d"));
            g.fillRoundRect(boss.getX(), boss.getY(), boss.getWidth(), boss.getHeight(), 14, 14);
            g.setStroke(Color.ORANGERED);
            g.setLineWidth(3);
            g.strokeRoundRect(boss.getX(), boss.getY(), boss.getWidth(), boss.getHeight(), 14, 14);
            g.setLineWidth(1);
            g.setFill(Color.WHITE);
            g.setFont(Font.font("System", FontWeight.BOLD, 16));
            centerAt(g, "BOSS", cx, boss.getY() + 26);
        }

        // แถบ HP เหนือหัวบอส
        double barY = boss.getY() - 12;
        g.setFill(Color.web("#374151"));
        g.fillRect(boss.getX(), barY, boss.getWidth(), 6);
        g.setFill(ratio > 0.4 ? Color.LIMEGREEN : Color.ORANGERED);
        g.fillRect(boss.getX(), barY, boss.getWidth() * ratio, 6);
    }

    private void drawBall(GraphicsContext g, Ball ball) {
        double s = Math.max(0, ballSquash) / SQUASH_TIME;
        double bw = ball.getWidth() * (1 + 0.5 * s), bh = ball.getHeight() * (1 - 0.35 * s);
        double bcx = ball.getX() + ball.getWidth() / 2, bcy = ball.getY() + ball.getHeight() / 2;
        g.setFill(world.getPierceLeft() > 0 ? Color.MAGENTA : Color.ORANGE);
        g.fillOval(bcx - bw / 2, bcy - bh / 2, bw, bh);
        g.setFill(Color.WHITE);
        g.fillOval(bcx - bw / 4 - 1, bcy - bh / 3, bw / 4, bh / 4);
    }

    /** F2: แสดง sprite sheet พร้อมกรอบพิกัดที่ตั้งไว้ ใช้ไล่ปรับค่าใน SpriteAtlas */
    private void renderDebug(GraphicsContext g) {
        g.setFill(Color.web("#808080"));
        g.fillRect(0, 0, GameWorld.WIDTH, GameWorld.HEIGHT);
        if (bossSprites != null) { bossSprites.drawDebug(g); return; }
        if (atlas == null) {
            g.setFill(Color.WHITE);
            g.setFont(Font.font(18));
            g.fillText("No sprite sheet loaded (check sprites/sheet.png)", 20, 40);
            return;
        }
        double k = 1 / SpriteAtlas.SCALE;
        g.setImageSmoothing(false);
        g.drawImage(atlas.getSheet(), 0, 0, 674 * SpriteAtlas.SCALE, 480 * SpriteAtlas.SCALE,
                0, 0, 674, 480);

        g.setLineWidth(1);
        g.setStroke(Color.MAGENTA);
        g.setFill(Color.BLACK);
        g.setFont(Font.font(9));
        for (Map.Entry<String, Frame[]> e : SpriteAtlas.all().entrySet()) {
            Frame[] fs = e.getValue();
            for (Frame f : fs) {
                if (f.y() * k > 480) continue;   // COVER / BG อยู่ใต้ส่วนนี้ ไม่แสดง
                g.strokeRect(f.x() * k + 0.5, f.y() * k + 0.5, f.w() * k, f.h() * k);
            }
            if (fs[0].y() * k <= 480) g.fillText(e.getKey(), fs[0].x() * k, fs[0].y() * k + fs[0].h() * k + 9);
        }
        g.setFill(Color.WHITE);
        g.setFont(Font.font(14));
        g.fillText("DEBUG (F2 to close)  -  magenta boxes = frames in SpriteAtlas.java", 20, 510);
        g.fillText("Each box must tightly wrap one sprite. Edit numbers, rerun, compare.", 20, 532);
    }

    private void overlay(GraphicsContext g) {
        g.setFill(Color.rgb(0, 0, 0, 0.75));
        g.fillRect(0, 0, GameWorld.WIDTH, GameWorld.HEIGHT);
    }

    public static void main(String[] args) { launch(args); }
}