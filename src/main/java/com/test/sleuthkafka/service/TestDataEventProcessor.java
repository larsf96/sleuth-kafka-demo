package com.test.sleuthkafka.service;

import com.test.sleuthkafka.model.TestEvent;
import com.test.sleuthkafka.model.TestOutEvent;

import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;


public class TestDataEventProcessor implements Processor<String, TestEvent, String, TestOutEvent> {

    private ProcessorContext<String, TestOutEvent> context;

    @Override
    public void init(ProcessorContext<String, TestOutEvent> context) {
        this.context = context;
    }

    @Override
    public void process(Record<String, TestEvent> record) {
        final long start = System.currentTimeMillis();
        try {
            
            final TestEvent testData = record.value();
            TestOutEvent result = new TestOutEvent();
            result.setId(testData.getId());
            result.setVerifiedData(testData.getData());

            final Record<String, TestOutEvent> rec = new Record<>(record.key(), result, System.currentTimeMillis());
            context.forward(rec, "OutputData");
        } catch (Exception e) {
            throw new RuntimeException("Error parsing data!", e);
        }
    }
}
