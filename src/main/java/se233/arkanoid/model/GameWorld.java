package se233.arkanoid.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se233.arkanoid.exception.GameException;
import se233.arkanoid.stage.StageFactory;
import se233.arkanoid.upgrade.Upgrade;
import se233.arkanoid.upgrade.Upgrades;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class GameWorld {
    private static final Logger log = LogManager.getLogger(GameWorld.class);

    public static final double WIDTH = 800, HEIGHT = 600;
    public static final double LASER_SECONDS = 10;
    private static final double LASER_COOLDOWN = 0.4;
    private static final double[] EXTRA_ANGLES = {-120, -90, -30, -150};

    public enum State { PLAYING, UPGRADE_SELECT, GAME_OVER, WON }

    /** thread แยกสำหรับนับเวลาโหมดเลเซอร์ (daemon จะได้ไม่ค้างตอนปิดเกม) */
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "laser-timer");
                t.setDaemon(true);
                return t;
            });

    private final Paddle paddle = new Paddle(350, 560, 100, 15, 520);
    private Ball ball = new Ball(0, 0, 12);
    private final List<Ball> extraBalls = new ArrayList<>();
    private List<Brick> bricks = new ArrayList<>();
    private List<Upgrade> choices = new ArrayList<>();
    private final List<BrickEvent> events = new ArrayList<>();
    private final List<PowerUp> powerUps = new ArrayList<>();
    private final List<Laser> lasers = new ArrayList<>();
    private final List<double[]> bounces = new ArrayList<>();
    private final Random rng = new Random();

    private double ballSpeed = 380;
    private double scoreMultiplier = 1.0;
    private double dropChance = 0.05;
    private int score = 0;
    private int lives = 3;
    private int stage = 1;
    private int combo = 0;
    private int bestCombo = 0;
    private int extraBallCount = 0;   // จำนวนบอลเสริมที่ได้ตอนปล่อยบอล
    private int pierceBudget = 0;     // จำนวนทะลุต่อด่าน
    private int pierceLeft = 0;       // ที่เหลือในด่านนี้
    private boolean launched = false;
    private State state = State.PLAYING;

    // ใช้ร่วมกันระหว่าง game thread กับ laser-timer thread
    private volatile boolean laserActive = false;
    private volatile long laserEndNanos = 0;
    private ScheduledFuture<?> laserTask;
    private double laserCooldown = 0;

    public GameWorld() {
        bricks = StageFactory.create(stage);
        resetBall();
        log.info("New game started: stage={}, lives={}", stage, lives);
    }

    private void resetBall() {
        launched = false;
        extraBalls.clear();
        ball.stop();
        placeBallOnPaddle();
    }

    private void placeBallOnPaddle() {
        ball.setPosition(paddle.getX() + paddle.getWidth() / 2 - ball.getWidth() / 2,
                paddle.getY() - ball.getHeight());
    }

    public void launch() {
        if (state == State.PLAYING && !launched) {
            launched = true;
            ball.launch(ballSpeed);
            for (int i = 0; i < extraBallCount; i++) {
                Ball e = new Ball(ball.getX(), ball.getY(), ball.getWidth());
                e.launch(ballSpeed, EXTRA_ANGLES[i % EXTRA_ANGLES.length]);
                extraBalls.add(e);
            }
            log.info("Player launched the ball (speed={}, extra balls={})", ballSpeed, extraBalls.size());
        }
    }

    public void update(double dt, int paddleDirection) {
        if (state != State.PLAYING) return;

        paddle.move(paddleDirection, dt, WIDTH);
        if (laserCooldown > 0) laserCooldown -= dt;

        updatePowerUps(dt);
        updateLasers(dt);

        if (bricks.stream().allMatch(Brick::isDestroyed)) { completeStage(); return; }

        if (!launched) { placeBallOnPaddle(); return; }

        stepBall(ball, dt);
        for (Ball e : new ArrayList<>(extraBalls)) stepBall(e, dt);
        extraBalls.removeIf(e -> e.isOut(HEIGHT));

        if (bricks.stream().allMatch(Brick::isDestroyed)) { completeStage(); return; }

        if (ball.isOut(HEIGHT)) {
            if (!extraBalls.isEmpty()) {
                ball = extraBalls.remove(0);   // บอลสำรองขึ้นมาแทน ไม่เสียชีวิต
                log.info("Main ball lost, extra ball takes over ({} left)", extraBalls.size());
                return;
            }
            lives--;
            resetCombo();
            log.warn("Life lost, lives left={}", lives);
            if (lives <= 0) {
                state = State.GAME_OVER;
                log.warn("GAME OVER with score={}", score);
            } else {
                resetBall();
            }
        }
    }

    /** เคลื่อนบอล 1 ลูก: ชนกำแพง, paddle, อิฐ */
    private void stepBall(Ball b, double dt) {
        double pvx = b.getVx(), pvy = b.getVy();
        b.update(dt);
        b.bounceWalls(WIDTH);
        if (b.bounceOffPaddle(paddle)) {
            log.debug("Ball bounced off paddle");
            resetCombo();
        }

        for (Brick br : bricks) {
            if (!br.isDestroyed() && b.intersects(br)) {
                if (pierceLeft > 0) {
                    pierceLeft--;
                    while (br.getHp() > 1) br.hit();   // ทะลุ = อิฐแตกทันที ไม่เด้ง
                    log.info("Ball pierced a brick ({} pierces left)", pierceLeft);
                } else {
                    b.bounceOffBrick(br);
                }
                damageBrick(br, "ball");
                break;
            }
        }

        // ถ้าทิศความเร็วเปลี่ยน แปลว่าบอลเด้ง (UI ใช้ทำ animation บอลเด้ง)
        if (Math.signum(b.getVx()) != Math.signum(pvx)
                || Math.signum(b.getVy()) != Math.signum(pvy)) {
            bounces.add(new double[]{b.getX() + b.getWidth() / 2,
                    b.getY() + b.getHeight() / 2});
        }
    }

    /** ตัวคูณคะแนนจาก combo */
    public static double comboMultiplier(int combo) {
        if (combo >= 15) return 3.0;
        if (combo >= 10) return 2.0;
        if (combo >= 5) return 1.5;
        return 1.0;
    }

    private void resetCombo() {
        if (combo >= 5) log.info("Combo broken at x{}", combo);
        combo = 0;
    }

    /** ใช้ร่วมกันทั้งบอลและเลเซอร์ (เฉพาะบอลที่นับ combo) */
    private void damageBrick(Brick b, String source) {
        boolean byBall = source.equals("ball");
        if (byBall) {
            combo++;
            bestCombo = Math.max(bestCombo, combo);
            if (combo > 1) log.debug("Combo x{}", combo);
        }
        if (b.hit()) {
            int gained = (int) Math.round(b.getPoints() * scoreMultiplier
                    * (byBall ? comboMultiplier(combo) : 1.0));
            score += gained;
            events.add(new BrickEvent(b, true));
            log.info("Brick destroyed by {}: +{} points (combo x{}), score={}",
                    source, gained, combo, score);
            if (rng.nextDouble() < dropChance) {
                powerUps.add(new PowerUp(b.getX() + b.getWidth() / 2 - 14,
                        b.getY() + b.getHeight()));
                log.debug("Power-up dropped");
            }
        } else {
            events.add(new BrickEvent(b, false));
            log.debug("Brick hit by {}, hp left={}", source, b.getHp());
        }
    }

    private void updatePowerUps(double dt) {
        Iterator<PowerUp> it = powerUps.iterator();
        while (it.hasNext()) {
            PowerUp p = it.next();
            p.update(dt);
            if (p.intersects(paddle)) {
                it.remove();
                activateLaser(LASER_SECONDS);
            } else if (p.isOut(HEIGHT)) {
                it.remove();
            }
        }
    }

    private void updateLasers(double dt) {
        Iterator<Laser> it = lasers.iterator();
        while (it.hasNext()) {
            Laser l = it.next();
            l.update(dt);
            if (l.isOut()) { it.remove(); continue; }
            for (Brick b : bricks) {
                if (!b.isDestroyed() && l.intersects(b)) {
                    damageBrick(b, "laser");
                    it.remove();
                    break;
                }
            }
        }
    }

    // ---------- Laser mode ----------
    public synchronized void activateLaser(double seconds) {
        if (laserTask != null) laserTask.cancel(false);
        laserActive = true;
        laserEndNanos = System.nanoTime() + (long) (seconds * 1_000_000_000L);
        laserTask = SCHEDULER.schedule(this::deactivateLaser,
                (long) (seconds * 1000), TimeUnit.MILLISECONDS);
        log.info("Laser mode ON for {} seconds", seconds);
    }

    /** ถูกเรียกจาก thread "laser-timer" เมื่อหมดเวลา */
    public synchronized void deactivateLaser() {
        if (laserActive) {
            laserActive = false;
            log.info("Laser mode OFF");
        }
    }

    /** ยิงเลเซอร์คู่ คืนค่า true ถ้ายิงสำเร็จ */
    public boolean fire() {
        if (state != State.PLAYING || !laserActive || laserCooldown > 0) return false;
        lasers.add(new Laser(paddle.getX() + 4, paddle.getY() - 14));
        lasers.add(new Laser(paddle.getX() + paddle.getWidth() - 8, paddle.getY() - 14));
        laserCooldown = LASER_COOLDOWN;
        log.debug("Player fired dual lasers");
        return true;
    }

    public boolean isLaserActive() { return laserActive; }

    public double getLaserSecondsLeft() {
        if (!laserActive) return 0;
        return Math.max(0, (laserEndNanos - System.nanoTime()) / 1_000_000_000.0);
    }

    // ---------- Stage flow ----------
    private void completeStage() {
        log.info("Stage {} cleared, score={}", stage, score);
        powerUps.clear();
        lasers.clear();
        if (stage >= StageFactory.MAX_STAGE) {
            state = State.WON;
            log.info("Player WON the game with score={}", score);
        } else {
            choices = Upgrades.randomChoices(3);
            state = State.UPGRADE_SELECT;
            log.debug("Upgrade choices offered: {}", choices.stream().map(Upgrade::getName).toList());
        }
    }

    /** ผู้เล่นเลือกอัปเกรด index 0..2 แล้วไปด่านถัดไป */
    public void chooseUpgrade(int index) {
        if (state != State.UPGRADE_SELECT) {
            throw new GameException("Cannot choose upgrade while state is " + state);
        }
        if (index < 0 || index >= choices.size()) {
            throw new GameException("Invalid upgrade index: " + index);
        }
        Upgrade u = choices.get(index);
        u.apply(this);
        log.info("Upgrade chosen: {}", u.getName());
        stage++;
        bricks = StageFactory.create(stage);
        pierceLeft = pierceBudget;
        resetBall();
        state = State.PLAYING;
        log.info("Starting stage {}", stage);
    }

    /** UI เรียกทุกเฟรมเพื่อเอาเหตุการณ์อิฐโดนตี/แตก แล้วล้างลิสต์ */
    public List<BrickEvent> drainEvents() {
        List<BrickEvent> copy = new ArrayList<>(events);
        events.clear();
        return copy;
    }

    /** จุดที่บอลเด้งในเฟรมนี้ (x, y) ใช้ทำเอฟเฟกต์ */
    public List<double[]> drainBounces() {
        List<double[]> copy = new ArrayList<>(bounces);
        bounces.clear();
        return copy;
    }

    // ---- ใช้โดย Upgrade ----
    public void addLife() { lives++; }
    public void scaleBallSpeed(double f) { ballSpeed *= f; }
    public void scaleScoreMultiplier(double f) { scoreMultiplier *= f; }
    public void addExtraBall() { extraBallCount++; }
    public void addPierce(int n) { pierceBudget += n; pierceLeft += n; }

    public void setDropChance(double chance) { this.dropChance = chance; }

    // ---- getters ----
    public Paddle getPaddle() { return paddle; }
    public Ball getBall() { return ball; }
    public List<Ball> getExtraBalls() { return extraBalls; }
    public List<Brick> getBricks() { return bricks; }
    public List<Upgrade> getChoices() { return choices; }
    public List<PowerUp> getPowerUps() { return powerUps; }
    public List<Laser> getLasers() { return lasers; }
    public int getScore() { return score; }
    public int getLives() { return lives; }
    public int getStage() { return stage; }
    public int getCombo() { return combo; }
    public int getBestCombo() { return bestCombo; }
    public int getPierceLeft() { return pierceLeft; }
    public boolean isLaunched() { return launched; }
    public State getState() { return state; }
}