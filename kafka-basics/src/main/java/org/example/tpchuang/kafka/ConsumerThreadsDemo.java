package org.example.tpchuang.kafka;

import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;

@Slf4j
public class ConsumerThreadsDemo {

  public static void main(String[] args) {
    ConsumerDemoWorker consumerDemoWorker = new ConsumerDemoWorker();
    new Thread(consumerDemoWorker).start();
    Runtime.getRuntime().addShutdownHook(new Thread(new ConsumerDemoCloser(consumerDemoWorker)));
  }

  private static class ConsumerDemoWorker implements Runnable {

    private CountDownLatch countDownLatch;
    private Consumer<String, String> consumer;

    @Override
    public void run() {
      countDownLatch = new CountDownLatch(1);

      consumer = new KafkaConsumer<>(getProperties());
      consumer.subscribe(Collections.singleton("demo_java"));

      final Duration pollTimeout = Duration.ofMillis(100);
      try {
        while (true) {
          final ConsumerRecords<String, String> consumerRecords = consumer.poll(pollTimeout);
          for (final ConsumerRecord<String, String> consumerRecord : consumerRecords) {
            log.info("Getting consumer record key: '" + consumerRecord.key() + "', value: '"
                + consumerRecord.value() + "', partition: " + consumerRecord.partition()
                + " and offset: " + consumerRecord.offset() + " at " + new Date(
                consumerRecord.timestamp()));
          }
        }
      } catch (WakeupException e) {
        log.info("Consumer poll woke up");
      } finally {
        consumer.close();
        countDownLatch.countDown();
      }
    }

    private static Properties getProperties() {
      final Properties properties = new Properties();

      properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
      properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
          StringDeserializer.class.getName());
      properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
          StringDeserializer.class.getName());
      properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "my-sixth-application");
      properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
      return properties;
    }

    void shutdown() throws InterruptedException {
      consumer.wakeup();
      countDownLatch.await();
      log.info("Consumer closed");
    }
  }

  @Slf4j
  @RequiredArgsConstructor
  private static class ConsumerDemoCloser implements Runnable {

    private final ConsumerDemoWorker consumerDemoWorker;

    @Override
    public void run() {
      try {
        consumerDemoWorker.shutdown();
      } catch (InterruptedException e) {
        log.error("Error shutting down consumer", e);
        Thread.currentThread().interrupt();
      }
    }
  }
}
