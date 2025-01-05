package com.example.tpchuang.kafka.wikimedia;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaFuture;

@Slf4j
@UtilityClass
public class KafkaTopicUtils {

  public static void createTopicIfNotExists(String bootstrapServers, String topic, int partitions,
      short replicationFactor) {
    Map<String, Object> config = Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
        bootstrapServers);
    try (AdminClient adminClient = AdminClient.create(config)) {
      KafkaFuture<Boolean> topicExistsFuture = adminClient.listTopics().names()
          .thenApply(names -> names.contains(topic));
      if (Boolean.FALSE.equals(topicExistsFuture.get())) {
        NewTopic newTopic = new NewTopic(topic, partitions, replicationFactor);
        adminClient.createTopics(Collections.singleton(newTopic)).all().get();
        log.info("Topic {} created.", topic);
      } else {
        log.info("Topic {} already exists.", topic);
      }
    } catch (InterruptedException e) {
      log.error("Topic verification interrupted.", e);
      Thread.currentThread().interrupt();
    } catch (ExecutionException e) {
      log.error("Error verifying topic.", e);
    }
  }
}
