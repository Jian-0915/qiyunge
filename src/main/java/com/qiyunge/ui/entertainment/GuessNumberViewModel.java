package com.qiyunge.ui.entertainment;

import com.qiyunge.app.AppContext;
import com.qiyunge.application.service.EntertainmentService;
import com.qiyunge.domain.entity.GameRecord;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GuessNumberViewModel {

    private final AppContext appContext;
    private int targetNumber;
    private int minRange = 1;
    private int maxRange = 100;
    private String difficulty = "easy";
    private int currentAttempts = 0;
    private boolean gameOver = false;
    private int lastGuessNumber = 0;
    private List<GuessEntry> history = new ArrayList<>();

    private final SimpleStringProperty inputTextProperty = new SimpleStringProperty("");
    private final SimpleStringProperty resultTextProperty = new SimpleStringProperty("");
    private final SimpleStringProperty statusTextProperty = new SimpleStringProperty("请输入你的猜测...");
    private final SimpleIntegerProperty attemptsProperty = new SimpleIntegerProperty(0);
    private final SimpleObjectProperty<GameRecord> bestRecordProperty = new SimpleObjectProperty<>(null);
    private final SimpleIntegerProperty lastGuessProperty = new SimpleIntegerProperty(0);

    public static class GuessEntry {
        private final int number;
        private final String direction; // "up" = 大了, "down" = 小了, "hit" = 猜中

        public GuessEntry(int number, String direction) {
            this.number = number;
            this.direction = direction;
        }

        public int getNumber() { return number; }
        public String getDirection() { return direction; }
        public boolean isUp() { return "up".equals(direction); }
        public boolean isDown() { return "down".equals(direction); }
        public boolean isHit() { return "hit".equals(direction); }
    }

    public GuessNumberViewModel(AppContext appContext) {
        this.appContext = appContext;
    }

    public void setDifficulty(String diff) {
        this.difficulty = diff;
        switch (diff) {
            case "normal" -> { minRange = 1; maxRange = 500; }
            case "hard"   -> { minRange = 1; maxRange = 1000; }
            default       -> { minRange = 1; maxRange = 100; }
        }
        resetGame();
    }

    public void makeGuess(String input) {
        if (gameOver) return;

        int guess;
        try {
            guess = Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            statusTextProperty.set("请输入有效的整数");
            return;
        }

        if (guess < minRange || guess > maxRange) {
            statusTextProperty.set("请输入 " + minRange + " ~ " + maxRange + " 之间的数字");
            return;
        }

        currentAttempts++;
        attemptsProperty.set(currentAttempts);
        lastGuessNumber = guess;
        lastGuessProperty.set(guess);

        if (guess == targetNumber) {
            gameOver = true;
            resultTextProperty.set("猜中了!");
            statusTextProperty.set("第 " + currentAttempts + " 次猜测");
            history.add(new GuessEntry(guess, "hit"));
            saveRecord();
        } else if (guess > targetNumber) {
            resultTextProperty.set("大了！往小了猜");
            statusTextProperty.set("第 " + currentAttempts + " 次猜测");
            history.add(new GuessEntry(guess, "up"));
        } else {
            resultTextProperty.set("小了！往大了猜");
            statusTextProperty.set("第 " + currentAttempts + " 次猜测");
            history.add(new GuessEntry(guess, "down"));
        }
    }

    public void resetGame() {
        targetNumber = new Random().nextInt(maxRange - minRange + 1) + minRange;
        currentAttempts = 0;
        gameOver = false;
        lastGuessNumber = 0;
        history.clear();
        inputTextProperty.set("");
        resultTextProperty.set("");
        statusTextProperty.set("请输入你的猜测...");
        attemptsProperty.set(0);
        lastGuessProperty.set(0);
        loadBestRecord();
    }

    private void saveRecord() {
        try {
            EntertainmentService service = appContext.getEntertainmentService();
            if (service == null) return;
            GameRecord record = new GameRecord();
            record.setGameType("guessNumber");
            record.setDifficulty(difficulty);
            record.setScore(currentAttempts);
            if (appContext.getUserSession().isLoggedIn()) {
                record.setUserId(appContext.getUserSession().getUserId());
            }
            service.saveGameRecord(record);
            GameRecord currentBest = bestRecordProperty.get();
            if (currentBest == null || currentAttempts < currentBest.getScore()) {
                GameRecord newBest = new GameRecord();
                newBest.setScore(currentAttempts);
                newBest.setDifficulty(difficulty);
                bestRecordProperty.set(newBest);
            }
        } catch (Exception e) {
            // 静默处理保存失败
        }
    }

    private void loadBestRecord() {
        try {
            EntertainmentService service = appContext.getEntertainmentService();
            if (service == null) {
                bestRecordProperty.set(null);
                return;
            }
            if (appContext.getUserSession().isLoggedIn()) {
                int userId = appContext.getUserSession().getUserId();
                GameRecord best = service.getBestScore(userId, "guessNumber", difficulty);
                bestRecordProperty.set(best);
            } else {
                bestRecordProperty.set(null);
            }
        } catch (Exception e) {
            bestRecordProperty.set(null);
        }
    }

    // --- New methods ---

    public int getLastGuessNumber() {
        if (gameOver) return targetNumber;
        return lastGuessNumber;
    }

    public double getProgressPercent() {
        if (currentAttempts == 0) return 0.0;
        int maxPossible = (int) Math.ceil(Math.log(maxRange - minRange + 1) / Math.log(2)) * 2;
        double pct = (double) currentAttempts / maxPossible;
        return Math.min(pct, 1.0);
    }

    public String getRangeText() {
        return minRange + " — " + maxRange;
    }

    public String getDifficultyName() {
        return switch (difficulty) {
            case "normal" -> "普通";
            case "hard"   -> "困难";
            default       -> "简单";
        };
    }

    // --- Existing getters ---

    public int getMinRange() {
        return minRange;
    }

    public int getMaxRange() {
        return maxRange;
    }

    public String getDifficultyLabel() {
        return switch (difficulty) {
            case "normal" -> "普通 (1-500)";
            case "hard"   -> "困难 (1-1000)";
            default       -> "简单 (1-100)";
        };
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public List<GuessEntry> getHistory() {
        return history;
    }

    // Property getters
    public SimpleStringProperty inputTextProperty() { return inputTextProperty; }
    public SimpleStringProperty resultTextProperty() { return resultTextProperty; }
    public SimpleStringProperty statusTextProperty() { return statusTextProperty; }
    public SimpleIntegerProperty attemptsProperty() { return attemptsProperty; }
    public SimpleObjectProperty<GameRecord> bestRecordProperty() { return bestRecordProperty; }
    public SimpleIntegerProperty lastGuessProperty() { return lastGuessProperty; }
    public String getDifficulty() { return difficulty; }
}
