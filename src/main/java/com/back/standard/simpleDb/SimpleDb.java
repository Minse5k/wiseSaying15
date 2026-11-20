package com.back.standard.simpleDb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC 를 감싼 유틸리티.
 * 커넥션은 스레드마다 하나씩(ThreadLocal) 열어서 쓰므로 여러 스레드에서 동시에 써도 안전하다.
 */
public class SimpleDb {
    private final String url;
    private final String username;
    private final String password;

    private final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();

    private boolean devMode = false;

    public SimpleDb(String host, String username, String password, String dbName) {
        this(host, 3306, username, password, dbName);
    }

    public SimpleDb(String host, int port, String username, String password, String dbName) {
        this.url = "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&connectionTimeZone=LOCAL&forceConnectionTimeZoneToSession=true"
                .formatted(host, port, dbName);
        this.username = username;
        this.password = password;
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    public Sql genSql() {
        return new Sql(this);
    }

    // DDL/DML 실행. 영향받은 행 수를 돌려준다.
    public int run(String sql, Object... params) {
        return update(sql, List.of(params));
    }

    public void startTransaction() {
        try {
            getConnection().setAutoCommit(false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void commit() {
        endTransaction(true);
    }

    public void rollback() {
        endTransaction(false);
    }

    private void endTransaction(boolean commit) {
        try {
            Connection connection = getConnection();
            if (commit) connection.commit();
            else connection.rollback();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // 현재 스레드의 커넥션을 닫는다.
    public void close() {
        Connection connection = connectionHolder.get();
        if (connection == null) return;

        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            connectionHolder.remove();
        }
    }

    private Connection getConnection() {
        try {
            Connection connection = connectionHolder.get();
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(url, username, password);
                connectionHolder.set(connection);
            }
            return connection;
        } catch (SQLException e) {
            throw new RuntimeException("DB 연결 실패: " + url, e);
        }
    }

    int update(String sql, List<Object> params) {
        logSql(sql, params);

        try (PreparedStatement stmt = prepare(sql, params, false)) {
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    long insert(String sql, List<Object> params) {
        logSql(sql, params);

        try (PreparedStatement stmt = prepare(sql, params, true)) {
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    List<Map<String, Object>> selectRows(String sql, List<Object> params) {
        logSql(sql, params);

        try (PreparedStatement stmt = prepare(sql, params, false);
             ResultSet rs = stmt.executeQuery()) {
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            List<Map<String, Object>> rows = new ArrayList<>();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                rows.add(row);
            }

            return rows;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private PreparedStatement prepare(String sql, List<Object> params, boolean returnKeys) throws SQLException {
        PreparedStatement stmt = returnKeys
                ? getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
                : getConnection().prepareStatement(sql);

        for (int i = 0; i < params.size(); i++) {
            stmt.setObject(i + 1, params.get(i));
        }

        return stmt;
    }

    private void logSql(String sql, List<Object> params) {
        if (!devMode) return;

        System.out.println("== rawSql ==");
        System.out.println(sql.trim());
        if (!params.isEmpty()) System.out.println("== params == " + Arrays.toString(params.toArray()));
    }
}
