import org.junit.jupiter.api.Test;
import se233.arkanoid.model.GameWorld;
import se233.arkanoid.upgrade.Upgrade;
import se233.arkanoid.upgrade.Upgrades;
import se233.arkanoid.model.Brick;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UpgradesTest {
    @Test
    void widePaddleIncreasesWidth() {
        GameWorld w = new GameWorld();
        double before = w.getPaddle().getWidth();
        new Upgrades.WidePaddle().apply(w);
        assertEquals(before * 1.25, w.getPaddle().getWidth(), 0.001);
    }

    @Test
    void fastPaddleIncreasesSpeed() {
        GameWorld w = new GameWorld();
        double before = w.getPaddle().getSpeed();
        new Upgrades.FastPaddle().apply(w);
        assertEquals(before * 1.25, w.getPaddle().getSpeed(), 0.001);
    }

    @Test
    void extraLifeAddsOneLife() {
        GameWorld w = new GameWorld();
        new Upgrades.ExtraLife().apply(w);
        assertEquals(4, w.getLives());
    }

    @Test
    void scoreBoostMultipliesBrickScore() {
        GameWorld w = new GameWorld();
        new Upgrades.ScoreBoost().apply(w);
        Brick last = w.getBricks().get(w.getBricks().size() - 1);
        w.launch();
        w.getBall().setPosition(last.getX() + 1, last.getY() + 1);
        w.update(0.0001, 0);
        assertEquals(15, w.getScore()); // 10 x 1.5
    }

    @Test
    void randomChoicesAreUnique() {
        List<Upgrade> choices = Upgrades.randomChoices(3);
        Set<String> names = new HashSet<>();
        choices.forEach(u -> names.add(u.getName()));
        assertEquals(3, names.size());
    }
}