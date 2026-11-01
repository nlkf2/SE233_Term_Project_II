package se233.arkanoid.model;

public class Paddle extends GameObject {
    private double speed;

    public Paddle(double x, double y, double width, double height, double speed) {
        super(x, y, width, height);
        this.speed = speed;
    }

    public void move(int direction, double dt, double areaWidth) {
        x += direction * speed * dt;
        if (x < 0) x = 0;
        if (x + width > areaWidth) x = areaWidth - width;
    }

    public void scaleWidth(double factor) {
        double cx = x + width / 2;
        width *= factor;
        x = cx - width / 2;
    }

    public void scaleSpeed(double factor) { speed *= factor; }
    public double getSpeed() { return speed; }
}