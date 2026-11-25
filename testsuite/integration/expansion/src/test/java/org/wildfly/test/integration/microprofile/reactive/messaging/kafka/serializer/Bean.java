/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.kafka.serializer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;

import io.smallrye.reactive.messaging.kafka.api.KafkaMetadataUtil;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@ApplicationScoped
public class Bean {
    private final CountDownLatch latch = new CountDownLatch(3);
    private List<Person> received = new ArrayList<>();
    private List<Integer> partitionReceived = new ArrayList<>();


    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @PreDestroy
    public void stop() {
        executorService.shutdown();
    }

    public CountDownLatch getLatch() {
        return latch;
    }

    @Outgoing("to-kafka")
    public PublisherBuilder<Person> source() {
        // We need to set the following in microprofile-config.properties for this approach to work
        //  mp.messaging.incoming.from-kafka.auto.offset.reset=earliest
        return ReactiveStreams.of(
                new Person("Kabir", 101),
                new Person("Bob", 18),
                new Person("Roger", 21));
    }

    @Incoming("from-kafka")
    public CompletionStage<Void> sink(Message<Person> msg) {
        received.add(msg.getPayload());
        partitionReceived.add(KafkaMetadataUtil.readIncomingKafkaMetadata(msg).get().getPartition());
        latch.countDown();
        return msg.ack();
    }

    public List<Person> getReceived() {
        return received;
    }

    public List<Integer> getPartitionReceived() {
        return partitionReceived;
    }
}
