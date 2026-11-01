package se233.arkanoid.model;

public class Bullet extends GameObject {
    private static final double SPEED = 260;

    public Bullet(double x, double y) { super(x, y, 6, 14); }

    public void update(double dt) { y += SPEED * dt; }

    public boolean isOut(double areaHeight) { return y > areaHeight; }
}