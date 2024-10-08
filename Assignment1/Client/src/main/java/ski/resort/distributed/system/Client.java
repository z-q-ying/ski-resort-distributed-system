package ski.resort.distributed.system;

import io.swagger.client.ApiClient;
import io.swagger.client.api.SkiersApi;
import ski.resort.distributed.system.models.Event;
import ski.resort.distributed.system.models.WorkerParam;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static ski.resort.distributed.system.constants.Constants.INITIAL_POSTS_PER_THREAD;
import static ski.resort.distributed.system.constants.Constants.INITIAL_THREAD_COUNT;
import static ski.resort.distributed.system.constants.Constants.TOTAL_POSTS;
import static ski.resort.distributed.system.constants.UserConfig.BASE_PATH;

public class Client {

  private static final AtomicInteger successfulRequests = new AtomicInteger(0);
  private static final AtomicInteger failedRequests = new AtomicInteger(0);
  private static final String path = BASE_PATH;
  private static final BlockingQueue<Event> events = new LinkedBlockingQueue<>();

  public static void main(String[] args) throws InterruptedException {

    System.out.println("======= Phase 1 Start =======");
    final long startTime = System.currentTimeMillis();

    // Start the lift event generator in a separate thread
    final Runnable eventGenerator = new EventGenerator(TOTAL_POSTS, events);
    final Thread eventThread = new Thread(eventGenerator);
    eventThread.start();

    // Phase 1: 32 threads each post 1000 events
    final CountDownLatch initialLatch = new CountDownLatch(INITIAL_THREAD_COUNT);
    ExecutorService executorService = Executors.newFixedThreadPool(INITIAL_THREAD_COUNT);

    printConfigMsg(INITIAL_THREAD_COUNT, INITIAL_POSTS_PER_THREAD);
    for (int i = 0; i < INITIAL_THREAD_COUNT; i++) {
      final WorkerParam params =
          generateWorkerParameters(
              initialLatch, successfulRequests, failedRequests, INITIAL_POSTS_PER_THREAD);
      executorService.submit(new PostRequestWorker(params));
    }

    initialLatch.await();
    System.out.println("===== Phase 1 Completed =====");

    System.out.println("\n======= Phase 2 Start =======");
    int remainingPosts = TOTAL_POSTS - INITIAL_POSTS_PER_THREAD * INITIAL_THREAD_COUNT;
    final int threadCount = (new Random().nextInt(3, 25)) * 10;
    final int postsPerThread = (int) Math.ceil((float) remainingPosts / threadCount);
    printConfigMsg(threadCount, postsPerThread);

    executorService = Executors.newFixedThreadPool(threadCount);
    final CountDownLatch dynamicLatch = new CountDownLatch(threadCount);
    while (remainingPosts > 0) {
      final int curPosts = Math.min(remainingPosts, postsPerThread);
      final WorkerParam params =
          generateWorkerParameters(dynamicLatch, successfulRequests, failedRequests, curPosts);
      executorService.submit(new PostRequestWorker(params));
      remainingPosts -= curPosts;
    }
    dynamicLatch.await();

    // Record the end time
    final long endTime = System.currentTimeMillis();
    System.out.println("===== Phase 2 Completed =====");

    // Print stats and shut down the executor service
    printStat(startTime, endTime);
    executorService.shutdown();
  }

  private static WorkerParam generateWorkerParameters(
      final CountDownLatch latch,
      final AtomicInteger successfulRequests,
      final AtomicInteger failedRequests,
      final Integer numOfRequests) {
    // Create an instance of ApiClient per thread to avoid any potential issues
    final ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(path);
    final SkiersApi skiersApi = new SkiersApi(apiClient);

    // Return the parameters
    return WorkerParam.builder()
        .eventBlockingQueue(events)
        .countDownLatch(latch)
        .successfulRequests(successfulRequests)
        .failedRequests(failedRequests)
        .skiersApi(skiersApi)
        .numOfRequests(numOfRequests)
        .build();
  }

  private static void printConfigMsg(final int threadCount, final int postsPerThread) {
    System.out.printf("Thread count:  %4d threads%n", threadCount);
    System.out.printf("Posts/thread:  %4d posts%n", postsPerThread);
  }

  private static void printStat(final long startTime, final long endTime) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("\n*****************************************\n");
    stringBuilder.append(String.format("%-18s %10d\n", "Total:", TOTAL_POSTS));
    stringBuilder.append(String.format("%-18s %10d posts\n", "Success:", successfulRequests.get()));
    stringBuilder.append(String.format("%-18s %10d posts\n", "Failed:", failedRequests.get()));

    final long totalRunTime = endTime - startTime;
    final double throughput = (double) TOTAL_POSTS / (totalRunTime / 1000.0);
    stringBuilder.append("*****************************************\n");
    stringBuilder.append(String.format("%-18s %10d ms\n", "Total run time:", totalRunTime));
    stringBuilder.append(
        String.format("%-18s %10.0f req/second\n", "Total throughput:", throughput));
    stringBuilder.append("*****************************************\n");
    System.out.println(stringBuilder);
  }
}
