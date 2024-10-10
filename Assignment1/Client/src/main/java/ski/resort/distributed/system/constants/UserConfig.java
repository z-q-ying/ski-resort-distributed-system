package ski.resort.distributed.system.constants;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Stores user configuration options such as server mode (local or remote) and logging preferences
 * (whether to log individual events).
 */
public class UserConfig {

  /** 1. Base path or URL for API requests */

  // (a) Define the local base path
  private static final String LOCAL_BASE_PATH = "http://localhost:8080/Server_war_exploded";

  // (b) Define the remote base path with the correct HOST_IP
  private static final String HOST_IP = Dotenv.load().get("MY_EC2_IP");
  private static final String REMOTE_BASE_PATH = "http://" + HOST_IP + ":8080/Server_war";

  // (c) Toggle between local and remote base path: true for remote, false for local
  public static final boolean USE_REMOTE = true;
  public static final String BASE_PATH = USE_REMOTE ? REMOTE_BASE_PATH : LOCAL_BASE_PATH;

  /** 2. Whether to note down latency for each post */
  public static final boolean RECORD_POSTS_IN_CSV = false;

  public UserConfig() {}
}
