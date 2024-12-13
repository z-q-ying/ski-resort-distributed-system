package ski.resort.distributed.system.dal;

import org.apache.commons.dbcp2.BasicDataSource;
import ski.resort.distributed.system.utils.Constants;

public class DBCPConnectionPool {

  // Database connection details
  private static final String HOST_NAME = Constants.DOTENV.get("RDS_HOST");
  private static final String PORT = Constants.DOTENV.get("RDS_PORT");
  private static final String USERNAME = Constants.DOTENV.get("RDS_USER");
  private static final String PASSWORD = Constants.DOTENV.get("RDS_PW");

  private static final String CREATE_TABLE_QUERY =
      String.format(
          "CREATE TABLE IF NOT EXISTS %s ("
              + "id INT NOT NULL AUTO_INCREMENT, "
              + "skierID INT NOT NULL, "
              + "resortID INT NOT NULL, "
              + "seasonID INT NOT NULL, "
              + "dayID INT NOT NULL, "
              + "time INT NOT NULL, "
              + "liftID INT NOT NULL, "
              + "PRIMARY KEY (id)"
              + ") ENGINE=InnoDB;",
          Constants.TABLE);

  private static final String CREATE_DB_QUERY =
      String.format("CREATE DATABASE IF NOT EXISTS %s", Constants.DATABASE);

  private static BasicDataSource dataSource;

  public static synchronized void init() {
    // make sure only one collection pool exists
    if (dataSource != null) {
      return; // already initialized
    }

    // initialize the connection pool
    dataSource = new BasicDataSource();
    try {
      Class.forName("com.mysql.cj.jdbc.Driver");
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Can not find MySQL JDBC driver class", e);
    }

    String url =
        String.format(
            "jdbc:mysql://%s:%s/%s?readOnly=true&serverTimezone=UTC", HOST_NAME, PORT, Constants.DATABASE);
    dataSource.setUrl(url);
    dataSource.setUsername(USERNAME);
    dataSource.setPassword(PASSWORD);

    // connection pool config
    dataSource.setInitialSize(Constants.INITIAL_POOL_SIZE);
    dataSource.setMaxTotal(Constants.MAX_TOTAL_CONNECTIONS);
    dataSource.setMaxIdle(Constants.MAX_IDLE_CONNECTIONS);
    dataSource.setMinIdle(Constants.MIN_IDLE_CONNECTIONS);
    dataSource.setMaxWaitMillis(Constants.MAX_WAIT_MILLIS);
  }

  public static BasicDataSource getDataSource() {
    return dataSource;
  }

  public static void close() throws Exception {
    if (dataSource != null) {
      dataSource.close();
    }
  }
}
