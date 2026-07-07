package com.qiyunge.app;

import com.qiyunge.infrastructure.storage.ConfigStorage;
import javafx.scene.Scene;

import java.util.Arrays;
import java.util.List;

public class ThemeService {

    public enum Theme {
        MORNING("晨雾", "morning"),
        FOREST("松林", "forest"),
        DUSK("暮色", "dusk"),
        MOON("月白", "moon");

        private final String displayName;
        private final String cssClass;

        Theme(String displayName, String cssClass) {
            this.displayName = displayName;
            this.cssClass = cssClass;
        }

        public String getDisplayName() { return displayName; }
        public String getCssClass() { return cssClass; }
    }

    private Theme currentTheme = Theme.MORNING;
    private final ConfigStorage configStorage;

    public ThemeService(ConfigStorage configStorage) {
        this.configStorage = configStorage;
        // Load saved theme
        String saved = configStorage.get("theme", "morning");
        for (Theme t : Theme.values()) {
            if (t.cssClass.equals(saved)) {
                currentTheme = t;
                break;
            }
        }
    }

    public void applyTheme(Scene scene) {
        scene.getRoot().getStyleClass().removeAll(
            Arrays.stream(Theme.values()).map(Theme::getCssClass).toArray(String[]::new)
        );
        scene.getRoot().getStyleClass().add(currentTheme.getCssClass());
    }

    public void setTheme(Theme theme, Scene scene) {
        currentTheme = theme;
        configStorage.set("theme", theme.getCssClass());
        applyTheme(scene);
    }

    public Theme getCurrentTheme() { return currentTheme; }
    public List<Theme> getAllThemes() { return Arrays.asList(Theme.values()); }
}
