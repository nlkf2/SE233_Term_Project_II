package se233.arkanoid.sprite;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import se233.arkanoid.exception.GameException;

import java.io.IOException;
import java.io.InputStream;

public final class BossSprites {
    // ===== ปรับพิกัดตรงนี้ถ้าเฟรมเพี้ยน =====
    private static final double X0 = 0;
    private static final double STEP_X = 63.5;
    private static final double FW = 63;
    private static final double FH = 92;
    private static final double Y_NORMAL = 143;
    private static final double Y_HIT = 238;
    private static final double Y_DYING = 333;
    private static final int COLS = 4;

    private final Image sheet;

    public BossSprites(String path) {
        try (InputStream in = BossSprites.class.getResourceAsStream(path)) {
            if (in == null) throw new GameException("Boss sprite not found: " + path);
            sheet = new Image(in);
        } catch (IOException e) {
            throw new GameException("Cannot read boss sprite: " + path, e);
        }
        if (sheet.isError()) throw new GameException("Invalid boss sprite: " + path);
    }

    /** state: 0 = normal, 1 = hit, 2 = dying */
    public void draw(GraphicsContext g, int state, int col, double dx, double dy, double dw, double dh) {
        double sy = state == 1 ? Y_HIT : state == 2 ? Y_DYING : Y_NORMAL;
        double sx = X0 + (col % COLS) * STEP_X;
        g.setImageSmoothing(false);
        g.drawImage(sheet, sx, sy, FW, FH, dx, dy, dw, dh);
    }

    public Image getSheet() { return sheet; }

    /** วาดทั้งแผ่นพร้อมกรอบเฟรมที่ตั้งไว้ ใช้ไล่ปรับค่า */
    public void drawDebug(GraphicsContext g) {
        g.setImageSmoothing(false);
        g.drawImage(sheet, 0, 0);
        g.setLineWidth(1);
        g.setStroke(javafx.scene.paint.Color.MAGENTA);
        double[] ys = {Y_NORMAL, Y_HIT, Y_DYING};
        for (double y : ys) {
            for (int c = 0; c < COLS; c++) {
                g.strokeRect(X0 + c * STEP_X + 0.5, y + 0.5, FW, FH);
            }
        }
        g.setFill(javafx.scene.paint.Color.BLACK);
        g.fillText("sheet " + (int) sheet.getWidth() + " x " + (int) sheet.getHeight(), 10, 12);
    }
}