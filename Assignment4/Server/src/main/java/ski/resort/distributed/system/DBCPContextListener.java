package ski.resort.distributed.system;

import ski.resort.distributed.system.dal.DBCPConnectionPool;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class DBCPContextListener implements ServletContextListener {
  @Override
  public void contextInitialized(ServletContextEvent sce) {
    System.out.println("Initializing DBCP Connection Pool...");
    try {
      synchronized (DBCPConnectionPool.class) { // make sure only one pool exists
        DBCPConnectionPool.init();
        System.out.println("Finished DBCP Connection Pool initialization...");
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to initialize DBCP Connection Pool.", e);
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    System.out.println("Shutting down DBCP Connection Pool...");
    try {
      DBCPConnectionPool.close();
    } catch (Exception e) {
      throw new RuntimeException("Failed to close DBCP Connection Pool.", e);
    }
  }
}
