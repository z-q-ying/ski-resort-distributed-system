package ski.resort.distributed.system;

import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SkiersApi;
import ski.resort.distributed.system.models.Event;
import ski.resort.distributed.system.models.WorkerParam;

import static ski.resort.distributed.system.constants.Constants.HTTP_CLIENT_ERROR;
import static ski.resort.distributed.system.constants.Constants.HTTP_CREATED;
import static ski.resort.distributed.system.constants.Constants.HTTP_OK;
import static ski.resort.distributed.system.constants.Constants.HTTP_SERVER_ERROR;
import static ski.resort.distributed.system.constants.Constants.MAX_RETRIES;

public class PostRequestWorker implements Runnable {

  private final WorkerParam params;

  public PostRequestWorker(final WorkerParam params) {
    this.params = params;
  }

  @Override
  public void run() {
    try {
      for (int i = 0; i < params.getNumOfRequests(); i++) {
        Event event = params.getEventBlockingQueue().take();
        processRequest(event);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      System.err.println("Thread interrupted. Thread ID: " + Thread.currentThread().getId());
    } finally {
      params.getCountDownLatch().countDown();
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
    return skiersApi.writeNewLiftRideWithHttpInfo(
        event.getLiftRide(),
        event.getResortID(),
        event.getSeasonId(),
        event.getDayId(),
        event.getSkierID());
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
