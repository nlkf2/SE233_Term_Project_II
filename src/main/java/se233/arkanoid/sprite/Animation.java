package se233.arkanoid.sprite;

import se233.arkanoid.sprite.SpriteAtlas.Frame;

public class Animation {
    private final Frame[] frames;
    private final double frameTime;
    private final boolean loop;
    private double t = 0;

    public Animation(Frame[] frames, double frameTime, boolean loop) {
        this.frames = frames;
        this.frameTime = frameTime;
        this.loop = loop;
    }

    public void update(double dt) { t += dt; }
    public void reset() { t = 0; }

    public Frame current() {
        int i = (int) (t / frameTime);
        i = loop ? i % frames.length : Math.min(i, frames.length - 1);
        return frames[i];
    }

    public boolean isFinished() { return !loop && t >= frames.length * frameTime; }
}