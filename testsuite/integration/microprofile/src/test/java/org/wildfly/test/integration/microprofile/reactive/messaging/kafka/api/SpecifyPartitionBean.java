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
public class SpecifyPartitionBean {


    private final CountDownLatch latch = new CountDownLatch(40);
    private final Map<Integer, IncomingKafkaRecordMetadata<String, Integer>> noPartitionSpecifiedMetadatas6 = Collections.synchronizedMap(new HashMap<>());
    private final Map<Integer, IncomingKafkaRecordMetadata<String, Integer>> partitionSpecifiedMetadatas6 = Collections.synchronizedMap(new HashMap<>());
    private final Map<Integer, IncomingKafkaRecordMetadata<String, Integer>> noPartitionSpecifiedMetadatas7 = Collections.synchronizedMap(new HashMap<>());
    private final Map<Integer, IncomingKafkaRecordMetadata<String, Integer>> partitionSpecifiedMetadatas7 = Collections.synchronizedMap(new HashMap<>());

    public CountDownLatch getLatch() {
        return latch;
    }

    public Map<Integer, IncomingKafkaRecordMetadata<String, Integer>> getNoPartitionSpecifiedMetadatas6() {
        return noPartitionSpecifiedMetadatas6;
    }

    public Map<Integer, IncomingKafkaRecordMetadata<String, Integer>> getPartitionSpecifiedMetadatas6() {
        return partitionSpecifiedMetadatas6;
    }

    public Map<Integer, IncomingKafkaRecordMetadata<String, Integer>> getNoPartitionSpecifiedMetadatas7() {
        return noPartitionSpecifiedMetadatas7;
    }

    public Map<Integer, IncomingKafkaRecordMetadata<String, Integer>> getPartitionSpecifiedMetadatas7() {
        return partitionSpecifiedMetadatas7;
    }

    @Outgoing("invm6")
    public Publisher<Integer> source6() {
        return ReactiveStreams.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20).buildRs();
    }

    @Incoming("invm6")
    @Outgoing("to-kafka6")
    public Message<Integer> sendToKafka6(Integer i) {
        Message<Integer> msg = Message.of(i);

        OutgoingKafkaRecordMetadata.OutgoingKafkaRecordMetadataBuilder<String> mb = OutgoingKafkaRecordMetadata.<String>builder()
                .withKey("KEY-" + i);
        if (i > 10) {
            mb.withPartition(1);
        }

        msg = KafkaMetadataUtil.writeOutgoingKafkaMetadata(msg, mb.build());
        return msg;
    }

    @Incoming("from-kafka6")
    public CompletionStage<Void> receiveFromKafka6(Message<Integer> msg) {
        IncomingKafkaRecordMetadata<String, Integer> metadata = KafkaMetadataUtil.readIncomingKafkaMetadata(msg).get();

        if (msg.getPayload() <= 10) {
            noPartitionSpecifiedMetadatas6.put(msg.getPayload(), metadata);
        } else {
            partitionSpecifiedMetadatas6.put(msg.getPayload(), metadata);
        }
        latch.countDown();
        return msg.ack();
    }


    @Outgoing("invm7")
    public Publisher<Integer> source7() {
        return ReactiveStreams.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20).buildRs();
    }

    @Incoming("invm7")
    @Outgoing("to-kafka7")
    public Message<Integer> sendToKafka7(Integer i) {
        Message<Integer> msg = Message.of(i);

        OutgoingKafkaRecordMetadata.OutgoingKafkaRecordMetadataBuilder<String> mb = OutgoingKafkaRecordMetadata.<String>builder()
                .withKey("KEY-" + i);
        if (i > 10) {
            mb.withPartition(0);
        }

        msg = KafkaMetadataUtil.writeOutgoingKafkaMetadata(msg, mb.build());
        return msg;
    }

    @Incoming("from-kafka7")
    public CompletionStage<Void> receiveFromKafka7(Message<Integer> msg) {
        IncomingKafkaRecordMetadata<String, Integer> metadata = KafkaMetadataUtil.readIncomingKafkaMetadata(msg).get();

        if (msg.getPayload() <= 10) {
            noPartitionSpecifiedMetadatas7.put(msg.getPayload(), metadata);
        } else {
            partitionSpecifiedMetadatas7.put(msg.getPayload(), metadata);
        }
        latch.countDown();
        return msg.ack();
    }
}
