package se233.arkanoid.exception;

public class GameExceptionHandler {

    public static void handle(GameException e) {
        System.err.println("Game Error: " + e.getMessage());
    }
}