package com.github.ipantazi.carsharing.util.controller;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

public class DatabaseTestUtil {
    private DatabaseTestUtil() {
    }

    public static void executeSqlScript(DataSource dataSource, String... scripts) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(true);
            for (String path : scripts) {
                ScriptUtils.executeSqlScript(connection, new ClassPathResource(path));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
