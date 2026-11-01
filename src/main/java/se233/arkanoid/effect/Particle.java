package se233.arkanoid.effect;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Particle {
    private double x, y, vx, vy, life;
    private final double maxLife, size;
    private final Color color;

    public Particle(double x, double y, double vx, double vy,
                    double life, double size, Color color) {
        this.x = x; this.y = y; this.vx = vx; this.vy = vy;
        this.life = life; this.maxLife = life;
        this.size = size; this.color = color;
    }

    public void update(double dt) {
        x += vx * dt;
        y += vy * dt;
        vy += 400 * dt;   // แรงโน้มถ่วง
        life -= dt;
    }

    public boolean isDead() { return life <= 0; }

    public void draw(GraphicsContext g) {
        g.setGlobalAlpha(Math.max(0, life / maxLife));
        g.setFill(color);
        g.fillRect(x, y, size, size);
        g.setGlobalAlpha(1);
    }
}