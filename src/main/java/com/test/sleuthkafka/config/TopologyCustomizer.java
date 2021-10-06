package com.test.sleuthkafka.config;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.Topology;
import org.springframework.kafka.config.KafkaStreamsInfrastructureCustomizer;
import com.test.sleuthkafka.model.TestEvent;
import com.test.sleuthkafka.service.TestDataEventProcessor;

/**
 * The Class TopologyCustomizer.
 */
public class TopologyCustomizer implements KafkaStreamsInfrastructureCustomizer {

    /** The gdl raw data topic. */
    private final String testDataTopic;

    private final String outTopic;

    /** The key serde. */
    private final Serde<String> keySerde;

    /** The raw data event serde. */
    private final Serde<String> testDataEventSerde;


    public TopologyCustomizer(String testDataTopic, Serde<String> keySerde, Serde<String> testDataEventSerde, String outTopic) {
        this.testDataTopic = testDataTopic;
        this.keySerde = keySerde;
        this.testDataEventSerde = testDataEventSerde;
        this.outTopic = outTopic;

    }

    /**
     * @see org.springframework.kafka.config.KafkaStreamsInfrastructureCustomizer#configureTopology(org.apache.kafka.streams.Topology)
     */
    @Override
    public void configureTopology(Topology topology) {
        topology.addSource("RawDataSource", keySerde.deserializer(), testDataEventSerde.deserializer(), testDataTopic);

        topology.addProcessor("RawDataEventProcessor", () -> new TestDataEventProcessor(), "RawDataSource");

       

        
        topology.addSink("OutputData", outTopic, keySerde.serializer(), testDataEventSerde.serializer(),
                "RawDataEventProcessor");
    }

}
