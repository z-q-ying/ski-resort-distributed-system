package ski.resort.distributed.system.dal;

import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class AbstractDao {
  protected static BasicDataSource dataSource;

  public AbstractDao() {
    dataSource = DBCPConnectionPool.getDataSource();
  }

  protected void closeQuietly(Connection conn, PreparedStatement ps, ResultSet resultSet) {
    try {
      if (conn != null) {
        conn.close();
      }
      if (ps != null) {
        ps.close();
      }
      if (resultSet != null) {
        resultSet.close();
      }
    } catch (SQLException e) {
      System.err.println("!!! Error closing resources: : " + e.getMessage());
    }
  }
}
