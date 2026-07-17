package com.qiyunge.application.service;

import com.qiyunge.domain.entity.GameRecord;
import com.qiyunge.infrastructure.repository.GameRecordRepository;

import java.util.List;

/**
 * 百趣园娱乐服务：处理游戏记录的保存与查询，统一通过 Repository 层访问数据库。
 */
public class EntertainmentService {

    private final GameRecordRepository gameRecordRepository;

    public EntertainmentService(GameRecordRepository gameRecordRepository) {
        this.gameRecordRepository = gameRecordRepository;
    }

    public void saveGameRecord(GameRecord record) {
        gameRecordRepository.save(record);
    }

    public List<GameRecord> getGameRecords(int userId, String gameType) {
        return gameRecordRepository.findByUserAndType(userId, gameType);
    }

    public GameRecord getBestScore(int userId, String gameType, String difficulty) {
        return gameRecordRepository.findBestScore(userId, gameType, difficulty);
    }

    public GameRecord getBestTime(int userId, String gameType, String difficulty) {
        return gameRecordRepository.findBestTime(userId, gameType, difficulty);
    }

    public int getTotalGameCount(int userId) {
        return gameRecordRepository.countByUser(userId);
    }

    public int getGameCountByType(int userId, String gameType) {
        return gameRecordRepository.countByUserAndType(userId, gameType);
    }

    public List<GameRecord> getRecentRecords(int userId, int limit) {
        return gameRecordRepository.findRecentByUser(userId, limit);
    }
}
