package example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

@Component
public class SequenceInitializer implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(SequenceInitializer.class);

    private final DataSource dataSource;

    public SequenceInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        Map<String, String> tablesToSync = Map.of(
                "Employee", "employee_id",
                "Account", "account_id"
        );

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            for (Map.Entry<String, String> entry : tablesToSync.entrySet()) {
                String table = entry.getKey();
                String idColumn = entry.getValue();

                try {
                    String sql = "IF EXISTS (SELECT 1 FROM " + table + ") " +
                            "DBCC CHECKIDENT ('" + table + "', RESEED, (SELECT MAX(" + idColumn + ") FROM " + table + ")) " +
                            "ELSE DBCC CHECKIDENT ('" + table + "', RESEED, 0)";

                    log.info("Syncing IDENTITY for table {} (ID: {}) with SQL: {}", table, idColumn, sql);
                    stmt.execute(sql);
                } catch (SQLException ex) {
                    log.warn("Could not sync IDENTITY for table {} (ID: {}): {}", table, idColumn, ex.getMessage());
                }
            }
        } catch (SQLException e) {
            log.error("Failed to obtain DB connection to sync sequences: {}", e.getMessage());
        }
    }
}