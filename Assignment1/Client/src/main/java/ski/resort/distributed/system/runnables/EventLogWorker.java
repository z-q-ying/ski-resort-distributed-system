package ski.resort.distributed.system.runnables;

import ski.resort.distributed.system.models.EventLog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class EventLogWorker implements Runnable {
  private final String filePath;
  private final BlockingQueue<EventLog> logBlockingQueue;

  public EventLogWorker(final String filePath, final BlockingQueue<EventLog> logQueue) {
    this.filePath = filePath;
    this.logBlockingQueue = logQueue;
    initializeCSVFile();
  }

  private void initializeCSVFile() {
    File file = new File(filePath);
    // If the file exists, it will be replaced by opening it in non-append mode
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
      writer.write("startTime,requestType,latency,responseCode");
      writer.newLine();
    } catch (IOException e) {
      System.err.println("Error while initializing CSV file: " + e.getMessage());
    }
  }

  @Override
  public void run() {
    while (true) {
      try {
        EventLog eventLog = logBlockingQueue.poll(30, TimeUnit.SECONDS);
        assert eventLog != null;
        writeLogToCSV(formLogStr(eventLog));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        System.out.println("Event log worker interrupted");
        break;
      } catch (NullPointerException e) {
        System.out.println("Event log worker is null");
        break;
      }
    }
  }

  private void writeLogToCSV(final String logStr) {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
      writer.write(logStr);
    } catch (IOException e) {
      System.err.println("Error while writing to CSV file: " + e.getMessage());
    }
  }

  public String formLogStr(final EventLog eventLog) {
    return String.format(
        "%s,%s,%d,%s\n",
        eventLog.getStartTime(),
        eventLog.getType(),
        eventLog.getEndTime() - eventLog.getStartTime(),
        eventLog.getResponseCode() == null ? "Null" : eventLog.getResponseCode().toString());
  }
}
