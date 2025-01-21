/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.multiple.deployment.kafka;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class KafkaMessagingBean {

    private final List<String> received = Collections.synchronizedList(new ArrayList<>());

    @Inject
    @Channel("source")
    Emitter<String> emitter;

    @Incoming("sink")
    public void sink(String word) {
        System.out.println("Received " + word);
        received.add(word);
    }

    public void send(String word) {
        System.out.println("Sending " + word);
        emitter.send(word);
    }

    public List<String> getReceived() {
        synchronized (received) {
            return new ArrayList<>(received);
        }
    }
}
