package ski.resort.distributed.system.dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static ski.resort.distributed.system.utils.Constants.TABLE;

public class SkierResortTotalsDao extends AbstractDao {
  private static final String COL_LABEL_TOTAL_VERTICAL = "totalVertical";
  private static final String SELECT_QUERY =
      String.format(
          "SELECT SUM(liftID) * 10 AS totalVertical  FROM %s "
              + "WHERE skierID = ? AND resortID = ? AND seasonID = ?",
          TABLE);

  public SkierResortTotalsDao() {}

  public int getTotalVertical(int skierID, int resortID, int seasonID) {
    Connection conn = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    int totalVertical = 0;

    try {
      // use JSONObject methods to get the metadata
      conn = dataSource.getConnection();
      preparedStatement = conn.prepareStatement(SELECT_QUERY);
      preparedStatement.setInt(1, skierID);
      preparedStatement.setInt(2, resortID);
      preparedStatement.setInt(3, seasonID);

      // execute insert SQL statement
      resultSet = preparedStatement.executeQuery();
      if (resultSet.next()) {
        totalVertical = resultSet.getInt(COL_LABEL_TOTAL_VERTICAL);
      }
    } catch (SQLException e) {
      System.err.println("!!! Error fetching total vertical: " + e.getMessage());
    }

    closeQuietly(conn, preparedStatement, resultSet);
    return totalVertical;
  }
}
