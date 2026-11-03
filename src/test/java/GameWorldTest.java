package se233.arkanoid;

import org.junit.jupiter.api.Test;
import se233.arkanoid.exception.GameException;
import se233.arkanoid.model.Brick;
import se233.arkanoid.model.GameWorld;
import se233.arkanoid.model.Laser;
import se233.arkanoid.stage.StageFactory;
import se233.arkanoid.upgrade.Upgrades;

import static org.junit.jupiter.api.Assertions.*;

class GameWorldTest {
    private static final double DT = 0.0001;

    // ---------- helpers ----------

    /** ทำให้ลูกบอลตกจอ 1 ครั้ง */
    private void loseOneLife(GameWorld w) {
        w.launch();
        w.getBall().setPosition(100, 700);
        w.update(DT, 0);
    }

    private void destroyAllBricks(GameWorld w) {
        for (Brick b : w.getBricks()) {
            while (!b.isDestroyed()) b.hit();
        }
    }

    /** ทำลายอิฐทั้งด่านแล้ว update 1 ครั้งให้เกมรู้ว่าเคลียร์ด่าน */
    private void clearStage(GameWorld w) {
        destroyAllBricks(w);
        w.update(DT, 0);
    }

    // ---------- เริ่มเกม / ชีวิต ----------

    @Test
    void startsWithThreeLivesAndZeroScore() {
        GameWorld w = new GameWorld();
        assertEquals(3, w.getLives());
        assertEquals(0, w.getScore());
        assertEquals(1, w.getStage());
        assertEquals(GameWorld.State.PLAYING, w.getState());
    }

    @Test
    void losingBallCostsOneLife() {
        GameWorld w = new GameWorld();
        loseOneLife(w);
        assertEquals(2, w.getLives());
        assertFalse(w.isLaunched());
    }

    @Test
    void gameOverWhenAllLivesLost() {
        GameWorld w = new GameWorld();
        loseOneLife(w);
        loseOneLife(w);
        loseOneLife(w);
        assertEquals(0, w.getLives());
        assertEquals(GameWorld.State.GAME_OVER, w.getState());
    }

    @Test
    void noUpdatesAfterGameOver() {
        GameWorld w = new GameWorld();
        loseOneLife(w);
        loseOneLife(w);
        loseOneLife(w);
        assertEquals(GameWorld.State.GAME_OVER, w.getState());
        int score = w.getScore();
        w.update(1.0, 1);
        assertEquals(score, w.getScore());
        assertEquals(0, w.getLives());
    }

    // ---------- ลูกบอล / paddle ----------

    @Test
    void launchTwiceDoesNotRelaunch() {
        GameWorld w = new GameWorld();
        w.launch();
        double vx = w.getBall().getVx(), vy = w.getBall().getVy();
        w.launch();
        assertEquals(vx, w.getBall().getVx());
        assertEquals(vy, w.getBall().getVy());
    }

    @Test
    void paddleStaysInsideArea() {
        GameWorld w = new GameWorld();
        for (int i = 0; i < 200; i++) w.update(0.05, -1);
        assertTrue(w.getPaddle().getX() >= 0);
        for (int i = 0; i < 400; i++) w.update(0.05, 1);
        assertTrue(w.getPaddle().getX() + w.getPaddle().getWidth() <= GameWorld.WIDTH);
    }

    @Test
    void slowBallReducesLaunchSpeed() {
        GameWorld normal = new GameWorld();
        normal.launch();
        double base = normal.getBall().getSpeed();

        GameWorld slow = new GameWorld();
        new Upgrades.SlowBall().apply(slow);
        slow.launch();
        assertEquals(base * 0.85, slow.getBall().getSpeed(), 0.001);
    }

    // ---------- คะแนน / อิฐ ----------

    @Test
    void destroyingBrickGivesItsPoints() {
        GameWorld w = new GameWorld();
        Brick last = w.getBricks().get(w.getBricks().size() - 1); // อิฐธรรมดา hp 1 = 10 คะแนน
        w.launch();
        w.getBall().setPosition(last.getX() + 1, last.getY() + 1);
        w.update(DT, 0);
        assertTrue(last.isDestroyed());
        assertEquals(10, w.getScore());
    }

    @Test
    void strongBrickFirstHitGivesNoScore() {
        GameWorld w = new GameWorld();
        w.setDropChance(0);
        Brick strong = w.getBricks().get(0); // hp 2
        w.launch();
        w.getBall().setPosition(strong.getX() + 1, strong.getY() + 1);
        w.update(DT, 0);
        assertFalse(strong.isDestroyed());
        assertEquals(0, w.getScore());
    }

    @Test
    void laserDoesNotIncreaseCombo() {
        GameWorld w = new GameWorld();
        w.setDropChance(0);
        Brick last = w.getBricks().get(w.getBricks().size() - 1);
        w.getLasers().add(new Laser(last.getX() + 10, last.getY() + 5));
        w.update(DT, 0);
        assertEquals(0, w.getCombo());
    }

    // ---------- เคลียร์ด่าน / อัปเกรด / ชนะ ----------

    @Test
    void clearingStageOffersUpgradeChoices() {
        GameWorld w = new GameWorld();
        w.launch();
        clearStage(w);
        assertEquals(GameWorld.State.UPGRADE_SELECT, w.getState());
        assertEquals(3, w.getChoices().size());
    }

    @Test
    void clearingStageRecordsHighestCleared() {
        GameWorld w = new GameWorld();
        w.launch();
        clearStage(w);
        assertEquals(1, w.getHighestCleared());
    }

    @Test
    void choosingUpgradeStartsNextStage() {
        GameWorld w = new GameWorld();
        w.launch();
        clearStage(w);
        w.chooseUpgrade(0);
        assertEquals(2, w.getStage());
        assertEquals(GameWorld.State.PLAYING, w.getState());
    }

    @Test
    void clearingFinalStageWinsTheGame() {
        GameWorld w = new GameWorld(StageFactory.MAX_STAGE);
        w.launch();
        clearStage(w);
        assertEquals(GameWorld.State.WON, w.getState());
        assertEquals(StageFactory.MAX_STAGE, w.getHighestCleared());
    }

    // ---------- Exception ----------

    @Test
    void choosingUpgradeOutsideSelectionThrows() {
        GameWorld w = new GameWorld();
        assertThrows(GameException.class, () -> w.chooseUpgrade(0));
    }

    @Test
    void invalidUpgradeIndexThrows() {
        GameWorld w = new GameWorld();
        w.launch();
        clearStage(w);
        assertThrows(GameException.class, () -> w.chooseUpgrade(7));
    }
}
