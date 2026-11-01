package se233.arkanoid.sprite;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import se233.arkanoid.exception.GameException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.IntStream;

public final class SpriteAtlas {
    /** ถ้า PNG จริงใหญ่/เล็กกว่า 674 px กว้าง ให้ปรับตัวคูณนี้ */
    public static final double SCALE = 1.0;

    public record Frame(double x, double y, double w, double h) {}

    private static Frame f(double x, double y, double w, double h) {
        return new Frame(x * SCALE, y * SCALE, w * SCALE, h * SCALE);
    }

    // ===== พิกัดประมาณการ (x, y, กว้าง, สูง) ปรับได้ที่นี่ =====
    // paddle ปกติ 4 เฟรม (แถวบนสุดซ้าย)
    public static final Frame[] PADDLE = {
            f(2, 2, 68, 22), f(75, 2, 68, 22), f(147, 2, 68, 22), f(220, 2, 68, 22)};
    // paddle โหมดเลเซอร์ (สีน้ำเงิน)
    public static final Frame[] PADDLE_LASER = {f(2, 337, 68, 22), f(75, 337, 68, 22)};
    // อิฐสี 8 สี (แถวบนสุดฝั่งขวา)
    private static final double[] BRICK_X = {290, 322, 354, 387, 419, 451, 483, 515};
    public static final Frame[] BRICK_COLORS =
            Arrays.stream(BRICK_X).mapToObj(x -> f(x, 0, 31, 16)).toArray(Frame[]::new);
    public static final Frame BRICK_SILVER = f(579, 0, 31, 16);
    // แคปซูล L (เลเซอร์) หมุน 8 เฟรม
    public static final Frame[] CAPSULE_L =
            IntStream.range(0, 8).mapToObj(i -> f(2 + 48 * i, 231, 40, 20)).toArray(Frame[]::new);
    // ระเบิด 6 เฟรม (เล็ก -> ใหญ่)
    public static final Frame[] EXPLOSION = {
            f(290, 129, 32, 32), f(322, 129, 32, 32), f(354, 129, 32, 32),
            f(386, 129, 32, 32), f(418, 129, 32, 32), f(450, 129, 32, 32)};
    public static final Frame[] EXPLOSION_SMALL = Arrays.copyOfRange(EXPLOSION, 0, 3); // ตอนบอลเด้ง
    public static final Frame[] EXPLOSION_BIG = Arrays.copyOfRange(EXPLOSION, 2, 6);   // ตอนอิฐแตก
    // ภาพหน้าปกและพื้นหลัง (ส่วนล่างของแผ่น)
    public static final Frame COVER = f(1, 483, 320, 480);
    public static final Frame BACKGROUND = f(1, 1485, 320, 438);

    /** ใช้แสดงกรอบในโหมด debug */
    public static Map<String, Frame[]> all() {
        Map<String, Frame[]> m = new LinkedHashMap<>();
        m.put("PADDLE", PADDLE);
        m.put("PADDLE_LASER", PADDLE_LASER);
        m.put("BRICKS", BRICK_COLORS);
        m.put("SILVER", new Frame[]{BRICK_SILVER});
        m.put("CAPSULE_L", CAPSULE_L);
        m.put("BOOM", EXPLOSION);
        m.put("COVER", new Frame[]{COVER});
        m.put("BG", new Frame[]{BACKGROUND});
        return m;
    }

    private final Image sheet;

    public SpriteAtlas(String resourcePath) {
        Image img;
        try (InputStream in = SpriteAtlas.class.getResourceAsStream(resourcePath)) {
            if (in == null) throw new GameException("Sprite sheet not found: " + resourcePath);
            img = new Image(in);
        } catch (IOException e) {
            throw new GameException("Cannot read sprite sheet: " + resourcePath, e);
        }
        if (img.isError()) throw new GameException("Invalid sprite sheet image: " + resourcePath);
        this.sheet = img;
    }

    public Image getSheet() { return sheet; }

    public void draw(GraphicsContext g, Frame f, double dx, double dy, double dw, double dh) {
        g.setImageSmoothing(false);
        g.drawImage(sheet, f.x(), f.y(), f.w(), f.h(), dx, dy, dw, dh);
    }
}