package ski.resort.distributed.system.utils;

import io.github.cdimascio.dotenv.Dotenv;

public class Constants {
  private Constants() {}

  public static final Dotenv DOTENV = Dotenv.load();
  public static final String DATABASE = "ski_resort_db";
  public static final String TABLE = "liftRides";
  public static final String TABLE_B = "resortSkiersCount";

  // database configuration
  public static final int INITIAL_POOL_SIZE = 100;
  public static final int MAX_TOTAL_CONNECTIONS = 450; // 128 * 3 = 384 threads
  public static final int MAX_IDLE_CONNECTIONS = 300;
  public static final int MIN_IDLE_CONNECTIONS = 100;
  public static final int MAX_WAIT_MILLIS = 2000; // wait time when connection exhausts
}
