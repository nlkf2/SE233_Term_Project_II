package se233.arkanoid.model;

public abstract class GameObject {
    protected double x, y, width, height;

    protected GameObject(double x, double y, double width, double height) {
        this.x = x; this.y = y; this.width = width; this.height = height;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }

    public boolean intersects(GameObject o) {
        return x < o.x + o.width && x + width > o.x
                && y < o.y + o.height && y + height > o.y;
    }
}