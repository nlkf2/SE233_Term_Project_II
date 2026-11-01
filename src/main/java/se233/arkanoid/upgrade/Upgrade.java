package se233.arkanoid.upgrade;

import se233.arkanoid.model.GameWorld;

public interface Upgrade {
    String getName();
    String getDescription();
    void apply(GameWorld world);
}