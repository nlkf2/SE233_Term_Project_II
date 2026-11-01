package se233.arkanoid.progress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se233.arkanoid.stage.StageFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class Progress {
    private static final Logger log = LogManager.getLogger(Progress.class);
    private final Path file;
    private int unlocked = 1;

    public Progress(Path file) {
        this.file = file;
        load();
    }

    private void load() {
        if (!Files.exists(file)) return;
        try (InputStream in = Files.newInputStream(file)) {
            Properties p = new Properties();
            p.load(in);
            int v = Integer.parseInt(p.getProperty("unlocked", "1").trim());
            unlocked = Math.max(1, Math.min(StageFactory.MAX_STAGE, v));
            log.info("Progress loaded: unlocked stage {}", unlocked);
        } catch (IOException | NumberFormatException e) {
            log.warn("Cannot read progress file, starting fresh: {}", e.getMessage());
            unlocked = 1;
        }
    }

    /** ปลดล็อกถึงด่านที่ระบุ (ไม่ลดค่าที่เคยปลดแล้ว) */
    public void unlock(int stage) {
        int s = Math.min(stage, StageFactory.MAX_STAGE);
        if (s <= unlocked) return;
        unlocked = s;
        save();
        log.info("Unlocked stage {}", unlocked);
    }

    private void save() {
        try (OutputStream out = Files.newOutputStream(file)) {
            Properties p = new Properties();
            p.setProperty("unlocked", String.valueOf(unlocked));
            p.store(out, "Roguelike Arkanoid progress");
        } catch (IOException e) {
            log.warn("Cannot save progress: {}", e.getMessage());
        }
    }

    public int getUnlocked() { return unlocked; }
}