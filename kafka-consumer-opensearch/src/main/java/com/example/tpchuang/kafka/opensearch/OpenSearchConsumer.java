package com.example.tpchuang.kafka.opensearch;

import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.CooperativeStickyAssignor;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetIndexRequest;
import org.opensearch.common.xcontent.XContentType;

@Slf4j
@RequiredArgsConstructor
public class OpenSearchConsumer {

  private final String kafkaBootstrapServers;
  private final String kafkaTopic;
  private final String openSearchUrl;

  public void run() throws IOException {
    try (RestHighLevelClient openSearchClient = createOpenSearchClient(); KafkaConsumer<String, String> kafkaConsumer = createKafkaConsumer()) {
      kafkaConsumer.subscribe(List.of(kafkaTopic));

      final Thread mainThread = Thread.currentThread();
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        log.info("Detected shutdown.");
        kafkaConsumer.wakeup();
        try {
          mainThread.join();
        } catch (InterruptedException e) {
          log.error("Thread interrupted", e);
          Thread.currentThread().interrupt();
        }
      }));

      String index = "wikimedia";
      boolean indexExists = openSearchClient.indices()
          .exists(new GetIndexRequest(index), RequestOptions.DEFAULT);
      if (!indexExists) {
        openSearchClient.indices().create(new CreateIndexRequest(index), RequestOptions.DEFAULT);
        log.info("Index '{}' created", index);
      } else {
        log.info("Use existing index '{}'", index);
      }

      try {
        while (true) {
          // Poll every 2 to increase the likelihood of a larger batch.
          ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(2000));
          if (records.count() > 0) {
            log.info("Received {} records", records.count());
            BulkRequest bulkRequest = new BulkRequest();
            for (ConsumerRecord<String, String> r : records) {
              IndexRequest indexRequest = new IndexRequest(index).source(r.value(),
                  XContentType.JSON).id(r.key());
              bulkRequest.add(indexRequest);
            }
            BulkResponse bulkResponse = openSearchClient.bulk(bulkRequest, RequestOptions.DEFAULT);
            log.info("Inserted {} records into OpenSearch. Response status: {}, item count: {}.",
                records.count(), bulkResponse.status(), bulkResponse.getItems().length);
          }
        }
      } catch (WakeupException e) {
        log.info("Shutting down consumer.");
      } catch (Exception e) {
        log.error("Error detected.", e);
      }
    }
  }

  RestHighLevelClient createOpenSearchClient() {
    RestHighLevelClient restHighLevelClient;
    URI connUri = URI.create(openSearchUrl);
    String userInfo = connUri.getUserInfo();

    RestClientBuilder restClientBuilder = RestClient.builder(
        new HttpHost(connUri.getHost(), connUri.getPort(), connUri.getScheme()));

    if (userInfo == null) {
      restHighLevelClient = new RestHighLevelClient(restClientBuilder);
    } else {
      String[] auth = userInfo.split(":");
      CredentialsProvider cp = new BasicCredentialsProvider();
      cp.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(auth[0], auth[1]));
      restHighLevelClient = new RestHighLevelClient(restClientBuilder.setHttpClientConfigCallback(
          httpAsyncClientBuilder -> httpAsyncClientBuilder.setDefaultCredentialsProvider(cp)
              .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())));
    }
    return restHighLevelClient;
  }

  private KafkaConsumer<String, String> createKafkaConsumer() {
    String groupId = "os1";
    Map<String, Object> properties = Map.of(BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers,
        KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class, VALUE_DESERIALIZER_CLASS_CONFIG,
        StringDeserializer.class, GROUP_ID_CONFIG, groupId, AUTO_OFFSET_RESET_CONFIG,
        OffsetResetStrategy.LATEST.toString(), PARTITION_ASSIGNMENT_STRATEGY_CONFIG,
        List.of(CooperativeStickyAssignor.class));
    return new KafkaConsumer<>(properties);
  }

}
