package ski.resort.distributed.system.dal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.dbcp2.BasicDataSource;

public class DBCPDataSource {

    // Database connection details: Remote
    private static final String HOST_NAME = "database-mysql-1.cn68i2q00b8v.us-west-2.rds.amazonaws.com";
    private static final String PORT = "3306";
    private static final String DATABASE = "ski_resort_db";
    private static final String TABLE = "liftRides";
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "Dev159*Q=";

    private static final String CREATE_TABLE_QUERY = String.format(
            "CREATE TABLE IF NOT EXISTS %s (" +
                    "id INT NOT NULL AUTO_INCREMENT, " +
                    "skierID INT NOT NULL, " +
                    "resortID INT NOT NULL, " +
                    "seasonID INT NOT NULL, " +
                    "dayID INT NOT NULL, " +
                    "time INT NOT NULL, " +
                    "liftID INT NOT NULL, " +
                    "PRIMARY KEY (id)" +
                    ") ENGINE=InnoDB;",
            TABLE
    );

    private static final String CREATE_DB_QUERY = String.format("CREATE DATABASE IF NOT EXISTS %s", DATABASE);

    private static BasicDataSource dataSource;

    static {
        // Initialize the connection pool
        dataSource = new BasicDataSource();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        String url = String.format("jdbc:mysql://%s:%s/%s?serverTimezone=UTC", HOST_NAME, PORT, DATABASE);
        dataSource.setUrl(url);
        dataSource.setUsername(USERNAME);
        dataSource.setPassword(PASSWORD);
        dataSource.setInitialSize(50);
        dataSource.setMaxTotal(59); // max_connections is 60
        dataSource.setMaxIdle(50);
        dataSource.setMinIdle(50);
    }

    public static void createDatabaseAndTableIfNotExists() throws SQLException {
        String urlWithoutDb = String.format("jdbc:mysql://%s:%s/?serverTimezone=UTC", HOST_NAME, PORT);
        try (Connection conn = DriverManager.getConnection(urlWithoutDb, USERNAME, PASSWORD);
             Statement stmt = conn.createStatement()) {

            // Create database if not exists
            stmt.executeUpdate(CREATE_DB_QUERY);

            // Connect to the database to create table
            String urlWithDb = String.format("jdbc:mysql://%s:%s/%s?serverTimezone=UTC", HOST_NAME, PORT, DATABASE);
            try (Connection dbConn = DriverManager.getConnection(urlWithDb, USERNAME, PASSWORD);
                 Statement dbStmt = dbConn.createStatement()) {
                dbStmt.executeUpdate(CREATE_TABLE_QUERY);
            }
        }
    }

    public static BasicDataSource getDataSource() {
        return dataSource;
    }
}