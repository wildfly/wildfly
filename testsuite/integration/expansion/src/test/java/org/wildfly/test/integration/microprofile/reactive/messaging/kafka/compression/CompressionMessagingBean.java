/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.kafka.compression;

import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata;
import io.smallrye.reactive.messaging.kafka.api.KafkaMetadataUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@ApplicationScoped
public class CompressionMessagingBean {
    private static final Logger LOGGER = Logger.getLogger(CompressionMessagingBean.class.getName());

    private final CountDownLatch latch = new CountDownLatch(4);
    private List<String> words = new ArrayList<>();

    public CountDownLatch getLatch() {
        return latch;
    }

    @Channel("to-kafka-gzip")
    @Inject
    Emitter<String> gzipEmitter;

    @Channel("to-kafka-snappy")
    @Inject
    Emitter<String> snappyEmitter;

    @Channel("to-kafka-lz4")
    @Inject
    Emitter<String> lz4Emitter;

    @Channel("to-kafka-zstd")
    @Inject
    Emitter<String> zstdEmitter;

    @Incoming("from-kafka")
    public CompletionStage<Void> sink(Message<String> message) {
        IncomingKafkaRecordMetadata<String, Integer> metadata = KafkaMetadataUtil.readIncomingKafkaMetadata(message).get();
        words.add(message.getPayload());
        latch.countDown();
        return message.ack();
    }

    public List<String> getWords() {
        return words;
    }

    public void sendGzip(String...words) {
        for (String word : words) {
            send("gzip", gzipEmitter, word);
        }
    }

    public void sendSnappy(String...words) {
        for (String word : words) {
            send("snappy", snappyEmitter, word);
        }
    }

    public void sendLz4(String...words) {
        for (String word : words) {
            send("lz4", lz4Emitter, word);
        }
    }

    public void sendZstd(String...words) {
        for (String word : words) {
            send("zstd", zstdEmitter, word);
        }
    }

    private void send(String compression, Emitter<String> emitter, String word) {
        LOGGER.info(() -> "Submitting " + compression + " Kafka message: " + word);
        try {
            emitter.send(word).whenComplete((ignored, failure) -> {
                if (failure == null) {
                    LOGGER.info(() -> compression + " Kafka send completed: " + word);
                } else {
                    LOGGER.log(Level.SEVERE, compression + " Kafka send failed: " + word, failure);
                }
            });
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, compression + " Kafka send threw before returning a completion stage: " + word, e);
            throw e;
        }
    }
}
