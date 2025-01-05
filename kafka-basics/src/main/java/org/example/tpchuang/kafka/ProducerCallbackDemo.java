package org.example.tpchuang.kafka;

import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;

import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

@Slf4j
public class ProducerCallbackDemo {

  public static void main(String[] args) {
    log.info("Kafka Producer with send callback");

    Properties properties = new Properties();
    properties.setProperty(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    properties.setProperty(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    properties.setProperty(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

    KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

    for (int i = 0; i < 10; i++) {
      var rec = new ProducerRecord<>("second_topic", String.format("K-%2d", i), "Message " + i);
      producer.send(rec, sendRecordCallback());
    }

    producer.flush();
    log.info("After flushing producer");

    for (int i = 0; i < 10; i++) {
      var rec = new ProducerRecord<>("second_topic", String.format("K-%2d", i), "Message " + i);
      producer.send(rec, sendRecordCallback());
    }

    log.info("Before closing producer");
    producer.close();
  }

  private static Callback sendRecordCallback() {
    return (meta, e) -> {
      if (e != null) {
        log.error("Error while producing.", e);
        return;
      }
      log.info("Received new metadata: topic: {}, partition: {}, offset: {}, ts: {}.", meta.topic(),
          meta.partition(), meta.offset(), meta.timestamp());
    };
  }
}
