import org.junit.jupiter.api.Test;
import se233.arkanoid.model.GameWorld;

import static org.junit.jupiter.api.Assertions.*;

class BounceEventTest {
    @Test
    void wallBounceIsReported() {
        GameWorld w = new GameWorld();
        w.launch();
        w.getBall().setPosition(-3, 300);
        w.getBall().setVelocity(-100, 0);
        w.update(0.0001, 0);
        assertFalse(w.drainBounces().isEmpty());
    }

    @Test
    void noBounceInOpenSpace() {
        GameWorld w = new GameWorld();
        w.launch();
        w.getBall().setPosition(400, 350);
        w.getBall().setVelocity(50, -50);
        w.update(0.0001, 0);
        assertTrue(w.drainBounces().isEmpty());
    }
}