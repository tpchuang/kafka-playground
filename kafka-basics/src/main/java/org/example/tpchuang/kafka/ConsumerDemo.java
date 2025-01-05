package org.example.tpchuang.kafka;

import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.CooperativeStickyAssignor;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;

@Slf4j
public class ConsumerDemo {

  public static void main(String[] args) {
    log.info("Kafka client");

    KafkaConsumer<String, String> consumer = createKafkaConsumer();

    final Thread mainThread = Thread.currentThread();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.info("Detected shutdown.");
      consumer.wakeup();
      try {
        mainThread.join();
      } catch (InterruptedException e) {
        log.error("Thread interrupted", e);
        Thread.currentThread().interrupt();
      }
    }));

    String topic = "second_topic";
    try {
      consumer.subscribe(List.of(topic));
      log.info("Polling");
      while (true) {
        var records = consumer.poll(Duration.ofMillis(5000));
        for (var rec : records) {
          log.info("Timestamp: {}, Partition: {}, Offset: {}, Key: {}, Value: {}", rec.timestamp(),
              rec.partition(), rec.offset(), rec.key(), rec.value());
        }
      }
    } catch (WakeupException e) {
      log.info("Shutting down consumer.");
    } catch (Exception e) {
      log.error("Error detected.", e);
    } finally {
      consumer.close();
    }
  }

  private static KafkaConsumer<String, String> createKafkaConsumer() {
    String groupId = "cg1";
    Map<String, Object> properties = Map.of(
        BOOTSTRAP_SERVERS_CONFIG, "localhost:19092",
        KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
        VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
        GROUP_ID_CONFIG, groupId,
        AUTO_OFFSET_RESET_CONFIG, OffsetResetStrategy.LATEST.toString(),
        PARTITION_ASSIGNMENT_STRATEGY_CONFIG, List.of(CooperativeStickyAssignor.class));

    return new KafkaConsumer<>(properties);
  }
}
