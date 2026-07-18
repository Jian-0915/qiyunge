package com.qiyunge.test;

import com.qiyunge.application.service.PomodoroService;
import com.qiyunge.application.service.PomodoroService.*;
import com.qiyunge.domain.entity.PomodoroTask;
import com.qiyunge.domain.entity.PomodoroTaskTemplate;
import com.qiyunge.infrastructure.database.DatabaseManager;
import com.qiyunge.infrastructure.repository.AchievementRepository;
import com.qiyunge.infrastructure.repository.PomodoroSessionRepository;
import com.qiyunge.infrastructure.repository.PomodoroTaskRepository;
import com.qiyunge.infrastructure.repository.PomodoroTaskTemplateRepository;
import com.qiyunge.infrastructure.storage.AppStorage;
import com.qiyunge.infrastructure.storage.ConfigStorage;
import com.qiyunge.infrastructure.util.DateTimeUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

public class PomodoroServiceTest {

    private static int passed = 0;
    private static int failed = 0;
    private static List<String> failures = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        System.out.println("=".repeat(60));
        System.out.println("  PomodoroService 后端接口测试");
        System.out.println("=".repeat(60));

        Path testDir = Paths.get(System.getProperty("java.io.tmpdir"), "qiyunge_pomodoro_test_" + System.currentTimeMillis());
        Files.createDirectories(testDir);
        System.out.println("[SETUP] 测试目录: " + testDir);

        try {
            TestAppStorage testStorage = new TestAppStorage(testDir);
            testStorage.ensureDirectories();

            DatabaseManager dbManager = new DatabaseManager(testStorage);
            dbManager.initialize();

            ConfigStorage configStorage = new ConfigStorage(testStorage);

            PomodoroSessionRepository sessionRepo = new PomodoroSessionRepository(dbManager);
            PomodoroTaskRepository taskRepo = new PomodoroTaskRepository(dbManager);
            PomodoroTaskTemplateRepository templateRepo = new PomodoroTaskTemplateRepository(dbManager);
            AchievementRepository achievementRepo = new AchievementRepository(dbManager);

            PomodoroService service = new PomodoroService(sessionRepo, taskRepo, templateRepo, achievementRepo, configStorage);

            int testUserId = 1;

            System.out.println("\n--- 1. 设置管理测试 ---");
            testSettings(service);

            System.out.println("\n--- 2. 任务管理测试 ---");
            testTasks(service, testUserId);

            System.out.println("\n--- 3. 任务模板测试 ---");
            testTemplates(service, testUserId);

            System.out.println("\n--- 4. 会话创建测试 ---");
            testSessions(service, testUserId);

            System.out.println("\n--- 5. 统计数据测试 ---");
            testStatistics(service, testUserId);

            System.out.println("\n--- 6. 成就系统测试 ---");
            testAchievements(service, testUserId, achievementRepo);

            System.out.println("\n--- 7. 预计完成时间测试 ---");
            testEstimatedFinishTime(service, testUserId);

            System.out.println("\n" + "=".repeat(60));
            System.out.println("  测试结果: " + passed + " 通过, " + failed + " 失败");
            if (!failures.isEmpty()) {
                System.out.println("  失败项:");
                for (String f : failures) {
                    System.out.println("    - " + f);
                }
            }
            System.out.println("=".repeat(60));

            dbManager.close();
            configStorage.shutdown();

        } finally {
            deleteRecursively(testDir.toFile());
        }

        System.exit(failed > 0 ? 1 : 0);
    }

    private static void testSettings(PomodoroService service) {
        try {
            PomodoroSettings defaults = service.loadSettings();
            assertEq("默认专注时长", 25, defaults.getFocusMinutes());
            assertEq("默认短休息", 5, defaults.getShortBreakMinutes());
            assertEq("默认长休息", 15, defaults.getLongBreakMinutes());
            assertEq("默认长休息间隔", 4, defaults.getLongBreakInterval());
            assertEq("默认自动开始", false, defaults.isAutoStartNext());
            assertEq("默认窗口置顶", false, defaults.isAlwaysOnTop());

            PomodoroSettings modified = service.loadSettings();
            modified.setFocusMinutes(30);
            modified.setShortBreakMinutes(8);
            modified.setLongBreakMinutes(20);
            modified.setAutoStartNext(true);
            modified.setSoundType("woodfish");
            modified.setSoundVolume(80);
            modified.setFocusVolume(65);
            modified.setBreakVolume(45);
            modified.setBreakMusicBehavior(BreakMusicBehavior.CONTINUE);
            service.saveSettings(modified);

            PomodoroSettings reloaded = service.loadSettings();
            assertEq("保存后专注时长", 30, reloaded.getFocusMinutes());
            assertEq("保存后短休息", 8, reloaded.getShortBreakMinutes());
            assertEq("保存后长休息", 20, reloaded.getLongBreakMinutes());
            assertEq("保存后自动开始", true, reloaded.isAutoStartNext());
            assertEq("保存后提示音", "woodfish", reloaded.getSoundType());
            assertEq("保存后提示音量", 80, reloaded.getSoundVolume());
            assertEq("保存后专注音量", 65, reloaded.getFocusVolume());
            assertEq("保存后休息音量", 45, reloaded.getBreakVolume());
            assertEq("保存后休息音乐行为", BreakMusicBehavior.CONTINUE, reloaded.getBreakMusicBehavior());

            pass("设置管理测试全部通过");
        } catch (Exception e) {
            fail("设置管理测试异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testTasks(PomodoroService service, int userId) {
        try {
            List<PomodoroTask> initial = service.getTodayTasks(userId);
            assertEq("初始任务数", 0, initial.size());

            int task1Id = service.createTask(userId, "撰写需求文档", 3, "工作");
            int task2Id = service.createTask(userId, "阅读论文", 2, "学习");
            int task3Id = service.createTask(userId, "代码审查", 1, "工作");

            List<PomodoroTask> tasks = service.getTodayTasks(userId);
            assertEq("创建后任务数", 3, tasks.size());
            assertEq("任务1标题", "撰写需求文档", tasks.get(0).getTitle());
            assertEq("任务1预估番茄", 3, tasks.get(0).getEstimatedPomodoros());
            assertEq("任务1标签", "工作", tasks.get(0).getTag());

            PomodoroTask task = service.getTask(task1Id);
            assertNotNull("获取单个任务", task);
            assertEq("单个任务标题", "撰写需求文档", task.getTitle());

            service.toggleTaskComplete(task3Id);
            PomodoroTask completed = service.getTask(task3Id);
            assertEq("任务完成状态", 1, completed.getIsCompleted());

            service.deleteTask(task2Id);
            List<PomodoroTask> afterDelete = service.getTodayTasks(userId);
            assertEq("删除后任务数", 2, afterDelete.size());

            PomodoroTask updateTask = service.getTask(task1Id);
            updateTask.setTitle("撰写需求文档（修订版）");
            updateTask.setEstimatedPomodoros(5);
            service.updateTask(updateTask);
            PomodoroTask updated = service.getTask(task1Id);
            assertEq("更新后标题", "撰写需求文档（修订版）", updated.getTitle());
            assertEq("更新后预估", 5, updated.getEstimatedPomodoros());

            pass("任务管理测试全部通过");
        } catch (Exception e) {
            fail("任务管理测试异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testTemplates(PomodoroService service, int userId) {
        try {
            List<PomodoroTaskTemplate> initial = service.getTemplates(userId);
            assertEq("初始模板数", 0, initial.size());

            int t1Id = service.createTemplate(userId, "每日阅读", 2, "学习");
            int t2Id = service.createTemplate(userId, "晨间写作", 1, "写作");

            List<PomodoroTaskTemplate> templates = service.getTemplates(userId);
            assertEq("创建后模板数", 2, templates.size());

            int taskFromTemplateId = service.createTaskFromTemplate(userId, t1Id);
            assertTrue("从模板创建任务成功", taskFromTemplateId > 0);
            PomodoroTask created = service.getTask(taskFromTemplateId);
            assertEq("模板任务标题", "每日阅读", created.getTitle());
            assertEq("模板任务预估", 2, created.getEstimatedPomodoros());
            assertEq("模板任务标签", "学习", created.getTag());

            service.deleteTemplate(t2Id);
            List<PomodoroTaskTemplate> afterDelete = service.getTemplates(userId);
            assertEq("删除后模板数", 1, afterDelete.size());

            pass("任务模板测试全部通过");
        } catch (Exception e) {
            fail("任务模板测试异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testSessions(PomodoroService service, int userId) {
        try {
            String now = DateTimeUtil.DT_FMT.format(LocalDateTime.now());
            String later = DateTimeUtil.DT_FMT.format(LocalDateTime.now().plusMinutes(25));

            int taskId = service.createTask(userId, "专注测试任务", 2, "工作");

            int session1 = service.createSession(userId, taskId, 25, SessionType.FOCUS, now, later, true, "工作");
            assertTrue("创建专注会话成功", session1 > 0);

            int session2 = service.createSession(userId, null, 5, SessionType.SHORT_BREAK, later,
                DateTimeUtil.DT_FMT.format(LocalDateTime.now().plusMinutes(30)), true, null);
            assertTrue("创建休息会话成功", session2 > 0);

            PomodoroTask taskAfter = service.getTask(taskId);
            assertEq("任务已完成番茄数", 1, taskAfter.getCompletedPomodoros());

            int session3 = service.createSession(userId, taskId, 25, SessionType.FOCUS,
                DateTimeUtil.DT_FMT.format(LocalDateTime.now()),
                DateTimeUtil.DT_FMT.format(LocalDateTime.now().plusMinutes(25)),
                true, "工作");
            PomodoroTask taskAfter2 = service.getTask(taskId);
            assertEq("任务再次完成后番茄数", 2, taskAfter2.getCompletedPomodoros());

            pass("会话创建测试全部通过");
        } catch (Exception e) {
            fail("会话创建测试异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testStatistics(PomodoroService service, int userId) {
        try {
            DailyStats daily = service.getDailyStats(userId);
            assertTrue("今日专注分钟数 >= 50", daily.getFocusMinutes() >= 50);
            assertTrue("今日完成番茄数 >= 2", daily.getCompletedPomodoros() >= 2);
            assertTrue("今日连续天数 >= 1", daily.getStreakDays() >= 1);

            TotalStats total = service.getTotalStats(userId);
            assertTrue("总专注分钟数 >= 50", total.getTotalFocusMinutes() >= 50);
            assertTrue("总番茄数 >= 2", total.getTotalCompletedPomodoros() >= 2);
            assertTrue("当前连续 >= 1", total.getCurrentStreak() >= 1);

            Map<String, Integer> weekly = service.getWeeklyStats(userId);
            assertEq("一周7天数据", 7, weekly.size());

            Map<String, Integer> monthly = service.getMonthlyStats(userId);
            assertTrue("本月天数 > 0", monthly.size() > 0);

            pass("统计数据测试全部通过");
        } catch (Exception e) {
            fail("统计数据测试异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testAchievements(PomodoroService service, int userId, AchievementRepository achievementRepo) {
        try {
            boolean firstUnlocked = achievementRepo.isUnlocked(userId, "pomodoro_first");
            assertEq("初试锋芒已解锁", true, firstUnlocked);

            Map<String, String> titles = service.getAchievementTitles();
            assertEq("成就数", 12, titles.size());
            assertEq("初试锋芒中文名", "初试锋芒", titles.get("pomodoro_first"));

            List<UnlockResult> results = service.onFocusSessionComplete(userId);
            assertNotNull("成就检测返回非空", results);

            pass("成就系统测试全部通过");
        } catch (Exception e) {
            fail("成就系统测试异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testEstimatedFinishTime(PomodoroService service, int userId) {
        try {
            String time = service.calculateEstimatedFinishTime(userId);
            assertNotNull("预计完成时间非空", time);
            assertTrue("时间格式包含冒号", time.contains(":"));

            pass("预计完成时间测试通过");
        } catch (Exception e) {
            fail("预计完成时间测试异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void assertEq(String name, Object expected, Object actual) {
        if (Objects.equals(expected, actual)) {
            System.out.println("  [PASS] " + name + ": " + expected);
            passed++;
        } else {
            System.out.println("  [FAIL] " + name + ": 期望 " + expected + ", 实际 " + actual);
            failed++;
            failures.add(name);
        }
    }

    private static void assertNotNull(String name, Object obj) {
        if (obj != null) {
            System.out.println("  [PASS] " + name);
            passed++;
        } else {
            System.out.println("  [FAIL] " + name + ": 对象为空");
            failed++;
            failures.add(name);
        }
    }

    private static void assertTrue(String name, boolean condition) {
        if (condition) {
            System.out.println("  [PASS] " + name);
            passed++;
        } else {
            System.out.println("  [FAIL] " + name + ": 条件不成立");
            failed++;
            failures.add(name);
        }
    }

    private static void pass(String msg) {
        System.out.println("  [OK] " + msg);
    }

    private static void fail(String msg) {
        System.out.println("  [ERROR] " + msg);
        failed++;
        failures.add(msg);
    }

    private static void deleteRecursively(java.io.File file) {
        if (file.isDirectory()) {
            for (java.io.File f : file.listFiles()) {
                deleteRecursively(f);
            }
        }
        file.delete();
    }

    public static class TestAppStorage extends AppStorage {
        private final Path testDir;
        public TestAppStorage(Path testDir) {
            this.testDir = testDir;
        }
        @Override public Path getAppDataPath() { return testDir.resolve(".qiyunge"); }
        @Override public Path getDatabasePath() { return testDir.resolve(".qiyunge").resolve("test.db"); }
        @Override public Path getConfigPath() { return testDir.resolve(".qiyunge").resolve("config.json"); }
        @Override public Path getCachePath() { return testDir.resolve(".qiyunge").resolve("cache"); }
        @Override public Path getMusicCachePath() { return getCachePath().resolve("music"); }
        @Override public Path getMusicAudioPath() { return getMusicCachePath().resolve("audio"); }
        @Override public Path getMusicCoverPath() { return getMusicCachePath().resolve("covers"); }
        @Override public Path getMusicLyricPath() { return getMusicCachePath().resolve("lyrics"); }
        @Override public Path getGalleryCachePath() { return getCachePath().resolve("gallery"); }
        @Override public Path getFaceDataPath() { return getAppDataPath().resolve("face_data"); }
        @Override public Path getLogsPath() { return getAppDataPath().resolve("logs"); }
        @Override public Path getBackupsPath() { return getAppDataPath().resolve("backups"); }
        @Override public void ensureDirectories() {
            try {
                Files.createDirectories(getAppDataPath());
                Files.createDirectories(getCachePath());
                Files.createDirectories(getMusicCachePath());
                Files.createDirectories(getMusicAudioPath());
                Files.createDirectories(getMusicCoverPath());
                Files.createDirectories(getMusicLyricPath());
                Files.createDirectories(getGalleryCachePath());
                Files.createDirectories(getFaceDataPath());
                Files.createDirectories(getLogsPath());
                Files.createDirectories(getBackupsPath());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
