/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.kafka.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.reactivestreams.Publisher;

import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata;
import io.smallrye.reactive.messaging.kafka.api.KafkaMetadataUtil;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@ApplicationScoped
public class NoTopicSetupOverrideForAllMessagesBean {


    private final CountDownLatch latch = new CountDownLatch(6);
    private final Map<Integer, IncomingKafkaRecordMetadata<String, Integer>> testing4Metadatas = Collections.synchronizedMap(new HashMap<>());
    private final Map<Integer, IncomingKafkaRecordMetadata<String, Integer>> testing5Metadatas = Collections.synchronizedMap(new HashMap<>());



    public CountDownLatch getLatch() {
        return latch;
    }

    public Map<Integer, IncomingKafkaRecordMetadata<String, Integer>> getTesting4Metadatas() {
        return testing4Metadatas;
    }

    public Map<Integer, IncomingKafkaRecordMetadata<String, Integer>> getTesting5Metadatas() {
        return testing5Metadatas;
    }

    @Outgoing("invm4")
    public Publisher<Integer> source1() {
        return ReactiveStreams.of(1, 2, 3, 4, 5, 6).buildRs();
    }

    @Incoming("invm4")
    @Outgoing("to-kafka4or5")
    public Message<Integer> sendToKafka4or5(Integer i) {
        Message<Integer> msg = Message.of(i);

        String topic = "testing" + ((i % 2 == 0) ? "5" : "4");
        System.out.println("-----> Sending to " + topic);
        OutgoingKafkaRecordMetadata.OutgoingKafkaRecordMetadataBuilder<String> mb = OutgoingKafkaRecordMetadata.<String>builder()
                .withKey("KEY-" + i)
                .withTopic(topic);
        msg = KafkaMetadataUtil.writeOutgoingKafkaMetadata(msg, mb.build());
        return msg;
    }

    @Incoming("from-kafka4")
    public CompletionStage<Void> receiveFromKafka4(Message<Integer> msg) {
        IncomingKafkaRecordMetadata<String, Integer> metadata = KafkaMetadataUtil.readIncomingKafkaMetadata(msg).get();
        testing4Metadatas.put(msg.getPayload(), metadata);
        latch.countDown();
        return msg.ack();
    }


    @Incoming("from-kafka5")
    public CompletionStage<Void> receiveFromKafka5(Message<Integer> msg) {
        IncomingKafkaRecordMetadata<String, Integer> metadata = KafkaMetadataUtil.readIncomingKafkaMetadata(msg).get();
        testing5Metadatas.put(msg.getPayload(), metadata);
        latch.countDown();
        return msg.ack();
    }
}
