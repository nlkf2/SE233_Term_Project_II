package se233.arkanoid.stage;

import se233.arkanoid.exception.GameException;
import se233.arkanoid.model.Boss;
import se233.arkanoid.model.Brick;

import java.util.ArrayList;
import java.util.List;

public final class StageFactory {
    public static final int MAX_STAGE = 4;   // ด่าน 4 = บอส
    private static final double WIDTH = 800;

    private StageFactory() {}

    public static boolean isBossStage(int stage) { return stage == MAX_STAGE; }

    public static List<Brick> create(int stage) {
        if (stage < 1 || stage > MAX_STAGE) {
            throw new GameException("Invalid stage number: " + stage);
        }
        if (isBossStage(stage)) return createBossStage();

        List<Brick> bricks = new ArrayList<>();
        int rows = 4 + stage;
        int cols = 10;
        double w = 70, h = 24, gap = 6;
        double startX = (WIDTH - (cols * w + (cols - 1) * gap)) / 2;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (stage == 2 && (r + c) % 2 == 0) continue;
                if (stage == 3 && c % 3 == 1 && r > 1) continue;
                int hp = 1 + (r < stage ? 1 : 0);
                bricks.add(new Brick(startX + c * (w + gap), 60 + r * (h + gap),
                        w, h, hp, 10 * hp));
            }
        }
        return bricks;
    }

    private static List<Brick> createBossStage() {
        List<Brick> bricks = new ArrayList<>();
        bricks.add(new Boss(320, 70, 160, 40, 25, 500));
        int cols = 10;
        double w = 70, h = 24, gap = 6;
        double startX = (WIDTH - (cols * w + (cols - 1) * gap)) / 2;
        for (int c = 0; c < cols; c++) {
            bricks.add(new Brick(startX + c * (w + gap), 190, w, h, 1, 10));
        }
        return bricks;
    }
}