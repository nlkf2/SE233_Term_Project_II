package se233.arkanoid.model;

import se233.arkanoid.exception.GameException;

public class Brick extends GameObject {
    private int hp;
    private final int points;

    public Brick(double x, double y, double width, double height, int hp, int points) {
        super(x, y, width, height);
        if (hp <= 0) throw new GameException("Brick hp must be positive: " + hp);
        this.hp = hp;
        this.points = points;
    }

    public boolean hit() {
        if (hp > 0) hp--;
        return hp == 0;
    }

    public boolean isDestroyed() { return hp <= 0; }
    public int getHp() { return hp; }
    public int getPoints() { return points; }
}