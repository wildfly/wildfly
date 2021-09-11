/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.kafka.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;

import javax.enterprise.context.ApplicationScoped;

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
