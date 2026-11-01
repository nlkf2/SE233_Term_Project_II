import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import se233.arkanoid.exception.GameException;
import se233.arkanoid.model.GameWorld;
import se233.arkanoid.progress.Progress;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProgressTest {
    @Test
    void startsAtStageOneWhenNoFile(@TempDir Path dir) {
        assertEquals(1, new Progress(dir.resolve("p.properties")).getUnlocked());
    }

    @Test
    void unlockIsSavedAndReloaded(@TempDir Path dir) {
        Path f = dir.resolve("p.properties");
        new Progress(f).unlock(3);
        assertEquals(3, new Progress(f).getUnlocked());
    }

    @Test
    void unlockNeverGoesDown(@TempDir Path dir) {
        Progress p = new Progress(dir.resolve("p.properties"));
        p.unlock(3);
        p.unlock(2);
        assertEquals(3, p.getUnlocked());
    }

    @Test
    void worldCanStartAtChosenStage() {
        assertEquals(3, new GameWorld(3).getStage());
        assertThrows(GameException.class, () -> new GameWorld(9));
    }
}