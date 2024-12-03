package ski.resort.distributed.system.runnables;

import ski.resort.distributed.system.models.EventLog;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A runnable that retrieves EventLog instances from a blocking queue and writes them to a CSV file
 * if logging is enabled.
 */
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
        getAndPrintStats();
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

  private void getAndPrintStats() {
    List<Integer> latencies = new ArrayList<>();

    try (BufferedReader br = new BufferedReader(new FileReader(this.filePath))) {
      String line;
      br.readLine(); // Skip the header
      while ((line = br.readLine()) != null) {
        String[] values = line.split(",");
        latencies.add(Integer.parseInt(values[2])); // latency is the third field
      }
    } catch (IOException e) {
      System.out.println("Error reading file: " + e.getMessage());
      return;
    }

    if (latencies.isEmpty()) {
      System.out.println("No latency data found.");
      return;
    }

    Collections.sort(latencies);

    // Calculating statistics
    double mean = latencies.stream().mapToInt(Integer::intValue).average().orElse(0.0);
    double median = getPercentile(latencies, 50);
    double p95 = getPercentile(latencies, 95);
    double p99 = getPercentile(latencies, 99);
    int max = latencies.get(latencies.size() - 1);

    // Printing results
    String statistics =
        String.format("%-25s %10.2f ms\n", "Mean:", mean)
            + String.format("%-25s %10.2f ms\n", "Median:", median)
            + String.format("%-25s %10.2f ms\n", "95th Percentile (p95):", p95)
            + String.format("%-25s %10.2f ms\n", "99th Percentile (p99):", p99)
            + String.format("%-25s %10d ms\n", "Max:", max);
    System.out.println("=============== Statistics ==============");
    System.out.println(statistics);
  }

  private double getPercentile(List<Integer> latencies, int percentile) {
    int index = (int) Math.ceil((percentile / 100.0) * latencies.size()) - 1;
    return latencies.get(index);
  }
}
