package ski.resort.distributed.system;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import ski.resort.distributed.system.dal.DBCPDataSource;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

public class Consumer {
    private static final int NUM_THREADS = 55;
    private static final String QUEUE_NAME = "SkierServletPostQueue";

    public static void main(String[] args) {
        // Ensure database and table are created
        DBCPDataSource.getDataSource();
        try {
            DBCPDataSource.createDatabaseAndTableIfNotExists();
        } catch (SQLException e) {
            System.err.println("Failed to ensure database and table exist: " + e.getMessage());
            return;
        }

        // RabbitMQ set up
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("172.31.12.171"); // "localhost" for local dev, only line needed
        connectionFactory.setPort(5672);
        connectionFactory.setUsername("zqiuying");
        connectionFactory.setPassword("LoveCoding");

        ExecutorService rabbitMQExecutor = Executors.newFixedThreadPool(NUM_THREADS);
        Connection connection;
        try {
            connection = connectionFactory.newConnection(rabbitMQExecutor);
        } catch (IOException | TimeoutException e) {
            System.err.println("Error establishing connection to RabbitMQ: " + e.getMessage());
            return;
        }

        ExecutorService pool = Executors.newFixedThreadPool(NUM_THREADS);
        for (int i = 0; i < NUM_THREADS; i++) {
            pool.execute(new ConsumerRunnable(QUEUE_NAME, connection));
        }
    }
}