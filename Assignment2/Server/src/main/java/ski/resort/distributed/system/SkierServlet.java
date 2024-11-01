package ski.resort.distributed.system;

import com.rabbitmq.client.*;
import org.json.JSONObject;

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

@WebServlet(value = "/skiers/*")
public class SkierServlet extends HttpServlet {

  private static final int NUM_OF_URL_PARTS = 8;
  private static final int NUM_CHANNEL = 50;
  private static final String QUEUE_NAME = "SkierServletPostQueue";

  private BlockingQueue<Channel> channelPool;

  @Override
  public void init() throws ServletException {
    super.init();

    ConnectionFactory connectionFactory = new ConnectionFactory();
    connectionFactory.setHost("34.208.98.209");
    connectionFactory.setPort(5672);
    connectionFactory.setUsername("zqiuying");
    connectionFactory.setPassword("LoveCoding");

    Connection connection = null;
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

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    res.setContentType("text/plain");

    final String urlPath = req.getPathInfo();
    if (!isUrlValid(urlPath, res)) return;

    res.setStatus(HttpServletResponse.SC_OK);
    res.getWriter().write("GET request successfully processed!");
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    resp.setContentType("application/json"); // According to the API spec

    final String urlPath = req.getPathInfo();
    if (!isUrlValid(urlPath, resp)) return;

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
    jsonObject.put("resortID", Integer.valueOf(pathParts[1]))
            .put("seasonID", pathParts[3])
            .put("dayID", pathParts[5])
            .put("skierID", Integer.valueOf(pathParts[7]));

    Channel channel = null;
    try {
      channel = channelPool.take();
    } catch (InterruptedException e) {
      e.printStackTrace();
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    }

    channel.basicPublish("", QUEUE_NAME, null, jsonObject.toString().getBytes());
    channelPool.add(channel);

    resp.setStatus(HttpServletResponse.SC_CREATED);
    resp.getWriter().write("POST request has been sent to rabbitmq.");
  }

  private boolean isUrlValid(final String urlPath, HttpServletResponse resp) {
    try {
      if (urlPath == null || urlPath.isEmpty()) {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        resp.getWriter().write("Missing parameters");
        return false;
      }

      String[] pathParts = urlPath.split("/");
      if (pathParts.length != NUM_OF_URL_PARTS) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        resp.getWriter().write("Invalid URL format");
        return false;
      }

      try {
        final int resortID = Integer.parseInt(pathParts[1]);
        final String seasonID = pathParts[3];
        final String dayID = pathParts[5];
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
}
