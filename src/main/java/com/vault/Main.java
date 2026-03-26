package com.vault;

/**
 * Main.java — JavaFX Fat JAR Launcher Workaround
 *
 * JavaFX applications cannot be launched directly from a shaded/fat JAR
 * if the main class extends javafx.application.Application.
 * This plain (non-JavaFX) wrapper class is the actual entry point declared
 * in the JAR manifest. It simply delegates to App.main(), which then
 * triggers the JavaFX runtime correctly.
 */
public class Main {
    public static void main(String[] args) {
        App.main(args);
    }
}
