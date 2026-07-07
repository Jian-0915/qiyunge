package com.qiyunge.app;

import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class NavigationService {

    public enum Page {
        DASHBOARD, MUSIC, GALLERY, ENTERTAINMENT, PROFILE, ADMIN, SETTINGS
    }

    private Page currentPage = Page.DASHBOARD;
    private final Map<Page, Supplier<javafx.scene.Node>> pageFactories = new HashMap<>();
    private StackPane contentArea;
    private UserSession userSession;
    private final List<Runnable> navigationListeners = new ArrayList<>();

    public NavigationService() {
    }

    public void setUserSession(UserSession userSession) {
        this.userSession = userSession;
    }

    public void registerPage(Page page, Supplier<javafx.scene.Node> factory) {
        pageFactories.put(page, factory);
    }

    public void setContentArea(StackPane contentArea) {
        this.contentArea = contentArea;
    }

    public boolean canNavigate(Page page) {
        if (userSession == null || !userSession.isLoggedIn()) {
            return false;
        }
        if (page == Page.ADMIN && !userSession.isAdmin()) {
            return false;
        }
        if (!userSession.isActive()) {
            return false;
        }
        return true;
    }

    public void addNavigationListener(Runnable listener) {
        navigationListeners.add(listener);
    }

    public void navigateTo(Page page) {
        if (!canNavigate(page)) {
            System.err.println("Navigation denied to: " + page);
            return;
        }
        if (page == currentPage && contentArea != null && !contentArea.getChildren().isEmpty()) {
            return;
        }
        currentPage = page;
        if (contentArea != null && pageFactories.containsKey(page)) {
            javafx.scene.Node view = pageFactories.get(page).get();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        }
        notifyNavigationListeners();
    }

    private void notifyNavigationListeners() {
        for (Runnable listener : navigationListeners) {
            listener.run();
        }
    }

    public boolean requireAdmin() {
        return userSession != null && userSession.isAdmin();
    }

    public boolean requireLogin() {
        return userSession != null && userSession.isLoggedIn() && userSession.isActive();
    }

    public Page getCurrentPage() { return currentPage; }
}
