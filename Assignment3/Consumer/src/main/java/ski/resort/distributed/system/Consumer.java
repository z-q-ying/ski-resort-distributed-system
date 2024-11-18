package ski.resort.distributed.system;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

public class Consumer {
    private static final int NUM_THREADS = 10;
    private static final String QUEUE_NAME = "SkierServletPostQueue";

    public static final Map<Integer, List<JSONObject>> record = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        ConnectionFactory connectionFactory = new ConnectionFactory();
//        connectionFactory.setHost("35.163.88.75");
//        connectionFactory.setPort(5672);
//        connectionFactory.setUsername("zqiuying");
//        connectionFactory.setPassword("LoveCoding");
        connectionFactory.setHost("localhost");

        ExecutorService rabbitMQExecutor = Executors.newFixedThreadPool(NUM_THREADS);
        Connection connection = null;
        try {
            connection = connectionFactory.newConnection(rabbitMQExecutor);
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }

        ExecutorService pool = Executors.newFixedThreadPool(NUM_THREADS);
        for (int i = 0; i < NUM_THREADS; i++) {
            pool.execute(new ConsumerRunnable(QUEUE_NAME, connection));
        }
    }
}