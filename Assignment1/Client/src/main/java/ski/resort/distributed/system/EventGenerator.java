package ski.resort.distributed.system;

import io.swagger.client.model.LiftRide;
import ski.resort.distributed.system.models.Event;

import java.util.Random;
import java.util.concurrent.BlockingQueue;

import static ski.resort.distributed.system.constants.Constants.*;

public class EventGenerator implements Runnable {

  private static final Random random = new Random();

  private final Integer eventCount;
  private final BlockingQueue<Event> eventBlockingQueue;

  public EventGenerator(Integer eventCount, BlockingQueue<Event> eventBlockingQueue) {
    this.eventCount = eventCount;
    this.eventBlockingQueue = eventBlockingQueue;
  }

  @Override
  public void run() {
    for (int i = 0; i < eventCount; i++) {
      Event event = generateEvent();
      try {
        // put: inserts the element into this queue, waiting if needed for space to become available
        // offer: inserts the element if possible immediately; returns true/false; preferable to add
        // add: same as above but failing to insert an element result in throwing an exception
        eventBlockingQueue.put(event);
      } catch (InterruptedException e) {
        System.err.println("!!! Exception when putting events into the blocking queue");
        System.err.println("!!! " + e.getMessage());
        throw new RuntimeException(e);
      }
    }
  }

  private static Event generateEvent() {
    final int liftID = random.nextInt(MAX_LIFT_ID - MIN_LIFT_ID + 1) + MIN_LIFT_ID;
    final Integer time = random.nextInt(MAX_TIME - MIN_TIME + 1) + MIN_TIME;
    final LiftRide liftRide = new LiftRide().time(time).liftID(liftID);
    return Event.builder()
        .skierID(random.nextInt(MAX_SKIER_ID - MIN_SKIER_ID + 1) + MIN_SKIER_ID)
        .resortID(random.nextInt(MAX_RESORT_ID - MIN_RESORT_ID + 1) + MIN_SKIER_ID)
        .liftID(liftID)
        .seasonId(SEASON_ID)
        .dayId(DAY_ID)
        .time(time)
        .liftRide(liftRide)
        .build();
  }
}
