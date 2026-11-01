package se233.arkanoid.model;

public class Boss extends Brick {
    private static final double SPEED = 120;
    private final int maxHp;
    private double dir = 1;

    public Boss(double x, double y, double width, double height, int hp, int points) {
        super(x, y, width, height, hp, points);
        this.maxHp = hp;
    }

    public void update(double dt, double areaWidth) {
        x += dir * SPEED * dt;
        if (x < 0) { x = 0; dir = 1; }
        if (x + width > areaWidth) { x = areaWidth - width; dir = -1; }
    }

    public int getMaxHp() { return maxHp; }
}