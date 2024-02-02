/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.sanity;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@ApplicationScoped
public class Bean {
    private final CountDownLatch latch = new CountDownLatch(4);
    private StringBuilder phrase = new StringBuilder();

    public CountDownLatch getLatch() {
        return latch;
    }

    @Outgoing("source")
    public PublisherBuilder<String> source() {
        return ReactiveStreams.of("hello", "with", "SmallRye", "reactive", "message");
    }

    @Incoming("source")
    @Outgoing("processed-a")
    public String toUpperCase(String payload) {
        return payload.toUpperCase(Locale.ENGLISH);
    }

    @Incoming("processed-a")
    @Outgoing("processed-b")
    public PublisherBuilder<String> filter(PublisherBuilder<String> input) {
        return input.filter(item -> item.length() > 4);
    }

    @Incoming("processed-b")
    public void sink(String word) {
        if (phrase.length() > 0) {
            phrase.append(" ");
        }
        this.phrase.append(word);
        latch.countDown();
    }

    public String getPhrase() {
        return phrase.toString();
    }
}
