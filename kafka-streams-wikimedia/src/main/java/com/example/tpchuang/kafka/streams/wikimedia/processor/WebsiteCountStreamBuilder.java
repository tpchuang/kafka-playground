package com.example.tpchuang.kafka.streams.wikimedia.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.WindowedSerdes;

public class WebsiteCountStreamBuilder {

  private static final String WEBSITE_COUNT_STORE = "website-count-store";
  private static final String WEBSITE_COUNT_TOPIC = "wikimedia.stats.website";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final KStream<String, String> inputStream;

  public WebsiteCountStreamBuilder(KStream<String, String> inputStream) {
    this.inputStream = inputStream;
  }

  public void setup() {
    final TimeWindows timeWindows = TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1L));
    this.inputStream
        .selectKey((k, changeJson) -> {
          try {
            final JsonNode jsonNode = OBJECT_MAPPER.readTree(changeJson);
            return jsonNode.get("server_name").asText();
          } catch (IOException e) {
            return "parse-error";
          }
        })
        .groupByKey()
        .windowedBy(timeWindows)
        .count(Materialized.as(WEBSITE_COUNT_STORE))
        .toStream()
        .mapValues((key, value) -> {
          final Map<String, Object> kvMap = Map.of(
              "website", key.key(),
              "count", value
          );
          try {
            return OBJECT_MAPPER.writeValueAsString(kvMap);
          } catch (JsonProcessingException e) {
            return null;
          }
        })
        .to(WEBSITE_COUNT_TOPIC, Produced.with(
            WindowedSerdes.timeWindowedSerdeFrom(String.class, timeWindows.size()),
            Serdes.String()
        ));
  }
}
