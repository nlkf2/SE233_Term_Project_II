import org.junit.jupiter.api.Test;
import se233.arkanoid.model.Ball;
import se233.arkanoid.model.Paddle;

import static org.junit.jupiter.api.Assertions.*;

class BallTest {
    @Test
    void bouncesOffLeftWall() {
        Ball ball = new Ball(-5, 100, 12);
        ball.setVelocity(-100, 0);
        ball.bounceWalls(800);
        assertEquals(0, ball.getX());
        assertTrue(ball.getVx() > 0);
    }

    @Test
    void bouncesOffRightWall() {
        Ball ball = new Ball(795, 100, 12);
        ball.setVelocity(100, 0);
        ball.bounceWalls(800);
        assertTrue(ball.getVx() < 0);
    }

    @Test
    void bouncesOffTopWall() {
        Ball ball = new Ball(100, -3, 12);
        ball.setVelocity(0, -100);
        ball.bounceWalls(800);
        assertTrue(ball.getVy() > 0);
    }

    @Test
    void bouncesUpFromPaddle() {
        Paddle paddle = new Paddle(350, 560, 100, 15, 520);
        Ball ball = new Ball(394, 550, 12);
        ball.setVelocity(0, 300);
        assertTrue(ball.bounceOffPaddle(paddle));
        assertTrue(ball.getVy() < 0);
    }

    @Test
    void ballBelowScreenIsOut() {
        Ball ball = new Ball(100, 650, 12);
        assertTrue(ball.isOut(600));
    }
}