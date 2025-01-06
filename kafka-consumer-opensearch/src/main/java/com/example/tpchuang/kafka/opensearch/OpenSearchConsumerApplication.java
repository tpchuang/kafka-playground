package com.example.tpchuang.kafka.opensearch;

import java.io.IOException;

public class OpenSearchConsumerApplication {

  public static void main(String[] args) throws IOException {
    PropertiesLoader properties = new PropertiesLoader();

    String openSearchUrl = System.getenv("URL");
    if (openSearchUrl == null || openSearchUrl.isEmpty()) {
      throw new IllegalArgumentException("Environment variable 'URL' is not set or empty");
    }

    OpenSearchConsumer consumer = new OpenSearchConsumer(properties.getBootstrapServers(),
        properties.getTopic(), openSearchUrl);
    consumer.run();
  }
}
