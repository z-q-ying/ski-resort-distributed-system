package ski.resort.distributed.system.models;

import lombok.Builder;
import lombok.Getter;

/** Encapsulates event log data, which is to be written to a CSV file. */
@Builder
@Getter
public class EventLog {
  private final String type;
  private final Integer responseCode;
  private final long startTime;
  private final long endTime;
}
