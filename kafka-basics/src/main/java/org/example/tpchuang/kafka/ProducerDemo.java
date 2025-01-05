package org.example.tpchuang.kafka;

import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;

import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

@Slf4j
public class ProducerDemo {

  public static void main(String[] args) {
    log.info("Kafka Producer");

    Properties properties = new Properties();
    // properties.setProperty(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    properties.setProperty(BOOTSTRAP_SERVERS_CONFIG, "localhost:19092");
    // properties.setProperty(BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
    properties.setProperty(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    properties.setProperty(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

    KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

    String topic = "demo_java";
    for (int i = 0; i < 10; i++) {
      var rec = new ProducerRecord<>(topic, Integer.toString(i), "Message " + i);
      producer.send(rec);
    }

    producer.close();
  }
}
