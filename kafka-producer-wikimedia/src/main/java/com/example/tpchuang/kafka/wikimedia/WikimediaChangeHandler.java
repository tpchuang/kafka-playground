package com.example.tpchuang.kafka.wikimedia;

import com.google.gson.JsonParser;
import com.launchdarkly.eventsource.EventHandler;
import com.launchdarkly.eventsource.MessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

@Slf4j
@RequiredArgsConstructor
public class WikimediaChangeHandler implements EventHandler {

  private final KafkaProducer<String, String> kafkaProducer;
  private final String topic;

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
    String id = JsonParser.parseString(data).getAsJsonObject().get("meta").getAsJsonObject()
        .get("id").getAsString();
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
}
