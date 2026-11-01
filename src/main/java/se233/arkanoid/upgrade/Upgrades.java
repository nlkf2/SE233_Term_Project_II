package se233.arkanoid.upgrade;

import se233.arkanoid.model.GameWorld;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Upgrades {
    private Upgrades() {}

    public static abstract class Base implements Upgrade {
        private final String name, description;
        protected Base(String name, String description) {
            this.name = name; this.description = description;
        }
        @Override public String getName() { return name; }
        @Override public String getDescription() { return description; }
    }

    public static class WidePaddle extends Base {
        public WidePaddle() { super("Wide Paddle", "Paddle +25% wider"); }
        @Override public void apply(GameWorld w) { w.getPaddle().scaleWidth(1.25); }
    }

    public static class FastPaddle extends Base {
        public FastPaddle() { super("Fast Paddle", "Paddle moves +25% faster"); }
        @Override public void apply(GameWorld w) { w.getPaddle().scaleSpeed(1.25); }
    }

    public static class SlowBall extends Base {
        public SlowBall() { super("Slow Ball", "Ball speed -15%"); }
        @Override public void apply(GameWorld w) { w.scaleBallSpeed(0.85); }
    }

    public static class ExtraLife extends Base {
        public ExtraLife() { super("Extra Life", "+1 life"); }
        @Override public void apply(GameWorld w) { w.addLife(); }
    }

    public static class ScoreBoost extends Base {
        public ScoreBoost() { super("Score Boost", "Brick score x1.5"); }
        @Override public void apply(GameWorld w) { w.scaleScoreMultiplier(1.5); }
    }

    public static class PierceBall extends Base {
        public PierceBall() { super("Piercing Ball", "Pierce 8 bricks per stage"); }
        @Override public void apply(GameWorld w) { w.addPierce(8); }
    }

    public static class ExtraBall extends Base {
        public ExtraBall() { super("Extra Ball", "+1 extra ball per launch"); }
        @Override public void apply(GameWorld w) { w.addExtraBall(); }
    }

    /** สุ่มตัวเลือกไม่ซ้ำกัน */
    public static List<Upgrade> randomChoices(int count) {
        List<Upgrade> all = new ArrayList<>(List.of(
                new WidePaddle(), new FastPaddle(), new SlowBall(), new ExtraLife(),
                new ScoreBoost(), new PierceBall(), new ExtraBall()));
        Collections.shuffle(all);
        return new ArrayList<>(all.subList(0, Math.min(count, all.size())));
    }
}