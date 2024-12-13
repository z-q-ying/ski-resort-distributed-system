package ski.resort.distributed.system.constants;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Stores user configuration options such as server mode (local or remote) and logging preferences
 * (whether to log individual events).
 */
public class UserConfig {

  /** 1. Base path or URL for API requests */
  private static final String HOST_IP = Dotenv.load().get("MY_EC2_IP");
  public static final String BASE_PATH = "http://" + HOST_IP + ":8080/Server_war";
  // public static final String BASE_PATH = "http://" + HOST_IP + ":8080/Server_war_exploded"; // for local dev

  /** 2. Set number of posting threads */
  public static final int NUM_OF_POST_THREADS = 112;

  /** 3. Whether to note down latency for each post */
  public static final boolean RECORD_POSTS_IN_CSV = true;

  public UserConfig() {}
}
