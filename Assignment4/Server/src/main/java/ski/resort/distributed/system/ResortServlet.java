package ski.resort.distributed.system;

import org.json.JSONObject;
import ski.resort.distributed.system.dal.ResortSkiersDayDao;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(value = "/resorts/*")
public class ResortServlet extends HttpServlet {

  private static final int MIN_RESORT_ID = 1;
  private static final int MAX_RESORT_ID = 10;
  private static final int NUM_OF_URL_PARTS = 7;

  /** GET /resorts/{resortID}/seasons/{seasonID}/day/{dayID}/skiers */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    res.setContentType("application/json");

    final String urlPath = req.getPathInfo();
    if (!isUrlValid(urlPath, res)) return;

    final String[] pathParts = urlPath.split("/");
    final int resortID = Integer.parseInt(pathParts[1]);
    final int seasonID = Integer.parseInt(pathParts[3]);
    final int dayID = Integer.parseInt(pathParts[5]);

    final ResortSkiersDayDao resortSkiersDayDao = new ResortSkiersDayDao();
    final int uniqueSkiers = resortSkiersDayDao.getUniqueSkiers(resortID, seasonID, dayID);
    final String msg = new JSONObject().put("numSkiers", uniqueSkiers).toString();

    res.setStatus(HttpServletResponse.SC_OK);
    res.getWriter().write(msg);
  }

  private boolean isUrlValid(final String urlPath, HttpServletResponse resp) {
    try {
      if (urlPath == null || urlPath.isEmpty()) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        resp.getWriter().write("Empty ULR.");
        return false;
      }

      String[] pathParts = urlPath.split("/");
      if (pathParts.length != NUM_OF_URL_PARTS) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        resp.getWriter().write("Invalid URL length.");
        return false;
      }

      try {
        final int resortID = Integer.parseInt(pathParts[1]);
        if (resortID < MIN_RESORT_ID || resortID > MAX_RESORT_ID) {
          resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
          resp.getWriter().write("Resort ID must be between 1 and 10.");
          return false;
        }
        final int seasonID = Integer.parseInt(pathParts[3]);
        final int dayID = Integer.parseInt(pathParts[5]);
      } catch (NumberFormatException e) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        resp.getWriter().write("Invalid number format in path parameters.");
        return false;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return true;
  }
}
