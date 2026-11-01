import org.junit.jupiter.api.Test;
import se233.arkanoid.model.Boss;
import se233.arkanoid.model.Brick;
import se233.arkanoid.model.Bullet;
import se233.arkanoid.model.GameWorld;
import se233.arkanoid.stage.StageFactory;

import static org.junit.jupiter.api.Assertions.*;

class BossTest {
    private GameWorld worldAtBossStage() {
        GameWorld w = new GameWorld();
        for (int i = 0; i < 3; i++) {
            for (Brick b : w.getBricks()) while (!b.isDestroyed()) b.hit();
            w.update(0.0001, 0);
            w.chooseUpgrade(0);
        }
        return w;
    }

    @Test
    void bossStageContainsOneBoss() {
        long n = StageFactory.create(4).stream().filter(b -> b instanceof Boss).count();
        assertEquals(1, n);
        assertTrue(StageFactory.isBossStage(4));
    }

    @Test
    void bossNeedsManyHits() {
        Boss boss = new Boss(0, 0, 160, 40, 25, 500);
        for (int i = 0; i < 24; i++) assertFalse(boss.hit());
        assertTrue(boss.hit());
    }

    @Test
    void bossBouncesAtWall() {
        Boss boss = new Boss(630, 70, 160, 40, 25, 500);
        boss.update(1.0, 800);
        assertEquals(640, boss.getX(), 0.001);
        boss.update(0.5, 800);
        assertTrue(boss.getX() < 640);
    }

    @Test
    void bossShotHitsPaddleCostsLife() {
        GameWorld w = worldAtBossStage();
        assertEquals(4, w.getStage());
        int before = w.getLives();
        w.launch();
        w.getBossShots().add(new Bullet(w.getPaddle().getX() + 10, w.getPaddle().getY()));
        w.update(0.0001, 0);
        assertEquals(before - 1, w.getLives());
    }
}