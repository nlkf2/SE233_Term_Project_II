package se233.arkanoid.stage;

import se233.arkanoid.exception.GameException;
import se233.arkanoid.model.Brick;

import java.util.ArrayList;
import java.util.List;

public final class StageFactory {
    public static final int MAX_STAGE = 3;
    private static final double WIDTH = 800;

    private StageFactory() {}

    public static List<Brick> create(int stage) {
        if (stage < 1 || stage > MAX_STAGE) {
            throw new GameException("Invalid stage number: " + stage);
        }

        List<Brick> bricks = new ArrayList<>();
        int rows = 4 + stage;          // ด่านสูง = แถวเยอะขึ้น
        int cols = 10;
        double w = 70, h = 24, gap = 6;
        double startX = (WIDTH - (cols * w + (cols - 1) * gap)) / 2;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (stage == 2 && (r + c) % 2 == 0) continue;    // ด่าน 2: ลายหมากรุก
                if (stage == 3 && c % 3 == 1 && r > 1) continue; // ด่าน 3: มีช่องว่าง
                int hp = 1 + (r < stage ? 1 : 0);                // แถวบนแข็งขึ้นตามด่าน
                bricks.add(new Brick(startX + c * (w + gap), 60 + r * (h + gap),
                        w, h, hp, 10 * hp));
            }
        }
        return bricks;
    }
}