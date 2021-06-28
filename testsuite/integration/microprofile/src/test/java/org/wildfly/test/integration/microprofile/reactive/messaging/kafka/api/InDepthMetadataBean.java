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

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;

import javax.enterprise.context.ApplicationScoped;

import org.apache.kafka.common.header.internals.RecordHeader;
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
public class InDepthMetadataBean {
    private final CountDownLatch latch = new CountDownLatch(6);
    private final Map<Integer, IncomingKafkaRecordMetadata<String, Integer>> metadatas = Collections.synchronizedMap(new HashMap<>());
    private final Instant timestampEntry5Topic1 = Instant.now().minus(Duration.ofSeconds(10)).truncatedTo(ChronoUnit.SECONDS);

    public CountDownLatch getLatch() {
        return latch;
    }

    public Map<Integer, IncomingKafkaRecordMetadata<String, Integer>> getMetadatas() {
        return metadatas;
    }

    public Instant getTimestampEntry5Topic1() {
        return timestampEntry5Topic1;
    }

    @Outgoing("invm1")
    public Publisher<Integer> source() {
        return ReactiveStreams.of(1, 2, 3, 4, 5, 6).buildRs();
    }

    @Incoming("invm1")
    @Outgoing("to-kafka1")
    public Message<Integer> sendToKafka(Integer i) {
        Message<Integer> msg = Message.of(i);

        if (i <= 5) {
            // For 6 we don't want any metadata. If we want to tweak what is set in the metadata use another entry
            OutgoingKafkaRecordMetadata.OutgoingKafkaRecordMetadataBuilder<String> mb = OutgoingKafkaRecordMetadata.<String>builder()
                    .withKey("KEY-" + i);

            if (i == 5) {
                mb.withHeaders(Collections.singletonList(new RecordHeader("simple", new byte[]{0, 1, 2})));
                mb.withTimestamp(timestampEntry5Topic1);
            }
            msg = KafkaMetadataUtil.writeOutgoingKafkaMetadata(msg, mb.build());
            return msg;
        }
        return msg;
    }

    @Incoming("from-kafka1")
    public CompletionStage<Void> receiveFromKafka(Message<Integer> msg) {
        IncomingKafkaRecordMetadata<String, Integer> metadata = KafkaMetadataUtil.readIncomingKafkaMetadata(msg).get();
        metadatas.put(msg.getPayload(), metadata);
        latch.countDown();
        return msg.ack();
    }
}
