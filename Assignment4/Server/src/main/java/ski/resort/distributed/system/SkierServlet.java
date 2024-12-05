package ski.resort.distributed.system;

import com.rabbitmq.client.*;
import org.json.JSONObject;
import ski.resort.distributed.system.dal.SkierDayVerticalDao;
import ski.resort.distributed.system.dal.SkierResortTotalsDao;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;

import static ski.resort.distributed.system.utils.Constants.DOTENV;

@WebServlet(value = "/skiers/*")
public class SkierServlet extends HttpServlet {

  private static final int NUM_OF_URL_PARTS_A = 8;
  private static final int NUM_OF_URL_PARTS_B = 5;
  private static final int NUM_CHANNEL = 50;
  private static final int DEFAULT_SEASON = 2024;
  private static final String QUEUE_NAME = "SkierServletPostQueue";

  private BlockingQueue<Channel> channelPool;

  @Override
  public void init() throws ServletException {
    super.init();

    // set up RabbitMQ connections
    setUpRabbitMQConnectionPool();
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    res.setContentType("application/json");

    final String urlPath = req.getPathInfo();
    final String[] pathParts = urlPath.split("/");

    if (pathParts.length == NUM_OF_URL_PARTS_A) {
      // GET /skiers/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}
      if (!isValidSkierLongUrl(urlPath, res)) return;

      final int resortID = Integer.parseInt(pathParts[1]);
      final int seasonID = Integer.parseInt(pathParts[3]);
      final int dayID = Integer.parseInt(pathParts[5]);
      final int skierID = Integer.parseInt(pathParts[7]);

      final SkierDayVerticalDao skierDayVerticalDao = new SkierDayVerticalDao();
      final int tv = skierDayVerticalDao.getTotalVertical(resortID, seasonID, dayID, skierID);
      final String msg = new JSONObject().put("totalVertical", tv).toString();

      res.setStatus(HttpServletResponse.SC_OK);
      res.getWriter().write(msg);

    } else if (pathParts.length == NUM_OF_URL_PARTS_B) {
      // GET /skiers/{skierID}/resorts/{resortID}/vertical
      if (!isValidSkierShortUrl(urlPath, res)) return;
      final int skierID = Integer.parseInt(pathParts[1]);
      final int resortID = Integer.parseInt(pathParts[3]);

      SkierResortTotalsDao skierResortTotalsDao = new SkierResortTotalsDao();
      final int tv = skierResortTotalsDao.getTotalVertical(skierID, resortID, DEFAULT_SEASON);
      final String msg = new JSONObject().put("totalVertical", tv).toString();

      res.setStatus(HttpServletResponse.SC_OK);
      res.getWriter().write(msg);
    } else {
      res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      res.getWriter().write("Invalid URL format");
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    resp.setContentType("application/json"); // According to the API spec

    final String urlPath = req.getPathInfo();
    if (!isValidSkierLongUrl(urlPath, resp)) return;

    // Parse and convert the req info into string for further JSON processing.
    StringBuilder jsonBuffer = new StringBuilder();
    String line;
    try (BufferedReader reader = req.getReader()) {
      while ((line = reader.readLine()) != null) {
        jsonBuffer.append(line);
      }
    } catch (IOException e) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      resp.getWriter().write("bad request");
      return;
    }
    JSONObject jsonObject = new JSONObject(jsonBuffer.toString());

    // Add other info.
    String[] pathParts = urlPath.split("/");
    jsonObject
        .put("resortID", Integer.valueOf(pathParts[1]))
        .put("seasonID", pathParts[3])
        .put("dayID", pathParts[5])
        .put("skierID", Integer.valueOf(pathParts[7]));

    Channel channel = null;
    try {
      channel = channelPool.take(); // Acquire channel from the pool
      channel.confirmSelect(); // Enable confirm mode

      // Publish message
      channel.basicPublish("", QUEUE_NAME, null, jsonObject.toString().getBytes());

      // Wait for RabbitMQ ACK
      if (channel.waitForConfirms()) {
        // Message successfully acknowledged
        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.getWriter().write("POST request has been successfully processed.");
      } else {
        // Message not acknowledged
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        resp.getWriter().write("Failed to process POST request.");
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      resp.getWriter().write("Internal server error.");
    } catch (IOException e) {
      e.printStackTrace();
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      resp.getWriter().write("Failed to communicate with RabbitMQ.");
    } finally {
      if (channel != null) {
        channelPool.add(channel); // Return channel to the pool
      }
    }
  }

  private void setUpRabbitMQConnectionPool() {
    // init RabbitMQ connections
    ConnectionFactory connectionFactory = new ConnectionFactory();
    connectionFactory.setHost(DOTENV.get("RMQ_HOST"));
    connectionFactory.setPort(Integer.parseInt(DOTENV.get("RMQ_PORT")));
    connectionFactory.setUsername(DOTENV.get("RMQ_USER"));
    connectionFactory.setPassword(DOTENV.get("RMQ_PW"));

    Connection connection;
    try {
      connection = connectionFactory.newConnection();
    } catch (IOException | TimeoutException e) {
      e.printStackTrace();
      return;
    }

    // channelPool setup.
    channelPool = new LinkedBlockingQueue<>();
    for (int i = 0; i < NUM_CHANNEL; i++) {
      try {
        Channel newChannel = connection.createChannel();
        newChannel.queueDeclare(QUEUE_NAME, false, false, false, null);
        channelPool.add(newChannel);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private boolean isValidSkierLongUrl(final String urlPath, HttpServletResponse resp) {
    try {
      if (urlPath == null || urlPath.isEmpty()) {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        resp.getWriter().write("Missing parameters");
        return false;
      }

      String[] pathParts = urlPath.split("/");
      if (pathParts.length != NUM_OF_URL_PARTS_A) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        resp.getWriter().write("Invalid URL format");
        return false;
      }

      try {
        final int resortID = Integer.parseInt(pathParts[1]);
        final int seasonID = Integer.parseInt(pathParts[3]);
        final int dayID = Integer.parseInt(pathParts[5]);
        final int skierID = Integer.parseInt(pathParts[7]);
      } catch (NumberFormatException e) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        resp.getWriter().write("Invalid number format in path parameters");
        return false;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return true;
  }

  private boolean isValidSkierShortUrl(final String urlPath, HttpServletResponse resp) {
    try {
      String[] pathParts = urlPath.split("/");
      try {
        final int skierID = Integer.parseInt(pathParts[1]);
        final int resortID = Integer.parseInt(pathParts[3]);
      } catch (NumberFormatException e) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        resp.getWriter().write("Invalid number format in path parameters");
        return false;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return true;
  }
}
