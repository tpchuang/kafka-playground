package com.example.tpchuang.kafka.opensearch;

import java.util.Properties;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class PropertiesLoader {

  private final String bootstrapServers;
  private final String topic;

  public PropertiesLoader() {
    Properties properties = new Properties();
    try {
      properties.load(
          PropertiesLoader.class.getClassLoader().getResourceAsStream("application.properties"));
      bootstrapServers = properties.getProperty("kafka.bootstrapservers");
      topic = properties.getProperty("kafka.topic");

      log.info("bootstrap servers: {}", bootstrapServers);
      log.info("topic: {}", topic);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to load application properties.");
    }
  }
}
