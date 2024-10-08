package ski.resort.distributed.system.models;

import io.swagger.client.api.SkiersApi;
import lombok.Builder;
import lombok.Getter;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

@Builder
@Getter
public class WorkerParam {
  private final BlockingQueue<Event> eventBlockingQueue;
  private final CountDownLatch countDownLatch;
  private final AtomicInteger successfulRequests;
  private final AtomicInteger failedRequests;
  private final SkiersApi skiersApi;
  private final Integer numOfRequests;
}
