package ski.resort.distributed.system.runnables;

import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SkiersApi;
import ski.resort.distributed.system.models.Event;
import ski.resort.distributed.system.models.EventLog;
import ski.resort.distributed.system.models.PostWorkerParam;

import static ski.resort.distributed.system.constants.Constants.HTTP_CLIENT_ERROR;
import static ski.resort.distributed.system.constants.Constants.HTTP_CREATED;
import static ski.resort.distributed.system.constants.Constants.HTTP_OK;
import static ski.resort.distributed.system.constants.Constants.HTTP_SERVER_ERROR;
import static ski.resort.distributed.system.constants.Constants.MAX_RETRIES;
import static ski.resort.distributed.system.constants.UserConfig.RECORD_POSTS_IN_CSV;

public class PostRequestWorker implements Runnable {

  private final PostWorkerParam params;

  public PostRequestWorker(final PostWorkerParam params) {
    this.params = params;
  }

  @Override
  public void run() {
    try {
      for (int i = 0; i < params.getNumOfRequests(); i++) {
        Event event = params.getEventBlockingQueue().take();
        if (RECORD_POSTS_IN_CSV) {
          final long startTime = System.currentTimeMillis();
          processRequest(event);
          final long endTime = System.currentTimeMillis();
          recordRequestSent("POST", event.getResponseCode(), startTime, endTime);
        } else {
          processRequest(event);
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      System.err.println("Thread interrupted. Thread ID: " + Thread.currentThread().getId());
    } finally {
      params.getCountDownLatch().countDown();
    }
  }

  private void recordRequestSent(
      final String type, final Integer responseCode, final long startTime, final long endTime) {
    try {
      params
          .getLogBlockingQueue()
          .put(
              EventLog.builder()
                  .type(type)
                  .responseCode(responseCode)
                  .startTime(startTime)
                  .endTime(endTime)
                  .build());
    } catch (InterruptedException e) {
      System.err.println(
          "Thread interrupted while waiting to put event log. Thread ID: "
              + Thread.currentThread().getId());
    }
  }

  private void processRequest(final Event event) {
    int attempt = 0;
    boolean success = false;

    // Retry logic encapsulated in a separate method
    while (attempt < MAX_RETRIES && !success) {
      attempt++;
      success = trySendingRequest(event, attempt);
    }

    if (!success) {
      logFailedRequest(event);
    }
  }

  private boolean trySendingRequest(final Event event, int attempt) {
    ApiResponse<Void> response;
    try {
      response = sendPostRequest(event);
    } catch (ApiException e) {
      System.err.println("Skier API error (attempt " + attempt + "): " + e.getMessage());
      return false;
    }
    return handleResponse(response);
  }

  private ApiResponse<Void> sendPostRequest(final Event event) throws ApiException {
    final SkiersApi skiersApi = params.getSkiersApi();
    final ApiResponse<Void> response =
        skiersApi.writeNewLiftRideWithHttpInfo(
            event.getLiftRide(),
            event.getResortID(),
            event.getSeasonId(),
            event.getDayId(),
            event.getSkierID());
    if (response != null) event.setResponseCode(response.getStatusCode());
    return response;
  }

  private boolean handleResponse(ApiResponse<Void> response) {
    if (response == null) {
      System.err.println("Skier API returned null response");
      return false;
    }

    if (response.getStatusCode() == HTTP_OK || response.getStatusCode() == HTTP_CREATED) {
      params.getSuccessfulRequests().incrementAndGet();
      return true;
    }

    if (response.getStatusCode() >= HTTP_SERVER_ERROR) {
      System.out.println("Server error: " + response.getStatusCode());
    } else if (response.getStatusCode() >= HTTP_CLIENT_ERROR) {
      System.out.println("Client error: " + response.getStatusCode());
    }
    return false;
  }

  private void logFailedRequest(Event event) {
    int idx = params.getFailedRequests().incrementAndGet();
    System.out.println("Failed request #" + idx + " after " + MAX_RETRIES + " attempts: " + event);
  }
}
