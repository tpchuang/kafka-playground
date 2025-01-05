package com.example.tpchuang.kafka.wikimedia;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.launchdarkly.eventsource.EventHandler;
import com.launchdarkly.eventsource.MessageEvent;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

@Slf4j
@RequiredArgsConstructor
public class WikimediaChangeHandler implements EventHandler {

  private final KafkaProducer<String, String> kafkaProducer;
  private final String topic;
  private final Gson gson = new Gson();

  @Override
  public void onOpen() {
    // nada
  }

  @Override
  public void onClosed() {
    kafkaProducer.close();
  }

  @Override
  public void onMessage(String event, MessageEvent messageEvent) {
    String data = messageEvent.getData();
    Map<String, Object> dataMap = parseObjectMap(data);
    String id = BigDecimal.valueOf((Double) dataMap.get("id")).toPlainString();
    log.info("Change received: {} {}", id, data);
    kafkaProducer.send(new ProducerRecord<>(topic, id, data));
  }

  @Override
  public void onComment(String comment) {
    // nada
  }

  @Override
  public void onError(Throwable t) {
    log.error("Error in handling Wikimedia Change", t);
  }

  private Map<String, Object> parseObjectMap(String jsonData) {
    try {
      Type type = new TypeToken<Map<String, Object>>() {
      }.getType();
      return gson.fromJson(jsonData, type);
    } catch (Exception e) {
      log.warn("Unable to parse string as JSON object: {}", jsonData);
      return ImmutableMap.of();
    }
  }
}
