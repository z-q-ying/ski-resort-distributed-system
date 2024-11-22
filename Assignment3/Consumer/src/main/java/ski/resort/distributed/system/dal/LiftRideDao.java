package ski.resort.distributed.system.dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.commons.dbcp2.BasicDataSource;
import org.json.JSONObject;

public class LiftRideDao {

    private static final String TABLE = "liftRides";

    private static final String INSERT_QUERY = String.format("INSERT INTO %s (skierId, resortId, seasonId, dayId, time, liftId) " + "VALUES (?,?,?,?,?,?)", TABLE);

    private static BasicDataSource dataSource;

    public LiftRideDao() {
        dataSource = DBCPDataSource.getDataSource();
    }

    public void createLiftRide(final JSONObject liftRide) {
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        try {
            conn = dataSource.getConnection();
            preparedStatement = conn.prepareStatement(INSERT_QUERY);

            // use JSONObject methods to get the metadata
            preparedStatement.setInt(1, liftRide.getInt("skierID"));
            preparedStatement.setInt(2, liftRide.getInt("resortID"));
            preparedStatement.setInt(3, liftRide.getInt("seasonID"));
            preparedStatement.setInt(4, liftRide.getInt("dayID"));
            preparedStatement.setInt(5, liftRide.getInt("time"));
            preparedStatement.setInt(6, liftRide.getInt("liftID"));

            // execute insert SQL statement
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
}