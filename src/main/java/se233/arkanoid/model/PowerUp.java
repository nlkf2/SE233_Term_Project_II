package se233.arkanoid.model;

public class PowerUp extends GameObject {
    private static final double FALL_SPEED = 150;

    public PowerUp(double x, double y) {
        super(x, y, 28, 14);
    }

    public void update(double dt) { y += FALL_SPEED * dt; }

    public boolean isOut(double areaHeight) { return y > areaHeight; }
}