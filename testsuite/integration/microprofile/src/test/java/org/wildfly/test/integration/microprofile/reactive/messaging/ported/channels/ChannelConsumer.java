/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.ported.channels;

import java.util.List;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;

/**
 * Ported from Quarkus and adjusted
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@ApplicationScoped
public class ChannelConsumer {

    @Inject
    @Channel("source-channel")
    PublisherBuilder<Message<String>> sourceStream;

    public List<String> consume() throws Exception {
        CompletionStage<List<String>> cs = sourceStream
                .map(Message::getPayload)
                .toList()
                .run();
        return cs.toCompletableFuture().get();
    }

    @Outgoing("source-channel")
    public PublisherBuilder<String> source() {
        return ReactiveStreams.of("hello", "with", "SmallRye", "reactive", "message");
    }

}