package ski.resort.distributed.system;

import io.swagger.client.ApiClient;
import io.swagger.client.api.SkiersApi;
import ski.resort.distributed.system.models.Event;
import ski.resort.distributed.system.models.EventLog;
import ski.resort.distributed.system.models.PostWorkerParam;
import ski.resort.distributed.system.runnables.EventGenerator;
import ski.resort.distributed.system.runnables.EventLogWorker;
import ski.resort.distributed.system.runnables.PostRequestWorker;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static ski.resort.distributed.system.constants.Constants.CSV_PATH;
import static ski.resort.distributed.system.constants.Constants.INITIAL_THREAD_COUNT;
import static ski.resort.distributed.system.constants.Constants.INITIAL_POSTS_PER_THREAD;
import static ski.resort.distributed.system.constants.Constants.TOTAL_POSTS;
import static ski.resort.distributed.system.constants.UserConfig.BASE_PATH;
import static ski.resort.distributed.system.constants.UserConfig.RECORD_POSTS_IN_CSV;
import static ski.resort.distributed.system.constants.UserConfig.USE_REMOTE;

public class Client {

  private static final AtomicInteger successfulRequests = new AtomicInteger(0);
  private static final AtomicInteger failedRequests = new AtomicInteger(0);
  private static final String path = BASE_PATH;
  private static final BlockingQueue<Event> events = new LinkedBlockingQueue<>();
  private static final BlockingQueue<EventLog> logs = new LinkedBlockingQueue<>();

  public static void main(String[] args) {

    printTestInfo();

    // Prepare to log events in a new thread when needed
    if (RECORD_POSTS_IN_CSV) {
      new Thread(new EventLogWorker(CSV_PATH, logs)).start();
    }

    // Start the lift event generator in a separate thread
    new Thread(new EventGenerator(TOTAL_POSTS, events)).start();

    try {
      // Phase 1
      final long start1 = System.currentTimeMillis();
      doPhaseOne();
      final long end1 = System.currentTimeMillis();
      final int successCnt1 = successfulRequests.get();
      final int failedCnt1 = failedRequests.get();
      printStat(start1, end1, successCnt1, failedCnt1);

      // Phase 2
      final long start2 = System.currentTimeMillis();
      doPhaseTwo();
      final long end2 = System.currentTimeMillis();
      final int successCnt2 = successfulRequests.get() - successCnt1;
      final int failedCnt2 = failedRequests.get() - failedCnt1;
      printStat(start2, end2, successCnt2, failedCnt2);

      // Summary
      System.out.println("========== Summary ==========");
      printStat(start1, end2, successCnt1 + successCnt2, failedCnt1 + failedCnt2);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static void doPhaseOne() throws InterruptedException {
    System.out.println("========== Phase 1 ==========");
    printConfigMsg(INITIAL_THREAD_COUNT, INITIAL_POSTS_PER_THREAD);

    final CountDownLatch initialLatch = new CountDownLatch(INITIAL_THREAD_COUNT);
    ExecutorService executorService = Executors.newFixedThreadPool(INITIAL_THREAD_COUNT);

    for (int i = 0; i < INITIAL_THREAD_COUNT; i++) {
      final PostWorkerParam params =
          generateWorkerParameters(
              initialLatch, successfulRequests, failedRequests, INITIAL_POSTS_PER_THREAD);
      executorService.submit(new PostRequestWorker(params));
    }

    initialLatch.await();
    executorService.shutdown();
  }

  private static void doPhaseTwo() throws InterruptedException {
    System.out.println("========== Phase 2 ==========");
    int remainingPosts = TOTAL_POSTS - INITIAL_POSTS_PER_THREAD * INITIAL_THREAD_COUNT;
    final int threadCount = (new Random().nextInt(3, 25)) * 10;
    final int postsPerThread = (int) Math.ceil((float) remainingPosts / threadCount);
    printConfigMsg(threadCount, postsPerThread);

    final CountDownLatch dynamicLatch = new CountDownLatch(threadCount);
    ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

    while (remainingPosts > 0) {
      final int curPosts = Math.min(remainingPosts, postsPerThread);
      final PostWorkerParam params =
          generateWorkerParameters(dynamicLatch, successfulRequests, failedRequests, curPosts);
      executorService.submit(new PostRequestWorker(params));
      remainingPosts -= curPosts;
    }
    dynamicLatch.await();
    executorService.shutdown();
  }

  private static PostWorkerParam generateWorkerParameters(
      final CountDownLatch latch,
      final AtomicInteger successfulRequests,
      final AtomicInteger failedRequests,
      final Integer numOfRequests) {
    // Create an instance of ApiClient per thread to avoid any potential issues
    final ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(path);
    final SkiersApi skiersApi = new SkiersApi(apiClient);

    // Return the parameters
    return PostWorkerParam.builder()
        .eventBlockingQueue(events)
        .logBlockingQueue(RECORD_POSTS_IN_CSV ? logs : null)
        .countDownLatch(latch)
        .successfulRequests(successfulRequests)
        .failedRequests(failedRequests)
        .skiersApi(skiersApi)
        .numOfRequests(numOfRequests)
        .build();
  }

  private static void printTestInfo() {
    System.out.println(
        new StringBuilder()
            .append("\nCurrent test is sending requests to the ")
            .append(USE_REMOTE ? "REMOTE" : "LOCAL")
            .append(" server.\nThis test will ")
            .append(RECORD_POSTS_IN_CSV ? "" : "NOT ")
            .append("record CSV entries.\n"));
  }

  private static void printConfigMsg(final int threadCount, final int postsPerThread) {
    System.out.printf("Thread count:  %4d threads%n", threadCount);
    System.out.printf("Posts/thread:  %4d posts%n", postsPerThread);
  }

  private static void printStat(
      final long startTime, final long endTime, final int successReq, final int failedReq) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(String.format("\n%-18s %10d\n", "Total:", successReq + failedReq));
    stringBuilder.append(String.format("%-18s %10d posts\n", "Success:", successReq));
    stringBuilder.append(String.format("%-18s %10d posts\n", "Failed:", failedReq));

    final long totalRunTime = endTime - startTime;
    final double throughput = (double) (successReq + failedReq) / (totalRunTime / 1000.0);
    stringBuilder.append(String.format("%-18s %10d ms\n", "Total run time:", totalRunTime));
    stringBuilder.append(
        String.format("%-18s %10.0f req/second\n", "Total throughput:", throughput));
    System.out.println(stringBuilder);
  }
}
