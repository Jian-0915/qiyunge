package com.qiyunge.ui.entertainment;

import com.qiyunge.app.AppContext;
import com.qiyunge.application.service.EntertainmentService;
import com.qiyunge.domain.entity.GameRecord;
import javafx.beans.property.*;

import java.util.*;

/**
 * 记忆翻牌游戏 ViewModel。
 */
public class MemoryFlipViewModel {

    private final AppContext appContext;

    // 游戏配置
    private int gridSize = 4; // 4 or 6
    private int totalPairs;
    private String difficulty = "easy"; // "easy" = 4x4, "hard" = 6x6

    // 游戏状态
    private List<String> cardSymbols; // 每张牌的符号
    private List<Boolean> matchedCards; // 哪些牌已配对
    private List<Boolean> faceUpCards; // 哪些牌当前正面朝上
    private int firstFlippedIndex = -1;
    private int secondFlippedIndex = -1;
    private int flipCount = 0; // 翻牌次数（每翻2张算1次）
    private int matchedPairs = 0;
    private boolean gameOver = false;
    private boolean processing = false; // 防止快速点击

    // 计时
    private long startTime = 0;
    private long elapsedTime = 0;
    private boolean timerRunning = false;

    // JavaFX Properties
    private final SimpleIntegerProperty flipCountProperty = new SimpleIntegerProperty(0);
    private final SimpleIntegerProperty matchedPairsProperty = new SimpleIntegerProperty(0);
    private final SimpleIntegerProperty totalPairsProperty = new SimpleIntegerProperty(0);
    private final SimpleBooleanProperty gameOverProperty = new SimpleBooleanProperty(false);
    private final SimpleStringProperty timeProperty = new SimpleStringProperty("0:00");
    private final SimpleObjectProperty<GameRecord> bestRecordProperty = new SimpleObjectProperty<>(null);

    // 栖云阁主题符号池（诗意命名）
    private static final String[] SYMBOL_POOL = {
        "山", "云", "月", "花", "风", "雪", "雨", "霜",
        "竹", "松", "鹤", "琴", "棋", "书", "画", "茶",
        "梦", "烟", "星", "泉"
    };

    public MemoryFlipViewModel(AppContext appContext) {
        this.appContext = appContext;
    }

    /**
     * 设置难度并重置游戏。
     */
    public void setDifficulty(String diff) {
        this.difficulty = diff;
        if ("hard".equals(diff)) {
            gridSize = 6;
        } else {
            gridSize = 4;
        }
        resetGame();
    }

    /**
     * 重置游戏。
     */
    public void resetGame() {
        totalPairs = (gridSize * gridSize) / 2;
        
        // 从符号池中选取所需数量的符号
        List<String> selectedSymbols = new ArrayList<>();
        List<String> pool = new ArrayList<>(Arrays.asList(SYMBOL_POOL));
        Collections.shuffle(pool);
        for (int i = 0; i < totalPairs && i < pool.size(); i++) {
            selectedSymbols.add(pool.get(i));
        }
        
        // 创建配对列表
        List<String> pairs = new ArrayList<>();
        for (String symbol : selectedSymbols) {
            pairs.add(symbol);
            pairs.add(symbol);
        }
        Collections.shuffle(pairs);
        
        this.cardSymbols = pairs;
        this.matchedCards = new ArrayList<>(Collections.nCopies(pairs.size(), false));
        this.faceUpCards = new ArrayList<>(Collections.nCopies(pairs.size(), false));
        this.firstFlippedIndex = -1;
        this.secondFlippedIndex = -1;
        this.flipCount = 0;
        this.matchedPairs = 0;
        this.gameOver = false;
        this.processing = false;
        this.startTime = 0;
        this.elapsedTime = 0;
        this.timerRunning = false;
        
        // Update properties
        flipCountProperty.set(0);
        matchedPairsProperty.set(0);
        totalPairsProperty.set(totalPairs);
        gameOverProperty.set(false);
        timeProperty.set("0:00");
        
        loadBestRecord();
    }

    /**
     * 翻牌操作。返回翻转的牌索引，或 -1 如果不能翻。
     */
    public FlipResult flipCard(int index) {
        if (gameOver || processing) return null;
        if (index < 0 || index >= cardSymbols.size()) return null;
        if (matchedCards.get(index) || faceUpCards.get(index)) return null;
        
        // 第一次翻牌时启动计时器
        if (!timerRunning) {
            startTime = System.currentTimeMillis();
            timerRunning = true;
        }
        
        // 翻开这张牌
        faceUpCards.set(index, true);
        
        FlipResult result = new FlipResult();
        result.index = index;
        result.symbol = cardSymbols.get(index);
        result.isMatch = false;
        result.isFirstOfPair = (firstFlippedIndex == -1);
        result.isComplete = false;
        
        if (firstFlippedIndex == -1) {
            // 翻开第一张
            firstFlippedIndex = index;
        } else {
            // 翻开第二张
            secondFlippedIndex = index;
            flipCount++;
            flipCountProperty.set(flipCount);
            processing = true;
            
            // 检查是否配对
            if (cardSymbols.get(firstFlippedIndex).equals(cardSymbols.get(secondFlippedIndex))) {
                // 配对成功
                matchedCards.set(firstFlippedIndex, true);
                matchedCards.set(secondFlippedIndex, true);
                matchedPairs++;
                matchedPairsProperty.set(matchedPairs);
                result.isMatch = true;
                
                // 检查游戏是否结束
                if (matchedPairs == totalPairs) {
                    gameOver = true;
                    timerRunning = false;
                    elapsedTime = System.currentTimeMillis() - startTime;
                    gameOverProperty.set(true);
                    result.isComplete = true;
                    saveRecord();
                }
                
                processing = false;
                firstFlippedIndex = -1;
                secondFlippedIndex = -1;
            } else {
                // 不配对，需要在View中延迟翻回
                result.isMatch = false;
                // processing remains true until hideUnmatched() is called
            }
        }
        
        return result;
    }

    /**
     * 翻回不配对的牌。由View在延迟动画后调用。
     */
    public void hideUnmatched() {
        if (firstFlippedIndex >= 0) faceUpCards.set(firstFlippedIndex, false);
        if (secondFlippedIndex >= 0) faceUpCards.set(secondFlippedIndex, false);
        firstFlippedIndex = -1;
        secondFlippedIndex = -1;
        processing = false;
    }

    /**
     * 更新计时器显示。
     */
    public void updateTimer() {
        if (!timerRunning) return;
        long elapsed = System.currentTimeMillis() - startTime;
        timeProperty.set(formatTime(elapsed));
    }

    private String formatTime(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return minutes + ":" + String.format("%02d", seconds);
    }

    private void saveRecord() {
        try {
            EntertainmentService service = appContext.getEntertainmentService();
            if (service == null) return;
            GameRecord record = new GameRecord();
            record.setGameType("memoryFlip");
            record.setDifficulty(difficulty);
            record.setScore(flipCount); // 翻牌次数作为成绩
            record.setTimeSeconds((int) (elapsedTime / 1000));
            if (appContext.getUserSession().isLoggedIn()) {
                record.setUserId(appContext.getUserSession().getUserId());
            }
            service.saveGameRecord(record);
            GameRecord currentBest = bestRecordProperty.get();
            if (currentBest == null || flipCount < currentBest.getScore()) {
                GameRecord newBest = new GameRecord();
                newBest.setScore(flipCount);
                newBest.setDifficulty(difficulty);
                bestRecordProperty.set(newBest);
            }
        } catch (Exception e) {
            // 静默处理
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
                GameRecord best = service.getBestScore(userId, "memoryFlip", difficulty);
                bestRecordProperty.set(best);
            } else {
                bestRecordProperty.set(null);
            }
        } catch (Exception e) {
            bestRecordProperty.set(null);
        }
    }

    // Getters
    public int getGridSize() { return gridSize; }
    public String getDifficulty() { return difficulty; }
    public String getDifficultyName() { return "hard".equals(difficulty) ? "6x6" : "4x4"; }
    public int getTotalPairs() { return totalPairs; }
    public List<String> getCardSymbols() { return cardSymbols; }
    public boolean isCardMatched(int index) { return matchedCards.get(index); }
    public boolean isCardFaceUp(int index) { return faceUpCards.get(index); }
    public boolean isProcessing() { return processing; }
    public String getCardSymbol(int index) { return cardSymbols.get(index); }

    // Property getters
    public SimpleIntegerProperty flipCountProperty() { return flipCountProperty; }
    public SimpleIntegerProperty matchedPairsProperty() { return matchedPairsProperty; }
    public SimpleIntegerProperty totalPairsProperty() { return totalPairsProperty; }
    public SimpleBooleanProperty gameOverProperty() { return gameOverProperty; }
    public SimpleStringProperty timeProperty() { return timeProperty; }
    public SimpleObjectProperty<GameRecord> bestRecordProperty() { return bestRecordProperty; }

    /**
     * 翻牌结果
     */
    public static class FlipResult {
        public int index;
        public String symbol;
        public boolean isMatch;
        public boolean isFirstOfPair;
        public boolean isComplete;
    }
}
