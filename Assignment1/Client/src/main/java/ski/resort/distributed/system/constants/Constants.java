package ski.resort.distributed.system.constants;

public class Constants {

  // HTTP Status
  public static final int HTTP_OK = 200;
  public static final int HTTP_CREATED = 201;
  public static final int HTTP_CLIENT_ERROR = 400;
  public static final int HTTP_SERVER_ERROR = 500;

  // Retry config
  public static final int MAX_RETRIES = 5;

  // Parameters for testing
  public static final int TOTAL_POSTS = 200000;
  public static final int INITIAL_THREAD_COUNT = 32;
  public static final int INITIAL_POSTS_PER_THREAD = 1000;

  // Constants for skier lift ride data generation
  public static final int MIN_SKIER_ID = 1;
  public static final int MAX_SKIER_ID = 100000;

  public static final int MIN_RESORT_ID = 1;
  public static final int MAX_RESORT_ID = 10;

  public static final int MIN_LIFT_ID = 1;
  public static final int MAX_LIFT_ID = 40;

  public static final String SEASON_ID = "2024"; // Fixed season ID

  public static final String DAY_ID = "1"; // Fixed day ID

  public static final int MIN_TIME = 1;
  public static final int MAX_TIME = 360;

  private Constants() {}
}
