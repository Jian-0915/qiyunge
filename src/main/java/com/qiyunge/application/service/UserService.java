package com.qiyunge.application.service;

import com.qiyunge.domain.entity.User;
import com.qiyunge.infrastructure.repository.UserRepository;

import java.util.Optional;

/**
 * 用户业务服务：封装用户资料查询与修改，禁止 UI 层直接访问 Repository。
 */
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findById(int userId) {
        return userRepository.findById(userId);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public boolean updateDisplayName(int userId, String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) return false;
        return userRepository.updateDisplayName(userId, displayName.trim());
    }

    public boolean updateAvatarColor(int userId, String avatarColor) {
        if (avatarColor == null || avatarColor.trim().isEmpty()) return false;
        return userRepository.updateAvatarColor(userId, avatarColor.trim());
    }

    public boolean updateLastLoginAt(int userId) {
        return userRepository.updateLastLoginAt(userId);
    }
}
