package ski.resort.distributed.system.dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static ski.resort.distributed.system.utils.Constants.TABLE_B;

public class ResortSkiersDayDao extends AbstractDao {

  private static final String COL_LABEL_UNIQUE_SKIERS = "uniqueSkiers";

  private static final String SELECT_QUERY =
      String.format(
          "SELECT uniqueSkiers FROM %s WHERE resortId = ? AND seasonId = ? AND dayId = ?",
          TABLE_B);

  public ResortSkiersDayDao() {}

  public int getUniqueSkiers(int resortID, int seasonID, int dayID) {
    Connection conn = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    int uniqueSkiers = 0;

    try {
      // use JSONObject methods to get the metadata
      conn = dataSource.getConnection();
      preparedStatement = conn.prepareStatement(SELECT_QUERY);
      preparedStatement.setInt(1, resortID);
      preparedStatement.setInt(2, seasonID);
      preparedStatement.setInt(3, dayID);

      // execute insert SQL statement
      resultSet = preparedStatement.executeQuery();
      if (resultSet.next()) {
        uniqueSkiers = resultSet.getInt(COL_LABEL_UNIQUE_SKIERS);
      }
    } catch (SQLException e) {
      System.err.println("Error fetching unique skiers: " + e.getMessage());
    }

    closeQuietly(conn, preparedStatement, resultSet);
    return uniqueSkiers;
  }
}
