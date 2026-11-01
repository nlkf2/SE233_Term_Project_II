import org.junit.jupiter.api.Test;
import se233.arkanoid.exception.GameException;
import se233.arkanoid.model.Brick;
import se233.arkanoid.model.GameWorld;

import static org.junit.jupiter.api.Assertions.*;

class GameWorldTest {
    private static final double DT = 0.0001;

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
    void clearingStageOffersUpgradeChoices() {
        GameWorld w = new GameWorld();
        w.launch();
        destroyAllBricks(w);
        w.update(DT, 0);
        assertEquals(GameWorld.State.UPGRADE_SELECT, w.getState());
        assertEquals(3, w.getChoices().size());
    }

    @Test
    void choosingUpgradeStartsNextStage() {
        GameWorld w = new GameWorld();
        w.launch();
        destroyAllBricks(w);
        w.update(DT, 0);
        w.chooseUpgrade(0);
        assertEquals(2, w.getStage());
        assertEquals(GameWorld.State.PLAYING, w.getState());
    }

    @Test
    void choosingUpgradeOutsideSelectionThrows() {
        GameWorld w = new GameWorld();
        assertThrows(GameException.class, () -> w.chooseUpgrade(0));
    }

    @Test
    void invalidUpgradeIndexThrows() {
        GameWorld w = new GameWorld();
        w.launch();
        destroyAllBricks(w);
        w.update(DT, 0);
        assertThrows(GameException.class, () -> w.chooseUpgrade(7));
    }
}