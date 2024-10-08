package ski.resort.distributed.system.models;

import io.swagger.client.model.LiftRide;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Event {
  private Integer skierID;
  private Integer resortID;
  private Integer liftID;
  private String seasonId;
  private String dayId;
  private Integer time;
  private LiftRide liftRide;
}
