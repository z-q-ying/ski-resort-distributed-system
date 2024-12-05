package ski.resort.distributed.system.utils;

import io.github.cdimascio.dotenv.Dotenv;

public class Constants {
  private Constants() {}

  public static final Dotenv DOTENV = Dotenv.load();

  public static final String OPERATION_ID = "operationId";

  public static final String OID_GET_RESORT_SKIERS_DAY = "getResortSkiersDay";
}
