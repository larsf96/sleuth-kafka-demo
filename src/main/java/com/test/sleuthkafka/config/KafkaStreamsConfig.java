package com.test.sleuthkafka.config;

import java.util.Collections;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;
import org.apache.kafka.streams.processor.WallclockTimestampExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;

import brave.Tracing;
import brave.kafka.streams.KafkaStreamsTracing;
import brave.sampler.Sampler;

import com.test.sleuthkafka.model.TestEvent;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;

@Configuration
@EnableKafka
public class KafkaStreamsConfig {

    private String testDataTopic = "test-topic";

    private String outTopic = "out-topic";


    @Value("${spring.kafka.properties.schema.registry.url}")
    private String schemaRegistryUrl;

    @Autowired
    private Environment env;

    @Autowired
    private Tracing tracing;


    @Bean
    public KafkaStreamsConfiguration kafkaStreamsConfigConfiguration() {
        return new KafkaStreamsConfiguration(Map.ofEntries(Map.entry(StreamsConfig.APPLICATION_ID_CONFIG, env.getProperty("spring.application.name")),
                // Map.entry(StreamsConfig.CLIENT_ID_CONFIG, env.getProperty("spring.kafka.streams.client-id")),
                Map.entry(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, WallclockTimestampExtractor.class.getName()),
                Map.entry(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, env.getProperty("spring.kafka.bootstrap-servers")),
                Map.entry(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 3),
                // Map.entry(StreamsConfig.STATE_DIR_CONFIG, "./tmp-states"),
                Map.entry(StreamsConfig.consumerPrefix(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG), 30000),
                Map.entry(StreamsConfig.consumerPrefix(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG), "earliest"),
                Map.entry(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndContinueExceptionHandler.class),
                // replication factor for internal topics currently not needed
                //Map.entry(StreamsConfig.REPLICATION_FACTOR_CONFIG, env.getProperty("spring.kafka.properties.state-stores.replicas", Integer.class)), 
                Map.entry(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 10 * 1024 * 1024L), // 10MB cache
                // Map.entry(StreamsConfig.topicPrefix(TopicConfig.RETENTION_MS_CONFIG), Integer.MAX_VALUE),
                Map.entry(StreamsConfig.producerPrefix(ProducerConfig.ACKS_CONFIG), "all"),
                Map.entry(StreamsConfig.producerPrefix(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG), 2147483647),
                Map.entry(StreamsConfig.producerPrefix(ProducerConfig.MAX_BLOCK_MS_CONFIG), 9223372036854775807L),
                Map.entry(StreamsConfig.producerPrefix(ProducerConfig.MAX_REQUEST_SIZE_CONFIG), 5000000)));
    }

    @Bean
    @Primary
    public StreamsBuilderFactoryBean streamsBuilderFactoryBean(KafkaStreamsConfiguration kafkaStreamsConfigConfiguration) throws Exception {
        final Map<String, String> serdeConfig =
                Collections.singletonMap(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);

        final Serde<String> keySerde = Serdes.String();

        final Serde<TestEvent> testDataEventSerde = new SpecificAvroSerde<>();
        testDataEventSerde.configure(serdeConfig, false);


        final StreamsBuilderFactoryBean streamsBuilderFactoryBean = new StreamsBuilderFactoryBean(kafkaStreamsConfigConfiguration);
        streamsBuilderFactoryBean.setClientSupplier(KafkaStreamsTracing.create(tracing).kafkaClientSupplier());
        streamsBuilderFactoryBean.afterPropertiesSet();
        streamsBuilderFactoryBean.setInfrastructureCustomizer( //
                new TopologyCustomizer( //
                        testDataTopic,
                        keySerde, testDataEventSerde, outTopic));
        streamsBuilderFactoryBean.setCloseTimeout(10); // 10 seconds
        streamsBuilderFactoryBean.setAutoStartup(true);
        return streamsBuilderFactoryBean;
    }

}
