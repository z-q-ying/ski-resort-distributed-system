package ski.resort.distributed.system;

import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8); // Bytes
                JSONObject jsonObject = new JSONObject(message);
                Integer skierID = jsonObject.getInt("skierID");
                if (Consumer.record.containsKey(skierID)) {
                    Consumer.record.get(skierID).add(jsonObject);
                } else {
                    List<JSONObject> newSkierInfo = Collections.synchronizedList(new ArrayList<>());
                    newSkierInfo.add(jsonObject);
                    Consumer.record.put(skierID, newSkierInfo);
                }
                // System.out.println(Thread.currentThread().getId() + " - thread received " + jsonObject.toString());
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            };
            CancelCallback cancelCallback = (consumerTag) -> {};

            channel.basicConsume(this.queueName, false, deliverCallback, cancelCallback);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
