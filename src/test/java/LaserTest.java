
import org.junit.jupiter.api.Test;
import se233.arkanoid.model.Brick;
import se233.arkanoid.model.GameWorld;
import se233.arkanoid.model.Laser;
import se233.arkanoid.model.PowerUp;

import static org.junit.jupiter.api.Assertions.*;

class LaserTest {
    private static final double DT = 0.0001;

    @Test
    void cannotFireWithoutLaserMode() {
        GameWorld w = new GameWorld();
        assertFalse(w.fire());
        assertTrue(w.getLasers().isEmpty());
    }

    @Test
    void firingCreatesTwoLasers() {
        GameWorld w = new GameWorld();
        w.activateLaser(10);
        assertTrue(w.fire());
        assertEquals(2, w.getLasers().size());
    }

    @Test
    void fireHasCooldown() {
        GameWorld w = new GameWorld();
        w.activateLaser(10);
        assertTrue(w.fire());
        assertFalse(w.fire());      // ยังอยู่ใน cooldown
        w.update(0.5, 0);
        assertTrue(w.fire());       // cooldown หมดแล้ว
    }

    @Test
    void laserDestroysBrickAndGivesScore() {
        GameWorld w = new GameWorld();
        w.setDropChance(0);
        Brick last = w.getBricks().get(w.getBricks().size() - 1); // hp 1 = 10 คะแนน
        w.getLasers().add(new Laser(last.getX() + 10, last.getY() + 5));
        w.update(DT, 0);
        assertTrue(last.isDestroyed());
        assertEquals(10, w.getScore());
        assertTrue(w.getLasers().isEmpty());
    }

    @Test
    void collectingPowerUpActivatesLaserMode() {
        GameWorld w = new GameWorld();
        w.getPowerUps().add(new PowerUp(w.getPaddle().getX() + 10, w.getPaddle().getY()));
        w.update(DT, 0);
        assertTrue(w.isLaserActive());
        assertTrue(w.getPowerUps().isEmpty());
    }

    @Test
    void laserModeEndsOnTimerThread() throws InterruptedException {
        GameWorld w = new GameWorld();
        w.activateLaser(0.05);
        assertTrue(w.isLaserActive());
        Thread.sleep(300);          // รอให้ thread "laser-timer" ปิดโหมด
        assertFalse(w.isLaserActive());
        assertFalse(w.fire());
    }
}