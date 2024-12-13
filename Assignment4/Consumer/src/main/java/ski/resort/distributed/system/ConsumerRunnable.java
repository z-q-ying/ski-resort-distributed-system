package ski.resort.distributed.system;

import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import org.json.JSONObject;
import ski.resort.distributed.system.dal.LiftRideDao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ConsumerRunnable implements Runnable {
  private final String queueName;
  private final Connection connection;

  ConsumerRunnable(String queueName, Connection connection) {
    this.queueName = queueName;
    this.connection = connection;
  }

  @Override
  public void run() {
    try {
      Channel channel = connection.createChannel();
      channel.queueDeclare(queueName, false, false, false, null);

      DeliverCallback deliverCallback =
          (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8); // Bytes
            JSONObject jsonObject = new JSONObject(message);

            // Add record to database
            LiftRideDao liftRideDao = new LiftRideDao();
            liftRideDao.createLiftRide(jsonObject);
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
          };

      CancelCallback cancelCallback = (consumerTag) -> {};

      channel.basicConsume(this.queueName, false, deliverCallback, cancelCallback);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
