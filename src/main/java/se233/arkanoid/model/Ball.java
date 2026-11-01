package se233.arkanoid.model;


public class Ball extends GameObject {
    public double getVx() { return vx; }
    public double getVy() { return vy; }
    public void setVelocity(double vx, double vy) { this.vx = vx; this.vy = vy; }

    private double vx, vy;

    public Ball(double x, double y, double size) {
        super(x, y, size, size);
    }

    public void launch(double speed) { launch(speed, -60); }

    /** angleDeg: -90 = ขึ้นตรง, -60 = เฉียงขวา, -120 = เฉียงซ้าย */
    public void launch(double speed, double angleDeg) {
        double angle = Math.toRadians(angleDeg);
        vx = speed * Math.cos(angle);
        vy = speed * Math.sin(angle);
    }

    public void setPosition(double x, double y) { this.x = x; this.y = y; }
    public void stop() { vx = 0; vy = 0; }
    public double getSpeed() { return Math.hypot(vx, vy); }

    public void update(double dt) {
        x += vx * dt;
        y += vy * dt;
    }

    public void bounceWalls(double areaWidth) {
        if (x < 0) { x = 0; vx = Math.abs(vx); }
        if (x + width > areaWidth) { x = areaWidth - width; vx = -Math.abs(vx); }
        if (y < 0) { y = 0; vy = Math.abs(vy); }
    }

    public boolean isOut(double areaHeight) { return y > areaHeight; }

    public boolean bounceOffPaddle(Paddle p) {
        if (vy > 0 && intersects(p)) {
            y = p.getY() - height;
            double offset = ((x + width / 2) - (p.getX() + p.getWidth() / 2)) / (p.getWidth() / 2);
            offset = Math.max(-1, Math.min(1, offset));
            double speed = getSpeed();
            double angle = offset * Math.toRadians(60); // ชนขอบ paddle เด้งเฉียงขึ้น
            vx = speed * Math.sin(angle);
            vy = -speed * Math.cos(angle);
            return true;
        }
        return false;
    }

    public void bounceOffBrick(Brick b) {
        double overlapX = Math.min(x + width, b.getX() + b.getWidth()) - Math.max(x, b.getX());
        double overlapY = Math.min(y + height, b.getY() + b.getHeight()) - Math.max(y, b.getY());
        if (overlapX < overlapY) vx = -vx; else vy = -vy;
    }
}