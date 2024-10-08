package ski.resort.distributed.system;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

@WebServlet(value = "/skiers/*")
public class SkierServlet extends HttpServlet {

  private static final int NUM_OF_URL_PARTS = 8;

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

    // Parse and convert the req info into string for further JSON processing
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

    resp.setStatus(HttpServletResponse.SC_OK);
    resp.getWriter().write("POST request successfully processed!");
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
