package se233.arkanoid.model;

public class Laser extends GameObject {
    private static final double SPEED = 600;

    public Laser(double x, double y) {
        super(x, y, 4, 14);
    }

    public void update(double dt) { y -= SPEED * dt; }

    public boolean isOut() { return y + height < 0; }
}