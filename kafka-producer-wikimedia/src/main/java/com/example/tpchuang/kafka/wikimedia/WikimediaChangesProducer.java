package com.example.tpchuang.kafka.wikimedia;

import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BATCH_SIZE_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.COMPRESSION_TYPE_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.LINGER_MS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.common.record.CompressionType.SNAPPY;

import com.launchdarkly.eventsource.EventHandler;
import com.launchdarkly.eventsource.EventSource;
import java.net.URI;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.StringSerializer;

@Slf4j
public class WikimediaChangesProducer {

  private static final String BOOTSTRAP_SERVERS;
  private static final String TOPIC;

  static {
    Properties properties = new Properties();
    try {
      properties.load(WikimediaChangesProducer.class.getClassLoader()
          .getResourceAsStream("application.properties"));
      BOOTSTRAP_SERVERS = properties.getProperty("kafka.bootstrapservers");
      TOPIC = properties.getProperty("kafka.topic");

      log.info("bootstrap servers: {}", BOOTSTRAP_SERVERS);
      log.info("topic: {}", TOPIC);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to load application properties.");
    }
  }

  public static void main(String[] args) throws InterruptedException {
    log.info("Start Wikimedia Changes Producer");

    KafkaTopicUtils.createTopicIfNotExists(BOOTSTRAP_SERVERS, TOPIC, 3, (short) 1);

    var producer = createKafkaProducer();

    EventHandler eventHandler = new WikimediaChangeHandler(producer, TOPIC);
    String url = "https://stream.wikimedia.org/v2/stream/recentchange";
    EventSource.Builder builder = new EventSource.Builder(eventHandler, URI.create(url));
    try (EventSource eventSource = builder.build()) {
      eventSource.start();
      TimeUnit.SECONDS.sleep(5);
    } catch (InterruptedException e) {
      log.error("Interrupted", e);
      throw e;
    } catch (Exception e) {
      log.error("Error while processing EventSource", e);
    }
  }

  private static KafkaProducer<String, String> createKafkaProducer() {
    Map<String, Object> properties = Map.of(
        BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS,
        KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
        VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
        COMPRESSION_TYPE_CONFIG, SNAPPY.toString(),
        BATCH_SIZE_CONFIG, 32 * 1024,
        LINGER_MS_CONFIG, 20);

    return new KafkaProducer<>(properties);
  }

}
