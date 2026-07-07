package com.qiyunge.application.service;

import com.qiyunge.infrastructure.repository.FavoriteSongRepository;
import com.qiyunge.infrastructure.repository.GalleryImageRepository;
import com.qiyunge.infrastructure.repository.PlayHistoryRepository;
import com.qiyunge.infrastructure.repository.PlaylistRepository;
import com.qiyunge.infrastructure.repository.RegistrationRequestRepository;
import com.qiyunge.infrastructure.repository.SongRepository;
import com.qiyunge.infrastructure.repository.UserImagePreferenceRepository;
import com.qiyunge.infrastructure.repository.UserRepository;

/**
 * 统计服务：为 Dashboard 等页面聚合跨表统计数字。
 * 通过 Repository 层访问数据库，不直接使用 DatabaseManager。
 */
public class StatisticsService {

    private final UserRepository userRepository;
    private final SongRepository songRepository;
    private final FavoriteSongRepository favoriteSongRepository;
    private final PlayHistoryRepository playHistoryRepository;
    private final PlaylistRepository playlistRepository;
    private final RegistrationRequestRepository registrationRequestRepository;
    private final GalleryImageRepository galleryImageRepository;
    private final UserImagePreferenceRepository userImagePreferenceRepository;

    public StatisticsService(UserRepository userRepository, SongRepository songRepository,
                              FavoriteSongRepository favoriteSongRepository,
                              PlayHistoryRepository playHistoryRepository,
                              PlaylistRepository playlistRepository,
                              RegistrationRequestRepository registrationRequestRepository,
                              GalleryImageRepository galleryImageRepository,
                              UserImagePreferenceRepository userImagePreferenceRepository) {
        this.userRepository = userRepository;
        this.songRepository = songRepository;
        this.favoriteSongRepository = favoriteSongRepository;
        this.playHistoryRepository = playHistoryRepository;
        this.playlistRepository = playlistRepository;
        this.registrationRequestRepository = registrationRequestRepository;
        this.galleryImageRepository = galleryImageRepository;
        this.userImagePreferenceRepository = userImagePreferenceRepository;
    }

    public int countActiveUsers() {
        return userRepository.countActive();
    }

    public int countPendingRequests() {
        return registrationRequestRepository.countPending();
    }

    public int countSongs() {
        return songRepository.count();
    }

    public int countGalleryImages() {
        return galleryImageRepository.countAll();
    }

    public int countPlaylistsByUser(int userId) {
        return playlistRepository.countByUser(userId);
    }

    public int countFavoritesByUser(int userId) {
        return favoriteSongRepository.countByUser(userId);
    }

    public int countPlayHistoryByUser(int userId) {
        return playHistoryRepository.countByUser(userId);
    }

    public int countImagesByUser(int userId) {
        return userImagePreferenceRepository.countByUser(userId);
    }
}
