import org.junit.jupiter.api.Test;
import se233.arkanoid.exception.GameException;
import se233.arkanoid.model.Brick;

import static org.junit.jupiter.api.Assertions.*;

class BrickTest {
    @Test
    void normalBrickIsDestroyedByOneHit() {
        Brick b = new Brick(0, 0, 70, 24, 1, 10);
        assertTrue(b.hit());
        assertTrue(b.isDestroyed());
    }

    @Test
    void strongBrickNeedsTwoHits() {
        Brick b = new Brick(0, 0, 70, 24, 2, 20);
        assertFalse(b.hit());
        assertEquals(1, b.getHp());
        assertTrue(b.hit());
        assertTrue(b.isDestroyed());
    }

    @Test
    void invalidHpThrowsGameException() {
        assertThrows(GameException.class, () -> new Brick(0, 0, 70, 24, 0, 10));
    }
}