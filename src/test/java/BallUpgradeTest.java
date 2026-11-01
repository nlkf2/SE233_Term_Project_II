import org.junit.jupiter.api.Test;
import se233.arkanoid.model.Brick;
import se233.arkanoid.model.GameWorld;
import se233.arkanoid.upgrade.Upgrades;

import static org.junit.jupiter.api.Assertions.*;

class BallUpgradeTest {
    private static final double DT = 0.0001;

    @Test
    void pierceDestroysStrongBrickInOneHit() {
        GameWorld w = new GameWorld();
        w.setDropChance(0);
        new Upgrades.PierceBall().apply(w);
        Brick strong = w.getBricks().get(0);   // แถวบนสุด hp 2
        assertEquals(2, strong.getHp());
        w.launch();
        w.getBall().setPosition(strong.getX() + 1, strong.getY() + 1);
        w.update(DT, 0);
        assertTrue(strong.isDestroyed());
        assertEquals(7, w.getPierceLeft());
    }

    @Test
    void extraBallSpawnsOnLaunch() {
        GameWorld w = new GameWorld();
        new Upgrades.ExtraBall().apply(w);
        w.launch();
        assertEquals(1, w.getExtraBalls().size());
    }

    @Test
    void losingExtraBallCostsNoLife() {
        GameWorld w = new GameWorld();
        new Upgrades.ExtraBall().apply(w);
        w.launch();
        w.getExtraBalls().get(0).setPosition(100, 700);
        w.update(DT, 0);
        assertEquals(3, w.getLives());
        assertTrue(w.getExtraBalls().isEmpty());
    }

    @Test
    void extraBallTakesOverWhenMainBallIsLost() {
        GameWorld w = new GameWorld();
        new Upgrades.ExtraBall().apply(w);
        w.launch();
        w.getBall().setPosition(100, 700);
        w.update(DT, 0);
        assertEquals(3, w.getLives());
        assertTrue(w.getExtraBalls().isEmpty());
        assertTrue(w.isLaunched());
    }
}