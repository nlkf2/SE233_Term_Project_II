import org.junit.jupiter.api.Test;
import se233.arkanoid.model.GameWorld;

import static org.junit.jupiter.api.Assertions.*;

class ComboTest {
    @Test
    void multiplierThresholds() {
        assertEquals(1.0, GameWorld.comboMultiplier(0));
        assertEquals(1.0, GameWorld.comboMultiplier(4));
        assertEquals(1.5, GameWorld.comboMultiplier(5));
        assertEquals(2.0, GameWorld.comboMultiplier(10));
        assertEquals(3.0, GameWorld.comboMultiplier(15));
    }

    @Test
    void brickHitByBallIncreasesCombo() {
        GameWorld w = new GameWorld();
        var last = w.getBricks().get(w.getBricks().size() - 1);
        w.launch();
        w.getBall().setPosition(last.getX() + 1, last.getY() + 1);
        w.update(0.0001, 0);
        assertEquals(1, w.getCombo());
    }

    @Test
    void losingLifeResetsCombo() {
        GameWorld w = new GameWorld();
        var last = w.getBricks().get(w.getBricks().size() - 1);
        w.launch();
        w.getBall().setPosition(last.getX() + 1, last.getY() + 1);
        w.update(0.0001, 0);
        assertEquals(1, w.getCombo());
        w.getBall().setPosition(100, 700);
        w.update(0.0001, 0);
        assertEquals(0, w.getCombo());
        assertEquals(1, w.getBestCombo());
    }
}