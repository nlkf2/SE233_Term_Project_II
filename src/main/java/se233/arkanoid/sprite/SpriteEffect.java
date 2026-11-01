package se233.arkanoid.sprite;

import javafx.scene.canvas.GraphicsContext;

public class SpriteEffect {
    private final Animation anim;
    private final double cx, cy, size;

    public SpriteEffect(Animation anim, double cx, double cy, double size) {
        this.anim = anim; this.cx = cx; this.cy = cy; this.size = size;
    }

    public void update(double dt) { anim.update(dt); }
    public boolean isDone() { return anim.isFinished(); }

    public void draw(SpriteAtlas atlas, GraphicsContext g) {
        atlas.draw(g, anim.current(), cx - size / 2, cy - size / 2, size, size);
    }
}