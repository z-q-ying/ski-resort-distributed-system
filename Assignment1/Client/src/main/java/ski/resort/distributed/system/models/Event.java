package ski.resort.distributed.system.models;

import io.swagger.client.model.LiftRide;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
public class Event {
  private final Integer skierID;
  private final Integer resortID;
  private final Integer liftID;
  private final String seasonId;
  private final String dayId;
  private final Integer time;
  private final LiftRide liftRide;
  @Setter private Integer responseCode;
}
