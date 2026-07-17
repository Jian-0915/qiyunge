package com.qiyunge.infrastructure.database;

import com.qiyunge.infrastructure.storage.AppStorage;
import org.flywaydb.core.Flyway;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 数据库连接管理器：每次操作新建连接，禁止共享 Connection。
 * 提供 withConnection / withTransaction 封装，确保连接正确关闭。
 */
public class DatabaseManager {

    private final String jdbcUrl;

    public DatabaseManager(AppStorage appStorage) {
        this.jdbcUrl = "jdbc:sqlite:" + appStorage.getDatabasePath().toAbsolutePath().toString()
            + "?journal_mode=WAL&synchronous=NORMAL&foreign_keys=on";
    }

    public void initialize() {
        long start = System.currentTimeMillis();

        // 1. 预热 JDBC 驱动（触发原生库解压，只在首次运行慢）
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found", e);
        }
        System.out.println("[DB] JDBC driver loaded in " + (System.currentTimeMillis() - start) + "ms");

        // 2. Flyway 迁移
        long flywayStart = System.currentTimeMillis();
        try {
            Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl, null, null)
                .locations("classpath:db/migration")
                .load();
            flyway.repair();
            var result = flyway.migrate();
            System.out.println("[DB] Flyway migration completed in " + (System.currentTimeMillis() - flywayStart) + "ms, " + result.migrationsExecuted + " migrations applied.");
        } catch (Exception e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            throw new RuntimeException("Database initialization failed", e);
        }

        // 3. 应用性能优化 PRAGMA（仅首次创建数据库时需要，WAL 模式持久化后自动生效）
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA synchronous=NORMAL");
            stmt.execute("PRAGMA temp_store=MEMORY");
            stmt.execute("PRAGMA mmap_size=67108864");
            stmt.execute("PRAGMA cache_size=-2000");
        } catch (SQLException e) {
            System.err.println("[DB] PRAGMA setup warning: " + e.getMessage());
        }

        System.out.println("[DB] Total initialization in " + (System.currentTimeMillis() - start) + "ms");
    }

    /**
     * 每次调用创建新连接。调用方负责关闭。
     */
    public Connection newConnection() {
        try {
            Connection conn = DriverManager.getConnection(jdbcUrl);
            conn.setAutoCommit(true);
            return conn;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create database connection", e);
        }
    }

    /**
     * 执行单个读操作，自动关闭连接。
     */
    public <T> T withConnection(SqlFunction<Connection, T> action) {
        try (Connection conn = newConnection()) {
            return action.apply(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Database operation failed", e);
        }
    }

    /**
     * 执行单个写操作，自动关闭连接。
     */
    public void withConnection(SqlConsumer<Connection> action) {
        try (Connection conn = newConnection()) {
            action.accept(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Database operation failed", e);
        }
    }

    /**
     * 在事务中执行操作，自动提交/回滚。
     */
    public <T> T withTransaction(SqlFunction<Connection, T> action) {
        try (Connection conn = newConnection()) {
            conn.setAutoCommit(false);
            try {
                T result = action.apply(conn);
                conn.commit();
                return result;
            } catch (Exception e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    RuntimeException ex2 = new RuntimeException("Failed to rollback transaction", rollbackEx);
                    ex2.addSuppressed(e);
                    throw ex2;
                }
                throw new RuntimeException("Transaction failed", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database transaction failed", e);
        }
    }

    public void withTransaction(SqlConsumer<Connection> action) {
        try (Connection conn = newConnection()) {
            conn.setAutoCommit(false);
            try {
                action.accept(conn);
                conn.commit();
            } catch (Exception e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    RuntimeException ex2 = new RuntimeException("Failed to rollback transaction", rollbackEx);
                    ex2.addSuppressed(e);
                    throw ex2;
                }
                throw new RuntimeException("Transaction failed", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database transaction failed", e);
        }
    }

    public void close() {
        // SQLite connections are per-operation now, nothing to close at manager level
    }

    /**
     * 允许抛出 SQLException 的 Function。
     */
    @FunctionalInterface
    public interface SqlFunction<T, R> {
        R apply(T t) throws SQLException;
    }

    /**
     * 允许抛出 SQLException 的 Consumer。
     */
    @FunctionalInterface
    public interface SqlConsumer<T> {
        void accept(T t) throws SQLException;
    }
}
